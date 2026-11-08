package com.kodomo.app.kiosk

import com.kodomo.app.data.remote.LiveUsageSocket
import com.kodomo.app.data.remote.UsageEventDto
import com.kodomo.app.di.ApplicationScope
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.domain.repository.ActivityRepository
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks every second a child spends in the kids space, attributes it to a category, persists it to the activity log,
 * fires the friendly 5-minute warning, and locks the space at the daily limit or bedtime.
 */
@Singleton
class ScreenTimeManager @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val settingsRepository: SettingsRepository,
    private val familyRepository: FamilyRepository,
    private val liveUsageSocket: LiveUsageSocket,
    @ApplicationScope private val scope: CoroutineScope,
    wellbeingStore: com.kodomo.app.data.prefs.WellbeingStore,
) {
    enum class LockReason { NONE, DAILY_LIMIT, BEDTIME, SCHOOL }

    @Volatile private var wellbeing = com.kodomo.app.data.prefs.Wellbeing()

    /** Until this time a parent lifted the bedtime / school / time-window lock (after passing the PIN gate). */
    @Volatile private var lockOverrideUntil = 0L
    private val overridden: Boolean get() = System.currentTimeMillis() < lockOverrideUntil

    init {
        scope.launch { wellbeingStore.state.collect { wellbeing = it } }
    }

    data class State(
        val child: Child? = null,
        val usedSeconds: Long = 0,
        val limitSeconds: Long = 0,
        val lock: LockReason = LockReason.NONE,
        val category: ActivityCategory = ActivityCategory.HOME,
        val title: String = "",
        val running: Boolean = false,
    ) {
        val remainingSeconds: Long get() = (limitSeconds - usedSeconds).coerceAtLeast(0)
        val progress: Float get() = if (limitSeconds == 0L) 0f else (usedSeconds.toFloat() / limitSeconds).coerceIn(0f, 1f)
    }

    sealed interface Event {
        data class FiveMinuteWarning(val child: Child) : Event
        data class TimeUp(val child: Child) : Event
        data class Bedtime(val child: Child) : Event
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val mutex = Mutex()
    private var ticker: Job? = null
    private var settings: ParentalSettings? = null
    private var persistedSeconds = 0L
    private var segmentStart: Instant = Instant.now()
    private var segmentSeconds = 0L
    private var contentId: String? = null
    private var warningIssued = false
    private var bonusSeconds = 0L
    private var bonusDate: LocalDate = LocalDate.now()
    private var sessionDate: LocalDate = LocalDate.now()

    fun startSession(childId: Long) {
        scope.launch {
            mutex.withLock {
                if (_state.value.child?.id == childId && ticker?.isActive == true) return@withLock
                flushLocked()
                val child = familyRepository.getChild(childId) ?: return@withLock
                settings = settingsRepository.getSettings(childId)
                sessionDate = LocalDate.now()
                persistedSeconds = activityRepository.usedSecondsOn(childId, sessionDate)
                if (bonusDate != sessionDate) {
                    bonusSeconds = 0
                    bonusDate = sessionDate
                }
                warningIssued = false
                contentId = null
                segmentStart = Instant.now()
                segmentSeconds = 0
                _state.value = State(
                    child = child,
                    usedSeconds = persistedSeconds,
                    limitSeconds = limitSeconds(),
                    category = ActivityCategory.HOME,
                    title = "خانه",
                    running = true,
                )
                evaluateLocked()
                startTicker()
            }
        }
        scope.launch { liveUsageSocket.ensureConnected() }
    }

    fun endSession() {
        externalAppActive = false
        scope.launch {
            mutex.withLock {
                ticker?.cancel()
                ticker = null
                flushLocked()
                _state.value = State()
            }
        }
    }

    /** True while the child uses a parent-approved external app; time keeps counting under the APP category. */
    @Volatile var externalAppActive: Boolean = false
        private set

    fun enterExternalApp(label: String, packageName: String) {
        externalAppActive = true
        setActivity(ActivityCategory.APP, label, packageName)
    }

    fun pause() {
        if (externalAppActive) return
        scope.launch {
            mutex.withLock {
                ticker?.cancel()
                ticker = null
                flushLocked()
                _state.update { it.copy(running = false) }
            }
        }
    }

    fun resume() {
        if (externalAppActive) {
            externalAppActive = false
            setActivity(ActivityCategory.HOME, "خانه")
        }
        scope.launch {
            mutex.withLock {
                val child = _state.value.child ?: return@withLock
                if (ticker?.isActive == true) return@withLock
                settings = settingsRepository.getSettings(child.id)
                if (LocalDate.now() != sessionDate) {
                    sessionDate = LocalDate.now()
                    warningIssued = false
                }
                persistedSeconds = activityRepository.usedSecondsOn(child.id, sessionDate)
                segmentStart = Instant.now()
                segmentSeconds = 0
                _state.update { it.copy(running = true, usedSeconds = persistedSeconds, limitSeconds = limitSeconds()) }
                evaluateLocked()
                startTicker()
            }
        }
    }

    fun setActivity(category: ActivityCategory, title: String, contentId: String? = null) {
        scope.launch {
            mutex.withLock {
                if (_state.value.category == category && _state.value.title == title) return@withLock
                flushLocked()
                this@ScreenTimeManager.contentId = contentId
                _state.update { it.copy(category = category, title = title) }
            }
        }
    }

    /** Called after a parent passes the gate and grants extra time for today. */
    fun grantExtraMinutes(minutes: Int) {
        scope.launch {
            mutex.withLock {
                bonusSeconds += minutes * 60L
                bonusDate = LocalDate.now()
                lockOverrideUntil = maxOf(lockOverrideUntil, System.currentTimeMillis()) + minutes * 60_000L
                warningIssued = false
                _state.update { it.copy(limitSeconds = limitSeconds()) }
                evaluateLocked()
            }
        }
    }

    /** Re-reads parental settings (after a parent changes them) without restarting the session. */
    fun refreshSettings() {
        scope.launch {
            mutex.withLock {
                val child = _state.value.child ?: return@withLock
                settings = settingsRepository.getSettings(child.id)
                _state.update { it.copy(limitSeconds = limitSeconds()) }
                evaluateLocked()
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            var tick = 0
            while (isActive) {
                delay(1_000)
                mutex.withLock {
                    if (_state.value.lock == LockReason.NONE) {
                        segmentSeconds++
                        _state.update { it.copy(usedSeconds = persistedSeconds + segmentSeconds) }
                    }
                    tick++
                    if (tick % FLUSH_EVERY_SECONDS == 0) flushLocked()
                    if (tick % HEARTBEAT_EVERY_SECONDS == 0) sendHeartbeat()
                    if (LocalDate.now() != sessionDate) {
                        flushLocked()
                        sessionDate = LocalDate.now()
                        persistedSeconds = 0
                        warningIssued = false
                    }
                    evaluateLocked()
                }
            }
        }
    }

    private suspend fun flushLocked() {
        val child = _state.value.child ?: return
        if (segmentSeconds > 0) {
            activityRepository.log(child.id, _state.value.category, _state.value.title, contentId, segmentStart, segmentSeconds)
            persistedSeconds += segmentSeconds
        }
        segmentSeconds = 0
        segmentStart = Instant.now()
    }

    private fun limitSeconds(): Long {
        val weekend = if (wellbeing.isWeekend(java.time.LocalDate.now().dayOfWeek)) wellbeing.weekendExtraMinutes * 60L else 0L
        return (settings?.dailyLimitMinutes ?: 60) * 60L + weekend + bonusSeconds
    }

    private fun evaluateLocked() {
        val current = _state.value
        val child = current.child ?: return
        val s = settings
        val newLock = when {
            !overridden && s != null && s.bedtimeEnabled && isBedtime(LocalTime.now(), s.bedtimeStartMinutes, s.bedtimeEndMinutes) -> LockReason.BEDTIME
            !overridden && wellbeing.inSchoolHours(java.time.LocalDateTime.now()) -> LockReason.SCHOOL
            current.usedSeconds >= current.limitSeconds -> LockReason.DAILY_LIMIT
            else -> LockReason.NONE
        }
        if (newLock != current.lock) {
            _state.update { it.copy(lock = newLock) }
            when (newLock) {
                LockReason.DAILY_LIMIT -> _events.tryEmit(Event.TimeUp(child))
                LockReason.BEDTIME -> _events.tryEmit(Event.Bedtime(child))
                LockReason.SCHOOL, LockReason.NONE -> Unit
            }
        }
        if (newLock == LockReason.NONE && !warningIssued && current.remainingSeconds in 1..WARNING_SECONDS) {
            warningIssued = true
            _events.tryEmit(Event.FiveMinuteWarning(child))
        }
    }

    private fun sendHeartbeat() {
        val s = _state.value
        val child = s.child ?: return
        liveUsageSocket.send(
            UsageEventDto(
                type = "usage", childLocalId = child.id, childName = child.name, category = s.category.name, title = s.title,
                usedSecondsToday = s.usedSeconds, limitSeconds = s.limitSeconds, timestamp = Instant.now().toString(),
            ),
        )
    }

    companion object {
        const val WARNING_SECONDS = 5 * 60L
        private const val FLUSH_EVERY_SECONDS = 60
        private const val HEARTBEAT_EVERY_SECONDS = 30

        /** Bedtime windows may cross midnight (e.g. 21:00 → 07:00). */
        fun isBedtime(now: LocalTime, startMinutes: Int, endMinutes: Int): Boolean {
            val minute = now.hour * 60 + now.minute
            return if (startMinutes <= endMinutes) minute in startMinutes until endMinutes else minute >= startMinutes || minute < endMinutes
        }
    }
}
