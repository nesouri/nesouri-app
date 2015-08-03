package io.github.nesouri;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import static android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;

public class PlaybackService extends Service {
    private static final String TAG = PlaybackService.class.getName();

    public class PlaybackBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    MediaSessionCompat mediaSession;
    PlaybackNotification mediaNotificationManager;

    IBinder playbackBinder = new PlaybackBinder();

    PlaybackWorker worker;

    AudioManager.OnAudioFocusChangeListener audiofocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        final ComponentName eventReceiver = new ComponentName(getPackageName(), PlaybackNotification.class.getName());
        Intent bcastIntent = new Intent(BuildConfig.APPLICATION_ID);
        final PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(this, 0x2A03, bcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mediaSession = new MediaSessionCompat(this, "nesouri-session", eventReceiver, buttonReceiverIntent);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(STATE_NONE, PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build());

        mediaNotificationManager = new PlaybackNotification(this, mediaSession, NotificationManagerCompat.from(this.getApplicationContext()));

        final AudioManager am = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        am.requestAudioFocus(audiofocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        worker = new io.github.nesouri.PlaybackWorker(this, mediaSession);
        worker.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            worker.stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        AudioManager am = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        am.abandonAudioFocus(audiofocusListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return playbackBinder;
    }

    public MediaSessionCompat.Token getSessionToken() {
        return mediaSession.getSessionToken();
    }
}