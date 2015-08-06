package io.github.nesouri;

import android.content.Intent;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import io.github.nesouri.PlaybackServiceConnection.PlaybackServiceConnectionListener;

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

		bindService(new Intent(this, PlaybackService.class), serviceConnection, BIND_AUTO_CREATE);

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
	}

	@Override
	public void onPlaybackServiceDisconnect() {
		mediaController.unregisterCallback(mediaControllerCallback);
		mediaController = null;
	}

	private MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
		@Override
		public void onPlaybackStateChanged(final PlaybackStateCompat state) {
			super.onPlaybackStateChanged(state);
			switch (state.getState()) {
				case PlaybackState.STATE_NONE:
				case PlaybackState.STATE_ERROR:
					Navigation.hidePlaybackControls(MainActivity.this);
					break;
				default:
					if (mediaController != null)
						Navigation.showPlaybackControls(MainActivity.this);
			}
		}
	};

	public MediaControllerCompat.TransportControls getTransportControls() {
		if (mediaController != null)
			return mediaController.getTransportControls();
		return null;
	}

}