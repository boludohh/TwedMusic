package com.twedmusic.app.feature.playback.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.twedmusic.app.R
import com.twedmusic.app.core.designsystem.theme.LocalCardL2Color
import com.twedmusic.app.core.designsystem.theme.LocalCoverPlaceholderIconColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemMetaColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor

/** Arrastre hacia abajo para cerrar Now Playing. */
private val CloseDragThreshold = 100.dp

/**
 * Pantalla completa de reproducción.
 *
 * Fondo = [MaterialTheme.colorScheme.background] (#000000 en oscuro).
 * Play/pause usan iconos *circle* ([R.drawable.ic_play_circle_filled] /
 * [R.drawable.ic_pause_circle_filled]), distintos del mini player.
 *
 * Cierre: [onClose] vía sistema back o swipe hacia abajo.
 * Shuffle / repeat / cola / favorito se muestran; la lógica real puede
 * llegar después sin rediseñar el layout.
 */
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val titleColor = LocalListItemTitleColor.current
    val subtitleColor = LocalListItemSubtitleColor.current
    val metaColor = LocalListItemMetaColor.current
    val placeholderBg = LocalCardL2Color.current
    val placeholderIconColor = LocalCoverPlaceholderIconColor.current
    val density = LocalDensity.current
    val closeThresholdPx = with(density) { CloseDragThreshold.toPx() }
    val coverPx = with(density) { 280.dp.roundToPx() }

    var dragAccum by remember { mutableFloatStateOf(0f) }
    // Seek local mientras el usuario arrastra el slider
    var scrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    val displayProgress = if (scrubbing) {
        scrubProgress
    } else {
        state.progress
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) dragAccum += dragAmount
                    },
                    onDragEnd = {
                        if (dragAccum >= closeThresholdPx) onClose()
                        dragAccum = 0f
                    },
                    onDragCancel = { dragAccum = 0f }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicador de “bajar”
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(metaColor.copy(alpha = 0.45f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Carátula grande
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(placeholderBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note_filled),
                    contentDescription = null,
                    tint = placeholderIconColor,
                    modifier = Modifier.size(96.dp)
                )
                val cover = state.coverFile
                if (cover != null) {
                    val request = ImageRequest.Builder(LocalContext.current)
                        .data(cover)
                        .size(CoilSize(coverPx, coverPx))
                        .build()
                    AsyncImage(
                        model = request,
                        contentDescription = state.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineSmall,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Seek bar (interactivo)
            Slider(
                value = displayProgress.coerceIn(0f, 1f),
                onValueChange = {
                    scrubbing = true
                    scrubProgress = it
                },
                onValueChangeFinished = {
                    val duration = state.durationMs
                    if (duration > 0L) {
                        onSeek((scrubProgress * duration).toLong().coerceIn(0L, duration))
                    }
                    scrubbing = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = titleColor,
                    activeTrackColor = titleColor,
                    inactiveTrackColor = metaColor.copy(alpha = 0.35f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlayerTime(
                        if (scrubbing && state.durationMs > 0L) {
                            (scrubProgress * state.durationMs).toLong()
                        } else {
                            state.positionMs
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = metaColor
                )
                Text(
                    text = formatPlayerTime(state.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = metaColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Controles principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NowPlayingIconButton(
                    iconRes = R.drawable.ic_shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    size = 28.dp,
                    onClick = { /* visual por ahora */ }
                )
                NowPlayingIconButton(
                    iconRes = R.drawable.ic_skip_previous_filled,
                    contentDescription = stringResource(R.string.cd_skip_previous),
                    size = 36.dp,
                    onClick = onSkipPrevious
                )
                NowPlayingIconButton(
                    iconRes = if (state.isPlaying) {
                        R.drawable.ic_pause_circle_filled
                    } else {
                        R.drawable.ic_play_circle_filled
                    },
                    contentDescription = stringResource(
                        if (state.isPlaying) R.string.cd_pause else R.string.cd_play
                    ),
                    size = 72.dp,
                    onClick = onPlayPause
                )
                NowPlayingIconButton(
                    iconRes = R.drawable.ic_skip_next_filled,
                    contentDescription = stringResource(R.string.cd_skip_next),
                    size = 36.dp,
                    onClick = onSkipNext
                )
                NowPlayingIconButton(
                    iconRes = R.drawable.ic_repeat,
                    contentDescription = stringResource(R.string.cd_repeat),
                    size = 28.dp,
                    onClick = { /* visual por ahora */ }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Fila inferior: cola / favorito (layout listo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NowPlayingIconButton(
                    iconRes = R.drawable.ic_queue_music,
                    contentDescription = stringResource(R.string.cd_queue),
                    size = 26.dp,
                    onClick = { /* visual por ahora */ }
                )
                NowPlayingIconButton(
                    iconRes = R.drawable.ic_favorite_outline,
                    contentDescription = stringResource(R.string.cd_favorite),
                    size = 26.dp,
                    onClick = { /* visual por ahora */ }
                )
            }
        }
    }
}

@Composable
private fun NowPlayingIconButton(
    iconRes: Int,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val tint = LocalListItemTitleColor.current
    Box(
        modifier = Modifier
            .size(size + 16.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}