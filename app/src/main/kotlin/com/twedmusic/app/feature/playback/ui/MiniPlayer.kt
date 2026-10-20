package com.twedmusic.app.feature.playback.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.twedmusic.app.R
import com.twedmusic.app.core.designsystem.theme.LocalCoverPlaceholderIconColor
import com.twedmusic.app.core.designsystem.theme.LocalCurrentTrackBorderColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor

/** Altura del mini player (alineada a TrackRow). */
private val MiniPlayerHeight = 64.dp

/** Radio superior, igual que la bottom nav (25 dp). */
private val MiniPlayerTopCorner = 25.dp

/** Grosor de la barra de progreso (solo lectura, sin fondo). */
private val ProgressStroke = 2.dp

/** Umbral de arrastre hacia abajo para cortar el stream. */
private val DismissDragThreshold = 80.dp

/**
 * Mini player global: extensión visual de la barra de navegación.
 *
 * - 64 dp de alto, esquinas superiores 25 dp, mismo `surfaceVariant` que la nav.
 * - Carátula y tipografía alineadas a [com.twedmusic.app.core.designsystem.components.TrackRow].
 * - Barra de progreso de borde a borde, sin track de fondo, no seekable.
 * - Controles: anterior, play/pause, siguiente.
 * - Deslizar hacia abajo por encima del umbral llama a [onDismiss] (corta el stream).
 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = LocalListItemTitleColor.current
    val subtitleColor = LocalListItemSubtitleColor.current
    val progressColor = LocalCurrentTrackBorderColor.current
    val placeholderBg = MaterialTheme.colorScheme.surfaceVariant
    val placeholderIconColor = LocalCoverPlaceholderIconColor.current
    val density = LocalDensity.current
    val thumbnailPx = with(density) { 52.dp.roundToPx() }
    val dismissThresholdPx = with(density) { DismissDragThreshold.toPx() }

    var dragAccum by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        // Solo acumular arrastre hacia abajo
                        if (dragAmount > 0f) {
                            dragAccum += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (dragAccum >= dismissThresholdPx) {
                            onDismiss()
                        }
                        dragAccum = 0f
                    },
                    onDragCancel = { dragAccum = 0f }
                )
            },
        shape = RoundedCornerShape(topStart = MiniPlayerTopCorner, topEnd = MiniPlayerTopCorner),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Barra de progreso: sin fondo, solo el trazo de avance
                .drawWithContent {
                    drawContent()
                    val strokePx = ProgressStroke.toPx()
                    val width = size.width * state.progress.coerceIn(0f, 1f)
                    if (width > 0f) {
                        drawRect(
                            color = progressColor,
                            topLeft = Offset(0f, size.height - strokePx),
                            size = Size(width, strokePx)
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Carátula 52 dp, start 24 dp (igual que TrackRow)
                Box(
                    modifier = Modifier
                        .padding(start = 24.dp, top = 6.dp, bottom = 6.dp)
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(placeholderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_music_note_filled),
                        contentDescription = null,
                        tint = placeholderIconColor,
                        modifier = Modifier.size(26.dp)
                    )
                    val cover = state.coverFile
                    if (cover != null) {
                        val request = ImageRequest.Builder(LocalContext.current)
                            .data(cover)
                            .size(CoilSize(thumbnailPx, thumbnailPx))
                            .build()
                        AsyncImage(
                            model = request,
                            contentDescription = state.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                        )
                    }
                }

                // Título + artista (mismas métricas que TrackRow)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        ),
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Controles
                MiniPlayerIconButton(
                    iconRes = R.drawable.ic_skip_previous_filled,
                    contentDescription = stringResource(R.string.cd_skip_previous),
                    onClick = onSkipPrevious
                )
                MiniPlayerIconButton(
                    iconRes = if (state.isPlaying) {
                        R.drawable.ic_pause_filled
                    } else {
                        R.drawable.ic_play_arrow_filled
                    },
                    contentDescription = stringResource(
                        if (state.isPlaying) R.string.cd_pause else R.string.cd_play
                    ),
                    onClick = onPlayPause
                )
                MiniPlayerIconButton(
                    iconRes = R.drawable.ic_skip_next_filled,
                    contentDescription = stringResource(R.string.cd_skip_next),
                    onClick = onSkipNext
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val tint = LocalListItemTitleColor.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}