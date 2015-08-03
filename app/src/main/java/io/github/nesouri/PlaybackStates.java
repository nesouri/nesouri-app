package io.github.nesouri;

import android.support.v4.media.session.PlaybackStateCompat;

import static android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_CONNECTING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

public abstract class PlaybackStates {
    private static PlaybackStateCompat simple(int state) {
        return new PlaybackStateCompat.Builder()
                .setState(state, PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build();
    }

    public static PlaybackStateCompat playing() {
        return simple(STATE_PLAYING);
    }

    public static PlaybackStateCompat paused() {
        return simple(STATE_PAUSED);
    }

    public static PlaybackStateCompat stopped() {
        return simple(STATE_STOPPED);
    }

    public static PlaybackStateCompat none() {
        return simple(STATE_NONE);
    }

    public static PlaybackStateCompat error(final String message) {
        return new PlaybackStateCompat.Builder()
                .setState(STATE_ERROR, PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .setErrorMessage(message)
        .build();
    }

    public static PlaybackStateCompat connecting() {
        return simple(STATE_CONNECTING);
    }

    public static PlaybackStateCompat buffering() {
        return simple(STATE_BUFFERING);
    }

    public static PlaybackStateCompat skipToNext() {
        return simple(STATE_SKIPPING_TO_NEXT);
    }

    public static PlaybackStateCompat skipToPrevious() {
        return simple(STATE_SKIPPING_TO_PREVIOUS);
    }
}
