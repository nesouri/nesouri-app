package io.github.nesouri;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import io.github.nesouri.fragments.PlaybackControl;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getName();

	private final PlaybackServiceConnection serviceConnection = new PlaybackServiceConnection(this);

	public PlaybackServiceConnection getServiceConnection() {
		return serviceConnection;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Util.enableStrictMode()

		bindService(new Intent(this, PlaybackService.class), serviceConnection, BIND_AUTO_CREATE);

		setContentView(R.layout.activity_main);

		if (savedInstanceState == null)
			Navigation.openGamesList(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		final PlaybackControl fragment = Util.findFragmentById(this, R.id.fragment_playback_controls);
		serviceConnection.setPlaybackControl(fragment);
		Navigation.hidePlaybackControls(this, R.id.fragment_playback_controls);
	}

	@Override
	public void onStop() {
		super.onStop();
		serviceConnection.setPlaybackControl(null);

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
}