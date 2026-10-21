package com.twedmusic.app.feature.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twedmusic.app.feature.main.components.BottomNavigationPanel
import com.twedmusic.app.feature.main.components.TopActionBar
import com.twedmusic.app.feature.playback.ui.MiniPlayer
import com.twedmusic.app.feature.playback.ui.NowPlayingScreen
import com.twedmusic.app.feature.playback.ui.PlayerViewModel
import com.twedmusic.app.feature.search.SearchScreen
import com.twedmusic.app.feature.tabs.albums.AlbumsScreen
import com.twedmusic.app.feature.tabs.artists.ArtistsScreen
import com.twedmusic.app.feature.tabs.home.HomeScreen
import com.twedmusic.app.feature.tabs.playlist.PlaylistScreen
import com.twedmusic.app.feature.tabs.tracks.TracksScreen

private val BottomPanelTopCorner = 25.dp
private val BottomPanelTopBorder = 1.dp
private val PanelAnimMs = 420

/**
 * Pantalla principal de TwedMusic.
 *
 * El panel inferior unificado **se estira** hasta Now Playing:
 * - Misma Surface: altura, esquinas y color animan juntos.
 * - Mini + nav se desvanecen; el contenido de Now Playing aparece.
 * - No es un overlay aparte: es el panel que crece (como en el video de referencia).
 */
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var isNowPlayingOpen by rememberSaveable { mutableStateOf(false) }

    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val panelBorderColor = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(playerState.isVisible) {
        if (!playerState.isVisible) isNowPlayingOpen = false
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        val fullHeight = maxHeight
        val collapsedHeight = if (playerState.isVisible) 194.dp else 130.dp
        val targetHeight =
            if (isNowPlayingOpen && playerState.isVisible) fullHeight else collapsedHeight

        val animatedHeight by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = tween(PanelAnimMs, easing = FastOutSlowInEasing),
            label = "panelHeight"
        )

        val expandFraction = if (fullHeight > collapsedHeight) {
            ((animatedHeight - collapsedHeight) / (fullHeight - collapsedHeight))
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        val cornerRadius by animateDpAsState(
            targetValue = if (isNowPlayingOpen) 0.dp else BottomPanelTopCorner,
            animationSpec = tween(PanelAnimMs, easing = FastOutSlowInEasing),
            label = "panelCorner"
        )

        // Padding de la lista: siempre el colapsado para que el panel crezca encima
        val listBottomPadding = collapsedHeight

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
            },
            label = "screenTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 43.dp, bottom = listBottomPadding)
        ) { tab ->
            when (tab) {
                NavTab.HOME -> HomeScreen()
                NavTab.TRACKS -> TracksScreen()
                NavTab.ALBUMS -> AlbumsScreen()
                NavTab.ARTISTS -> ArtistsScreen()
                NavTab.PLAYLISTS -> PlaylistScreen()
            }
        }

        val topBarAlpha by animateFloatAsState(
            targetValue = if (expandFraction > 0.25f) 0f else 1f,
            animationSpec = tween(220),
            label = "topBarAlpha"
        )
        if (topBarAlpha > 0.01f) {
            TopActionBar(
                onSearchClick = { isSearchOpen = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer { alpha = topBarAlpha }
            )
        }

        // Panel único que se estira
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(animatedHeight)
                .drawWithContent {
                    drawContent()
                    // Borde superior solo mientras no está a pantalla completa
                    if (expandFraction < 0.92f) {
                        val stroke = BottomPanelTopBorder.toPx()
                        val corner = cornerRadius.toPx().coerceAtLeast(0.1f)
                        val path = Path().apply {
                            moveTo(0f, corner)
                            arcTo(
                                rect = Rect(0f, 0f, corner * 2f, corner * 2f),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                            lineTo(size.width - corner, 0f)
                            arcTo(
                                rect = Rect(
                                    size.width - corner * 2f,
                                    0f,
                                    size.width,
                                    corner * 2f
                                ),
                                startAngleDegrees = 270f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                        }
                        drawPath(
                            path = path,
                            color = panelBorderColor,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                },
            shape = RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = cornerRadius
            ),
            // surfaceVariant → background (#000 en oscuro) al expandir
            color = androidx.compose.ui.graphics.lerp(
                surfaceVariant,
                backgroundColor,
                expandFraction
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Capa colapsada: mini + nav (se desvanece al subir)
                if (expandFraction < 0.99f) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                alpha = (1f - expandFraction * 1.4f).coerceIn(0f, 1f)
                            }
                    ) {
                        AnimatedVisibility(
                            visible = playerState.isVisible,
                            enter = expandVertically(
                                animationSpec = tween(280),
                                expandFrom = Alignment.Bottom
                            ) + fadeIn(animationSpec = tween(280)),
                            exit = shrinkVertically(
                                animationSpec = tween(220),
                                shrinkTowards = Alignment.Bottom
                            ) + fadeOut(animationSpec = tween(220))
                        ) {
                            MiniPlayer(
                                state = playerState,
                                onPlayPause = playerViewModel::togglePlayPause,
                                onSkipPrevious = playerViewModel::skipToPrevious,
                                onSkipNext = playerViewModel::skipToNext,
                                onDismiss = playerViewModel::dismissAndStop,
                                onExpand = { isNowPlayingOpen = true }
                            )
                        }
                        BottomNavigationPanel(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Capa expandida: Now Playing (aparece con el estirado)
                if (expandFraction > 0.02f && playerState.isVisible) {
                    NowPlayingScreen(
                        state = playerState,
                        onClose = { isNowPlayingOpen = false },
                        onPlayPause = playerViewModel::togglePlayPause,
                        onSkipPrevious = playerViewModel::skipToPrevious,
                        onSkipNext = playerViewModel::skipToNext,
                        onSeek = playerViewModel::seekTo,
                        expandProgress = expandFraction,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = ((expandFraction - 0.15f) / 0.55f).coerceIn(0f, 1f)
                            }
                    )
                }
            }
        }

        if (isSearchOpen) {
            SearchScreen(
                onClose = { isSearchOpen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}