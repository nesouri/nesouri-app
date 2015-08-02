package io.github.nesouri;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.concurrent.Callable;

import io.github.nesouri.engine.EngineType;
import io.github.nesouri.engine.GameMusicEmu;

import static android.media.AudioFormat.CHANNEL_OUT_STEREO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioTrack.MODE_STREAM;
import static java.io.File.createTempFile;

public class PlaybackWorker implements Runnable {
    private static final String TAG = PlaybackWorker.class.getName();

    private static class Item {
        private final int gameId;
        private final int track;

        public Item(final int gameId, final int track) {
            this.gameId = gameId;
            this.track = track;
        }

        public Item next() {
            return new Item(gameId, track + 1);
        }

        public Item prev() {
            return new Item(gameId, track - 1);
        }

        public int gameId() {
            return gameId;
        }

        public int track() {
            return track;
        }
    }

    private static final int SAMPLE_RATE = 44100;
    private static final int OUT_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT);
    private static final int IN_BUFFER_SIZE = OUT_BUFFER_SIZE * 2;
    private final AudioTrack audioTrack;
    private GameMusicEmu engine;
    private Thread thread;
    private final Context context;
    private final MediaSessionCompat mediaSession;

    private final Object lock = new Object();

    private volatile PlayerState action;
    private volatile Item item = new Item(-1, -1);

    public PlaybackWorker(Context context, MediaSessionCompat mediaSession) {
        this.audioTrack = new AudioTrack(STREAM_MUSIC, SAMPLE_RATE, CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT, OUT_BUFFER_SIZE, MODE_STREAM);
        this.context = context;
        this.mediaSession = mediaSession;
        this.mediaSession.setCallback(callbacks);
    }

    public void start() {
        if (this.thread != null)
            throw new IllegalStateException("PlaybackWorker already started");

        this.engine = GameMusicEmu.create(EngineType.NSF, SAMPLE_RATE);

        this.action = new IdleState();

        this.thread = new Thread(this, "Nesouri-PlaybackWorker");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void stop() throws InterruptedException {
        if (this.thread == null)
            throw new IllegalStateException("PlaybackWorker already stopped");

        this.thread.interrupt();
        this.thread.join();
        this.thread = null;

        this.engine.close();
        this.engine = null;
    }

    @Override
    public void run() {
        while (true) {
            try {
                final PlayerState next = this.action.call();
                Log.d(TAG, "Transitioning from " + this.action + " -> " + next);
                this.action = next;
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                this.action = new ErrorState(e);
            }
        }
    }

    abstract class PlayerState implements Callable<PlayerState> {
        private volatile PlayerState next = this;

        void cancel(final PlayerState next) {
            if (this != PlaybackWorker.this.action) {
                PlaybackWorker.this.action.cancel(next);
            } else {
                this.next = next;
                synchronized (PlaybackWorker.this.lock) {
                    PlaybackWorker.this.lock.notifyAll();
                }
            }
        }

        boolean cancelled() {
            return next != this;
        }

        @Override
        public PlayerState call() throws Exception {
            process();
            return next;
        }

        public void transition(final PlayerState state) {
            next = state;
        }

        abstract void process() throws Exception;
    }

    class IdleState extends PlayerState {
        @Override
        public void process() throws Exception {
            mediaSession.setPlaybackState(PlaybackStates.none());
            synchronized (PlaybackWorker.this.lock) {
                PlaybackWorker.this.lock.wait();
            }
        }

        @Override
        public String toString() {
            return "IdleState";
        }
    }

    class ErrorState extends PlayerState {
        private final Exception exception;

        ErrorState(final Exception exception) {
            this.exception = exception;
        }

        @Override
        void process() throws Exception {
            mediaSession.setPlaybackState(PlaybackStates.error(exception.getMessage()));
            synchronized (PlaybackWorker.this.lock) {
                PlaybackWorker.this.lock.wait();
            }
        }

        @Override
        public String toString() {
            return String.format("ErrorState[%s]", exception.getMessage());
        }
    }

    class PreparingState extends PlayerState {
        private final Item item;

        PreparingState(final Item item) {
            this.item = item;
        }

        private URLConnection connect(final String filename) throws IOException {
            final String[] parts = filename.split("/");
            final String encoded = URLEncoder.encode(parts[1], "UTF-8").replace("+", "%20");

            final URL url = new URL("http://nsf.joshw.info/" + parts[0] + "/" + encoded);

            final URLConnection conn = url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.connect();

            return conn;
        }

        private void fetch(final String filename) throws IOException {
            mediaSession.setPlaybackState(PlaybackStates.connecting());
            final URLConnection conn = connect(filename);
            mediaSession.setPlaybackState(PlaybackStates.buffering());

            final File f = createTempFile("nesouri", null);

            try (final InputStream is = conn.getInputStream();
                 final FileOutputStream os = new FileOutputStream(f)) {
                final byte[] data = new byte[4096];

                while (!cancelled()) {
                    int read = is.read(data);
                    if (read < 0)
                        break;
                    os.write(data, 0, read);
                }

                engine.load(Util.extract7zip(f));
                if (item.track() < 0 || item.track() > engine.trackCount())
                    throw new IllegalStateException("track out of bounds: " + item.track());
            } finally {
                f.delete();
            }
        }

        @Override
        void process() throws Exception {
            final DatabaseHelper db = DatabaseHelper.getInstance(context);
            final Cursor cursor = db.queryTrackInfo(item.gameId(), item.track());
            cursor.moveToFirst();
            try {
                if (PlaybackWorker.this.item.gameId() != item.gameId())
                    fetch(cursor.getString(0));

                engine.track((int) item.track() - 1);

                MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                builder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, item.track());
                builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, cursor.getLong(1));
                builder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, cursor.getString(2));
                builder.putText(MediaMetadataCompat.METADATA_KEY_DATE, cursor.getString(3));
                builder.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, cursor.getLong(4));
                builder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, cursor.getString(5));
                builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, cursor.getLong(6));
                mediaSession.setMetadata(builder.build());

                PlaybackWorker.this.item = item;

                transition(new PlayingState());
            } finally {
                cursor.close();
            }
        }

        @Override
        public String toString() {
            return String.format("PreparingState[%d/%d]", item.gameId(), item.track());
        }
    }

    class PlayingState extends PlayerState {
        private final byte[] buffer = new byte[IN_BUFFER_SIZE];

        @Override
        void process() throws Exception {
            audioTrack.play();
            audioTrack.flush();

            mediaSession.setPlaybackState(PlaybackStates.playing());
            while (!cancelled() && !engine.eof()) {
                final int bytesRead = engine.read(buffer);
                audioTrack.write(buffer, 0, bytesRead);
            }

            if (engine.eof())
                transition(new StoppedState());
        }

        @Override
        public String toString() {
            return String.format("PlayingState[%d/%d]", item.gameId(), item.track());
        }
    }

    class PausedState extends PlayerState {
        @Override
        void process() throws Exception {
            audioTrack.pause();
            mediaSession.setPlaybackState(PlaybackStates.paused());
            synchronized (PlaybackWorker.this.lock) {
                PlaybackWorker.this.lock.wait();
            }
        }

        @Override
        public String toString() {
            return String.format("PausedState[%d/%d]", item.gameId(), item.track());
        }
    }

    class StoppedState extends PlayerState {
        @Override
        void process() throws Exception {
            audioTrack.pause();
            audioTrack.flush();
            audioTrack.stop();
            engine.track(item.track() - 1);
            mediaSession.setPlaybackState(PlaybackStates.stopped());
            synchronized (PlaybackWorker.this.lock) {
                PlaybackWorker.this.lock.wait();
            }
        }

        @Override
        public String toString() {
            return String.format("StopState[%d/%d]", item.gameId(), item.track());
        }
    }

    class SkippingState extends PlayerState {
        public static final int NEXT = 0;
        public static final int PREVIOUS = 1;

        private final int direction;

        SkippingState(final int direction) {
            this.direction = direction;
        }

        @Override
        void process() throws Exception {
            if (direction == NEXT) {
                mediaSession.setPlaybackState(PlaybackStates.skipToNext());
                action.transition(new PreparingState(PlaybackWorker.this.item.next()));
            } else {
                mediaSession.setPlaybackState(PlaybackStates.skipToPrevious());
                action.transition(new PreparingState(PlaybackWorker.this.item.prev()));
            }
        }

        @Override
        public String toString() {
            return String.format("SkippingState[%d/%d, %s]", item.gameId(), item.track(), direction == NEXT ? "next" : "previous");
        }
    }

    final MediaSessionCompat.Callback callbacks = new MediaSessionCompat.Callback() {
        @Override
        public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            final int gameId = Integer.parseInt(mediaId);
            final int track = extras.getInt("position");
            action.cancel(new PreparingState(new Item(gameId, track)));
        }

        @Override
        public void onPause() {
            super.onPause();
            action.cancel(new PausedState());
        }

        @Override
        public void onPlay() {
            super.onPlay();
            action.cancel(new PlayingState());
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            action.cancel(new SkippingState(SkippingState.NEXT));
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            action.cancel(new SkippingState(SkippingState.PREVIOUS));
        }
    };
}
