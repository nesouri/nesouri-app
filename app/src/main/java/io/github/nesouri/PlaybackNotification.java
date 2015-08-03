package io.github.nesouri;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static io.github.nesouri.Util.unlessNull;

public class PlaybackNotification extends BroadcastReceiver {
	static final String TAG = PlaybackNotification.class.getName();

	static final String ACTION_PREV = "io.github.nesouri.next";
	static final String ACTION_NEXT = "io.github.nesouri.prev";
	static final String ACTION_PAUSE = "io.github.nesouri.pause";
	static final String ACTION_PLAY = "io.github.nesouri.play";

	static final int NOTIFICATION_ID = 6502;
	static final int REQUEST_CODE = 0x2A03;

	final Service service;
	final MediaSessionCompat mediaSession;
	final NotificationManagerCompat notificationManager;

	final PendingIntent prevIntent;
	final PendingIntent nextIntent;
	final PendingIntent playIntent;
	final PendingIntent pauseIntent;

	private MediaMetadataCompat metadata;

	private static PendingIntent createIntent(final Context context, final String action) {
		final Intent intent = new Intent(action).setPackage(context.getPackageName());
		return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private final MediaControllerCompat.Callback callbacks = new MediaControllerCompat.Callback() {
		@Override
		public void onPlaybackStateChanged(final PlaybackStateCompat state) {
			super.onPlaybackStateChanged(state);
			createNotification();
		}

		@Override
		public void onMetadataChanged(final MediaMetadataCompat metadata) {
			super.onMetadataChanged(metadata);
			PlaybackNotification.this.metadata = metadata;
			createNotification();
		}
	};

	public PlaybackNotification(final Service svc, final MediaSessionCompat ses, final NotificationManagerCompat mgr) {
		this.service = svc;
		this.mediaSession = ses;
		this.notificationManager = mgr;

		prevIntent = createIntent(svc, ACTION_PREV);
		nextIntent = createIntent(svc, ACTION_NEXT);
		pauseIntent = createIntent(svc, ACTION_PAUSE);
		playIntent = createIntent(svc, ACTION_PLAY);

		mgr.cancelAll();

		mediaSession.getController().registerCallback(callbacks);
	}

	private PendingIntent createContentIntent() {
		final Intent main = Intent.makeMainActivity(new ComponentName(service.getPackageName(), MainActivity.class.getName()));
		return PendingIntent.getActivity(service, REQUEST_CODE, main, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private Action createPlayPauseAction() {
		switch (mediaSession.getController().getPlaybackState().getState()) {
			case STATE_PLAYING:
			case STATE_SKIPPING_TO_NEXT:
			case STATE_SKIPPING_TO_PREVIOUS:
				return new Action(R.drawable.ic_stat_av_pause, "Pause", pauseIntent);
			default:
				return new Action(R.drawable.ic_stat_av_play_arrow, "Play", playIntent);
		}
	}

	private void createNotification() {
		final Notification notification = new NotificationCompat.Builder(service)
				.addAction(R.drawable.ic_stat_av_skip_previous, "Prev", prevIntent)
				.addAction(createPlayPauseAction())
				.addAction(R.drawable.ic_stat_av_skip_next, "Next", nextIntent)
				.setStyle(new NotificationCompat.MediaStyle()
						          .setShowActionsInCompactView(new int[] {1, 2})
						          .setMediaSession(mediaSession.getSessionToken()))
				.setSmallIcon(R.drawable.ic_stat_av_play_arrow)
				.setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.mipmap.ic_launcher))
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setShowWhen(false)
				.setContentIntent(createContentIntent())
				.setContentTitle(unlessNull(metadata, m -> m.getText(MediaMetadataCompat.METADATA_KEY_TITLE)))
				.setContentText(unlessNull(metadata, m -> m.getText(MediaMetadataCompat.METADATA_KEY_ALBUM)))
				.setSubText(unlessNull(metadata, m -> m.getText(MediaMetadataCompat.METADATA_KEY_DATE)))
				.setOngoing(mediaSession.getController().getPlaybackState().getState() == STATE_PLAYING)
				.build();

		final IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_PLAY);
		filter.addAction(ACTION_PAUSE);
		filter.addAction(ACTION_PREV);
		filter.addAction(ACTION_NEXT);

		service.registerReceiver(this, filter);
		service.startForeground(NOTIFICATION_ID, notification);
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		switch (intent.getAction()) {
			case ACTION_PLAY:
				mediaSession.getController().getTransportControls().play();
				break;
			case ACTION_PAUSE:
				mediaSession.getController().getTransportControls().pause();
				break;
			case ACTION_PREV:
				mediaSession.getController().getTransportControls().skipToPrevious();
				break;
			case ACTION_NEXT:
				mediaSession.getController().getTransportControls().skipToNext();
				break;
		}
	}
}