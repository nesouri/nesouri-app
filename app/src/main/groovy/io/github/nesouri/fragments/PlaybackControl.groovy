package io.github.nesouri.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.arasthel.swissknife.SwissKnife
import com.arasthel.swissknife.annotations.InjectView
import com.arasthel.swissknife.annotations.OnClick
import groovy.transform.CompileStatic
import io.github.nesouri.R

import static android.support.v4.media.session.PlaybackStateCompat.*

@CompileStatic
class PlaybackControl extends Fragment {
    static final String TAG = PlaybackControl.class.name

    @InjectView(R.id.play_pause)
    ImageButton playPauseButton

    MediaControllerCompat mediaController

    @Override
    View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playback_controls, container, false)
    }

    @Override
    void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState)
        SwissKnife.inject(this, view)
    }

    @OnClick(R.id.play_pause)
    void onButtonClicked() {
        if (mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
            mediaController.transportControls.pause()
        else
            mediaController.transportControls.play()
    }

    void onMediaControllerConnected(MediaControllerCompat mediaController) {
        this.mediaController = mediaController;
        this.mediaController.registerCallback(callbacks)
    }

    void onMediaControllerDisconnected() {
        this.mediaController.unregisterCallback(callbacks)
        this.mediaController = null;
    }

    MediaControllerCompat.Callback callbacks = new MediaControllerCompat.Callback() {
        @Override
        void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            super.onPlaybackStateChanged(playbackState)
            switch (mediaController.playbackState.state) {
                case STATE_PLAYING:
                case STATE_SKIPPING_TO_NEXT:
                case STATE_SKIPPING_TO_PREVIOUS:
                    playPauseButton.imageResource = R.drawable.ic_stat_av_pause_circle_outline
                    break
                default:
                    playPauseButton.imageResource = R.drawable.ic_stat_av_play_circle_outline
            }
        }

        @Override
        void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata)
            Log.d(TAG, "On metadata changed")
        }
    }
}