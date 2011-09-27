package de.evgeny.classico;

import greendroid.app.ActionBarActivity;
import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ActionBarItem.Type;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class Dashboard extends GDActivity {
	
	private static final String TAG = Dashboard.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setActionBarContentView(R.layout.dashboard);
		addActionBarItem(Type.Search, R.id.action_bar_search);
		
		//check if database exist
//		File dir = new File(Environment.getExternalStorageDirectory(),"Classico/");
//		File db = new File(dir, "classico.db");
//		if (!db.exists() || db.canRead()) {
//			new DownloadDialog(this).show();
//		}
		
		onNewIntent(getIntent());
	}
	
	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		Log.d(TAG, "onNewIntent(): " + intent.getAction());

		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Log.d(TAG, "ACTION_VIEW");
			// handles a click on a search suggestion; launches activity to show composition			
			final Intent scoreIntent = new Intent(getApplicationContext(), ScoreList.class);			
			scoreIntent.setData(intent.getData());
			scoreIntent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, "Score List");
			startActivity(scoreIntent);
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// handles a search query
			final String query = intent.getStringExtra(SearchManager.QUERY);
			Log.d(TAG, "ACTION_SEARCH, query: " + query);
			intent.setClass(getApplicationContext(), SearchableActivity.class);
			startActivity(intent);
		}
	}
	
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {			
		switch (item.getItemId()) {                
		case R.id.action_bar_search:
			onSearchRequested();
			break;
		default:
			return super.onHandleActionBarItemClick(item, position);
		}
		return true;
	}

	public void onSearchButtonClick(final View view) {
		onSearchRequested();
	}
	
	public void onCancelClick(View view) {
		Log.d(TAG, "onCancelClick(): ");
	}
	
	public void onOkPressedClick(View v) {
		Log.d(TAG, "onOkClick(): ");
	}
	
	public void onScoresClick(View view) {
		final Intent intent = new Intent(getApplicationContext(), CompositionList.class);			
		intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, "Scores");
		startActivity(intent);
	}
	
	public void onComposersClick(View view) {
		final Intent intent = new Intent(getApplicationContext(), ComposerList.class);			
		intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, "Composers");
		startActivity(intent);
	}
	
	private class DownloadDialog extends Dialog implements OnClickListener {
		
		private Button cancelButton;
		private Button okButton;

		public DownloadDialog(Context context) {
			super(context);
			
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.database_dialog);
			
			cancelButton = (Button) findViewById(R.id.d_database_ok);
			okButton = (Button) findViewById(R.id.d_database_cancel);
			
			cancelButton.setOnClickListener(this);
			okButton.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.d_database_cancel:
				this.dismiss();
				break;
			case R.id.d_database_ok:
				//TODO download the database
				break;
			default:
				break;
			}
		}
	}
}
