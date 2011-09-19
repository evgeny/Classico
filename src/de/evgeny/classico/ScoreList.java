package de.evgeny.classico;

import greendroid.app.GDActivity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ScoreList extends GDActivity {

	private final static String TAG = ScoreList.class.getSimpleName();
	private int mCompositionId;
	private Cursor imslpCursor;
	private ListView mListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setActionBarContentView(R.layout.scores);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));
		
		Uri uri = getIntent().getData();
		Log.d(TAG, "uri=" + uri);
		Dialog dialog = ProgressDialog.show(ScoreList.this, "Wait", 
				"Query database. Please wait...", true);
		final Cursor cursor = managedQuery(uri, null, null, null, null);
		
		if(cursor == null) {
			finish();
		} else {
			cursor.moveToFirst();
			mCompositionId = cursor.getInt(cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION_ID));
			Log.d(TAG, "composition id = " + mCompositionId);
			cursor.close();
		}
		
		Uri data = Uri.withAppendedPath(ComposerProvider.CONTENT_URI,
				 "imslp/" + String.valueOf(mCompositionId));
		
		Log.d(TAG, "get imslp cursor for uri=" + data.toString());
		imslpCursor = managedQuery(data, null, null, null, null);
		
		String[] from = new String[] { "imslp", "meta" };

		int[] to = new int[] { R.id.composer,
				R.id.composition };

		// Create a simple cursor adapter for the definitions and apply them to the ListView
		SimpleCursorAdapter scoresAdapter = new SimpleCursorAdapter(this,
				R.layout.result, imslpCursor, from, to);
		Log.d(TAG, "set imslp adapter");
		mListView.setAdapter(scoresAdapter);
				
		dialog.dismiss();
		// Define the on-click listener for the list items
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				imslpCursor.moveToPosition(position);
				final Intent partitureViewerIntent = new Intent(getApplicationContext(), GestureActivity.class);
				partitureViewerIntent.putExtra("imslp", imslpCursor.getString(imslpCursor.getColumnIndex("imslp")));
				startActivity(partitureViewerIntent);
			}
		});
	}
}
