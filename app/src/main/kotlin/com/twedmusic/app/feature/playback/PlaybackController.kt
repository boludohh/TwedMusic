package com.twedmusic.app.feature.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.twedmusic.app.core.data.repository.CoverRepository
import com.twedmusic.app.core.model.Song
import com.twedmusic.app.feature.playback.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Modo de repetición de la cola / pista actual. */
enum class RepeatMode {
    /** Reproduce la cola y para en la última pista. */
    OFF,
    /** Reproduce la cola en bucle (vuelve a la primera). */
    ALL,
    /** Repite solo la pista actual. */
    ONE
}

/** Entrada de la cola de reproducción actual. */
data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String
)

/**
 * Controller centralizado de reproducción para toda la app.
 *
 * Gestiona MediaController ↔ PlaybackService y expone Flows + API
 * de control (play, seek, shuffle, repeat, cola).
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coverRepository: CoverRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaController: MediaController? = null
    private var isConnecting: Boolean = false
    private var pendingPlay: (() -> Unit)? = null
    private var positionJob: Job? = null

    // ── Flows de UI ──────────────────────────────────────────────────────────

    private val _currentMediaId = MutableStateFlow<String?>(null)
    val currentMediaId: StateFlow<String?> = _currentMediaId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    // ── Listener ─────────────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMediaId.value = mediaItem?.mediaId
            _currentPositionMs.value = 0L
            mediaItem?.let { item ->
                val d = mediaController?.duration ?: 0L
                _durationMs.value = if (d > 0) {
                    d
                } else {
                    item.mediaMetadata.extras?.getLong("duration", 0L) ?: 0L
                }
            }
            mediaController?.let { refreshQueue(it) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
                mediaController?.let { ctrl ->
                    _currentPositionMs.value = ctrl.currentPosition.coerceAtLeast(0L)
                    if (ctrl.duration > 0) _durationMs.value = ctrl.duration
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    _currentMediaId.value = null
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    _durationMs.value = 0L
                    _queue.value = emptyList()
                    stopPositionTicker()
                }
                Player.STATE_READY, Player.STATE_BUFFERING -> {
                    mediaController?.let { ctrl ->
                        if (ctrl.duration > 0) _durationMs.value = ctrl.duration
                        _currentPositionMs.value = ctrl.currentPosition.coerceAtLeast(0L)
                    }
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = media3ToRepeatMode(repeatMode)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }
    }

    // ── Posición ─────────────────────────────────────────────────────────────

    private fun startPositionTicker() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                mediaController?.let { ctrl ->
                    _currentPositionMs.value = ctrl.currentPosition.coerceAtLeast(0L)
                    if (ctrl.duration > 0) _durationMs.value = ctrl.duration
                }
                delay(POSITION_TICK_MS)
            }
        }
    }

    private fun stopPositionTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    // ── Conexión ─────────────────────────────────────────────────────────────

    private fun ensureConnected() {
        if (mediaController != null || isConnecting) return

        isConnecting = true
        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            isConnecting = false
            if (future.isDone && !future.isCancelled) {
                mediaController = runCatching { future.get() }.getOrNull()
                mediaController?.let { ctrl ->
                    ctrl.addListener(playerListener)
                    _currentMediaId.value = ctrl.currentMediaItem?.mediaId
                    _isPlaying.value = ctrl.isPlaying
                    _currentPositionMs.value = ctrl.currentPosition
                    _durationMs.value = ctrl.duration
                    _repeatMode.value = media3ToRepeatMode(ctrl.repeatMode)
                    _shuffleEnabled.value = ctrl.shuffleModeEnabled
                    refreshQueue(ctrl)
                    if (ctrl.isPlaying) startPositionTicker()

                    val action = pendingPlay
                    pendingPlay = null
                    action?.invoke()
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // ── Cola ─────────────────────────────────────────────────────────────────

    private fun refreshQueue(ctrl: MediaController) {
        val count = ctrl.mediaItemCount
        if (count <= 0) {
            _queue.value = emptyList()
            return
        }
        val entries = ArrayList<QueueEntry>(count)
        for (i in 0 until count) {
            val item = ctrl.getMediaItemAt(i)
            val meta = item.mediaMetadata
            entries.add(
                QueueEntry(
                    mediaId = item.mediaId,
                    title = meta.title?.toString().orEmpty(),
                    artist = meta.artist?.toString().orEmpty()
                )
            )
        }
        _queue.value = entries
    }

    // ── API pública ──────────────────────────────────────────────────────────

    fun playSong(song: Song, queueId: String, queueSongs: List<Song>) {
        val playAction: () -> Unit = playAction@{
            val ctrl = mediaController ?: return@playAction

            scope.launch {
                val startIndex = queueSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

                val mediaItems = withContext(Dispatchers.IO) {
                    queueSongs.mapIndexed { i, s ->
                        val item = s.toMediaItem(coverRepository)
                        if (i == 0) {
                            MediaItem.Builder()
                                .setMediaId(item.mediaId)
                                .setUri(item.localConfiguration?.uri)
                                .setMediaMetadata(item.mediaMetadata)
                                .setTag(queueId)
                                .build()
                        } else {
                            item
                        }
                    }
                }

                // Conservar modos actuales al cargar nueva cola
                val keepRepeat = ctrl.repeatMode
                val keepShuffle = ctrl.shuffleModeEnabled

                ctrl.setMediaItems(mediaItems, startIndex, 0L)
                ctrl.repeatMode = keepRepeat
                ctrl.shuffleModeEnabled = keepShuffle
                ctrl.prepare()
                ctrl.play()
                refreshQueue(ctrl)
            }
        }

        if (mediaController != null) {
            playAction()
        } else {
            pendingPlay = playAction
            ensureConnected()
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun togglePlayPause() {
        val ctrl = mediaController ?: return
        if (ctrl.isPlaying) pause() else resume()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    /** Salta a la pista en [index] de la cola actual (0-based). */
    fun skipToQueueIndex(index: Int) {
        val ctrl = mediaController ?: return
        if (index in 0 until ctrl.mediaItemCount) {
            ctrl.seekTo(index, 0L)
        }
    }

    /**
     * Ciclo: OFF → ALL → ONE → OFF.
     *
     * - OFF: la cola se reproduce y para al final.
     * - ALL: la cola se repite en bucle.
     * - ONE: solo la pista actual se repite.
     */
    fun cycleRepeatMode() {
        val ctrl = mediaController ?: return
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        ctrl.repeatMode = repeatModeToMedia3(next)
        _repeatMode.value = next
    }

    /** Activa / desactiva el modo aleatorio de la cola. */
    fun toggleShuffle() {
        val ctrl = mediaController ?: return
        val next = !_shuffleEnabled.value
        ctrl.shuffleModeEnabled = next
        _shuffleEnabled.value = next
    }

    fun stopAndClear() {
        stopPositionTicker()
        mediaController?.let { ctrl ->
            ctrl.stop()
            ctrl.clearMediaItems()
        }
        _currentMediaId.value = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        _queue.value = emptyList()
        // No reseteamos shuffle/repeat: se conservan entre sesiones de cola
    }

    fun release() {
        stopPositionTicker()
        pendingPlay = null
        isConnecting = false
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
    }

    private fun media3ToRepeatMode(mode: Int): RepeatMode = when (mode) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

    private fun repeatModeToMedia3(mode: RepeatMode): Int = when (mode) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

    private companion object {
        /**
         * Intervalo del ticker de posición que alimenta la UI (barra de
         * progreso del mini player y de Now Playing).
         *
         * 200 ms (5 actualizaciones/segundo) es más que suficiente para
         * que una barra de progreso se vea fluida; no hace falta más
         * que eso. Antes estaba en 16 ms (~60/segundo): cada tick
         * recombina el [uiState] de [PlayerViewModel], así que un
         * intervalo tan corto multiplicaba por ~12 el trabajo
         * innecesario mientras suena música, sin ninguna mejora visual.
         */
        const val POSITION_TICK_MS = 200L
    }
}