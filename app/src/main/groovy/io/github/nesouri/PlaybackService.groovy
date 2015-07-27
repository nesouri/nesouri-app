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

    Thread thread;

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

        thread = new Thread(new io.github.nesouri.PlaybackWorker(this, mediaSession))
        thread.start();

        /*

        https://developer.android.com/reference/android/media/MediaPlayer.html


    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                LogHelper.d(TAG, "Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    mService.startService(i);
                }
            }
        }
    };


    // Register when beginning to play:
    // https://github.com/googlesamples/android-UniversalMusicPlayer/blob/master/mobile/src/main/java/com/example/android/uamp/LocalPlayback.java#L152
    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mService.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    // Pause/Stop/Destroy etc deal with audiofocus as well
    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mService.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }
         */
    }

    @Override
    void onDestroy() {
        super.onDestroy()
        thread.interrupt()
        thread.join()

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