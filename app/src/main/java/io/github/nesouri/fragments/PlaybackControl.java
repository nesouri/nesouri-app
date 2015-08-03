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
import io.github.nesouri.R;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;

public class PlaybackControl extends Fragment {
	static final String TAG = PlaybackControl.class.getName();

	@Bind(R.id.play_pause)
	ImageButton playPauseButton;

	@Bind(R.id.title)
	TextView titleText;

	@Bind(R.id.game)
	TextView gameText;

	MediaControllerCompat mediaController;

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_playback_controls, container, false);
		ButterKnife.bind(this, view);
		return view;
	}

	@OnClick(R.id.play_pause)
	public void onButtonClicked() {
		if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING)
			mediaController.getTransportControls().pause();
		else
			mediaController.getTransportControls().play();
	}

	public void onMediaControllerConnected(final MediaControllerCompat mediaController) {
		this.mediaController = mediaController;
		this.mediaController.registerCallback(callbacks);
	}

	public void onMediaControllerDisconnected() {
		this.mediaController.unregisterCallback(callbacks);
		this.mediaController = null;
	}

	private final MediaControllerCompat.Callback callbacks = new MediaControllerCompat.Callback() {
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

		@Override
		public void onMetadataChanged(final MediaMetadataCompat metadata) {
			super.onMetadataChanged(metadata);
			final CharSequence title = metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE);
			if (title == null || title.length() == 0) {
				titleText.setText(String.format("Track %d", metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)));
			} else {
				titleText.setText(title);
			}
			gameText.setText(metadata.getText(MediaMetadataCompat.METADATA_KEY_ALBUM));
		}
	};
}