package com.kodomo.app.ui.kids.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.OfflineDownload
import com.kodomo.app.domain.model.PeerOpinion
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.domain.repository.DownloadRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    contentRepository: ContentRepository,
    downloadRepository: DownloadRepository,
) : ViewModel() {
    private val childId = savedState.childId
    val query = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)

    private val allowed = contentRepository.observeAllowedForChild(childId, ContentType.VIDEO)
        .map { list -> list.filter { it.playableUrl != null } }

    val categories: StateFlow<List<String>> = combine(allowed, query) { items, _ -> items.map { it.category }.distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val videos: StateFlow<List<ContentItem>> = combine(allowed, query, category) { items, q, c ->
        items.filter { (c == null || it.category == c) && (q.isBlank() || it.title.contains(q.trim()) || it.description.contains(q.trim())) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<Set<String>> = downloadRepository.observeDownloads().map { list: List<OfflineDownload> -> list.map { it.contentId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
}

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {
    private val contentId: String = android.net.Uri.decode(savedState.get<String>(Routes.ARG_ID).orEmpty())

    data class State(
        val item: ContentItem? = null,
        val source: String? = null,
        val offline: Boolean = false,
        val downloadProgress: Float? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val item = contentRepository.getItem(contentId)
            val local = downloadRepository.localFileFor(contentId)
            _state.value = State(item = item, source = local ?: item?.playableUrl, offline = local != null)
        }
    }

    fun download() {
        val url = _state.value.item?.mediaUrl?.takeIf { com.kodomo.app.domain.model.VideoSource.parse(it) is com.kodomo.app.domain.model.VideoSource.Direct } ?: return
        if (_state.value.downloadProgress != null || _state.value.offline) return
        viewModelScope.launch {
            downloadRepository.download(contentId, url)
                .catch { e -> _state.value = _state.value.copy(downloadProgress = null, error = "دانلود انجام نشد: ${e.message}") }
                .collect { p -> _state.value = _state.value.copy(downloadProgress = p) }
            if (_state.value.error == null) {
                _state.value = _state.value.copy(downloadProgress = null, offline = true)
            }
        }
    }
}

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val rewardRepository: RewardRepository,
) : ViewModel() {
    private val childId = savedState.childId
    private val contentId: String = android.net.Uri.decode(savedState.get<String>(Routes.ARG_ID).orEmpty())

    val item: StateFlow<ContentItem?> = contentRepository.observeItem(contentId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val opinion: StateFlow<PeerOpinion> = contentRepository.observePeerOpinion(contentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeerOpinion(0, 0, emptyList()))

    fun submit(liked: Boolean, comment: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        contentRepository.submitFeedback(childId, contentId, liked, comment)
        val newMedal = rewardRepository.award(childId, AchievementCatalog.FIRST_FEEDBACK)
        onDone(newMedal)
    }
}
