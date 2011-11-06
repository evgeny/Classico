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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

public class ComposerList extends GDListActivity {
	
	private static final String TAG = ComposerList.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		new AsyncCursorLoader().execute(null);
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
	
	private final class AsyncCursorLoader extends AsyncTask<Object, Object, Cursor> {

		private Dialog waitingDialog;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			Log.d(TAG, "onPreExecute()");
			waitingDialog = ProgressDialog.show(ComposerList.this, "Wait", 
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
		protected Cursor doInBackground(Object... params) {
			Log.d(TAG, "doInBackground(): ");
			
			Cursor cursor = managedQuery(ClassicoProvider.COMPOSER_URI, null, null, null, null);
			Log.d(TAG, "" + cursor.getCount());
			
			return cursor;
		}
		
		@Override
		protected void onPostExecute(Cursor cursor) {
			super.onPostExecute(cursor);
			
			Log.d(TAG, "omPostExecute");
			ClassicoListAdapter listAdapter = new ClassicoListAdapter(
					ComposerList.this, android.R.layout.simple_list_item_1, 
					cursor, new String[]{ClassicoDatabase.KEY_COMPOSER}, new int[]{android.R.id.text1});
			
			getListView().setAdapter(listAdapter);
			getListView().setFastScrollEnabled(true);
			getListView().setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					Log.d(TAG, "onItemClick(): " + arg3);
					final Intent scoresIntent = new Intent(getApplicationContext(), CompositionList.class);	
					scoresIntent.putExtra("composer", ((TextView)arg1).getText());
					scoresIntent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, "Scores");
					startActivity(scoresIntent);
				}
			});
			waitingDialog.dismiss();
		}
	}
}
