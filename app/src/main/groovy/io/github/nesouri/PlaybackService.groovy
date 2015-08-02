package io.github.nesouri

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import groovy.transform.CompileStatic

import static android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE

@CompileStatic
class PlaybackService extends Service {
    static String TAG = PlaybackService.class.name

    @CompileStatic
    class PlaybackBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this
        }
    }

    MediaSessionCompat mediaSession
    PlaybackNotification mediaNotificationManager

    IBinder playbackBinder = new PlaybackBinder()

    PlaybackWorker worker;

    AudioManager.OnAudioFocusChangeListener audiofocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        void onAudioFocusChange(int focusChange) {
        }
    }

    @Override
    void onCreate() {
        super.onCreate()

        def eventReceiver = new ComponentName(packageName, PlaybackNotification.class.name)
        Intent bcastIntent = new Intent(BuildConfig.APPLICATION_ID)
        def buttonReceiverIntent = PendingIntent.getBroadcast(this, 0x2A03, bcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        mediaSession = new MediaSessionCompat(this, "nesouri-session", eventReceiver, buttonReceiverIntent)
        mediaSession.playbackState = new PlaybackStateCompat.Builder()
                .setState(STATE_NONE, PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()

        mediaNotificationManager = new PlaybackNotification(this, mediaSession, compatNotificationManager)

        final AudioManager am = this.getSystemService(AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus(audiofocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        worker = new io.github.nesouri.PlaybackWorker(this, mediaSession)
        worker.start();
    }

    @Override
    void onDestroy() {
        super.onDestroy()
        worker.stop()

        AudioManager am = this.getSystemService(AUDIO_SERVICE) as AudioManager
        am.abandonAudioFocus(audiofocusListener)
    }

    @Override
    IBinder onBind(Intent intent) {
        return playbackBinder
    }

    MediaSessionCompat.Token getSessionToken() {
        return mediaSession.sessionToken
    }
}