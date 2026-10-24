package com.twedmusic.app.feature.tabs.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twedmusic.app.R
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor
import com.twedmusic.app.core.designsystem.theme.LocalScreenTitleColor
import com.twedmusic.app.core.designsystem.theme.screenTitle
import com.twedmusic.app.core.model.Album
import com.twedmusic.app.core.model.Song
import com.twedmusic.app.feature.tabs.albums.components.AlbumCard
import com.twedmusic.app.feature.tabs.home.components.HomeArtistCard
import com.twedmusic.app.feature.tabs.home.components.HomeRecentTrackRow
import com.twedmusic.app.feature.tabs.home.components.HomeSectionHeader
import com.twedmusic.app.feature.tabs.home.components.StatsPanel
import java.io.File

/** Mismo padding inferior que AlbumsScreen (GridBottomPadding). */
private val HomeBottomPadding = 8.dp

/**
 * Pantalla de inicio.
 *
 * Geometría del header alineada con TracksScreen:
 * - start 24.dp (glifo del menú)
 * - Título: top 8.dp
 * - "Tu actividad": 8.dp bajo el título (como "100 pistas")
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backgroundColor = MaterialTheme.colorScheme.background
    val screenTitleColor = LocalScreenTitleColor.current
    val titleColor = LocalListItemTitleColor.current

    val formattedTime = formatPlayedTime(uiState.totalStats.totalPlayedMs)
    val playsLabel = stringResource(R.string.home_total_plays)
    val completedLabel = stringResource(R.string.home_completed_plays)
    val timeLabel = stringResource(R.string.home_hours_listened)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 20.dp,
                top = 0.dp,
                bottom = HomeBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item(key = "header_stats") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_screen_placeholder),
                        style = screenTitle(),
                        color = screenTitleColor
                    )

                    Text(
                        text = stringResource(R.string.home_your_activity),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = titleColor,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatsPanel(
                        stats = uiState.totalStats,
                        formattedTime = formattedTime,
                        totalPlaysLabel = playsLabel,
                        completedLabel = completedLabel,
                        timeLabel = timeLabel
                    )
                }
            }

            if (uiState.recentlyPlayed.isNotEmpty()) {
                item(key = "recent") {
                    RecentlyPlayedSection(
                        songs = uiState.recentlyPlayed,
                        coverFileOf = viewModel::coverFile
                    )
                }
            }

            if (uiState.recentArtists.isNotEmpty()) {
                item(key = "artists") {
                    ArtistsSection(
                        artists = uiState.recentArtists,
                        artistImageFileOf = viewModel::artistImageFile
                    )
                }
            }

            if (uiState.recentAlbums.isNotEmpty()) {
                item(key = "albums") {
                    AlbumsGridSection(
                        albums = uiState.recentAlbums,
                        coverFileOf = viewModel::coverFile
                    )
                }
            }

            if (uiState.songCount == 0 && uiState.totalStats.totalPlays == 0) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.home_no_stats),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalListItemSubtitleColor.current,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    songs: List<Song>,
    coverFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_recently_played),
            showSeeAll = false
        )
        songs.forEach { song ->
            HomeRecentTrackRow(
                title = song.title,
                artist = song.artist,
                coverFile = song.path.takeIf { it.isNotBlank() }?.let { coverFileOf(it) },
                onClick = { /* TODO: play */ },
                onMoreClick = { /* TODO: menú */ }
            )
        }
    }
}

@Composable
private fun ArtistsSection(
    artists: List<String>,
    artistImageFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_your_artists),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: Artistas */ }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = artists,
                key = { "artist-$it" }
            ) { name ->
                HomeArtistCard(
                    artistName = name,
                    imageFile = artistImageFileOf(name),
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
private fun AlbumsGridSection(
    albums: List<Album>,
    coverFileOf: (String) -> File?
) {
    Column {
        HomeSectionHeader(
            title = stringResource(R.string.home_albums),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: Álbumes */ }
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            albums.chunked(2).forEach { rowAlbums ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowAlbums.forEach { album ->
                        AlbumCard(
                            title = album.title,
                            artist = album.artist,
                            year = album.year,
                            trackCount = album.trackCount,
                            isNew = album.isNew,
                            coverFile = album.coverPath?.let { coverFileOf(it) },
                            onClick = { /* TODO: detalle álbum */ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAlbums.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun formatPlayedTime(playedMs: Long): String {
    val totalMinutes = playedMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.home_hours_minutes_format, hours.toInt(), minutes.toInt())
    } else {
        stringResource(R.string.home_only_minutes_format, minutes.toInt())
    }
}