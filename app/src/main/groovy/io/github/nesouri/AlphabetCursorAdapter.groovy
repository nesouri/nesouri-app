package io.github.nesouri

import android.content.Context
import android.database.Cursor
import android.support.v4.widget.SimpleCursorAdapter
import android.widget.AlphabetIndexer
import android.widget.SectionIndexer
import groovy.transform.CompileStatic

@CompileStatic
class AlphabetCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
    private AlphabetIndexer alphaIndexer

    AlphabetCursorAdapter(Context context, int layout, int sortColumn, List<String> from, List<Integer> to) {
        super(context, layout, null, from as String[], to as int[], 0)
        alphaIndexer = new AlphabetIndexer(null, sortColumn, " ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    }

    @Override
    int getPositionForSection(int section) {
        return alphaIndexer.getPositionForSection(section)
    }

    @Override
    int getSectionForPosition(int position) {
        return alphaIndexer.getSectionForPosition(position)
    }

    @Override
    Object[] getSections() {
        return alphaIndexer.sections
    }

    @Override
    Cursor swapCursor(Cursor c) {
        alphaIndexer.cursor = c
        return super.swapCursor(c)
    }
}
