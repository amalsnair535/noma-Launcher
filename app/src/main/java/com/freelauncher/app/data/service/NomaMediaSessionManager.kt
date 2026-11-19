package com.freelauncher.app.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaPlaybackInfo(
    val title: String = "No music playing",
    val artist: String = "Tap Play or open music app",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val hasActiveSession: Boolean = false,
    val hasNotificationAccess: Boolean = true
)

class NomaMediaSessionManager(private val context: Context) {

    private val _playbackInfo = MutableStateFlow(MediaPlaybackInfo())
    val playbackInfo: StateFlow<MediaPlaybackInfo> = _playbackInfo.asStateFlow()

    private var activeController: MediaController? = null

    private val mediaBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val track = intent.getStringExtra("track") ?: intent.getStringExtra("title") ?: intent.getStringExtra("song")
            val artist = intent.getStringExtra("artist") ?: intent.getStringExtra("albumartist")
            val playing = intent.getBooleanVariable("playing") ?: intent.getBooleanExtra("playstate", false)

            if (!track.isNullOrBlank()) {
                _playbackInfo.value = _playbackInfo.value.copy(
                    title = track,
                    artist = artist ?: _playbackInfo.value.artist,
                    isPlaying = playing,
                    hasActiveSession = true
                )
            }
        }

        private fun Intent.getBooleanVariable(name: String): Boolean? {
            return if (hasExtra(name)) getBooleanExtra(name, false) else null
        }
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata(metadata, activeController?.playbackState)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMetadata(activeController?.metadata, state)
        }
    }

    init {
        registerMediaReceiver()
    }

    private fun registerMediaReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction("com.android.music.metachanged")
                addAction("com.spotify.music.metadatachanged")
                addAction("com.htc.music.metachanged")
                addAction("com.real.IMP.metachanged")
                addAction("com.sonyericsson.music.metachanged")
                addAction("com.samsung.sec.android.music.metachanged")
            }
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                mediaBroadcastReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {}
    }

    fun updateMediaSession() {
        val permissionGranted = NomaNotificationListenerService.isPermissionGranted(context)
        if (!permissionGranted) {
            _playbackInfo.value = MediaPlaybackInfo(
                title = "Notification Access Needed",
                artist = "Grant permission for live track info",
                hasActiveSession = false,
                hasNotificationAccess = false
            )
            return
        }

        try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = NomaNotificationListenerService.getComponentName(context)
            val controllers = sessionManager?.getActiveSessions(componentName) ?: emptyList()
            val controller = controllers.firstOrNull()

            if (controller != activeController) {
                activeController?.unregisterCallback(callback)
                activeController = controller
                activeController?.registerCallback(callback)
            }

            if (controller != null) {
                updateMetadata(controller.metadata, controller.playbackState)
            } else {
                _playbackInfo.value = MediaPlaybackInfo(hasActiveSession = false, hasNotificationAccess = true)
            }
        } catch (e: Exception) {
            Log.e("NomaMediaSession", "Error fetching active sessions: ${e.message}")
            _playbackInfo.value = MediaPlaybackInfo(hasActiveSession = false, hasNotificationAccess = true)
        }
    }

    fun togglePlayPause() {
        val controller = activeController ?: return
        val isPlaying = activeController?.playbackState?.state == PlaybackState.STATE_PLAYING
        if (isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
        updateMediaSession()
    }

    fun skipToNext() {
        activeController?.transportControls?.skipToNext()
        updateMediaSession()
    }

    fun skipToPrevious() {
        activeController?.transportControls?.skipToPrevious()
        updateMediaSession()
    }

    private fun updateMetadata(metadata: MediaMetadata?, state: PlaybackState?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "No music playing"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: "Tap Play or open music app"
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING

        _playbackInfo.value = MediaPlaybackInfo(
            title = title,
            artist = artist,
            albumArt = albumArt,
            isPlaying = isPlaying,
            hasActiveSession = true,
            hasNotificationAccess = true
        )
    }

    fun release() {
        try {
            context.unregisterReceiver(mediaBroadcastReceiver)
            activeController?.unregisterCallback(callback)
            activeController = null
        } catch (_: Exception) {}
    }
}
