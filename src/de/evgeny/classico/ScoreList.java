package de.evgeny.classico;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ScoreList extends Activity {

	private final static String TAG = ScoreList.class.getSimpleName();
	private ListView mListView;
	private int mCompositionId;
	private Cursor imslpCursor;
	private ClassicoDatabase mClassicoDatabase;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		
		setContentView(R.layout.scores_layout);
		
		Uri uri = getIntent().getData();
		Log.d(TAG, "uri=" + uri);
		final Cursor cursor = managedQuery(uri, null, null, null, null);
		
		if(cursor == null) {
			finish();
		} else {
			cursor.moveToFirst();
			mCompositionId = cursor.getInt(cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION_ID));
			Log.d(TAG, "composition id = " + mCompositionId);
			cursor.close();
		}
		
		mListView = (ListView) findViewById(R.id.score_list);
		
		final String[] columns = new String[]{"_id", "imslp", "meta"};
		final String selection = "comp_id=?";
		final String[] selectionArgs = new String[]{String.valueOf(mCompositionId)};
		
		mClassicoDatabase = new ClassicoDatabase();
		Log.d(TAG, "get imslp cursor");
		imslpCursor = mClassicoDatabase.getCursor(
				ClassicoDatabase.SCORE_TABLE, columns, selection, selectionArgs);
		String[] from = new String[] { "imslp", "meta" };

		int[] to = new int[] { R.id.composer,
				R.id.composition };

		// Create a simple cursor adapter for the definitions and apply them to the ListView
		SimpleCursorAdapter scores = new SimpleCursorAdapter(this,
				R.layout.result, imslpCursor, from, to);
		Log.d(TAG, "set imslp adapter");
		mListView.setAdapter(scores);

		// Define the on-click listener for the list items
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				imslpCursor.moveToPosition(position);
				final Intent partitureViewerIntent = new Intent(getApplicationContext(), PartitureViewer.class);
				partitureViewerIntent.putExtra("imslp", imslpCursor.getString(imslpCursor.getColumnIndex("imslp")));
				startActivity(partitureViewerIntent);
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		imslpCursor.close();
		mClassicoDatabase.close();
	}
}
