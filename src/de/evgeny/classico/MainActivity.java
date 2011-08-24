package de.evgeny.classico;

import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ActionBarItem.Type;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MainActivity extends GDActivity {
	private final static String TAG = MainActivity.class.getSimpleName();

	//	public static final int LOAD_DATA_START = 0;
	//	public static final int LOAD_DATA_FINISH = 1;

	private TextView mTextView;
	private ListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//setContentView(R.layout.main);
		setActionBarContentView(R.layout.main);
		
		addActionBarItem(Type.Search, R.id.action_bar_search);

		Log.d(TAG, "onCreate(): ");	
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		//setContentView(R.layout.main);	

		mTextView = (TextView) findViewById(R.id.text);
		mListView = (ListView) findViewById(R.id.list_titles);

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
			startActivity(scoreIntent);
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// handles a search query
			final String query = intent.getStringExtra(SearchManager.QUERY);
			Log.d(TAG, "ACTION_SEARCH, query: " + query);
			showResults(query);
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

	/**
	 * Searches the dictionary and displays results for the given query.
	 * @param query The search query
	 */
	private void showResults(String query) {

		Cursor cursor = managedQuery(ComposerProvider.CONTENT_URI, null, null,
				new String[] {query}, null);

		if (cursor == null) {
			// There are no results
			mTextView.setText(getString(R.string.no_results, new Object[] {query}));
		} else {
			// Display the number of results
			int count = cursor.getCount();
			String countString = getResources().getQuantityString(R.plurals.search_results,
					count, new Object[] {count, query});
			mTextView.setText(countString);

			// Specify the columns we want to display in the result
			String[] from = new String[] { ClassicoDatabase.KEY_COMPOSER,
					ClassicoDatabase.KEY_COMPOSITION };

			// Specify the corresponding layout elements where we want the columns to go
			int[] to = new int[] { R.id.composer,
					R.id.composition };

			// Create a simple cursor adapter for the definitions and apply them to the ListView
			SimpleCursorAdapter words = new SimpleCursorAdapter(this,
					R.layout.result, cursor, from, to);
			mListView.setAdapter(words);

			// Define the on-click listener for the list items
			mListView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					// Build the Intent used to open WordActivity with a specific word Uri					
					final Intent scoreIntent = new Intent(getApplicationContext(), ScoreList.class);	
					Uri data = Uri.withAppendedPath(ComposerProvider.CONTENT_URI,
							String.valueOf(id));
					scoreIntent.setData(data);
					startActivity(scoreIntent);
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.search:
			onSearchRequested();
			return true;
		case R.id.exit:
			finish();
			return true;
		default:
			return false;
		}
	}
}