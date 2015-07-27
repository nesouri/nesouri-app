package io.github.nesouri;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.github.nesouri.engine.EngineType;
import io.github.nesouri.engine.GameMusicEmu;

import static android.media.AudioFormat.CHANNEL_OUT_STEREO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioTrack.MODE_STREAM;
import static android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static java.lang.Thread.currentThread;

public class PlaybackWorker implements Runnable {
    private static final String TAG = PlaybackWorker.class.getName();

    private static final int SAMPLE_RATE = 44100;
    private static final int OUT_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT);
    private static final int IN_BUFFER_SIZE = OUT_BUFFER_SIZE * 2;

    private final AudioTrack track;
    private final Context context;
    private final MediaSessionCompat mediaSession;
    private volatile boolean playing = false;

    public PlaybackWorker(Context context, MediaSessionCompat mediaSession) {
        this.track = new AudioTrack(STREAM_MUSIC, SAMPLE_RATE, CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT, OUT_BUFFER_SIZE, MODE_STREAM);
        this.context = context;
        this.mediaSession = mediaSession;
        this.mediaSession.setCallback(callbacks);
        track.play();
    }

    private static byte[] read(final Resources resources, final int resourceId) throws IOException {
        try (final InputStream is = resources.openRawResource(resourceId)) {
            final byte[] buffer = new byte[4096];
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            while (is.read(buffer) > 0)
                os.write(buffer);
            return os.toByteArray();
        }
    }

    @Override
    public void run() {
        try {
            final byte[] data = read(context.getResources(), R.raw.silius);
            GameMusicEmu emu = GameMusicEmu.create(data, SAMPLE_RATE);

            final int trackCount = emu.trackCount();
            int position = 0;
            emu.track(position);

            final byte[] bytes = new byte[IN_BUFFER_SIZE];

            while (!currentThread().isInterrupted()) {
                switch (mediaSession.getController().getPlaybackState().getState()) {
                    case STATE_PLAYING:
                        if (emu.eof()) {
                            if (position != (trackCount - 1)) {
                                position += 1;
                                emu.track(position);
                            } else {
                                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                                        .setState(STATE_STOPPED, PLAYBACK_POSITION_UNKNOWN, 1.0f)
                                        .build());
                            }
                        } else {
                            final int bytesRead = emu.read(bytes);
                            track.write(bytes, 0, bytesRead);
                        }
                        break;
                    case STATE_SKIPPING_TO_PREVIOUS:
                    case STATE_SKIPPING_TO_NEXT:
                        final int offset = mediaSession.getController().getPlaybackState().getState() == STATE_SKIPPING_TO_NEXT ? 1 : -1;
                        position = Math.max(0, Math.min(trackCount - 1, position + offset));
                        emu.track(position);
                        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                                .setState(STATE_PLAYING, PLAYBACK_POSITION_UNKNOWN, 1.0f)
                                .build());
                        break;
                    default:
                        Thread.sleep(100);

                }
            }
        } catch (InterruptedException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    final MediaSessionCompat.Callback callbacks = new MediaSessionCompat.Callback() {
        @Override
        public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.d(TAG, "Media Id: " + mediaId + " Extras: " + extras);
            // TODO: Enqueue whole track, and jump to position
        }

        @Override
        public void onPause() {
            super.onPause();
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(STATE_PAUSED, PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build());
        }

        @Override
        public void onPlay() {
            super.onPlay();
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(STATE_PLAYING, PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build());
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(STATE_SKIPPING_TO_NEXT, PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build());
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(STATE_SKIPPING_TO_PREVIOUS, PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build());
        }
    };
}
