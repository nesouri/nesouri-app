package io.github.nesouri.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.nesouri.PlaybackServiceConnection.PlaybackServiceConnectionListener;
import io.github.nesouri.R;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static io.github.nesouri.Util.isNullOrEmpty;

public class PlaybackControl extends Fragment implements PlaybackServiceConnectionListener {
	static final String TAG = PlaybackControl.class.getName();

	@Bind(R.id.play_pause)
	ImageButton playPauseButton;

	@Bind(R.id.title)
	TextView titleText;

	@Bind(R.id.game)
	TextView gameText;

	private MediaControllerCompat mediaController;

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_playback_controls, container, false);
		ButterKnife.bind(this, view);
		return view;
	}

	@Override
	public void onPlaybackServiceConnect(final MediaControllerCompat mediaControllerCompat) {
		mediaController = mediaControllerCompat;
		mediaController.registerCallback(mediaControllerCallbacks);
		if (mediaController.getMetadata() != null)
			mediaControllerCallbacks.onMetadataChanged(mediaController.getMetadata());
		if (mediaController.getPlaybackState() != null)
			mediaControllerCallbacks.onPlaybackStateChanged(mediaController.getPlaybackState());
	}

	@Override
	public void onPlaybackServiceDisconnect() {
		mediaController.unregisterCallback(mediaControllerCallbacks);
		mediaController = null;
	}

	@OnClick(R.id.play_pause)
	public void onButtonClicked() {
		if (mediaController.getPlaybackState().getState() == STATE_PLAYING)
			mediaController.getTransportControls().pause();
		else
			mediaController.getTransportControls().play();
	}

	private final MediaControllerCompat.Callback mediaControllerCallbacks = new MediaControllerCompat.Callback() {
		@Override
		public void onPlaybackStateChanged(final PlaybackStateCompat playbackState) {
			super.onPlaybackStateChanged(playbackState);
			switch (mediaController.getPlaybackState().getState()) {
				case STATE_PLAYING:
				case STATE_SKIPPING_TO_NEXT:
				case STATE_SKIPPING_TO_PREVIOUS:
					playPauseButton.setImageResource(R.drawable.ic_stat_av_pause_circle_outline);
					break;
				default:
					playPauseButton.setImageResource(R.drawable.ic_stat_av_play_circle_outline);
			}
		}

		private CharSequence formatTitle(final MediaMetadataCompat metadata) {
			final CharSequence title = metadata.getText(METADATA_KEY_TITLE);
			if (isNullOrEmpty(title))
				return String.format("Track %d", metadata.getLong(METADATA_KEY_TRACK_NUMBER));
			return title;
		}

		@Override
		public void onMetadataChanged(final MediaMetadataCompat metadata) {
			super.onMetadataChanged(metadata);
			titleText.setText(formatTitle(metadata));
			gameText.setText(metadata.getText(METADATA_KEY_ALBUM));
		}
	};
}