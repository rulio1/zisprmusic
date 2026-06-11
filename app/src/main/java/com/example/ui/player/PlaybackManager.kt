package com.example.ui.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import com.example.data.Track
import com.example.data.ZisprRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class PlaybackManager(
    private val context: Context,
    private val repository: ZisprRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _trackDuration = MutableStateFlow(0L)
    val trackDuration: StateFlow<Long> = _trackDuration.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        setupMediaPlayer()
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { mp ->
                _isLoading.value = false
                _trackDuration.value = mp.duration.toLong()
                mp.start()
                _isPlaying.value = true
                startProgressTracker()
            }
            setOnCompletionListener {
                _isPlaying.value = false
                stopProgressTracker()
                skipToNext()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("PlaybackManager", "MediaPlayer Error: what=$what extra=$extra")
                _isLoading.value = false
                _isPlaying.value = false
                false
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = emptyList()) {
        if (newQueue.isNotEmpty()) {
            _queue.value = newQueue
        }
        
        // Log playback to Room history in background
        scope.launch(Dispatchers.IO) {
            repository.addToHistory(track.id)
        }

        _currentTrack.value = track
        _currentPosition.value = 0L
        _trackDuration.value = track.durationMs

        mediaPlayer?.let { player ->
            try {
                player.reset()
                _isLoading.value = true
                _isPlaying.value = false
                player.setDataSource(track.streamUrl)
                player.prepareAsync() // Prepares in background, trigger prepared listener when done
            } catch (e: IOException) {
                Log.e("PlaybackManager", "Error setting data source", e)
                _isLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        val current = _currentTrack.value ?: return

        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressTracker()
        } else {
            if (_isPlaying.value == false && _currentPosition.value > 0L) {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            } else {
                // If it wasn't playing and had no progress, start playing
                playTrack(current)
            }
        }
    }

    fun skipToNext() {
        val tracks = _queue.value
        val current = _currentTrack.value ?: return
        if (tracks.isEmpty()) return

        val currentIndex = tracks.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < tracks.size - 1) {
            playTrack(tracks[currentIndex + 1])
        } else if (tracks.isNotEmpty()) {
            // Loop back to start
            playTrack(tracks[0])
        }
    }

    fun skipToPrevious() {
        val tracks = _queue.value
        val current = _currentTrack.value ?: return
        if (tracks.isEmpty()) return

        // If simple position is > 3s, restart track
        if (_currentPosition.value > 3000L) {
            seekTo(0)
            return
        }

        val currentIndex = tracks.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex > 0) {
            playTrack(tracks[currentIndex - 1])
        } else if (tracks.isNotEmpty()) {
            // Wrap around to end
            playTrack(tracks[tracks.size - 1])
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs.toInt())
                _currentPosition.value = positionMs
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Error seeking", e)
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition.toLong()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        scope.cancel()
        mediaPlayer?.let { player ->
            try {
                player.reset()
                player.release()
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Error releasing player", e)
            }
        }
        mediaPlayer = null
    }
}
