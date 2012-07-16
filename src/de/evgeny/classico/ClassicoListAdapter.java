package de.evgeny.classico;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

public class ClassicoListAdapter extends SimpleCursorAdapter implements
		SectionIndexer {

	AlphabetIndexer mAlphaIndexer;

	public ClassicoListAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to, 0);
		mAlphaIndexer = new AlphabetIndexer(c, 1, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	@Override
	public int getPositionForSection(int section) {
		return mAlphaIndexer.getPositionForSection(section);
	}

	@Override
	public int getSectionForPosition(int position) {
		return mAlphaIndexer.getSectionForPosition(position);
	}

	@Override
	public Object[] getSections() {
		return mAlphaIndexer.getSections();
	}

	@Override
	public Cursor swapCursor(Cursor c) {
		mAlphaIndexer.setCursor(c);
		return super.swapCursor(c);
	}

}
