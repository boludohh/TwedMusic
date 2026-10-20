package com.twedmusic.app.feature.main.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.twedmusic.app.R
import com.twedmusic.app.core.designsystem.theme.LocalNavIconActiveColor
import com.twedmusic.app.core.designsystem.theme.LocalNavIconInactiveColor
import com.twedmusic.app.feature.main.NavTab

/**
 * Contenido de iconos del panel inferior de navegación.
 *
 * Forma, color de fondo y borde superior los aporta el contenedor
 * unificado en [com.twedmusic.app.feature.main.MainScreen], para que
 * mini player y nav sean el mismo panel.
 *
 * - Altura fija de 130 dp (incluye padding de navigationBars).
 * - Crossfade outline/filled + elevación del icono activo (300 ms).
 * - Sin ripple.
 */
@Composable
fun BottomNavigationPanel(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                NavIcon(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavIcon(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = LocalNavIconActiveColor.current
    val inactiveColor = LocalNavIconInactiveColor.current
    val interactionSource = remember { MutableInteractionSource() }

    val offsetAnimation by animateDpAsState(
        targetValue = if (isSelected) (-10).dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "iconElevation"
    )

    val (outlineRes, filledRes) = when (tab) {
        NavTab.HOME -> R.drawable.ic_home_outline to R.drawable.ic_home_filled
        NavTab.TRACKS -> R.drawable.ic_music_note_outline to R.drawable.ic_music_note_filled
        NavTab.ALBUMS -> R.drawable.ic_album_outline to R.drawable.ic_album_filled
        NavTab.ARTISTS -> R.drawable.ic_artist_outline to R.drawable.ic_artist_filled
        NavTab.PLAYLISTS -> R.drawable.ic_playlist_play to R.drawable.ic_playlist_play
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .size(48.dp)
            .offset(y = offsetAnimation)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = isSelected,
            animationSpec = tween(durationMillis = 300),
            label = "navIconCrossfade"
        ) { selected ->
            val painter = if (selected) painterResource(filledRes) else painterResource(outlineRes)
            val tint = if (selected) activeColor else inactiveColor

            Icon(
                painter = painter,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}