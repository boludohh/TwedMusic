package com.twedmusic.app.feature.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twedmusic.app.feature.main.components.BottomNavigationPanel
import com.twedmusic.app.feature.main.components.TopActionBar
import com.twedmusic.app.feature.playback.ui.MiniPlayer
import com.twedmusic.app.feature.playback.ui.PlayerViewModel
import com.twedmusic.app.feature.search.SearchScreen
import com.twedmusic.app.feature.tabs.albums.AlbumsScreen
import com.twedmusic.app.feature.tabs.artists.ArtistsScreen
import com.twedmusic.app.feature.tabs.home.HomeScreen
import com.twedmusic.app.feature.tabs.playlist.PlaylistScreen
import com.twedmusic.app.feature.tabs.tracks.TracksScreen

/** Esquinas superiores del panel inferior unificado (nav ± mini player). */
private val BottomPanelTopCorner = 25.dp

/** Grosor del borde superior del panel (siempre visible). */
private val BottomPanelTopBorder = 1.dp

/**
 * Pantalla principal de TwedMusic.
 *
 * Contenedor de pestañas, top bar y **panel inferior unificado**:
 * un solo Surface con esquinas 25 dp y borde superior; dentro viven
 * el mini player (si hay sesión) y la navegación. Así el mini no
 * parece un bloque superpuesto sino la misma barra estirada.
 *
 * Padding inferior del contenido:
 * - Solo nav: 130 dp
 * - Nav + mini: 194 dp (130 + 64)
 */
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }

    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val bottomContentPadding = if (playerState.isVisible) 194.dp else 130.dp
    val panelBorderColor = MaterialTheme.colorScheme.outline

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

        // Panel inferior único: forma + color + borde superior
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    val stroke = BottomPanelTopBorder.toPx()
                    drawLine(
                        color = panelBorderColor,
                        start = Offset(0f, stroke / 2f),
                        end = Offset(size.width, stroke / 2f),
                        strokeWidth = stroke
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
                        onDismiss = playerViewModel::dismissAndStop
                    )
                }

                BottomNavigationPanel(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth()
                )
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