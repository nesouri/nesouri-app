package io.github.nesouri

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import groovy.transform.CompileStatic
import io.github.nesouri.fragments.PlaybackControl

@CompileStatic
class PlaybackServiceConnection implements ServiceConnection {
    static String TAG = PlaybackServiceConnection.class.name

    private final Context context
    private PlaybackService playbackService
    MediaControllerCompat mediaController
    private PlaybackControl playbackControl

    PlaybackServiceConnection(Context context) {
        this.context = context
    }

    @Override
    void onServiceConnected(ComponentName name, IBinder service) {
        def binder = (PlaybackService.PlaybackBinder) service
        playbackService = binder.getService()
        mediaController = new MediaControllerCompat(context, playbackService.sessionToken)
        if (playbackControl != null)
            playbackControl.onMediaControllerConnected(mediaController)
    }

    @Override
    void onServiceDisconnected(ComponentName name) {
        mediaController = null
        playbackService = null
        if (playbackControl != null)
            playbackControl.onMediaControllerDisconnected()
    }

    PlaybackControl dispatch(PlaybackControl playbackControl) {
        if (mediaController != null && playbackControl != null)
            playbackControl.onMediaControllerConnected(mediaController)
        return playbackControl
    }

    void setPlaybackControl(PlaybackControl playbackControl) {
        if (this.playbackControl != null)
            this.playbackControl.onMediaControllerDisconnected()
        this.playbackControl = dispatch(playbackControl)
    }
}