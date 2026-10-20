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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller centralizado de reproducción para toda la app.
 *
 * Gestiona la conexión/desconexión de [MediaController] con
 * [PlaybackService] y expone una API simple para controlar la
 * reproducción desde cualquier pantalla.
 *
 * \## Ventajas sobre MediaController local en cada pantalla
 * - **Una sola conexión**: evita que cada pantalla cree su propio
 *   MediaController (ahorra recursos y simplifica el ciclo de vida).
 * - **Flows reactivos compartidos**: `currentMediaId`, `isPlaying`,
 *   `currentPositionMs` y `durationMs` se actualizan en tiempo real
 *   y son consumidos por todas las pantallas (indicadores de pista
 *   actual, mini player, etc.).
 * - **API simple**: `playSong(song, queueId, queueSongs)` construye
 *   la cola, busca el índice y llama a `setMediaItems` + `play`.
 * - **Thread-safe**: todos los Flows son StateFlow con emisión en Main.
 *
 * \## Conexión automática
 * El controller se conecta a PlaybackService al primer uso (lazy
 * connection). Si la conexión falla, los métodos de control no hacen
 * nada (degradación elegante). Las órdenes de reproducción emitidas
 * mientras la conexión está en curso se encolan y se ejecutan en
 * cuanto el MediaController queda listo (evita el fallo de la primera
 * reproducción tras un arranque en frío).
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coverRepository: CoverRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** MediaController conectado a PlaybackService; null si aún no conectado. */
    private var mediaController: MediaController? = null

    /** true mientras se está construyendo el MediaController de forma asíncrona. */
    private var isConnecting: Boolean = false

    /**
     * Acción de reproducción pendiente de ejecutar en cuanto el
     * MediaController esté listo. Solo se guarda la última orden
     * (la más reciente del usuario).
     */
    private var pendingPlay: (() -> Unit)? = null

    /** Job del polling de posición (\~16 ms) mientras suena. */
    private var positionJob: Job? = null

    // =========================================================================
    // Flows reactivos (consumidos por UI)
    // =========================================================================

    /** mediaId de la pista actualmente en reproducción (null si no hay nada sonando). */
    private val _currentMediaId = MutableStateFlow<String?>(null)
    val currentMediaId: StateFlow<String?> = _currentMediaId.asStateFlow()

    /** true si el reproductor está en estado de reproducción activa. */
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Posición actual de reproducción en milisegundos. */
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    /** Duración total de la pista actual en milisegundos. */
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // =========================================================================
    // Listener del player (actualiza Flows)
    // =========================================================================

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
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
                // Última lectura al pausar
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
    }

    // =========================================================================
    // Polling de posición (\~16 ms)
    // =========================================================================

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

    // =========================================================================
    // Conexión a PlaybackService
    // =========================================================================

    /**
     * Conecta el MediaController a PlaybackService si aún no está
     * conectado. Seguro para llamar múltiples veces (idempotente).
     * Cuando la conexión termina con éxito, ejecuta la acción
     * pendiente de reproducción si existe.
     */
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
                    if (ctrl.isPlaying) startPositionTicker()

                    val action = pendingPlay
                    pendingPlay = null
                    action?.invoke()
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // =========================================================================
    // API pública: control de reproducción
    // =========================================================================

    /**
     * Reproduce [song] en el contexto de [queueId], construyendo la
     * cola completa con [queueSongs] y saltando directamente a la
     * canción objetivo.
     *
     * Si el MediaController aún no está conectado (arranque en frío),
     * la orden se encola y se ejecuta automáticamente en cuanto la
     * conexión termina.
     */
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

                ctrl.setMediaItems(mediaItems, startIndex, 0L)
                ctrl.prepare()
                ctrl.play()
            }
        }

        if (mediaController != null) {
            playAction()
        } else {
            pendingPlay = playAction
            ensureConnected()
        }
    }

    /** Pausa la reproducción actual. */
    fun pause() {
        mediaController?.pause()
    }

    /** Reanuda la reproducción actual. */
    fun resume() {
        mediaController?.play()
    }

    /** Alterna entre pausa y reproducción. */
    fun togglePlayPause() {
        val ctrl = mediaController ?: return
        if (ctrl.isPlaying) pause() else resume()
    }

    /**
     * Salta a una posición específica en la pista actual.
     * @param positionMs Posición en milisegundos desde el inicio.
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    /** Pista siguiente en la cola actual. */
    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    /** Pista anterior en la cola actual. */
    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    /**
     * Detiene la reproducción y vacía la cola, como si se hubiera
     * cerrado la app por completo. El mini player se oculta hasta
     * el próximo [playSong] real.
     */
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
    }

    /** Libera el MediaController. Llamar desde onDestroy de la Application. */
    fun release() {
        stopPositionTicker()
        pendingPlay = null
        isConnecting = false
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
    }

    private companion object {
        const val POSITION_TICK_MS = 16L
    }
}