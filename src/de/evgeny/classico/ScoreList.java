package de.evgeny.classico;

import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ActionBarItem.Type;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;

public class ScoreList extends GDActivity implements LoaderCallbacks<Cursor>, OnItemClickListener {

	private final static String TAG = ScoreList.class.getSimpleName();
	private int mCompositionId;
	private ListView mListView;
	private SimpleCursorAdapter mScoresAdapter;
	private String mComposition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");

		setActionBarContentView(R.layout.scores);
		
		addActionBarItem(Type.Share);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));

		Uri uri = getIntent().getData();
		Log.d(TAG, "uri=" + uri);

		final Cursor cursor = managedQuery(uri, null, null, null, null);

		if (cursor.moveToFirst()) {
			mCompositionId = cursor.getInt(cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION_ID));
			Log.d(TAG, "composition id = " + mCompositionId);
			
			mComposition = cursor.getString(
					cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION));
			final String composer = cursor.getString(
					cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSER));

			final ContentValues values = new ContentValues();
			values.put(ClassicoDatabase.KEY_COMPOSITION, mComposition);
			values.put(ClassicoDatabase.KEY_COMPOSER, composer);
			values.put(ClassicoDatabase.KEY_COMPOSITION_ID, mCompositionId);
			getContentResolver().insert(ClassicoProvider.RECENT_TITLES_URI, values);
			
			//send flurry report
			final HashMap<String, String> paramsMap = new HashMap<String, String>();
			paramsMap.put("title", mComposition);
			FlurryAgent.onEvent("imslp selected", paramsMap);
		} else {
			finish();
		}
		
		cursor.close();

		fillScoresList();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		switch (position) {
		case 0:
			Log.d(TAG, "Share score");
			final String shareString = getString(R.string.share_message, mComposition);
//				getString(R.string.share_template, mTitleString, getHashtagsString(), mUrl);
	        final Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareString);
            startActivity(Intent.createChooser(intent, getText(R.string.title_share)));
			break;
		default:
			break;
		}
		return super.onHandleActionBarItemClick(item, position);
	}
    
	private void fillScoresList() {
		Log.i(TAG, "fillRecentScoresList(): ");
		
		mScoresAdapter = new SimpleCursorAdapter(
				this, android.R.layout.simple_list_item_1, null, 
				new String[]{"imslp", "pages"}, 
				new int[]{android.R.id.text1}, 0);

		mListView.setAdapter(mScoresAdapter);
		mListView.setOnItemClickListener(this);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "JE85NZ7FLJEGWB36XYPR");
		FlurryAgent.onPageView();
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		final Cursor cursor = ((SimpleCursorAdapter)arg0.getAdapter()).getCursor();
		cursor.moveToPosition(arg2);
		final Intent partitureViewer = new Intent(getApplicationContext(), GestureActivity.class);

		//send flurry report
		final HashMap<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("imslp", cursor.getString(cursor.getColumnIndex("imslp")));
		FlurryAgent.onEvent("imslp selected", paramsMap);
		partitureViewer.putExtra("imslp", cursor.getString(cursor.getColumnIndex("imslp")));
		partitureViewer.putExtra("pages", cursor.getInt(cursor.getColumnIndex("pages")));
		startActivity(partitureViewer);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Uri data = Uri.withAppendedPath(ClassicoProvider.CONTENT_URI,
				"imslp/" + String.valueOf(mCompositionId));

		Log.d(TAG, "get imslp cursor for uri=" + data.toString());
		return new CursorLoader(ScoreList.this, data, null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		mScoresAdapter.swapCursor(arg1);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mScoresAdapter.swapCursor(null);
	}
}
