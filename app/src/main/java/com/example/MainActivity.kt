package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.Playlist
import com.example.data.Track
import com.example.ui.*
import com.example.ui.theme.ZisprTheme
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZisprTheme {
                val context = LocalContext.current
                val app = context.applicationContext as Application
                val viewModelFactory = remember { ZisprViewModelFactory(app) }
                val view_model: ZisprViewModel = viewModel(factory = viewModelFactory)

                ZisprMainScreen(view_model)
            }
        }
    }
}

// ==========================================
// CUSTOM VECTOR ICONS (Zero-dependency & Lightweight)
// ==========================================

@Composable
fun PauseIcon(tint: Color = Color.Black, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Row(
        modifier = Modifier.size(size),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width((size.value * 0.18f).dp).fillMaxHeight(0.55f).background(tint, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width((size.value * 0.18f).dp).fillMaxHeight(0.55f).background(tint, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun SkipNextIcon(tint: Color = Color.White, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.2f)
            lineTo(w * 0.62f, h * 0.5f)
            lineTo(w * 0.25f, h * 0.8f)
            close()
        }
        drawPath(path, color = tint)
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.66f, h * 0.25f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
    }
}

@Composable
fun SkipPreviousIcon(tint: Color = Color.White, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val path = Path().apply {
            moveTo(w * 0.75f, h * 0.2f)
            lineTo(w * 0.38f, h * 0.5f)
            lineTo(w * 0.75f, h * 0.8f)
            close()
        }
        drawPath(path, color = tint)
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.25f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
    }
}

@Composable
fun MusicNoteIcon(tint: Color = Color.White, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        drawOval(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.25f)
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.65f),
            end = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.2f),
            strokeWidth = 4f
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.22f),
            end = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.32f),
            strokeWidth = 4f
        )
    }
}

