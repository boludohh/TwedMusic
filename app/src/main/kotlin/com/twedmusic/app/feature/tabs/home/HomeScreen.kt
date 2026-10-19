package com.twedmusic.app.feature.tabs.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twedmusic.app.R
import com.twedmusic.app.core.database.TotalStats
import com.twedmusic.app.core.model.Song
import com.twedmusic.app.feature.tabs.home.components.HomeAlbumCard
import com.twedmusic.app.feature.tabs.home.components.HomeArtistCard
import com.twedmusic.app.feature.tabs.home.components.HomeSectionHeader
import com.twedmusic.app.feature.tabs.home.components.HomeTrackCard
import com.twedmusic.app.feature.tabs.home.components.PlaylistCard
import com.twedmusic.app.feature.tabs.home.components.ResumeCard
import com.twedmusic.app.feature.tabs.home.components.ShuffleAllButton
import com.twedmusic.app.feature.tabs.home.components.StatsPanel
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor
import com.twedmusic.app.core.designsystem.theme.LocalTracksCountTextColor
import java.util.Calendar
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

/**
 * Pantalla de inicio de TwedMusic (rediseño basado en mockup HTML).
 *
 * Muestra un resumen vivo de la actividad musical del usuario con
 * secciones jerárquicas:
 *
 * 1. **Saludo personalizado** según la hora del día
 * 2. **Seguir escuchando**: card con la última canción reproducida
 * 3. **Tu actividad**: panel de estadísticas agregadas
 * 4. **Reproducir todo aleatoriamente**: botón grande con contador
 * 5. **Escuchado recientemente**: fila horizontal de canciones
 * 6. **Tus artistas**: fila horizontal de artistas con avatar circular
 * 7. **Agregado recientemente**: fila horizontal de canciones recientes
 * 8. **Playlists**: grid 2 columnas de playlists con mosaicos
 * 9. **Álbumes**: fila horizontal de álbumes recientes
 *
 * @param modifier Modificador de Compose opcional.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val attributionColor = LocalTracksCountTextColor.current
    val titleColor = LocalListItemTitleColor.current
    val subtitleColor = LocalListItemSubtitleColor.current
    val backgroundColor = MaterialTheme.colorScheme.background

    // Todo el estado de la pantalla viene del ViewModel
    val state by viewModel.uiState.collectAsState()
    val totalStats = state.totalStats
    val recentlyPlayed = state.recentlyPlayed
    val recentlyAdded = state.recentlyAdded
    val uniqueArtists = state.uniqueArtists
    val recentAlbums = state.recentAlbums
    val songCount = state.songCount

    // Saludo según hora del día
    val greeting = rememberGreeting()

    // Canción para "Seguir escuchando": la más reciente reproducida
    val resumeSong = recentlyPlayed.firstOrNull()

    // Tiempo formateado para el panel de estadísticas
    val formattedTime = formatPlayedTime(totalStats.totalPlayedMs)

    // Strings localizados
    val statsTitle = stringResource(R.string.home_total_plays)
    val completedLabel = stringResource(R.string.home_completed_plays)
    val timeLabel = stringResource(R.string.home_hours_listened)
    val shuffleTitle = stringResource(R.string.home_shuffle_all)
    val shuffleCount = stringResource(R.string.home_shuffle_count, songCount)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            // ── Saludo ──
            item(key = "greeting") {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    color = titleColor,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // ── Seguir escuchando ──
            if (resumeSong != null) {
                item(key = "resume_section") {
                    ResumeSection(
                        song = resumeSong,
                        coverFileOf = viewModel::coverFile
                    )
                }
            }

            // ── Tu actividad ──
            if (totalStats.totalPlays > 0) {
                item(key = "stats_section") {
                    StatsSection(
                        stats = totalStats,
                        formattedTime = formattedTime,
                        statsTitle = statsTitle,
                        completedLabel = completedLabel,
                        timeLabel = timeLabel
                    )
                }
            }

            // ── Shuffle all ──
            if (songCount > 0) {
                item(key = "shuffle") {
                    ShuffleAllButton(
                        titleText = shuffleTitle,
                        countText = shuffleCount,
                        onClick = { /* TODO: shuffle all */ }
                    )
                }
            }

            // ── Escuchado recientemente ──
            if (recentlyPlayed.isNotEmpty()) {
                item(key = "recent_section") {
                    RecentlyPlayedSection(
                        songs = recentlyPlayed,
                        coverFileOf = viewModel::coverFile
                    )
                }
            }

            // ── Tus artistas ──
            if (uniqueArtists.isNotEmpty()) {
                item(key = "artists_section") {
                    ArtistsSection(
                        artists = uniqueArtists,
                        artistImageFileOf = viewModel::artistImageFile
                    )
                }
            }

            // ── Agregado recientemente ──
            if (recentlyAdded.isNotEmpty()) {
                item(key = "added_section") {
                    RecentlyAddedSection(
                        songs = recentlyAdded,
                        coverFileOf = viewModel::coverFile
                    )
                }
            }

            // ── Playlists ──
            item(key = "playlists_section") {
                PlaylistsSection()
            }

            // ── Álbumes ──
            if (recentAlbums.isNotEmpty()) {
                item(key = "albums_section") {
                    AlbumsSection(
                        albums = recentAlbums,
                        coverFileOf = viewModel::coverFile
                    )
                }
            }

            // ── Estado vacío ──
            if (songCount == 0 && totalStats.totalPlays == 0) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_no_stats),
                            style = MaterialTheme.typography.bodyLarge,
                            color = subtitleColor
                        )
                    }
                }
            }
        }

        // Atribución de Deezer (términos de uso)
        Text(
            text = stringResource(R.string.artist_images_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = attributionColor,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 8.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECCIONES HELPER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResumeSection(
    song: Song,
    coverFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_resume_section),
            showSeeAll = false
        )
        ResumeCard(
            title = song.title,
            artist = song.artist,
            context = stringResource(R.string.home_resume_playing_from),
            coverFile = coverFileOf(song.path),
            onCardClick = { /* TODO: navegar */ }
        )
    }
}

