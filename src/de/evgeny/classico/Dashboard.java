package de.evgeny.classico;

import greendroid.app.ActionBarActivity;
import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ActionBarItem.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class Dashboard extends GDActivity {

	private static final String TAG = Dashboard.class.getSimpleName();
	private static final String dbLink = 
		"https://docs.google.com/uc?id=0B8p4GKsUuQg_ZThkNGQwNTktNTUzNy00ZmFmLWExMGUtNzE2YThlNTBmZDBj&export=download&hl=en_US";
	
	private File dbFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setActionBarContentView(R.layout.dashboard);
		addActionBarItem(Type.Search, R.id.action_bar_search);

		//check if database exist
		File dir = new File(Environment.getExternalStorageDirectory(),"Classico/");
		if (!dir.exists()) dir.mkdir();
		
		dbFile = new File(dir, "classico.db");
		if (!dbFile.exists() || !dbFile.canRead()) {
			Log.d(TAG, "download database");
			new DownloadDialog(this).show();
		}

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
				Dashboard.this.finish();
				break;
			case R.id.d_database_ok:
				new DownloaderAsyncTask().execute(null);
				this.dismiss();
				break;
			default:
				break;
			}
		}
	}

	private class DownloaderAsyncTask extends AsyncTask<Object, Integer, Object> {

		private ProgressDialog progressDialog;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			progressDialog = new ProgressDialog(Dashboard.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Be patient. Database downloading in progress.");
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					dialog.cancel();
					Dashboard.this.finish();
				}
			});
			
			progressDialog.show();
		}
		
		@Override
		protected Object doInBackground(Object... params) {

			URL url;
			try {
				url = new URL(dbLink);

				URLConnection ucon = url.openConnection();
				ucon.setUseCaches(true);
				ucon.setConnectTimeout(5000);

				final int fileLength = ucon.getContentLength();
				InputStream is = ucon.getInputStream();

				FileOutputStream out = new FileOutputStream(dbFile);
				byte[] buffer = new byte[8192];
				int bufferLength = 0;
				float downloadedSize = 0;
				while ((bufferLength = is.read(buffer)) > 0) {
					out.write(buffer, 0, bufferLength);
					downloadedSize += bufferLength;
					publishProgress((int)(downloadedSize/fileLength*100));
				}
				out.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			progressDialog.dismiss();
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			progressDialog.setProgress(values[0]);
		}
	}
}
