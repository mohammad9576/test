package com.kodomo.app.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.data.prefs.SessionState
import com.kodomo.app.data.remote.LiveUsageSocket
import com.kodomo.app.domain.model.ActivityLog
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.DailyUsage
import com.kodomo.app.domain.model.Family
import com.kodomo.app.domain.model.ForumTopic
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.domain.model.RewardRedemption
import com.kodomo.app.domain.repository.ActivityRepository
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.ForumRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.domain.repository.SettingsRepository
import com.kodomo.app.kiosk.DeviceOwnerManager
import com.kodomo.app.kiosk.ScreenTimeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentHubViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val settingsRepository: SettingsRepository,
    private val activityRepository: ActivityRepository,
    private val rewardRepository: RewardRepository,
    contentRepository: ContentRepository,
    private val forumRepository: ForumRepository,
    private val session: SessionPreferences,
    val deviceOwner: DeviceOwnerManager,
    private val screenTime: ScreenTimeManager,
    liveUsageSocket: LiveUsageSocket,
    private val kodoumo: com.kodomo.app.data.kodoumo.KodoumoImporter,
    private val chores: com.kodomo.app.data.prefs.ChoreStore,
    val learning: com.kodomo.app.data.prefs.LearningStore,
    private val wellbeingStore: com.kodomo.app.data.prefs.WellbeingStore,
) : ViewModel() {
    fun setKodoumoAutoShow(on: Boolean) = viewModelScope.launch {
        wellbeingStore.save(wellbeingStore.current().copy(kodoumoAutoShow = on, kodoumoChoiceAsked = true))
    }
    private val contentRepo = contentRepository
    val wellbeing = wellbeingStore.state.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), com.kodomo.app.data.prefs.Wellbeing())
    fun allowedVideos(childId: Long) = contentRepo.observeAllowedForChild(childId, com.kodomo.app.domain.model.ContentType.VIDEO)
    val studyReward = learning.reward.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), com.kodomo.app.data.prefs.StudyReward())
    val studyLog = learning.log.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setStudyReward(r: com.kodomo.app.data.prefs.StudyReward) = viewModelScope.launch { learning.setReward(r) }
    fun setGrade(childId: Long, grade: com.kodomo.app.domain.learn.Grade?) = viewModelScope.launch { learning.setGrade(childId, grade) }
    fun approveStudyReward(childId: Long) = viewModelScope.launch {
        val minutes = learning.approvePending(childId)
        if (minutes > 0) screenTime.grantExtraMinutes(minutes)
    }
    /** Chores the children marked as done, waiting for the parent. */
    val pendingChores = chores.pending.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    fun resolveChore(item: com.kodomo.app.data.prefs.PendingChore, approved: Boolean) = viewModelScope.launch {
        chores.resolve(item, approved)
        if (approved) {
            familyRepository.addStars(item.childId, item.chore.stars)
            chores.addMinutes(item.childId, com.kodomo.app.data.prefs.ChoreStore.GOOD_DEED_MINUTES)
        }
    }
    val badCatalog = com.kodomo.app.data.prefs.ChoreStore.badCatalog
    fun badToday(childId: Long) = chores.badToday(childId)
    fun recordBad(childId: Long, deed: com.kodomo.app.data.prefs.BadDeed) = viewModelScope.launch { chores.recordBad(childId, deed) }
    fun undoBad(childId: Long, e: com.kodomo.app.data.prefs.BadDeedEntry) = viewModelScope.launch { chores.undoBad(childId, e) }
    private fun <T> stateOf(flow: kotlinx.coroutines.flow.Flow<T>, initial: T) = flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val family: StateFlow<Family?> = stateOf(familyRepository.observeFamily(), null)
    val children: StateFlow<List<Child>> = stateOf(familyRepository.observeChildren(), emptyList())
    val sessionState: StateFlow<SessionState?> = stateOf(session.state, null)
    val liveStatus = liveUsageSocket.status

    val selectedChildId = MutableStateFlow<Long?>(null)

    private val effectiveChildId = combine(selectedChildId, children) { selected, list ->
        selected?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
    }

    val selectedChild: StateFlow<Child?> = stateOf(
        combine(effectiveChildId, children) { id, list -> list.firstOrNull { it.id == id } }, null,
    )

    val settings: StateFlow<ParentalSettings?> = stateOf(
        effectiveChildId.flatMapLatest { id -> if (id == null) flowOf(null) else settingsRepository.observeSettings(id) }, null,
    )

    val usedTodaySeconds: StateFlow<Long> = stateOf(
        effectiveChildId.flatMapLatest { id -> if (id == null) flowOf(0L) else activityRepository.observeUsedSecondsToday(id) }, 0L,
    )

    val weekUsage: StateFlow<List<DailyUsage>> = stateOf(
        effectiveChildId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else activityRepository.observeDailyUsage(id, 7) }, emptyList(),
    )

    val timeline: StateFlow<List<ActivityLog>> = stateOf(
        effectiveChildId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else activityRepository.observeTimeline(id, 60) }, emptyList(),
    )

    val redemptions: StateFlow<List<RewardRedemption>> = stateOf(rewardRepository.observeRedemptions(), emptyList())

    val ratedContent: StateFlow<List<ContentItem>> = stateOf(
        combine(
            listOf(
                ContentType.VIDEO, ContentType.BOOK, ContentType.MUSIC, ContentType.LEARNING,
                ContentType.BOOK_REVIEW, ContentType.GAME_REVIEW, ContentType.TOY_REVIEW,
            ).map { contentRepository.observeByType(it) },
        ) { lists -> lists.toList().flatMap { it }.sortedByDescending { it.sourceUrl != null } },
        emptyList(),
    )

    val kodoumoStatus = kodoumo.status
    val kodoumoCount: StateFlow<Int> = stateOf(kodoumo.importedCount, 0)

    fun syncKodoumo() = viewModelScope.launch { kodoumo.sync() }
    /** Opening the ratings tab keeps the catalogue fresh by itself (at most every 30 minutes). */
    fun autoSyncKodoumo() = viewModelScope.launch { kodoumo.syncIfStale(30 * 60_000L) }

    val articles: StateFlow<List<ContentItem>> = stateOf(contentRepository.observeByType(ContentType.ARTICLE), emptyList())
    val weekendIdeas: StateFlow<List<ContentItem>> = stateOf(contentRepository.observeByType(ContentType.WEEKEND_IDEA), emptyList())
    val topics: StateFlow<List<ForumTopic>> = stateOf(forumRepository.observeTopics(), emptyList())

    fun selectChild(id: Long) {
        selectedChildId.value = id
    }

    fun updateSettings(transform: (ParentalSettings) -> ParentalSettings) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.saveSettings(transform(current))
            screenTime.refreshSettings()
        }
    }

    fun markFulfilled(id: Long) = viewModelScope.launch { rewardRepository.markFulfilled(id) }

    fun setKioskEnabled(enabled: Boolean, applyPolicies: () -> Boolean) = viewModelScope.launch {
        session.setKioskEnabled(enabled)
        if (enabled) applyPolicies() else deviceOwner.clearKioskPolicies()
    }

    fun releaseDeviceOwner() = viewModelScope.launch {
        session.setKioskEnabled(false)
        deviceOwner.releaseDeviceOwnership()
    }

    fun createTopic(title: String, body: String, category: String) = viewModelScope.launch {
        val name = family.value?.let { f -> f.motherName.ifBlank { f.fatherName }.ifBlank { "والد" } } ?: "والد"
        forumRepository.createTopic(title, body, category, "والد ($name)")
    }

    fun refreshForum() = viewModelScope.launch { forumRepository.refreshFromServer() }

    fun changePin(old: String, new: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(familyRepository.changePin(old, new))
    }

    fun updateFamily(familyName: String, father: String, mother: String) = viewModelScope.launch {
        familyRepository.updateFamily(familyName, father, mother)
    }

    fun deleteChild(id: Long) = viewModelScope.launch { familyRepository.deleteChild(id) }
}
