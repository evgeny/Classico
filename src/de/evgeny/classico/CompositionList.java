package de.evgeny.classico;

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
import android.support.v4.actionbar.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;

public class CompositionList extends ActionBarActivity {
	
	private static final String TAG = CompositionList.class.getSimpleName();
	private String composer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.composer_list);
		final Bundle bundle = getIntent().getExtras();
		composer = bundle.getString("composer");
		Log.d(TAG, "choosed composer " + composer);
		
		if (composer == null) {
			new AsyncCursorLoader().execute();
		} else new AsyncCursorLoader().execute(composer);
	}
	
	private Activity getActivity() {
		return this;
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
				cursor = managedQuery(ClassicoProvider.TITLES_URI, null, null, null, null);
				Log.d(TAG, "" + cursor.getCount());
			} else {
				cursor = managedQuery(ClassicoProvider.CONTENT_URI, null, null,
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
			ListView list = (ListView)findViewById(android.R.id.list);
		
			list.setAdapter(listAdapter);
//			setListAdapter(listAdapter);
			
			list.setFastScrollEnabled(true);
//			getListView().setFastScrollEnabled(true);
			list.setOnItemClickListener(new OnItemClickListener() {
//			getListView().setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					Log.d(TAG, "onItemClick(): " + arg3);
					final Intent scoreIntent = new Intent(getApplicationContext(), ScoreList.class);	
					Uri data = Uri.withAppendedPath(ClassicoProvider.CONTENT_URI,
							String.valueOf(arg3));
					scoreIntent.setData(data);
					startActivity(scoreIntent);
				}
			});
			waitingDialog.dismiss();
		}
	}
}
