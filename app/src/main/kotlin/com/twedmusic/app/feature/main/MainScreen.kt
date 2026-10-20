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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/**
 * Pantalla principal de TwedMusic.
 *
 * Contenedor de navegación de las 5 pestañas, barra superior,
 * panel inferior y mini player global (encima de la nav).
 *
 * Padding inferior del contenido:
 * - Solo nav: 130 dp
 * - Nav + mini player visible: 194 dp (130 + 64)
 */
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }

    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val bottomContentPadding = if (playerState.isVisible) 194.dp else 130.dp

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

        // Nav + mini player: el mini se “estira” desde la nav hacia arriba
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
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
                    onDismiss = playerViewModel::dismissAndStop
                )
            }

            BottomNavigationPanel(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.fillMaxWidth()
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