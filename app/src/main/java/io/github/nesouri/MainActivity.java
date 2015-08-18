package io.github.nesouri;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import io.github.nesouri.PlaybackServiceConnection.PlaybackServiceConnectionListener;

import static android.media.session.PlaybackState.STATE_ERROR;
import static android.media.session.PlaybackState.STATE_NONE;
import static android.media.session.PlaybackState.STATE_STOPPED;
import static io.github.nesouri.Util.findFragmentById;

public class MainActivity extends AppCompatActivity implements PlaybackServiceConnectionListener {
	private static final String TAG = MainActivity.class.getName();

	private PlaybackServiceConnection serviceConnection;
	private MediaControllerCompat mediaController;

	public PlaybackServiceConnection getServiceConnection() {
		return serviceConnection;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		serviceConnection = new PlaybackServiceConnection(token -> new MediaControllerCompat(this, token));

		// Util.enableStrictMode()

		final Intent serviceIntent = new Intent(this, PlaybackService.class);
		startService(serviceIntent);
		bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

		setContentView(R.layout.activity_main);

		if (savedInstanceState == null)
			Navigation.openGamesList(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		Navigation.hidePlaybackControls(this);
		serviceConnection.registerListener(this);
		serviceConnection.registerListener(findFragmentById(this, R.id.fragment_playback_controls));
	}

	@Override
	public void onStop() {
		super.onStop();
		serviceConnection.unregisterListener(this);
		serviceConnection.unregisterListener(findFragmentById(this, R.id.fragment_playback_controls));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.action_settings)
			return true;
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPlaybackServiceConnect(final MediaControllerCompat mediaControllerCompat) {
		mediaController = mediaControllerCompat;
		mediaController.registerCallback(mediaControllerCallback);
		if (mediaController.getMetadata() != null)
			mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
		if (mediaController.getPlaybackState() != null)
			mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
	}

	@Override
	public void onPlaybackServiceDisconnect() {
		mediaController.unregisterCallback(mediaControllerCallback);
		mediaController = null;
	}

	private MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {

		private boolean isPlaying() {
			final PlaybackStateCompat playbackState = mediaController.getPlaybackState();
			if (playbackState == null)
				return false;
			switch (playbackState.getState()) {
				case STATE_NONE:
				case STATE_ERROR:
				case STATE_STOPPED:
					return false;
				default:
					return true;
			}
		}

		private void updateVisibility() {
			if (isPlaying() && mediaController.getMetadata() != null)
				Navigation.showPlaybackControls(MainActivity.this);
			else
				Navigation.hidePlaybackControls(MainActivity.this);
		}

		@Override
		public void onPlaybackStateChanged(final PlaybackStateCompat state) {
			super.onPlaybackStateChanged(state);
			updateVisibility();
		}

		@Override
		public void onMetadataChanged(final MediaMetadataCompat metadata) {
			super.onMetadataChanged(metadata);
			updateVisibility();
		}
	};

	public MediaControllerCompat.TransportControls getTransportControls() {
		if (mediaController != null)
			return mediaController.getTransportControls();
		return null;
	}

}