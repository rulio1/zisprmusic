package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import com.example.ui.player.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ZisprScreen {
    HOME,
    SEARCH,
    LIBRARY,
    AI_DJ
}

class ZisprViewModel(application: Application) : AndroidViewModel(application) {

    // Database initialization
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "zispr_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: ZisprRepository by lazy {
        ZisprRepository(database.zisprDao())
    }

    val playbackManager: PlaybackManager by lazy {
        PlaybackManager(application, repository)
    }

    private val geminiService = ZisprGeminiService()

    // Screen navigation
    private val _currentScreen = MutableStateFlow(ZisprScreen.HOME)
    val currentScreen: StateFlow<ZisprScreen> = _currentScreen.asStateFlow()

    // Database content trackers
    val allTracks: StateFlow<List<Track>> = repository.allTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val musicTracks: StateFlow<List<Track>> = repository.musicTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val podcastTracks: StateFlow<List<Track>> = repository.podcastTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyPlayed: StateFlow<List<Track>> = repository.recentlyPlayed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current selected item detail
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistTracks.asStateFlow()

    // Filter and search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _isPodcastFilter = MutableStateFlow<Boolean?>(null) // null = all, false = music, true = podcasts
    val isPodcastFilter: StateFlow<Boolean?> = _isPodcastFilter.asStateFlow()

    // AI DJ chat history
    private val _djMessages = MutableStateFlow<List<DjMessage>>(
        listOf(
            DjMessage(
                sender = "AI_DJ",
                text = "Fala dev! Sou o DJ Zispr, o seu mestre de cerimônias musical movido a IA. 🎧\n\nQue som você quer curtir hoje? Pode me pedir playlist para programar focado, batida relax de café, ou o que sua mente mandar. Manda bala!"
            )
        )
    )
    val djMessages: StateFlow<List<DjMessage>> = _djMessages.asStateFlow()

    private val _isDjLoading = MutableStateFlow(false)
    val isDjLoading: StateFlow<Boolean> = _isDjLoading.asStateFlow()

    private val _lastSuggestedTracks = MutableStateFlow<List<Track>>(emptyList())
    val lastSuggestedTracks: StateFlow<List<Track>> = _lastSuggestedTracks.asStateFlow()

    init {
        seedInitialDatabase()
    }

    private fun seedInitialDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            // Count tracks to see if database seeding is needed
            val isDbEmpty = database.zisprDao().getTrackById("sunset_drive") == null
            if (isDbEmpty) {
                // Populate default tracks
                val defaultTracks = listOf(
                    Track("sunset_drive", "Sunset Drive", "RetroVision", "Neon Horizons", 188000L, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop", "Synthwave"),
                    Track("neon_dreams", "Neon Dreams", "Vector Prime", "Grid Runner", 204000L, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&auto=format&fit=crop", "Electronic"),
                    Track("midnight_rain", "Midnight Rain", "Lofi Chillers", "Slow Coffee", 231000L, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?w=500&auto=format&fit=crop", "Lofi"),
                    Track("forest_whisper", "Forest Whisper", "Natura", "Organic Earth", 253000L, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "https://images.unsplash.com/photo-1448375240586-882707db888b?w=500&auto=format&fit=crop", "Ambient"),
                    Track("coding_rhythms", "Coding Rhythms", "ByteBeat", "Stack Overflow", 217000L, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500&auto=format&fit=crop", "Lofi"),
                    Track("cyberpunk_city", "Cyberpunk City", "Glitch Lord", "System Crash", 280000L, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500&auto=format&fit=crop", "Synthwave"),
                    Track("kotlin_standard", "O Padrão Kotlin", "Os Devs do Zispr", "Kotlin Talks", 302000L, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3", "https://images.unsplash.com/photo-1607799279861-4dd421887fb3?w=500&auto=format&fit=crop", "Podcast", isPodcast = true),
                    Track("devlife_weekly", "DevLife Semanal", "TechTalk Brasil", "Carreira de Dev", 345000L, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3", "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=500&auto=format&fit=crop", "Podcast", isPodcast = true)
                )
                repository.insertTracks(defaultTracks)

                // Seed some default playlists
                val playlist1Id = repository.createPlaylist("Foco no Código", "Sons sintetizados para concentrar e eliminar bugs complexos da mente.")
                val playlist2Id = repository.createPlaylist("Chill Vibes", "Batidas relaxantes e tranquilas para descontrair após um longo merge.")

                // Seed some default playlist track mapping
                repository.addTrackToPlaylist(playlist1Id, "neon_dreams")
                repository.addTrackToPlaylist(playlist1Id, "coding_rhythms")
                repository.addTrackToPlaylist(playlist1Id, "cyberpunk_city")

                repository.addTrackToPlaylist(playlist2Id, "midnight_rain")
                repository.addTrackToPlaylist(playlist2Id, "forest_whisper")
                repository.addTrackToPlaylist(playlist2Id, "coding_rhythms")
            }
        }
    }

    // Screen navigation setters
    fun navigateTo(screen: ZisprScreen) {
        _selectedPlaylist.value = null
        _selectedPlaylistTracks.value = emptyList()
        _currentScreen.value = screen
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        viewModelScope.launch(Dispatchers.IO) {
            repository.getTracksForPlaylist(playlist.id).collectLatest { tracks ->
                _selectedPlaylistTracks.value = tracks
            }
        }
    }

    fun unselectPlaylist() {
        _selectedPlaylist.value = null
        _selectedPlaylistTracks.value = emptyList()
    }

    // Liked songs, podcasts filter updates
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectGenre(genre: String?) {
        _selectedGenre.value = genre
    }

    fun setPodcastFilter(isPodcast: Boolean?) {
        _isPodcastFilter.value = isPodcast
    }

    // Toggle Favorite Action
    fun toggleFavorite(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(track.id, track.isFavorite)
        }
    }

    // Dynamic filtering for search
    fun getFilteredTracks(tracksList: List<Track>): List<Track> {
        val query = _searchQuery.value.trim().lowercase()
        val genre = _selectedGenre.value
        val podcastFilter = _isPodcastFilter.value

        return tracksList.filter { track ->
            val matchesQuery = if (query.isEmpty()) true else {
                track.title.lowercase().contains(query) ||
                track.artist.lowercase().contains(query) ||
                track.album.lowercase().contains(query) ||
                track.genre.lowercase().contains(query)
            }
            val matchesGenre = if (genre == null) true else track.genre.uppercase() == genre.uppercase()
            val matchesPodcast = if (podcastFilter == null) true else track.isPodcast == podcastFilter

            matchesQuery && matchesGenre && matchesPodcast
        }
    }

    // Playback control wrappers
    fun playSingleTrack(track: Track, fromList: List<Track>) {
        playbackManager.playTrack(track, fromList)
    }

    // AI DJ trigger
    fun sendDjMessage(prompt: String) {
        if (prompt.trim().isEmpty()) return

        // Append user prompt to list
        val userMessage = DjMessage(sender = "USER", text = prompt)
        _djMessages.value = _djMessages.value + userMessage
        _isDjLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val tracks = allTracks.value
            val response = geminiService.getDjResponse(prompt, tracks)

            withContext(Dispatchers.Main) {
                _isDjLoading.value = false
                
                // Append AI Response
                val djMessage = DjMessage(sender = "AI_DJ", text = response.message)
                _djMessages.value = _djMessages.value + djMessage

                // Find local tracks matching recommended IDs
                val recommended = tracks.filter { response.suggestedTrackIds.contains(it.id) }
                _lastSuggestedTracks.value = recommended
            }
        }
    }

    // Save DJ playlist locally
    fun saveDjPlaylist(name: String, description: String) {
        val recommendedTracks = _lastSuggestedTracks.value
        if (recommendedTracks.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val newPlaylistId = repository.createPlaylist(name, description, isAi = true)
            for (track in recommendedTracks) {
                repository.addTrackToPlaylist(newPlaylistId, track.id)
            }
            
            // Send back message of success from DJ
            withContext(Dispatchers.Main) {
                _djMessages.value = _djMessages.value + DjMessage(
                    sender = "AI_DJ",
                    text = "Perfeito, parceiro! Salvei a nossa playlist \"$name\" diretamente na sua Biblioteca do Zispr! 💾 Ficou irada d+, corre lá pra conferir!"
                )
                // Clear state
                _lastSuggestedTracks.value = emptyList()
            }
        }
    }

    // User creates a playlist
    fun createUserPlaylist(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createPlaylist(name, description, isAi = false)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Int, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}

data class DjMessage(
    val sender: String, // "USER" or "AI_DJ"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ZisprViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ZisprViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ZisprViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
