package io.github.nesouri;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import io.github.nesouri.fragments.GamesList;
import io.github.nesouri.fragments.TracksList;

public abstract class Navigation {
	public static void openGamesList(final FragmentActivity activity) {
		activity.getSupportFragmentManager().beginTransaction()
				.add(R.id.content_frame, new GamesList())
		.commit();
	}

	public static void openTrackList(final FragmentActivity activity, final long gameId) {
		final Bundle args = new Bundle();
		args.putLong("gameId", gameId);

		final TracksList tracksList = new TracksList();
		tracksList.setArguments(args);

		activity.getSupportFragmentManager().beginTransaction()
				.addToBackStack(null)
				.replace(R.id.content_frame, tracksList)
		.commit();
	}

	public static void showPlaybackControls(final FragmentActivity activity) {
		final Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_playback_controls);
		activity.getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom)
				.show(fragment)
		.commit();
	}

	public static void hidePlaybackControls(final FragmentActivity activity) {
		final Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_playback_controls);
		activity.getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom)
				.hide(fragment)
		.commit();
	}
}
