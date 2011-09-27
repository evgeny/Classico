package de.evgeny.classico;

import greendroid.app.ActionBarActivity;
import greendroid.app.GDListActivity;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
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

public class CompositionList extends GDListActivity {
	
	private static final String TAG = CompositionList.class.getSimpleName();
	private String composer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Bundle bundle = getIntent().getExtras();
		composer = bundle.getString("composer");
		Log.d(TAG, "choosed composer " + composer);
		
		if (composer == null) {
			new AsyncCursorLoader().execute(null);
		} else new AsyncCursorLoader().execute(composer);
	}
	
	private Activity getActivity() {
		return this;
	}
	
	private final class AsyncCursorLoader extends AsyncTask<String, Object, Cursor> {

		private Dialog waitingDialog;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			Log.d(TAG, "onPreExecute()");
			waitingDialog = ProgressDialog.show(CompositionList.this, "Wait", 
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
			if (params == null) {
				cursor = managedQuery(ComposerProvider.TITLES_URI, null, null, null, null);
				Log.d(TAG, "" + cursor.getCount());
			} else {
				cursor = managedQuery(ComposerProvider.CONTENT_URI, null, null,
						new String[] {params[0]}, null); 
			}
			return cursor;
		}
		
		@Override
		protected void onPostExecute(Cursor cursor) {
			super.onPostExecute(cursor);
			
			Log.d(TAG, "omPostExecute");
			ClassicoListAdapter listAdapter = new ClassicoListAdapter(
					CompositionList.this, android.R.layout.simple_list_item_1, 
					cursor, new String[]{ClassicoDatabase.KEY_COMPOSITION}, new int[]{android.R.id.text1});
			
			setListAdapter(listAdapter);
			
			getListView().setFastScrollEnabled(true);
			getListView().setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					Log.d(TAG, "onItemClick(): " + arg3);
					final Intent scoreIntent = new Intent(getApplicationContext(), ScoreList.class);	
					Uri data = Uri.withAppendedPath(ComposerProvider.CONTENT_URI,
							String.valueOf(arg3));
					scoreIntent.setData(data);
					scoreIntent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, "Score List");
					startActivity(scoreIntent);
				}
			});
			waitingDialog.dismiss();
		}
	}
}
