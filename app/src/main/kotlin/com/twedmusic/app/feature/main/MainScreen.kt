package com.twedmusic.app.feature.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * Pantalla principal de TwedMusic.
 *
 * Panel inferior unificado + Now Playing a pantalla completa:
 * - Abrir: tap en mini o swipe up (misma animación slide desde abajo).
 * - Cerrar: back del sistema o swipe down en Now Playing.
 * - Fondo de Now Playing = background del tema (#000000 en oscuro).
 */
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var isNowPlayingOpen by rememberSaveable { mutableStateOf(false) }

    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val bottomContentPadding = if (playerState.isVisible) 194.dp else 130.dp
    val panelBorderColor = MaterialTheme.colorScheme.outline

    // Si se corta el stream, cerrar Now Playing
    LaunchedEffect(playerState.isVisible) {
        if (!playerState.isVisible) isNowPlayingOpen = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
            },
            label = "screenTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 43.dp, bottom = bottomContentPadding)
        ) { tab ->
            when (tab) {
                NavTab.HOME -> HomeScreen()
                NavTab.TRACKS -> TracksScreen()
                NavTab.ALBUMS -> AlbumsScreen()
                NavTab.ARTISTS -> ArtistsScreen()
                NavTab.PLAYLISTS -> PlaylistScreen()
            }
        }

        TopActionBar(
            onSearchClick = { isSearchOpen = true },
            modifier = Modifier.align(Alignment.TopStart)
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    val stroke = BottomPanelTopBorder.toPx()
                    val corner = BottomPanelTopCorner.toPx()
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
                },
            shape = RoundedCornerShape(
                topStart = BottomPanelTopCorner,
                topEnd = BottomPanelTopCorner
            ),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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

        // Now Playing: sube desde abajo (misma animación que tap / swipe)
        AnimatedVisibility(
            visible = isNowPlayingOpen && playerState.isVisible,
            enter = slideInVertically(
                animationSpec = tween(380),
                initialOffsetY = { fullHeight -> fullHeight }
            ) + fadeIn(animationSpec = tween(280)),
            exit = slideOutVertically(
                animationSpec = tween(320),
                targetOffsetY = { fullHeight -> fullHeight }
            ) + fadeOut(animationSpec = tween(220)),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingScreen(
                state = playerState,
                onClose = { isNowPlayingOpen = false },
                onPlayPause = playerViewModel::togglePlayPause,
                onSkipPrevious = playerViewModel::skipToPrevious,
                onSkipNext = playerViewModel::skipToNext,
                onSeek = playerViewModel::seekTo,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isSearchOpen) {
            SearchScreen(
                onClose = { isSearchOpen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}