package com.twedmusic.app.feature.playback.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.CoverRepository
import com.twedmusic.app.core.database.AppDatabase
import com.twedmusic.app.feature.playback.PlaybackController
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

/**
 * Estado de UI del reproductor (mini player y, más adelante, Now Playing).
 *
 * No sustituye a [PlaybackController]: solo combina sus flows con
 * metadata de Room y la ruta de portada para la capa visual.
 *
 * Room se accede vía [AppDatabase] (proveído por Hilt en DatabaseModule);
 * no se inyecta SongDao directamente porque el módulo solo registra la DB.
 */
data class PlayerUiState(
    /** true cuando hay una sesión activa (mediaId no nulo). */
    val isVisible: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val coverFile: File? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** Progreso 0f..1f para la barra de solo lectura. */
    val progress: Float = 0f
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    database: AppDatabase,
    private val coverRepository: CoverRepository
) : ViewModel() {

    private val songDao = database.songDao()

    /**
     * Metadata de la pista actual (título, artista, path) resuelta
     * desde Room a partir de [PlaybackController.currentMediaId].
     * [mapLatest] permite llamar a [getById] (suspend) de forma segura.
     */
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
        currentSongInfo
    ) { mediaId, isPlaying, positionMs, durationMs, songInfo ->
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
            progress = progress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState()
    )

    fun togglePlayPause() {
        playbackController.togglePlayPause()
    }

    fun skipToNext() {
        playbackController.skipToNext()
    }

    fun skipToPrevious() {
        playbackController.skipToPrevious()
    }

    /**
     * Corta el stream por completo (equivalente a cerrar la sesión).
     * El mini player se oculta hasta el próximo play real.
     */
    fun dismissAndStop() {
        viewModelScope.launch {
            playbackController.stopAndClear()
        }
    }
}