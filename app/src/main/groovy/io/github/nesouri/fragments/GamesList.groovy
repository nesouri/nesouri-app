package io.github.nesouri.fragments

import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.SimpleCursorAdapter
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import android.widget.ListView
import groovy.transform.CompileStatic
import io.github.nesouri.AlphabetCursorAdapter
import io.github.nesouri.DatabaseHelper
import io.github.nesouri.R

@CompileStatic
class GamesList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    static final String TAG = GamesList.class.name

    private MenuItem searchItem
    private SearchView searchView
    private String currentFilter = null

    private void setCurrentFilter(final String value) {
        Log.d(TAG, "Setting current filter")
        if (value == null || value.length() > 2) {
            currentFilter = value
            activity.supportLoaderManager.restartLoader(0, null, this)
        }
    }

    @Override
    Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader")
        return DatabaseHelper.getInstance(activity).queryGames(currentFilter)
    }

    @Override
    void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished")
        (listAdapter as SimpleCursorAdapter).swapCursor(cursor)
    }

    @Override
    void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset")
        (listAdapter as SimpleCursorAdapter).swapCursor(null)
    }

    @Override
    View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity.title = "Games"
        listAdapter = new AlphabetCursorAdapter(
                activity, R.layout.fragment_games_list_entry,
                1, ["title"], [R.id.label]
        )
        activity.supportLoaderManager.initLoader(0, null, this)
        return inflater.inflate(R.layout.fragment_games_list, container, false)
    }

    @Override
    void onResume() {
        super.onResume()
        activity.supportLoaderManager.restartLoader(0, null, this)
    }

    @Override
    void onViewCreated(View view, Bundle savedInstanceState) {
        listView.fastScrollEnabled = true
        listView.scrollingCacheEnabled = true
    }

    @Override
    void onListItemClick(final ListView l, final View v, final int position, final long id) {
        super.onListItemClick(l, v, position, id)

        final Bundle args = new Bundle();
        args.putLong("gameId", id);

        final TracksList tracksList = TracksList.newInstance()
        tracksList.setArguments(args);

        if (searchView.isShown()) {
            MenuItemCompat.collapseActionView(searchItem)
            searchView.setQuery("", false)
            currentFilter = null
        }

        activity.supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.content_frame, tracksList)
        .commit()
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }


    @Override
    void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG, "onCreateOptionsMenu")
        super.onCreateOptionsMenu(menu, inflater)
        searchItem = menu.findItem(R.id.action_search)
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem)

        searchView.onQueryTextListener = [
                onQueryTextChange: { final String value ->
            Log.d(TAG, "onQueryTextListener")
            currentFilter = value
            return true
        },
                onQueryTextSubmit: {
                    Log.d(TAG, "onQueryTextSubmit")
                    return false
                }
                ] as SearchView.OnQueryTextListener

        searchView.onCloseListener = {
            Log.d(TAG, "onCloseListener")
            currentFilter = null
            return false
        } as SearchView.OnCloseListener

        searchView.onQueryTextFocusChangeListener = { final View view, final boolean hasFocus ->
            Log.d(TAG, "onQueryTextFocusChangeListener" + hasFocus)
            if (!hasFocus) {
                currentFilter = null
                searchView.clearFocus()
                MenuItemCompat.collapseActionView(searchItem)
            }
        } as View.OnFocusChangeListener
    }
}