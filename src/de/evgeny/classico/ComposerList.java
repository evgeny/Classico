package de.evgeny.classico;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.actionbar.ActionBarActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

public class ComposerList extends ActionBarActivity implements LoaderCallbacks<Cursor> {

	private static final String TAG = ComposerList.class.getSimpleName();
	private ClassicoListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(android.R.layout.list_content);

		mAdapter = new ClassicoListAdapter(this,
				android.R.layout.simple_list_item_1, null,
				new String[] { ClassicoDatabase.KEY_COMPOSER }, new int[] { android.R.id.text1 });

		ListView list = (ListView) findViewById(android.R.id.list);

		list.setAdapter(mAdapter);
		list.setFastScrollEnabled(true);
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Log.d(TAG, "onItemClick(): " + arg3);
				final Intent scoresIntent = new Intent(ComposerList.this, CompositionList.class);
				scoresIntent.putExtra("composer", ((TextView) arg1).getText());
				startActivity(scoresIntent);
			}
		});

		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "JE85NZ7FLJEGWB36XYPR");
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(this, ClassicoProvider.COMPOSER_URI, null, null, null,
				null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		mAdapter.swapCursor(arg1);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);

	}
}
