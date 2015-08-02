package io.github.nesouri

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.NotificationCompat
import groovy.transform.CompileStatic

import static android.support.v4.media.session.PlaybackStateCompat.*

@CompileStatic
class PlaybackNotification extends BroadcastReceiver {
    static final String TAG = PlaybackNotification.class.name

    static final String ACTION_PREV = "io.github.nesouri.next";
    static final String ACTION_NEXT = "io.github.nesouri.prev";
    static final String ACTION_PAUSE = "io.github.nesouri.pause";
    static final String ACTION_PLAY = "io.github.nesouri.play";

    static final int NOTIFICATION_ID = 6502
    static final int REQUEST_CODE = 0x2A03

    final Service service
    final MediaSessionCompat mediaSession
    final NotificationManagerCompat notificationManager

    final PendingIntent prevIntent
    final PendingIntent nextIntent
    final PendingIntent playIntent
    final PendingIntent pauseIntent

    MediaMetadataCompat metadata;

    static PendingIntent createIntent(final Context context, final String action) {
        final Intent intent = new Intent(action).setPackage(context.packageName)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    final MediaControllerCompat.Callback callbacks = new MediaControllerCompat.Callback() {
        @Override
        void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state)
            createNotification()
        }

        @Override
        void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata)
            PlaybackNotification.this.metadata = metadata
            createNotification()
        }
    }

    PlaybackNotification(final Service svc, final MediaSessionCompat ses, final NotificationManagerCompat mgr) {
        this.service = svc
        this.mediaSession = ses
        this.notificationManager = mgr

        prevIntent = createIntent(svc, ACTION_PREV)
        nextIntent = createIntent(svc, ACTION_NEXT)
        pauseIntent = createIntent(svc, ACTION_PAUSE)
        playIntent = createIntent(svc, ACTION_PLAY)

        mgr.cancelAll()

        mediaSession.controller.registerCallback(callbacks)
    }

    PendingIntent createContentIntent() {
        final Intent main = Intent.makeMainActivity(new ComponentName(service.packageName, MainActivity.class.name))
        return PendingIntent.getActivity(service, REQUEST_CODE, main, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    void createNotification() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(service)
        builder.addAction(R.drawable.ic_stat_av_skip_previous, "Prev", prevIntent)

        switch (mediaSession.controller.playbackState.state) {
            case STATE_PLAYING:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_SKIPPING_TO_PREVIOUS:
                builder.addAction(R.drawable.ic_stat_av_pause, "Pause", pauseIntent)
                break
            default:
                builder.addAction(R.drawable.ic_stat_av_play_arrow, "Play", playIntent)
        }

        builder.addAction(R.drawable.ic_stat_av_skip_next, "Next", nextIntent)

        builder.style = new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView([1, 2] as int[])
                .setMediaSession(mediaSession.sessionToken)
        builder.smallIcon = R.drawable.ic_stat_av_play_arrow
        builder.largeIcon = BitmapFactory.decodeResource(service.getResources(), R.mipmap.ic_launcher)
        builder.visibility = NotificationCompat.VISIBILITY_PUBLIC
        builder.showWhen = false
        builder.contentIntent = createContentIntent()
        if (metadata) {
            builder.contentTitle = metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE)
            builder.contentText = metadata.getText(MediaMetadataCompat.METADATA_KEY_ALBUM)
            builder.subText = metadata.getText(MediaMetadataCompat.METADATA_KEY_DATE)
        }
        builder.ongoing = mediaSession.controller.playbackState.state == STATE_PLAYING

        final IntentFilter filter = new IntentFilter()
        filter.addAction(ACTION_PLAY)
        filter.addAction(ACTION_PAUSE)
        filter.addAction(ACTION_PREV)
        filter.addAction(ACTION_NEXT)

        service.registerReceiver(this, filter)
        service.startForeground(NOTIFICATION_ID, builder.build())
    }

    @Override
    void onReceive(Context context, Intent intent) {
        switch (intent.action) {
            case ACTION_PLAY:
                mediaSession.controller.transportControls.play()
                break
            case ACTION_PAUSE:
                mediaSession.controller.transportControls.pause()
                break
            case ACTION_PREV:
                mediaSession.controller.transportControls.skipToPrevious()
                break
            case ACTION_NEXT:
                mediaSession.controller.transportControls.skipToNext()
                break
        }
    }
}
