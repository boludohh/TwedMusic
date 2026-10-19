package com.twedmusic.app.feature.tabs.tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.AudioRepository
import com.twedmusic.app.core.data.repository.CoverRepository
import com.twedmusic.app.core.model.Song
import com.twedmusic.app.feature.playback.PlaybackController
import com.twedmusic.app.feature.playback.engine.BassPlayerAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel de la pestaña Pistas.
 *
 * - [songs]: canciones ordenadas por fecha de agregada descendente
 *   (más recientes primero). El orden se calcula en un hilo de fondo.
 * - [currentMediaId]: id de la pista que suena (para el indicador).
 * - [onSongClick]: reproduce la pista con toda la lista como cola.
 */
@HiltViewModel
class TracksViewModel @Inject constructor(
    audioRepository: AudioRepository,
    private val playbackController: PlaybackController,
    private val coverRepository: CoverRepository
) : ViewModel() {

    val songs: StateFlow<List<Song>> = audioRepository.songs
        .map { list -> list.sortedByDescending { it.dateAdded } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val currentMediaId: StateFlow<String?> = playbackController.currentMediaId

    /** Archivo de portada en disco (consulta con caché); `null` si no hay. */
    fun coverFile(path: String): File? = coverRepository.coverFile(path)

    fun onSongClick(song: Song) {
        playbackController.playSong(
            song = song,
            queueId = BassPlayerAdapter.QUEUE_TRACKS_BY_DATE,
            queueSongs = songs.value
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
