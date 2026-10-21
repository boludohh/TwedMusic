package com.twedmusic.app.feature.playback.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.CoverRepository
import com.twedmusic.app.core.database.AppDatabase
import com.twedmusic.app.feature.playback.PlaybackController
import com.twedmusic.app.feature.playback.QueueEntry
import com.twedmusic.app.feature.playback.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isVisible: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val coverFile: File? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<QueueEntry> = emptyList(),
    val currentMediaId: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    database: AppDatabase,
    private val coverRepository: CoverRepository
) : ViewModel() {

    private val songDao = database.songDao()

    private val currentSongInfo = playbackController.currentMediaId
        .mapLatest { mediaId ->
            val id = mediaId?.toLongOrNull() ?: return@mapLatest null
            songDao.getById(id)?.let { entity ->
                Triple(entity.title, entity.artist, entity.path)
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackController.currentMediaId,
        playbackController.isPlaying,
        playbackController.currentPositionMs,
        playbackController.durationMs,
        currentSongInfo,
        playbackController.shuffleEnabled,
        playbackController.repeatMode,
        playbackController.queue
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val mediaId = values[0] as String?
        val isPlaying = values[1] as Boolean
        val positionMs = values[2] as Long
        val durationMs = values[3] as Long
        val songInfo = values[4] as Triple<String, String, String>?
        val shuffle = values[5] as Boolean
        val repeat = values[6] as RepeatMode
        val queue = values[7] as List<QueueEntry>

        val progress = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val path = songInfo?.third
        PlayerUiState(
            isVisible = mediaId != null,
            title = songInfo?.first.orEmpty(),
            artist = songInfo?.second.orEmpty(),
            coverFile = path?.let { coverRepository.coverFile(it) },
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            progress = progress,
            shuffleEnabled = shuffle,
            repeatMode = repeat,
            queue = queue,
            currentMediaId = mediaId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState()
    )

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun skipToNext() = playbackController.skipToNext()

    fun skipToPrevious() = playbackController.skipToPrevious()

    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    fun toggleShuffle() = playbackController.toggleShuffle()

    fun cycleRepeatMode() = playbackController.cycleRepeatMode()

    fun skipToQueueIndex(index: Int) = playbackController.skipToQueueIndex(index)

    fun dismissAndStop() {
        viewModelScope.launch {
            playbackController.stopAndClear()
        }
    }
}