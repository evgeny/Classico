package de.evgeny.classico;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter;

public class ScoreList extends ListActivity {

	private final static String TAG = ScoreList.class.getSimpleName();
	private int mCompositionId;
	private Cursor imslpCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		setContentView(R.layout.scores);
		
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
		
		Uri data = Uri.withAppendedPath(ComposerProvider.CONTENT_URI,
				 "imslp/" + String.valueOf(mCompositionId));
		
		Log.d(TAG, "get imslp cursor for uri=" + data.toString());
		imslpCursor = managedQuery(data, null, null, null, null);
		
		if(imslpCursor == null) {
			Log.d(TAG, "nothing founded");
		}
		
		String[] from = new String[] { "imslp", "meta" };

		int[] to = new int[] { R.id.composer,
				R.id.composition };
		Log.d(TAG, "2");
		// Create a simple cursor adapter for the definitions and apply them to the ListView
		SimpleCursorAdapter scoresAdapter = new SimpleCursorAdapter(this,
				R.layout.result, imslpCursor, from, to);
		Log.d(TAG, "set imslp adapter");
		setListAdapter(scoresAdapter);
		//mListView.setAdapter(scores);

		// Define the on-click listener for the list items
		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				imslpCursor.moveToPosition(position);
//				final Intent partitureViewerIntent = new Intent(getApplicationContext(), PartitureViewer.class);
				final Intent partitureViewerIntent = new Intent(getApplicationContext(), GestureActivity.class);
				partitureViewerIntent.putExtra("imslp", imslpCursor.getString(imslpCursor.getColumnIndex("imslp")));
				startActivity(partitureViewerIntent);
			}
		});
	}
}
