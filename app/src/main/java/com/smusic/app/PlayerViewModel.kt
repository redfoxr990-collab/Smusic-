package com.smusic.app

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: android.net.Uri,
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    var songs by mutableStateOf<List<Song>>(emptyList()); private set
    var loaded by mutableStateOf(false); private set
    var currentId by mutableStateOf<Long?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var shuffle by mutableStateOf(false); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var favorites by mutableStateOf<Set<Long>>(emptySet()); private set

    val currentSong: Song? get() = songs.firstOrNull { it.id == currentId }

    private var controller: MediaController? = null
    private var items: List<MediaItem> = emptyList()
    private var queueSet = false
    private val artCache = LruCache<Long, ImageBitmap>(40)
    private val prefs = app.getSharedPreferences("smusic", 0)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
    }

    init {
        favorites = prefs.getStringSet("fav", emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet()

        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = future.get()
            c.repeatMode = Player.REPEAT_MODE_ALL
            c.addListener(listener)
            controller = c
            sync(c)
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (true) {
                controller?.let { if (it.isPlaying) sync(it) }
                delay(300)
            }
        }
    }

    private fun sync(p: Player) {
        isPlaying = p.isPlaying
        shuffle = p.shuffleModeEnabled
        currentId = p.currentMediaItem?.mediaId?.toLongOrNull()
        duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: currentSong?.durationMs ?: 0L
        position = p.currentPosition
    }

    fun loadSongs() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { querySongs() }
            songs = list
            items = list.map {
                MediaItem.Builder()
                    .setMediaId(it.id.toString())
                    .setUri(it.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(it.title)
                            .setArtist(it.artist)
                            .setAlbumTitle(it.album)
                            .build()
                    )
                    .build()
            }
            queueSet = false
            loaded = true
        }
    }

    private fun querySongs(): List<Song> {
        val out = mutableListOf<Song>()
        val m = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
        )
        val sel = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        getApplication<Application>().contentResolver
            .query(m, proj, sel, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")
            ?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    fun clean(s: String?, fallback: String) =
                        if (s.isNullOrBlank() || s == "<unknown>") fallback else s
                    out += Song(
                        id = id,
                        title = clean(c.getString(1), "Unknown"),
                        artist = clean(c.getString(2), "Unknown artist"),
                        album = clean(c.getString(3), "Smusic"),
                        durationMs = c.getLong(4),
                        uri = ContentUris.withAppendedId(m, id),
                    )
                }
            }
        return out
    }

    suspend fun loadArt(song: Song): ImageBitmap? = withContext(Dispatchers.IO) {
        artCache.get(song.id)?.let { return@withContext it }
        try {
            getApplication<Application>().contentResolver
                .loadThumbnail(song.uri, Size(600, 600), null)
                .asImageBitmap()
                .also { artCache.put(song.id, it) }
        } catch (e: Exception) {
            null
        }
    }

    fun play(song: Song) {
        val c = controller ?: return
        val index = songs.indexOfFirst { it.id == song.id }
        if (index < 0) return
        if (!queueSet || c.mediaItemCount != items.size) {
            c.setMediaItems(items, index, 0L)
            queueSet = true
        } else {
            c.seekTo(index, 0L)
        }
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        when {
            c.mediaItemCount == 0 -> songs.firstOrNull()?.let { play(it) }
            c.isPlaying -> c.pause()
            else -> {
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                c.play()
            }
        }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun seekTo(fraction: Float) {
        val c = controller ?: return
        if (duration > 0) c.seekTo((duration * fraction).toLong())
        position = (duration * fraction).toLong()
    }

    fun toggleFavorite(id: Long) {
        favorites = if (id in favorites) favorites - id else favorites + id
        prefs.edit().putStringSet("fav", favorites.map { it.toString() }.toSet()).apply()
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        super.onCleared()
    }
}
