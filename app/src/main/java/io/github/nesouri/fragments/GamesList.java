package io.github.nesouri.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import io.github.nesouri.AlphabetCursorAdapter;
import io.github.nesouri.DatabaseHelper;
import io.github.nesouri.R;

public class GamesList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	static final String TAG = GamesList.class.getName();

	private MenuItem searchItem;
	private SearchView searchView;
	private String currentFilter = null;

	private void setCurrentFilter(final String value) {
		Log.d(TAG, "Setting current filter");
		if (value == null || value.length() > 2) {
			currentFilter = value;
			getActivity().getSupportLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		Log.d(TAG, "onCreateLoader");
		return DatabaseHelper.getInstance(getActivity()).queryGames(currentFilter);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
		Log.d(TAG, "onLoadFinished");
		((SimpleCursorAdapter) getListAdapter()).swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		Log.d(TAG, "onLoaderReset");
		((SimpleCursorAdapter) getListAdapter()).swapCursor(null);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		getActivity().setTitle("Games");
		setListAdapter(new AlphabetCursorAdapter(
				getActivity(), R.layout.fragment_games_list_entry,
				1, new String[] {"title"}, new int[] {R.id.label}
		));
		getActivity().getSupportLoaderManager().initLoader(0, null, this);
		return inflater.inflate(R.layout.fragment_games_list, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().getSupportLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		getListView().setFastScrollEnabled(true);
		getListView().setScrollingCacheEnabled(true);
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		super.onListItemClick(l, v, position, id);

		final Bundle args = new Bundle();
		args.putLong("gameId", id);

		final TracksList tracksList = new TracksList();
		tracksList.setArguments(args);

		if (searchView.isShown()) {
			MenuItemCompat.collapseActionView(searchItem);
			searchView.setQuery("", false);
			currentFilter = null;
		}

		getActivity().getSupportFragmentManager().beginTransaction()
				.addToBackStack(null)
				.replace(R.id.content_frame, tracksList)
		.commit();
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		Log.d(TAG, "onCreateOptionsMenu");
		super.onCreateOptionsMenu(menu, inflater);
		searchItem = menu.findItem(R.id.action_search);
		searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(final String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(final String newText) {
				Log.d(TAG, "onQueryTextListener");
				currentFilter = newText;
				return true;
			}
		});

		searchView.setOnCloseListener(() -> {
			Log.d(TAG, "onCloseListener");
			currentFilter = null;
			return false;
		});

		searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
			Log.d(TAG, "onQueryTextFocusChangeListener" + hasFocus);
			if (!hasFocus) {
				currentFilter = null;
				searchView.clearFocus();
				MenuItemCompat.collapseActionView(searchItem);
			}
		});
	}
}