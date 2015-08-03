package io.github.nesouri;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getName();
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "nesouri.db";

    private static DatabaseHelper instance;

    /* All blame to: http://www.androiddesignpatterns.com/2012/05/correctly-managing-your-sqlite-database.html */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null)
            instance = new DatabaseHelper(context.getApplicationContext());
        return instance;
    }

    private final Context context;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            populate(db, context.getResources().openRawResource(R.raw.nesouri));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        cleanup(db);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private static void populate(SQLiteDatabase db, InputStream is) throws IOException, SQLException {
        final StringBuilder sb = new StringBuilder();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            while (true) {
                final String line = reader.readLine();
                if (line == null)
                    break;
                if (line.endsWith(";")) {
                    if (sb.length() > 0) {
                        sb.append(line);
                        db.execSQL(sb.toString());
                        sb.setLength(0);
                    } else {
                        db.execSQL(line);
                    }
                } else {
                    sb.append(line);
                }
            }

            addSortColumn(db);
            addCaches(db);
        }
    }

    private static void addCaches(final SQLiteDatabase db) throws SQLException {
        db.execSQL("CREATE TABLE game_cache ( game_id INTEGER REFERENCES games(game_id), data BLOB, PRIMARY KEY (game_id) )");
        db.execSQL("CREATE TABLE image_cache ( game_id INTEGER REFERENCES games(game_id), data BLOB, PRIMARY KEY (game_id) )");
    }

    private static void addSortColumn(final SQLiteDatabase db) throws SQLException {
        db.execSQL("ALTER TABLE games ADD sort_title TEXT");
        db.execSQL("UPDATE games SET sort_title=SUBSTR(title,5) WHERE game_id IN (SELECT game_id FROM games WHERE title LIKE 'The %')");
        db.execSQL("UPDATE games SET sort_title=title WHERE game_id IN (SELECT game_id FROM games WHERE title NOT LIKE 'The %')");
    }

    private static void cleanup(final SQLiteDatabase db) {
        final Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        while (c.moveToNext()) {
            final String table = c.getString(0);
            if (table.equals("android_metadata"))
                continue;
            db.execSQL("DROP TABLE IF EXISTS ?", new String[] { table });
        }
    }

    private Cursor rawQuery(final String query, final String... args) {
        Log.d(TAG, String.format("%s %s", query, Arrays.toString(args)));
        return getReadableDatabase().rawQuery(query, args);
    }

    private static class DatabaseLoader extends CursorLoader {
        private final Callable<? extends Cursor> closure;

        DatabaseLoader(final Context context, final Callable<? extends Cursor> closure) {
            super(context);
            this.closure = closure;
        }

        @Override
        public Cursor loadInBackground() {
            try {
                return closure.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Loader<Cursor> queryGames(final String currentFilter) {
        return new DatabaseLoader(context, () -> {
            if (currentFilter == null)
                return rawQuery("SELECT game_id as _id, sort_title, title FROM games ORDER BY sort_title COLLATE NOCASE");
            return rawQuery("SELECT game_id as _id, sort_title, title FROM games WHERE title LIKE ? ORDER BY sort_title COLLATE NOCASE", "%" + currentFilter + "%");
        });
    }

    public Cursor queryTrackInfo(int gameId, int track) {
        return rawQuery("SELECT g.file, g.total_tracks, g.title, g.date, g.year, t.title, t.duration, t.looped FROM games AS g, tracks AS t ON t.game_id=g.game_id WHERE g.game_id=? AND t.track=?",
                Integer.toString(gameId), Integer.toString(track));
    }

    private static boolean isEmpty(final String value) {
        if (value == null)
            return true;
        return value.isEmpty();
    }

    public Loader<Cursor> queryTracks(long gameId) {
        return new DatabaseLoader(context, () -> {
            final Cursor tracksCursor = rawQuery("SELECT track, title, duration, replace(replace(looped, 0, \"→\"), 1, \"∞\") FROM tracks WHERE game_id=? ORDER BY track", Long.toString(gameId));
            final Cursor gameCursor = rawQuery("SELECT total_tracks FROM games WHERE game_id=?", Long.toString(gameId));
            gameCursor.moveToFirst();
            final int totalTracks = gameCursor.getInt(0);
            final MatrixCursor cursor = new MatrixCursor(new String[] {"_id", "title", "duration", "looped"});

            boolean hasMoreTracks = tracksCursor.moveToFirst();

            for (int i = 1; i <= totalTracks; i++) {
                final int track = hasMoreTracks ? tracksCursor.getInt(0) : -1;
                final String title = i == track && !isEmpty(tracksCursor.getString(1)) ? tracksCursor.getString(1) : "Track " + i;
                final long duration = (i == track ? tracksCursor.getLong(2) / 1000 : 0);
                final String looping = (i == track ? tracksCursor.getString(3) : "");
                cursor.addRow(new Object[] {i, title, String.format("%d:%02d", (int)(duration / 60), (int)(duration % 60)), looping});
                if (i == track)
                    hasMoreTracks = tracksCursor.moveToNext();
            }

            cursor.moveToFirst();

            return cursor;
        });
    }
}
