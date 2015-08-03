package io.github.nesouri;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;

import io.github.nesouri.fragments.PlaybackControl;

public class PlaybackServiceConnection implements ServiceConnection {
	private static String TAG = PlaybackServiceConnection.class.getName();

	private final Context context;
	private PlaybackService playbackService;
	public MediaControllerCompat mediaController;
	private PlaybackControl playbackControl;

	public PlaybackServiceConnection(Context context) {
		this.context = context;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		final PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
		playbackService = binder.getService();
		try {
			mediaController = new MediaControllerCompat(context, playbackService.getSessionToken());
			if (playbackControl != null)
				playbackControl.onMediaControllerConnected(mediaController);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mediaController = null;
		playbackService = null;
		if (playbackControl != null)
			playbackControl.onMediaControllerDisconnected();
	}

	private PlaybackControl dispatch(PlaybackControl playbackControl) {
		if (mediaController != null && playbackControl != null)
			playbackControl.onMediaControllerConnected(mediaController);
		return playbackControl;
	}

	public void setPlaybackControl(PlaybackControl playbackControl) {
		if (this.playbackControl != null)
			this.playbackControl.onMediaControllerDisconnected();
		this.playbackControl = dispatch(playbackControl);
	}
}