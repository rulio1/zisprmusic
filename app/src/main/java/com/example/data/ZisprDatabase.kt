package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Entities
// ==========================================

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val streamUrl: String,
    val imageUrl: String,
    val genre: String,
    val isFavorite: Boolean = false,
    val isPodcast: Boolean = false
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val imageUrl: String = "",
    val isAiGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrack(
    val playlistId: Int,
    val trackId: String
)

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trackId: String,
    val playedAt: Long = System.currentTimeMillis()
)

// ==========================================
// 2. Data Access Object (DAO)
// ==========================================

@Dao
interface ZisprDao {
    // Tracks
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): Track?

    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isPodcast = :isPodcast")
    fun getTracksByType(isPodcast: Boolean): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateTrackFavoriteStatus(id: String, isFavorite: Boolean)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Int): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    // Playlist Tracks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(playlistTrack: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: String)

    @Query("""
        SELECT t.* FROM tracks t 
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId 
        WHERE pt.playlistId = :playlistId
    """)
    fun getTracksForPlaylist(playlistId: Int): Flow<List<Track>>

    // Playback History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(history: PlaybackHistory)

    @Query("""
        SELECT DISTINCT t.id, t.title, t.artist, t.album, t.durationMs, t.streamUrl, t.imageUrl, t.genre, t.isFavorite, t.isPodcast 
        FROM tracks t 
        INNER JOIN playback_history h ON t.id = h.trackId 
        ORDER BY h.playedAt DESC LIMIT 30
    """)
    fun getRecentlyPlayedTracks(): Flow<List<Track>>
    
    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

// ==========================================
// 3. App Database
// ==========================================

@Database(
    entities = [Track::class, Playlist::class, PlaylistTrack::class, PlaybackHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zisprDao(): ZisprDao
}

// ==========================================
// 4. Repository Pattern
// ==========================================

class ZisprRepository(private val zisprDao: ZisprDao) {
    // Tracks
    val allTracks: Flow<List<Track>> = zisprDao.getAllTracks()
    val favoriteTracks: Flow<List<Track>> = zisprDao.getFavoriteTracks()
    val musicTracks: Flow<List<Track>> = zisprDao.getTracksByType(isPodcast = false)
    val podcastTracks: Flow<List<Track>> = zisprDao.getTracksByType(isPodcast = true)

    suspend fun getTrackById(id: String): Track? = zisprDao.getTrackById(id)

    suspend fun insertTrack(track: Track) = zisprDao.insertTrack(track)
    suspend fun insertTracks(tracks: List<Track>) = zisprDao.insertTracks(tracks)

    suspend fun toggleFavorite(id: String, isCurrentlyFavorite: Boolean) {
        zisprDao.updateTrackFavoriteStatus(id, !isCurrentlyFavorite)
    }

    // Playlists
    val allPlaylists: Flow<List<Playlist>> = zisprDao.getAllPlaylists()
    
    suspend fun getPlaylistById(id: Int): Playlist? = zisprDao.getPlaylistById(id)

    suspend fun createPlaylist(name: String, description: String, isAi: Boolean = false): Int {
        val playlist = Playlist(name = name, description = description, isAiGenerated = isAi)
        return zisprDao.insertPlaylist(playlist).toInt()
    }

    suspend fun deletePlaylist(playlistId: Int) = zisprDao.deletePlaylist(playlistId)

    suspend fun addTrackToPlaylist(playlistId: Int, trackId: String) {
        zisprDao.addTrackToPlaylist(PlaylistTrack(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: String) {
        zisprDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: Int): Flow<List<Track>> = zisprDao.getTracksForPlaylist(playlistId)

    // History
    val recentlyPlayed: Flow<List<Track>> = zisprDao.getRecentlyPlayedTracks()

    suspend fun addToHistory(trackId: String) {
        zisprDao.insertHistoryEntry(PlaybackHistory(trackId = trackId))
    }
    
    suspend fun clearHistory() = zisprDao.clearHistory()
}
