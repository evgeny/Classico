package de.evgeny.classico;

import greendroid.app.GDActivity;

import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.flurry.android.FlurryAgent;

public class ScoreList extends GDActivity {

	private final static String TAG = ScoreList.class.getSimpleName();
	private int mCompositionId;
	private ListView mListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");

		setActionBarContentView(R.layout.scores);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));

		Uri uri = getIntent().getData();
		Log.d(TAG, "uri=" + uri);

		final Cursor cursor = managedQuery(uri, null, null, null, null);

//		((ClassicoApplication)getApplication()).addScoreToHistory(uri.toString());
		if (cursor.moveToFirst()) {
			mCompositionId = cursor.getInt(cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION_ID));
			Log.d(TAG, "composition id = " + mCompositionId);
			
			final String composition = cursor.getString(
					cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION));
			final String composer = cursor.getString(
					cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSER));

			final ContentValues values = new ContentValues();
			values.put("_id", 0);
			values.put("composition", composition);
			values.put("composer", composer);
			values.put("comp_id", mCompositionId);
			getContentResolver().insert(ClassicoProvider.RECENT_TITLES_URI, values);
						
		} else {
			finish();
		}
		
		cursor.close();

		new AsyncCursorLoader().execute(null);
	}

	private Activity getActivity() {
		return this;
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

	private final class AsyncCursorLoader extends AsyncTask<String, Object, Cursor> {

		private Dialog waitingDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			Log.d(TAG, "onPreExecute()");
			waitingDialog = ProgressDialog.show(getActivity(), "Wait", 
					"Query database. Please wait...", true);
			waitingDialog.setCancelable(true);
			waitingDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					getActivity().finish();
				}
			});
		}

		@Override
		protected Cursor doInBackground(String... params) {
			Log.d(TAG, "doInBackground(): ");
			Cursor cursor;
			Uri data = Uri.withAppendedPath(ClassicoProvider.CONTENT_URI,
					"imslp/" + String.valueOf(mCompositionId));

			Log.d(TAG, "get imslp cursor for uri=" + data.toString());
			cursor = managedQuery(data, null, null, null, null);
			return cursor;
		}

		@Override
		protected void onPostExecute(final Cursor cursor) {
			super.onPostExecute(cursor);

			Log.d(TAG, "omPostExecute");
			String[] from = new String[] { "imslp", "pages" };

			SimpleCursorAdapter scoresAdapter = new SimpleCursorAdapter(getActivity(),
					android.R.layout.simple_list_item_1, cursor, from, new int[]{android.R.id.text1});

			mListView.setAdapter(scoresAdapter);

			mListView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					cursor.moveToPosition(position);
					final Intent partitureViewer = new Intent(getApplicationContext(), GestureActivity.class);

					//send flurry report
					final HashMap<String, String> paramsMap = new HashMap<String, String>();
					paramsMap.put("imslp", cursor.getString(cursor.getColumnIndex("imslp")));
					FlurryAgent.onEvent("imslp selected", paramsMap);
					partitureViewer.putExtra("imslp", cursor.getString(cursor.getColumnIndex("imslp")));
					partitureViewer.putExtra("pages", cursor.getInt(cursor.getColumnIndex("pages")));
					startActivity(partitureViewer);
				}
			});
			waitingDialog.dismiss();
		}
	}
}
