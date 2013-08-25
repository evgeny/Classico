package de.evgeny.classico;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;

public class CompositionList extends ActionBarActivity implements LoaderCallbacks<Cursor> {

	private static final String TAG = CompositionList.class.getSimpleName();
	private SimpleCursorAdapter mAdapter;
	private Dialog mWaitingDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.composer_list);

		// final Bundle extras = getIntent().getExtras();

		mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null,
				new String[] { ClassicoDatabase.KEY_COMPOSITION },
				new int[] { android.R.id.text1 }, 0);

		getSupportLoaderManager().initLoader(0, getIntent().getExtras(), this);

		ListView list = (ListView) findViewById(android.R.id.list);

		list.setAdapter(mAdapter);

		list.setFastScrollEnabled(true);
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Log.d(TAG, "onItemClick(): " + arg3);
				final Intent scoreIntent = new Intent(getApplicationContext(), ScoreList.class);
				Uri data = Uri.withAppendedPath(ClassicoProvider.CONTENT_URI, String.valueOf(arg3));
				scoreIntent.setData(data);
				startActivity(scoreIntent);
			}
		});

		mWaitingDialog = ProgressDialog.show(CompositionList.this, "Wait",
				"Query database. Please wait...", true);
		mWaitingDialog.setCancelable(true);
		mWaitingDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "JE85NZ7FLJEGWB36XYPR");
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

		if ((arg1 != null) && arg1.containsKey("composer")) {
			return new CursorLoader(this, ClassicoProvider.CONTENT_URI, null, null,
					new String[] { arg1.getString("composer") }, null);
		} else {
			return new CursorLoader(this, ClassicoProvider.TITLES_URI, null, null, null, null);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		mAdapter.swapCursor(arg1);
		mWaitingDialog.dismiss();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}
}
