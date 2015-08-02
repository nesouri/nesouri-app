package io.github.nesouri

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.util.Log
import groovy.transform.CompileStatic

import java.sql.SQLException

import static java.nio.charset.StandardCharsets.UTF_8

@CompileStatic
class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHelper.class.name
    private static final int DATABASE_VERSION = 1
    private static final String DATABASE_NAME = "nesouri.db"

    private static DatabaseHelper instance;

    /* All blame to: http://www.androiddesignpatterns.com/2012/05/correctly-managing-your-sqlite-database.html */
    static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null)
            instance = new DatabaseHelper(context.getApplicationContext());
        return instance;
    }

    private final Context context

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION)
        this.context = context
    }

    @Override
    void onCreate(SQLiteDatabase db) {
        populate(db, context.resources.openRawResource(R.raw.nesouri))
    }

    @Override
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        cleanup(db)
        onCreate(db)
    }

    @Override
    void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion)
    }

    static def populate(SQLiteDatabase db, InputStream is) {
        def sb = new StringBuilder()

        new BufferedReader(new InputStreamReader(is, UTF_8)).withReader {
            while (true) {
                def line = it.readLine()
                if (line == null)
                    break
                if (line.endsWith(";")) {
                    if (sb.length() > 0) {
                        sb.append(line)
                        db.execSQL(sb.toString())
                        sb.length = 0
                    } else {
                        db.execSQL(line)
                    }
                } else {
                    sb.append(line)
                }
            }

            addSortColumn(db)
            addCaches(db)
        }
    }

    static def addCaches(SQLiteDatabase db) throws SQLException {
        db.execSQL("CREATE TABLE game_cache ( game_id INTEGER REFERENCES games(game_id), data BLOB, PRIMARY KEY (game_id) )")
        db.execSQL("CREATE TABLE image_cache ( game_id INTEGER REFERENCES games(game_id), data BLOB, PRIMARY KEY (game_id) )")
    }

    static def addSortColumn(SQLiteDatabase db) throws SQLException {
        db.execSQL("ALTER TABLE games ADD sort_title TEXT")
        db.execSQL("UPDATE games SET sort_title=SUBSTR(title,5) WHERE game_id IN (SELECT game_id FROM games WHERE title LIKE 'The %')")
        db.execSQL("UPDATE games SET sort_title=title WHERE game_id IN (SELECT game_id FROM games WHERE title NOT LIKE 'The %')")
    }

    static def cleanup(SQLiteDatabase db) {
        def c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'")
        while (c.moveToNext()) {
            def table = c.getString(0)
            if (table.equals("android_metadata"))
                continue
            db.execSQL("DROP TABLE IF EXISTS ?", [table])
        }
    }

    Cursor rawQuery(final String query, final Object... args) {
        Log.d(TAG, String.format("%s %s", query, Arrays.toString(args)));
        return readableDatabase.rawQuery(query, args as String[])
    }

    @CompileStatic
    static class DatabaseLoader extends CursorLoader {
        final Closure<? extends Cursor> closure

        DatabaseLoader(final Context context, final Closure<? extends Cursor> closure) {
            super(context)
            this.closure = closure
        }

        @Override
        Cursor loadInBackground() {
            return closure.call()
        }
    }

    Loader<Cursor> queryGames(final String currentFilter) {
        return new DatabaseLoader(context, {
            if (!currentFilter)
                return rawQuery("SELECT game_id as _id, sort_title, title FROM games ORDER BY sort_title COLLATE NOCASE")
            return rawQuery("SELECT game_id as _id, sort_title, title FROM games WHERE title LIKE ? ORDER BY sort_title COLLATE NOCASE", "%" + currentFilter + "%")
        })
    }

    Cursor queryTrackInfo(int gameId, int track) {
        return rawQuery("SELECT g.file, g.total_tracks, g.title, g.date, g.year, t.title, t.duration, t.looped FROM games AS g, tracks AS t ON t.game_id=g.game_id WHERE g.game_id=? AND t.track=?", gameId, track)
    }

    Loader<Cursor> queryTracks(long gameId) {
        return new DatabaseLoader(context, {
            final Cursor tracksCursor = rawQuery("SELECT track, title, duration, replace(replace(looped, 0, \"→\"), 1, \"∞\") FROM tracks WHERE game_id=? ORDER BY track", gameId)
            final Cursor gameCursor = rawQuery("SELECT total_tracks FROM games WHERE game_id=?", gameId)
            gameCursor.moveToFirst()
            final int totalTracks = gameCursor.getInt(0)
            final MatrixCursor cursor = new MatrixCursor(["_id", "title", "duration", "looped"] as String[])
            final boolean hasMoreTracks = tracksCursor.moveToFirst()

            (1..totalTracks).eachWithIndex { i, _ ->
                def track = hasMoreTracks ? tracksCursor.getInt(0) : -1
                def title = i == track && tracksCursor.getString(1)?.trim() ? tracksCursor.getString(1) : "Track " + i
                def duration = (i == track ? tracksCursor.getLong(2) / 1000 : 0) as long
                def looping = (i == track ? tracksCursor.getString(3) : "")
                cursor.addRow([i, title, sprintf("%d:%02d", (duration / 60) as int, (duration % 60) as int), looping] as Object[])
                if (i == track)
                    hasMoreTracks = tracksCursor.moveToNext()
            }

            cursor.moveToFirst()

            return cursor
        })
    }
}
