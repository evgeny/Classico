package de.evgeny.classico;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

public class ScoreList extends ActionBarActivity implements LoaderCallbacks<Cursor>,
		OnItemClickListener {

	private final static String TAG = ScoreList.class.getSimpleName();
	public final static String TITLE = "Scores";
	private ListView mListView;
	private SimpleCursorAdapter mScoresAdapter;
	private String mComposition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");

		setContentView(R.layout.scores);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));

		getSupportLoaderManager().initLoader(2, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.scores, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Toast.makeText(this, "Tapped home", Toast.LENGTH_SHORT).show();
			break;

		case R.id.menu_share:
			Log.d(TAG, "Share score");
			final String shareString = getString(R.string.share_message, mComposition);
			final Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, shareString);
			startActivity(Intent.createChooser(intent, getText(R.string.title_share)));
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void fillScoresList(final int compositionId) {
		Log.i(TAG, "fillRecentScoresList(): ");

		mScoresAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null,
				new String[] { "imslp", "pages" }, new int[] { android.R.id.text1 }, 0);

		mListView.setAdapter(mScoresAdapter);
		mListView.setOnItemClickListener(this);
		final Bundle bundle = new Bundle();
		bundle.putInt("compositionId", compositionId);
		getSupportLoaderManager().initLoader(1, bundle, this);
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

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		final Cursor cursor = ((SimpleCursorAdapter) arg0.getAdapter()).getCursor();
		cursor.moveToPosition(arg2);
		final Intent partitureViewer = new Intent(getApplicationContext(), GestureActivity.class);

		// send flurry report
		final HashMap<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("imslp", cursor.getString(cursor.getColumnIndex("imslp")));
		FlurryAgent.onEvent("imslp selected", paramsMap);
		partitureViewer.putExtra("imslp", cursor.getString(cursor.getColumnIndex("imslp")));
		partitureViewer.putExtra("pages", cursor.getInt(cursor.getColumnIndex("pages")));
		startActivity(partitureViewer);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case 1:
			final int compositonId = args.getInt("compositionId");
			Uri data = Uri.withAppendedPath(ClassicoProvider.CONTENT_URI,
					"imslp/" + String.valueOf(compositonId));
			Log.d(TAG, "get imslp cursor for uri=" + data.toString());

			return new CursorLoader(ScoreList.this, data, null, null, null, null);

		case 2:
			Uri uri = getIntent().getData();
			Log.d(TAG, "uri=" + uri);
			return new CursorLoader(ScoreList.this, uri, null, null, null, null);
		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		switch (arg0.getId()) {
		case 1:
			mScoresAdapter.swapCursor(cursor);
			break;
		case 2:
			if (cursor.moveToFirst()) {
				final int compositionId = cursor.getInt(cursor
						.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION_ID));
				Log.d(TAG, "composition id = " + compositionId);

				mComposition = cursor.getString(cursor
						.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION));
				final String composer = cursor.getString(cursor
						.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSER));

				final ContentValues values = new ContentValues();
				values.put(ClassicoDatabase.KEY_COMPOSITION, mComposition);
				values.put(ClassicoDatabase.KEY_COMPOSER, composer);
				values.put(ClassicoDatabase.KEY_COMPOSITION_ID, compositionId);
				getContentResolver().insert(ClassicoProvider.RECENT_TITLES_URI, values);

				// cursor.close();

				fillScoresList(compositionId);
				// getExtras();

				// send flurry report
				final HashMap<String, String> paramsMap = new HashMap<String, String>();
				paramsMap.put("title", mComposition);
				FlurryAgent.onEvent("imslp selected", paramsMap);
			} else {
				cursor.close();
				finish();
			}

			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		Log.d(TAG, "onLoaderReset()...");
		switch (arg0.getId()) {
		case 1:
			mScoresAdapter.swapCursor(null);
			break;
		case 2:
			arg0.reset();
			break;
		}
	}

	/**
	 * get meta info for scores, use for this a imslp api
	 */
	// private void getExtras() {
	// String uri =
	// "http://imslp.org/imslpscripts/API.ISCR.php?disclaimer=accepted/account=testaccount/type=3/parent="
	// + Base64.encodeToString(mComposition.getBytes(), Base64.URL_SAFE |
	// Base64.NO_WRAP);
	//
	// HttpGet request = new HttpGet(uri);
	//
	// final HttpClient client = new DefaultHttpClient();
	// HttpResponse response;
	//
	// Log.i(TAG, request.getURI().toString());
	// try {
	// response = client.execute(request);
	// Log.i(TAG, response.toString());
	//
	// JSONArray extras = new JSONArray(
	// convertStreamToString(response.getEntity().getContent()));
	// Log.d(TAG, "LENGTH " + extras.length());
	// Log.d(TAG,
	// extras.getJSONObject(0).getJSONObject("intvals").getString("0"));
	// } catch (ClientProtocolException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (IllegalStateException e) {
	// e.printStackTrace();
	// } catch (JSONException e) {
	// e.printStackTrace();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public static String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;

		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		is.close();
		Log.d(TAG, "extras=" + sb.toString());
		return sb.toString();
	}
}
