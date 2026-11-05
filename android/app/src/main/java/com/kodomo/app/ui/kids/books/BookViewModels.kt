package com.kodomo.app.ui.kids.books

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.repository.ActivityRepository
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BookListViewModel @Inject constructor(savedState: SavedStateHandle, contentRepository: ContentRepository) : ViewModel() {
    val books: StateFlow<List<ContentItem>> = contentRepository.observeAllowedForChild(savedState.childId, ContentType.BOOK)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Real printed books reviewed by kodoumo.ir for this age – shown as «books to ask your parents for». */
    val printed: StateFlow<List<ContentItem>> = contentRepository.observeAllowedForChild(savedState.childId, ContentType.BOOK_REVIEW)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * Book reader state. Also owns the «برای من بخوان» parent-voice recordings: one AAC file per page stored in
 * app-private storage, recorded with [MediaRecorder] and played back with [MediaPlayer].
 */
@HiltViewModel
class BookReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val contentRepository: ContentRepository,
    private val familyRepository: FamilyRepository,
    private val rewardRepository: RewardRepository,
    private val activityRepository: ActivityRepository,
) : ViewModel() {
    private val childId = savedState.childId
    private val contentId: String = android.net.Uri.decode(savedState.get<String>(Routes.ARG_ID).orEmpty())

    data class State(
        val book: ContentItem? = null,
        val pages: List<ContentPage> = emptyList(),
        val recordedPages: Set<Int> = emptySet(),
        val recordingPage: Int? = null,
        val playingPage: Int? = null,
        val finished: Boolean = false,
        val medal: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private val voiceDir = File(context.filesDir, "parent_voice").apply { mkdirs() }

    init {
        viewModelScope.launch {
            val book = contentRepository.getItem(contentId)
            val pages = contentRepository.getPages(contentId)
            _state.value = State(book = book, pages = pages, recordedPages = pages.map { it.index }.filter { voiceFile(it).exists() }.toSet())
        }
    }

    private fun voiceFile(page: Int) = File(voiceDir, "${contentId}_$page.m4a")

    fun startRecording(page: Int) {
        stopPlayback()
        stopRecording()
        val file = voiceFile(page)
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(file.absolutePath)
        runCatching {
            r.prepare()
            r.start()
            recorder = r
            _state.value = _state.value.copy(recordingPage = page)
        }.onFailure { r.release() }
    }

    fun stopRecording() {
        val r = recorder ?: return
        val page = _state.value.recordingPage
        runCatching { r.stop() }
        r.release()
        recorder = null
        _state.value = _state.value.copy(
            recordingPage = null,
            recordedPages = if (page != null && voiceFile(page).exists()) _state.value.recordedPages + page else _state.value.recordedPages,
        )
    }

    fun play(page: Int, onComplete: () -> Unit) {
        stopPlayback()
        val file = voiceFile(page)
        if (!file.exists()) return
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                _state.value = _state.value.copy(playingPage = null)
                onComplete()
            }
            prepare()
            start()
        }
        _state.value = _state.value.copy(playingPage = page)
    }

    fun stopPlayback() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        _state.value = _state.value.copy(playingPage = null)
    }

    fun onFinished() {
        if (_state.value.finished) return
        _state.value = _state.value.copy(finished = true)
        viewModelScope.launch {
            familyRepository.addStars(childId, 3)
            val books = activityRepository.countCompleted(childId, ActivityCategory.BOOK, minSeconds = 30)
            val medal = books >= 3 && rewardRepository.award(childId, AchievementCatalog.BOOKWORM)
            _state.value = _state.value.copy(medal = medal)
        }
    }

    override fun onCleared() {
        stopRecording()
        stopPlayback()
    }
}
