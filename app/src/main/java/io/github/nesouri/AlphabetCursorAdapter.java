package io.github.nesouri;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

public class AlphabetCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
	private AlphabetIndexer alphaIndexer;

	public AlphabetCursorAdapter(final Context context, final int layout, final int sortColumn, final String[] from, final int[] to) {
		super(context, layout, null, from, to, 0);
		alphaIndexer = new AlphabetIndexer(null, sortColumn, " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	@Override
	public int getPositionForSection(final int section) {
		return alphaIndexer.getPositionForSection(section);
	}

	@Override
	public int getSectionForPosition(final int position) {
		return alphaIndexer.getSectionForPosition(position);
	}

	@Override
	public Object[] getSections() {
		return alphaIndexer.getSections();
	}

	@Override
	public Cursor swapCursor(final Cursor c) {
		alphaIndexer.setCursor(c);
		return super.swapCursor(c);
	}
}
