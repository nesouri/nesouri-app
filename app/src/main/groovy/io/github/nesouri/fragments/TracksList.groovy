package io.github.nesouri.fragments

import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.widget.SimpleCursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import groovy.transform.CompileStatic
import io.github.nesouri.DatabaseHelper
import io.github.nesouri.MainActivity
import io.github.nesouri.R

@CompileStatic
class TracksList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    @Override
    Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return DatabaseHelper.getInstance(activity).queryTracks(args.getLong("gameId"))
    }

    @Override
    void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
        (listAdapter as SimpleCursorAdapter).swapCursor(cursor)
    }

    @Override
    void onLoaderReset(final Loader<Cursor> loader) {
        (listAdapter as SimpleCursorAdapter).swapCursor(null)
    }

    @Override
    View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        hasOptionsMenu = false
        activity.title = "Tracks"
        listAdapter = new SimpleCursorAdapter(
                activity, R.layout.fragment_tracks_list_entry, null,
                ["_id", "title", "duration", "looped"] as String[],
                [R.id.position, R.id.title, R.id.duration, R.id.looped] as int[]
        )
        activity.supportLoaderManager.initLoader(1, arguments, this)
        return inflater.inflate(R.layout.fragment_tracks_list, container, false)
    }

    @Override
    void onResume() {
        super.onResume()
        activity.supportLoaderManager.restartLoader(1, arguments, this)
    }

    @Override
    void onViewCreated(final View view, final Bundle savedInstanceState) {
        listView.fastScrollEnabled = true
        listView.scrollingCacheEnabled = true
        listView.requestFocus()
    }

    @Override
    void onListItemClick(final ListView l, final View v, final int position, final long id) {
        super.onListItemClick(l, v, position, id)
        final long gameId = arguments.getLong("gameId")
        final Bundle bundle = new Bundle();
        bundle.putInt("position", (int) id)
        (activity as MainActivity).serviceConnection.mediaController.transportControls.playFromMediaId(gameId as String, bundle)
    }
}