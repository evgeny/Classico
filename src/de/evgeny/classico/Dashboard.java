package de.evgeny.classico;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.actionbar.ActionBarActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

/**
 * Start screen of classico app. Show recently partiture.
 * 
 * @author Evgeny Zinovyev
 *
 */
public class Dashboard extends ActionBarActivity implements LoaderCallbacks<Cursor>,
		OnItemClickListener {

	private static final String TAG = Dashboard.class.getSimpleName();
	private static final String dbLink = "https://docs.google.com/uc?id=0B8p4GKsUuQg_ZThkNGQwNTktNTUzNy00ZmFmLWExMGUtNzE2YThlNTBmZDBj&export=download&hl=en_US";

	private File dbFile;
	private SimpleCursorAdapter mRecentlyTitlesAdapter;
	private Dialog mDatabaseDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dashboard);

		// check if database exist
		dbFile = new File(getApplication().getApplicationContext().getExternalCacheDir(),
				"classico.db");
		if (!dbFile.exists() || !dbFile.canRead()) {
			Log.d(TAG, "download database");
			mDatabaseDialog = new DownloadDialog(this);
			mDatabaseDialog.show();
		} else {
			fillRecentScoresList();
		}

		onNewIntent(getIntent());
	}

	@Override
	protected void onResume() {
		super.onResume();

		if ((mDatabaseDialog == null) || (!mDatabaseDialog.isShowing())) {
			getSupportLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		Log.d(TAG, "onNewIntent(): " + intent.getAction());

		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Log.d(TAG, "ACTION_VIEW");
			// handles a click on a search suggestion; launches activity to show
			// composition
			final Intent scoreIntent = new Intent(getApplicationContext(), ScoreList.class);
			scoreIntent.setData(intent.getData());
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
	protected void onStart() {
		super.onStart();

		Log.d(TAG, "onStart(): ");
		FlurryAgent.onStartSession(this, "JE85NZ7FLJEGWB36XYPR");
		FlurryAgent.onPageView();
	}

	@Override
	protected void onStop() {
		super.onStop();

		FlurryAgent.onEndSession(this);
	}

	private void fillRecentScoresList() {
		Log.i(TAG, "fillRecentScoresList(): ");

		ListView recentlyShowed = (ListView) findViewById(R.id.recently_showed);

		mRecentlyTitlesAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2,
				null, new String[] { ClassicoDatabase.KEY_COMPOSER,
						ClassicoDatabase.KEY_COMPOSITION }, new int[] { android.R.id.text1,
						android.R.id.text2 }, 0);

		recentlyShowed.setAdapter(mRecentlyTitlesAdapter);
		recentlyShowed.setOnItemClickListener(this);

		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Toast.makeText(this, "Tapped home", Toast.LENGTH_SHORT).show();
			break;

		case R.id.menu_search:
			onSearchRequested();
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	// @Override
	// public boolean onHandleActionBarItemClick(ActionBarItem item, int
	// position) {
	// switch (item.getItemId()) {
	// case R.id.action_bar_search:
	// onSearchRequested();
	// break;
	// default:
	// return super.onHandleActionBarItemClick(item, position);
	// }
	// return true;
	// }

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
		startActivity(intent);
	}

	public void onComposersClick(View view) {
		final Intent intent = new Intent(getApplicationContext(), ComposerList.class);
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
				new DownloaderAsyncTask().execute();
				// getLoaderManager().initLoader(arg0, arg1, arg2)
				this.dismiss();
				break;
			default:
				break;
			}
		}
	}

	private class DownloaderAsyncTask extends AsyncTask<Object, Integer, Boolean> {

		private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			progressDialog = new ProgressDialog(Dashboard.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Be patient. Database downloading is in progress.");
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
		protected Boolean doInBackground(Object... params) {

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
					publishProgress((int) (downloadedSize / fileLength * 100));
				}
				out.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			progressDialog.dismiss();
			if (result) {
				fillRecentScoresList();
			} else {
				createAlertDialog();
			}

			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			progressDialog.setProgress(values[0]);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(this, ClassicoProvider.RECENT_TITLES_URI, null, null, null,
				"_id DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		mRecentlyTitlesAdapter.swapCursor(arg1);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mRecentlyTitlesAdapter.swapCursor(null);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		Uri data = Uri.withAppendedPath(ClassicoProvider.RECENT_TITLES_URI, String.valueOf(arg3));
		// Toast.makeText(this, data.toString(), Toast.LENGTH_SHORT).show();
		final Intent scoreIntent = new Intent(getApplicationContext(), ScoreList.class);
		scoreIntent.setData(data);
		startActivity(scoreIntent);
	}

	private void createAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("A problem occur by loading database. Would you try again?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						new DownloaderAsyncTask().execute();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						finish();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mRecentlyTitlesAdapter.getCursor() != null)
			mRecentlyTitlesAdapter.getCursor().close();
	}
}