// ==========================================
// CORE INTERFACE LAYER
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZisprMainScreen(viewModel: ZisprViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val playingTrack by viewModel.playbackManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playbackManager.isPlaying.collectAsStateWithLifecycle()
    val currentPlayPosition by viewModel.playbackManager.currentPosition.collectAsStateWithLifecycle()
    val totalDuration by viewModel.playbackManager.trackDuration.collectAsStateWithLifecycle()
    val isPlayerLoading by viewModel.playbackManager.isLoading.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()

    var showPlayerDetail by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistDialogName by remember { mutableStateOf("") }
    var playlistDialogDescription by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color.Transparent)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Inline floating expandable player bar
                playingTrack?.let { track ->
                    MiniPlayerRow(
                        track = track,
                        isPlaying = isPlaying,
                        isLoading = isPlayerLoading,
                        position = currentPlayPosition,
                        duration = totalDuration,
                        onTogglePlay = { viewModel.playbackManager.togglePlayPause() },
                        onClick = { showPlayerDetail = true }
                    )
                }

                // Core dark Spotify-style navigation bar
                NavigationBar(
                    containerColor = Color(0xFF0F0F0F),
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    NavigationBarItem(
                        selected = currentScreen == ZisprScreen.HOME && selectedPlaylist == null,
                        onClick = { viewModel.navigateTo(ZisprScreen.HOME) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Início") },
                        label = { Text("Início", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF2D2F20)
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == ZisprScreen.SEARCH,
                        onClick = { viewModel.navigateTo(ZisprScreen.SEARCH) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                        label = { Text("Buscar", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF2D2F20)
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == ZisprScreen.AI_DJ,
                        onClick = { viewModel.navigateTo(ZisprScreen.AI_DJ) },
                        icon = { 
                            Box {
                                Icon(Icons.Filled.Info, contentDescription = "DJ de IA")
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        },
                        label = { Text("DJ IA", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF2D2F20)
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == ZisprScreen.LIBRARY || selectedPlaylist != null,
                        onClick = { viewModel.navigateTo(ZisprScreen.LIBRARY) },
                        icon = { Icon(Icons.Filled.List, contentDescription = "Biblioteca") },
                        label = { Text("Biblioteca", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF2D2F20)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Screen content layout routing
            if (selectedPlaylist != null) {
                PlaylistDetailsLayout(
                    playlist = selectedPlaylist!!,
                    onBack = { viewModel.unselectPlaylist() },
                    viewModel = viewModel
                )
            } else {
                when (currentScreen) {
                    ZisprScreen.HOME -> HomeScreenLayout(viewModel = viewModel)
                    ZisprScreen.SEARCH -> SearchScreenLayout(viewModel = viewModel)
                    ZisprScreen.LIBRARY -> LibraryScreenLayout(
                        viewModel = viewModel,
                        onCreatePlaylistClick = { showCreatePlaylistDialog = true }
                    )
                    ZisprScreen.AI_DJ -> AiDjScreenLayout(viewModel = viewModel)
                }
            }
        }
    }

    // Full screen expandable playback sheet
    if (showPlayerDetail && playingTrack != null) {
        ExpandedPlayerSheet(
            track = playingTrack!!,
            isPlaying = isPlaying,
            isLoading = isPlayerLoading,
            position = currentPlayPosition,
            duration = totalDuration,
            onClose = { showPlayerDetail = false },
            onTogglePlay = { viewModel.playbackManager.togglePlayPause() },
            onSkipNext = { viewModel.playbackManager.skipToNext() },
            onSkipPrev = { viewModel.playbackManager.skipToPrevious() },
            onSeek = { viewModel.playbackManager.seekTo(it) },
            isFavorite = playingTrack!!.isFavorite,
            onFavoriteToggle = { viewModel.toggleFavorite(playingTrack!!) }
        )
    }

    // Playlist Creator Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Nova Playlist", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = playlistDialogName,
                        onValueChange = { playlistDialogName = it },
                        label = { Text("Nome da Playlist") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playlistDialogDescription,
                        onValueChange = { playlistDialogDescription = it },
                        label = { Text("Descrição (opcional)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistDialogName.isNotBlank()) {
                            viewModel.createUserPlaylist(playlistDialogName, playlistDialogDescription)
                            playlistDialogName = ""
                            playlistDialogDescription = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Criar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        playlistDialogName = ""
                        playlistDialogDescription = ""
                        showCreatePlaylistDialog = false
                    }
                ) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
        )
    }
}

// ==========================================
// SCREEN LAYOUTS
// ==========================================

@Composable
fun HomeScreenLayout(viewModel: ZisprViewModel) {
    val musicTracks by viewModel.musicTracks.collectAsStateWithLifecycle()
    val podcasts by viewModel.podcastTracks.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2D2F20).copy(alpha = 0.5f), Color(0xFF121212)),
                    startY = 0f,
                    endY = 400f
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Z",
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "Zispr",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Zispr Info",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Category Quick pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPill("Música", true) { }
                CategoryPill("Podcasts", false) { viewModel.navigateTo(ZisprScreen.SEARCH); viewModel.setPodcastFilter(true) }
            }
        }

        // Featured grid
        item {
            Text(
                "Sua Vibe Musical",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        item {
            val featuredPlaylists = playlists.take(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                featuredPlaylists.chunked(2).forEach { rowPlaylists ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPlaylists.forEach { pl ->
                            FeaturedRowCard(
                                playlist = pl,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectPlaylist(pl) }
                            )
                        }
                        if (rowPlaylists.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recently Played
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    "Tocados Recentemente",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentlyPlayed) { track ->
                        TrackRowCard(track = track) {
                            viewModel.playSingleTrack(track, recentlyPlayed)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Recommended Tracks Box
        item {
            Text(
                "Populares no Zispr",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        val allReadyTracks = musicTracks + podcasts
        items(allReadyTracks.take(6)) { track ->
            TrackListItem(
                track = track,
                onLikeClick = { viewModel.toggleFavorite(track) },
                onClick = { viewModel.playSingleTrack(track, allReadyTracks) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SearchScreenLayout(viewModel: ZisprViewModel) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val isPodcastFilter by viewModel.isPodcastFilter.collectAsStateWithLifecycle()

    val filteredList = viewModel.getFilteredTracks(allTracks)
    val genres = listOf("Synthwave", "Lofi", "Electronic", "Ambient", "Podcast")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Buscar",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Text Search bar input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("O que você quer ouvir?", color = Color.Gray) },
            prefix = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray, modifier = Modifier.padding(end = 6.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("search_input")
        )

        // Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryPill("Tudo", selectedGenre == null && isPodcastFilter == null) {
                viewModel.selectGenre(null)
                viewModel.setPodcastFilter(null)
            }
            CategoryPill("Lofi", selectedGenre == "Lofi") {
                viewModel.selectGenre("Lofi")
                viewModel.setPodcastFilter(false)
            }
            CategoryPill("Synthwave", selectedGenre == "Synthwave") {
                viewModel.selectGenre("Synthwave")
                viewModel.setPodcastFilter(false)
            }
            CategoryPill("Podcasts", isPodcastFilter == true) {
                viewModel.selectGenre(null)
                viewModel.setPodcastFilter(true)
            }
        }

        if (searchQuery.isNotEmpty() || selectedGenre != null || isPodcastFilter != null) {
            // Searched tracks list output
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma faixa de música encontrada 🔎", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                Text(
                    "Resultados",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredList) { track ->
                        TrackListItem(
                            track = track,
                            onLikeClick = { viewModel.toggleFavorite(track) },
                            onClick = { viewModel.playSingleTrack(track, filteredList) }
                        )
                    }
                }
            }
        } else {
            // Static Browse genres cards
            Text(
                "Navegar por seções",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val genrePairs = genres.chunked(2)
                items(genrePairs) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { title ->
                            val color = when (title) {
                                "Synthwave" -> Color(0xFFE91E63)
                                "Lofi" -> Color(0xFFFF9800)
                                "Electronic" -> Color(0xFF2196F3)
                                "Ambient" -> Color(0xFF4CAF50)
                                else -> Color(0xFF9C27B0)
                            }
                            GenreCard(
                                title = title,
                                color = color,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .clickable {
                                        if (title == "Podcast") {
                                            viewModel.setPodcastFilter(true)
                                            viewModel.selectGenre(null)
                                        } else {
                                            viewModel.selectGenre(title)
                                            viewModel.setPodcastFilter(false)
                                        }
                                    }
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreenLayout(viewModel: ZisprViewModel, onCreatePlaylistClick: () -> Unit) {
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Sua Biblioteca",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onCreatePlaylistClick,
                    modifier = Modifier.testTag("add_playlist_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Criar Playlist", tint = Color.White)
                }
            }
        }

        // Favorite songs banner card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2D2F20), Color(0xFF151610))
                        )
                    )
                    .clickable {
                        // Create a synthetic Playlist representing Favorite Tracks
                        val favPlaylist = Playlist(
                            id = -99, // special ID for liked tracks screen
                            name = "Músicas Curtidas",
                            description = "Todas as suas faixas favoritas em um só lugar."
                        )
                        viewModel.selectPlaylist(favPlaylist)
                    }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        "Músicas Curtidas",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${favoriteTracks.size} faixas salvas",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.TopEnd)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                "Playlists Criadas",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.List, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Você ainda não criou nenhuma playlist.\nClique no '+' no topo para começar!",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(playlists) { playlist ->
                PlaylistItemRow(
                    playlist = playlist,
                    onClick = { viewModel.selectPlaylist(playlist) },
                    onDelete = { viewModel.deletePlaylist(playlist.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun AiDjScreenLayout(viewModel: ZisprViewModel) {
    val messages by viewModel.djMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isDjLoading.collectAsStateWithLifecycle()
    val lastSuggestedTracks by viewModel.lastSuggestedTracks.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Keyframes animation for DJ waveform avatar
    val infiniteTransition = rememberInfiniteTransition(label = "Waveforms")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )
    val waveScale3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w3"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
    ) {
        // DJ banner header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF242424))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing DJ sphere
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D2F20)),
                contentAlignment = Alignment.Center
            ) {
                // Wave animated canvas lines
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(width = 4.dp, height = (24 * waveScale1).dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(width = 4.dp, height = (32 * waveScale2).dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(width = 4.dp, height = (18 * waveScale3).dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    "DJ Zispr",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Green, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Transmitindo via Gemini 3.5 Flash",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Messages area list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                DjBubble(message = msg)
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "O DJ está procurando faixas na biblioteca...",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Render suggested playlist widget if present
            if (lastSuggestedTracks.isNotEmpty()) {
                item {
                    DjSmartPlaylistCard(
                        tracks = lastSuggestedTracks,
                        onSave = {
                            viewModel.saveDjPlaylist(
                                name = "Mix do DJ: " + lastSuggestedTracks.first().genre,
                                description = "Playlist sugerida pelo DJ Zispr IA."
                            )
                        },
                        onPlayAll = {
                            viewModel.playSingleTrack(lastSuggestedTracks.first(), lastSuggestedTracks)
                        }
                    )
                }
            }
        }

        // User textbox input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Peça algo ao DJ Zispr...", color = Color.Gray, fontSize = 14.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF282828),
                    unfocusedContainerColor = Color(0xFF282828),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(22.dp),
                maxLines = 2,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendDjMessage(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendDjMessage(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .size(44.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = Color.Black)
            }
        }
    }
}

// ==========================================
// SECTOR HELPERS / CHILD COMPONENTS
// ==========================================

@Composable
fun PlaylistDetailsLayout(
    playlist: Playlist,
    onBack: () -> Unit,
    viewModel: ZisprViewModel
) {
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val playlistTracks by viewModel.selectedPlaylistTracks.collectAsStateWithLifecycle()
    
    val displayTracks = if (playlist.id == -99) favoriteTracks else playlistTracks

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2D2F20), Color(0xFF121212)),
                    startY = 0f,
                    endY = 300f
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playlist", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        // Playlist visual header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large placeholder or beautiful visual cover
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (playlist.isAiGenerated) {
                                    listOf(Color(0xFF3DCE80), Color(0xFF15482D))
                                } else {
                                    listOf(Color(0xFFD2E750), Color(0xFF2D2F20))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MusicNoteIcon(tint = Color.White, size = 64.dp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    playlist.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    playlist.description,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (displayTracks.isNotEmpty()) {
                            viewModel.playSingleTrack(displayTracks.first(), displayTracks)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.width(180.dp),
                    enabled = displayTracks.isNotEmpty()
                ) {
                    Text("TOCAR MIX", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Inner item lists
        if (displayTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Essa playlist está vazia.\nAdicione músicas buscando suas faixas favoritas!", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(displayTracks) { track ->
                TrackListItem(
                    track = track,
                    onLikeClick = { viewModel.toggleFavorite(track) },
                    onClick = { viewModel.playSingleTrack(track, displayTracks) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MiniPlayerRow(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onClick: () -> Unit
) {
    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF242424))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover thumbnail image
            AsyncImage(
                model = track.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onTogglePlay) {
                    if (isPlaying) {
                        PauseIcon(tint = Color.White, size = 28.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Tocar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Tiny sleek seek line at base of floating card
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Gray.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun ExpandedPlayerSheet(
    track: Track,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: Long,
    duration: Long,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit
) {
    // Sound wave equalizer canvas parameters inside dynamic loop
    val barsCount = 20
    val animVal = rememberInfiniteTransition(label = "music_pulse")
    val pulseList = (0 until barsCount).map { i ->
        animVal.animateFloat(
            initialValue = 10f,
            targetValue = 60f + (sin(i.toDouble()) * 20f).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400 + (i * 30), easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$i"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .statusBarsPadding()
    ) {
        // Blurred aesthetic background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(32.dp)
                .drawWithContent {
                    drawContent()
                    drawRect(Color.Black.copy(alpha = 0.7f))
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Dismiss down arrow row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Fechar player",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Tocando agora",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Mais opções",
                        tint = Color.White
                    )
                }
            }

            // Large album image
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                AsyncImage(
                    model = track.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Title, artist names and favorite heart
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist,
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Animated Equalizer Visualizer Custom Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val widthScale = size.width / barsCount
                    for (i in 0 until barsCount) {
                        val barHeight = if (isPlaying) pulseList[i].value else 8f
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.85f),
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = i * widthScale + (widthScale / 4),
                                y = size.height/2 - barHeight/2
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                width = widthScale / 2,
                                height = barHeight
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            // Slider Seeking and positions text
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (duration > 0) position.toFloat() else 0f,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.5f),
                        thumbColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), color = Color.Gray, fontSize = 12.sp)
                    Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Control Buttons Row (Skip, play, loop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrev) {
                    SkipPreviousIcon(tint = Color.White, size = 36.dp)
                }

                // Main circular pulsing play container
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(28.dp))
                    } else {
                        if (isPlaying) {
                            PauseIcon(tint = Color.Black, size = 32.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Tocar",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onSkipNext) {
                    SkipNextIcon(tint = Color.White, size = 36.dp)
                }
            }

            // Dynamic Spotify-style Karaoke Lyrics view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D2F20))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "Letras",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        if (track.isPodcast) {
                            "\"Obrigado por sintonizar no Zispr Podcasts! Curta essa discussão incrível produzida especialmente no Android Studio com Jetpack Compose.\""
                        } else {
                            "\"Eu programo a noite inteira...\nBugs sumindo da minha tela...\nCom Kotlin e Compose, a vibe eleva...\""
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TrackListItem(
    track: Track,
    onLikeClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Gray.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                color = Color.LightGray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favoritar",
                tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PlaylistItemRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (playlist.isAiGenerated) {
                                listOf(Color(0xFF3DCE80), Color(0xFF15482D))
                            } else {
                                listOf(Color(0xFFD2E750), Color(0xFF2D2F20))
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                MusicNoteIcon(tint = Color.White, size = 24.dp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    playlist.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (playlist.isAiGenerated) "Playlist Gerada por IA" else "Playlist de Usuário",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Deletar", tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun DjBubble(message: DjMessage) {
    val isDj = message.sender == "AI_DJ"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = if (isDj) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = if (isDj) Alignment.Start else Alignment.End,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isDj) 0.dp else 16.dp,
                            bottomEnd = if (isDj) 16.dp else 0.dp
                        )
                    )
                    .background(if (isDj) Color(0xFF242424) else MaterialTheme.colorScheme.primary)
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isDj) 0.dp else 16.dp,
                            bottomEnd = if (isDj) 16.dp else 0.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isDj) Color.White else Color.Black,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            Text(
                text = if (isDj) "DJ Zispr" else "Você",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun DjSmartPlaylistCard(
    tracks: List<Track>,
    onSave: () -> Unit,
    onPlayAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF242424))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Mix Recomendado pelo DJ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            tracks.forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlayAll,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tocar Mix", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onSave,
                    border = BorderStroke(1.dp, Color.LightGray),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salvar na Lib", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FeaturedRowCard(playlist: Playlist, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF242424))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF2D2F20), Color(0xFF151610))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            MusicNoteIcon(tint = MaterialTheme.colorScheme.primary, size = 24.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            playlist.name,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
fun TrackRowCard(track: Track, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = track.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.2f))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            track.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            track.artist,
            color = Color.Gray,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GenreCard(title: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(4.dp)
    ) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun CategoryPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (selected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
