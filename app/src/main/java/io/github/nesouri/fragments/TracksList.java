package io.github.nesouri.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import io.github.nesouri.DatabaseHelper;
import io.github.nesouri.MainActivity;
import io.github.nesouri.R;

import static io.github.nesouri.Loaders.TRACKS_LOADER;

public class TracksList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		return DatabaseHelper.getInstance(getActivity()).queryTracks(args.getLong("gameId"));
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
		((SimpleCursorAdapter) getListAdapter()).swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		((SimpleCursorAdapter) getListAdapter()).swapCursor(null);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		setHasOptionsMenu(false);
		getActivity().setTitle("Tracks");
		setListAdapter(new SimpleCursorAdapter(
				getActivity(), R.layout.fragment_tracks_list_entry, null,
				new String[] {"_id", "title", "duration", "looped"},
				new int[] {R.id.position, R.id.title, R.id.duration, R.id.looped}, 0
		));
		getActivity().getSupportLoaderManager().initLoader(TRACKS_LOADER, getArguments(), this);
		return inflater.inflate(R.layout.fragment_tracks_list, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().getSupportLoaderManager().restartLoader(TRACKS_LOADER, getArguments(), this);
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		getListView().setFastScrollEnabled(true);
		getListView().setScrollingCacheEnabled(true);
		getListView().requestFocus();
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		super.onListItemClick(l, v, position, id);
		final long gameId = getArguments().getLong("gameId");
		final Bundle bundle = new Bundle();
		bundle.putInt("position", (int) id);
		((MainActivity) getActivity()).getServiceConnection().mediaController.getTransportControls().playFromMediaId(Long.toString(gameId), bundle);
	}
}