@Composable
private fun StatsSection(
    stats: TotalStats,
    formattedTime: String,
    statsTitle: String,
    completedLabel: String,
    timeLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_your_activity),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: navegar a stats */ }
        )
        StatsPanel(
            stats = stats,
            formattedTime = formattedTime,
            totalPlaysLabel = statsTitle,
            completedLabel = completedLabel,
            timeLabel = timeLabel
        )
    }
}

@Composable
private fun RecentlyPlayedSection(
    songs: List<Song>,
    coverFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_recently_played),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: navegar a historial */ }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = songs,
                key = { "recent-${it.id}" }
            ) { song ->
                HomeTrackCard(
                    title = song.title,
                    subtitle = song.artist,
                    coverFile = coverFileOf(song.path),
                    onClick = { /* TODO: reproducir */ }
                )
            }
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
            onSeeAllClick = { /* TODO: navegar a artistas */ }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = artists,
                key = { "artist-$it" }
            ) { artistName ->
                val imageFile = artistImageFileOf(artistName)
                HomeArtistCard(
                    artistName = artistName,
                    imageFile = imageFile,
                    onClick = { /* TODO: navegar a detalle artista */ }
                )
            }
        }
    }
}

@Composable
private fun RecentlyAddedSection(
    songs: List<Song>,
    coverFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_recently_added),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: navegar */ }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = songs,
                key = { "added-${it.id}" }
            ) { song ->
                HomeTrackCard(
                    title = song.title,
                    subtitle = song.artist,
                    coverFile = coverFileOf(song.path),
                    onClick = { /* TODO: reproducir */ }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_playlists),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: navegar a playlists */ }
        )
        // Grid manual 2x2 (evita LazyVerticalGrid anidado en LazyColumn)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Fila 1
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlaylistCard(
                    title = "Playlist 1",
                    countText = stringResource(R.string.home_songs_count, 0),
                    coverFiles = emptyList(),
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: navegar */ }
                )
                PlaylistCard(
                    title = "Playlist 2",
                    countText = stringResource(R.string.home_songs_count, 0),
                    coverFiles = emptyList(),
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: navegar */ }
                )
            }
            // Fila 2
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlaylistCard(
                    title = "Playlist 3",
                    countText = stringResource(R.string.home_songs_count, 0),
                    coverFiles = emptyList(),
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: navegar */ }
                )
                PlaylistCard(
                    title = "Playlist 4",
                    countText = stringResource(R.string.home_songs_count, 0),
                    coverFiles = emptyList(),
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: navegar */ }
                )
            }
        }
    }
}

@Composable
private fun AlbumsSection(
    albums: List<HomeAlbumData>,
    coverFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_albums),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: navegar a álbumes */ }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = albums,
                key = { "album-${it.title}|${it.artist}" }
            ) { album ->
                HomeAlbumCard(
                    title = album.title,
                    artist = album.artist,
                    coverFile = album.coverPath?.let { coverFileOf(it) },
                    onClick = { /* TODO: navegar a detalle álbum */ }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FUNCIONES UTILITARIAS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Genera el saludo según la hora del día (HOUR_OF_DAY, 0-23).
 * - Madrugada/noche (20:00-04:59): "Buenas noches"
 * - Mañana (05:00-11:59): "Buenos días"
 * - Tarde (12:00-19:59): "Buenas tardes"
 */
@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> stringResource(R.string.home_greeting_morning)
        in 12..19 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_night)
    }
}

/**
 * Formatea milisegundos reproducidos a formato legible:
 * - Si >= 1 hora: "X h Y min"
 * - Si < 1 hora: "Y min"
 */
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

