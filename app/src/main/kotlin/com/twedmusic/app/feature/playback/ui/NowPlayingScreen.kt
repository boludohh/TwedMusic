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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.twedmusic.app.R
import com.twedmusic.app.core.designsystem.theme.LocalCardL2Color
import com.twedmusic.app.core.designsystem.theme.LocalCoverPlaceholderIconColor
import com.twedmusic.app.core.designsystem.theme.LocalCurrentTrackBorderColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemMetaColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor
import com.twedmusic.app.feature.playback.RepeatMode

private val CloseDragThreshold = 100.dp
private val CoverMini = 52.dp
private val CoverFull = 280.dp

/**
 * Contenido de Now Playing **dentro** del panel que se estira.
 *
 * Shuffle / repeat (3 estados) / cola conectados al [PlayerUiState].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSkipToQueueIndex: (Int) -> Unit,
    expandProgress: Float = 1f,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = expandProgress > 0.85f, onBack = onClose)

    val titleColor = LocalListItemTitleColor.current
    val subtitleColor = LocalListItemSubtitleColor.current
    val metaColor = LocalListItemMetaColor.current
    val accentColor = LocalCurrentTrackBorderColor.current
    val placeholderBg = LocalCardL2Color.current
    val placeholderIconColor = LocalCoverPlaceholderIconColor.current
    val density = LocalDensity.current
    val closeThresholdPx = with(density) { CloseDragThreshold.toPx() }

    val coverSize = lerp(CoverMini, CoverFull, expandProgress.coerceIn(0f, 1f))
    val coverPx = with(density) { coverSize.roundToPx() }
    val coverCorner = lerp(8.dp, 16.dp, expandProgress.coerceIn(0f, 1f))

    var dragAccum by remember { mutableFloatStateOf(0f) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }
    var showQueue by remember { mutableStateOf(false) }

    val displayProgress = if (scrubbing) scrubProgress else state.progress

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .pointerInput(expandProgress) {
                if (expandProgress < 0.85f) return@pointerInput
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
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .graphicsLayer { alpha = expandProgress }
                    .clip(RoundedCornerShape(2.dp))
                    .background(metaColor.copy(alpha = 0.45f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(coverSize)
                    .clip(RoundedCornerShape(coverCorner))
                    .background(placeholderBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note_filled),
                    contentDescription = null,
                    tint = placeholderIconColor,
                    modifier = Modifier.size(coverSize * 0.35f)
                )
                val cover = state.coverFile
                if (cover != null) {
                    val request = ImageRequest.Builder(LocalContext.current)
                        .data(cover)
                        .size(CoilSize(coverPx.coerceAtLeast(1), coverPx.coerceAtLeast(1)))
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = ((expandProgress - 0.35f) / 0.45f).coerceIn(0f, 1f)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NowPlayingIconButton(
                        iconRes = R.drawable.ic_shuffle,
                        contentDescription = stringResource(R.string.cd_shuffle),
                        size = 28.dp,
                        active = state.shuffleEnabled,
                        activeColor = accentColor,
                        onClick = onToggleShuffle
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
                    // Repeat: OFF = apagado · ALL = brillante · ONE = brillante + "1"
                    NowPlayingRepeatButton(
                        repeatMode = state.repeatMode,
                        inactiveColor = titleColor,
                        activeColor = accentColor,
                        onClick = onCycleRepeat
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        alpha = ((expandProgress - 0.5f) / 0.4f).coerceIn(0f, 1f)
                    },
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NowPlayingIconButton(
                    iconRes = R.drawable.ic_queue_music,
                    contentDescription = stringResource(R.string.cd_queue),
                    size = 26.dp,
                    onClick = { showQueue = true }
                )
                NowPlayingIconButton(
                    iconRes = R.drawable.ic_favorite_outline,
                    contentDescription = stringResource(R.string.cd_favorite),
                    size = 26.dp,
                    onClick = { /* favoritos: siguiente fase */ }
                )
            }
        }
    }

    if (showQueue) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = stringResource(R.string.cd_queue),
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                itemsIndexed(
                    items = state.queue,
                    key = { index, entry -> "${entry.mediaId}_$index" }
                ) { index, entry ->
                    val isCurrent = entry.mediaId == state.currentMediaId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSkipToQueueIndex(index)
                                showQueue = false
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title.ifEmpty { "—" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isCurrent) accentColor else titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (entry.artist.isNotEmpty()) {
                                Text(
                                    text = entry.artist,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = subtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingRepeatButton(
    repeatMode: RepeatMode,
    inactiveColor: Color,
    activeColor: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val active = repeatMode != RepeatMode.OFF
    val tint = if (active) activeColor else inactiveColor

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
            painter = painterResource(R.drawable.ic_repeat),
            contentDescription = stringResource(R.string.cd_repeat),
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        if (repeatMode == RepeatMode.ONE) {
            Text(
                text = "1",
                color = activeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun NowPlayingIconButton(
    iconRes: Int,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    active: Boolean = false,
    activeColor: Color = Color.Unspecified
) {
    val interaction = remember { MutableInteractionSource() }
    val defaultTint = LocalListItemTitleColor.current
    val tint = if (active && activeColor != Color.Unspecified) activeColor else defaultTint
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