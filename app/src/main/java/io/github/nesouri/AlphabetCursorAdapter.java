package io.github.nesouri;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

public class AlphabetCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
    private AlphabetIndexer alphaIndexer;

    public AlphabetCursorAdapter(Context context, int layout, int sortColumn, String[] from, int[] to) {
        super(context, layout, null, from, to, 0);
        alphaIndexer = new AlphabetIndexer(null, sortColumn, " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Override
    public int getPositionForSection(int section) {
        return alphaIndexer.getPositionForSection(section);
    }

    @Override
    public int getSectionForPosition(int position) {
        return alphaIndexer.getSectionForPosition(position);
    }

    @Override
    public Object[] getSections() {
        return alphaIndexer.getSections();
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        alphaIndexer.setCursor(c);
        return super.swapCursor(c);
    }
}
