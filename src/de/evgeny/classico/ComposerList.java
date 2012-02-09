package de.evgeny.classico;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

public class ComposerList extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final String TAG = ComposerList.class.getSimpleName();
	private ClassicoListAdapter mListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ClassicoListAdapter mListAdapter = new ClassicoListAdapter(getActivity(),
				android.R.layout.simple_list_item_1, null,
				new String[] { ClassicoDatabase.KEY_COMPOSER }, new int[] { android.R.id.text1 });

		setListAdapter(mListAdapter);
		getListView().setFastScrollEnabled(true);
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Log.d(TAG, "onItemClick(): " + arg3);
				final Intent scoresIntent = new Intent(getActivity(),
						CompositionList.class);
				scoresIntent.putExtra("composer", ((TextView) arg1).getText());
				startActivity(scoresIntent);
			}
		});

		// new AsyncCursorLoader().execute(null);

		getActivity().getSupportLoaderManager().initLoader(0, null, this);
	}

	// private Activity getActivity() {
	// return this;
	// }
	
	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(getActivity(), "JE85NZ7FLJEGWB36XYPR");
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(getActivity());
	}

//	private final class AsyncCursorLoader extends AsyncTask<Object, Object, Cursor> {
//
//		private Dialog waitingDialog;
//
//		@Override
//		protected void onPreExecute() {
//			super.onPreExecute();
//
//			Log.d(TAG, "onPreExecute()");
//			waitingDialog = ProgressDialog.show(ComposerList.this, "Wait",
//					"Query database. Please wait...", true);
//			waitingDialog.setCancelable(true);
//			waitingDialog.setOnCancelListener(new OnCancelListener() {
//
//				@Override
//				public void onCancel(DialogInterface dialog) {
//					getActivity().finish();
//				}
//			});
//		}
//
//		@Override
//		protected Cursor doInBackground(Object... params) {
//			Log.d(TAG, "doInBackground(): ");
//
//			Cursor cursor = managedQuery(ClassicoProvider.COMPOSER_URI, null, null, null, null);
//			Log.d(TAG, "" + cursor.getCount());
//
//			return cursor;
//		}
//
//		@Override
//		protected void onPostExecute(Cursor cursor) {
//			super.onPostExecute(cursor);
//
//			Log.d(TAG, "omPostExecute");
//			ClassicoListAdapter listAdapter = new ClassicoListAdapter(ComposerList.this,
//					android.R.layout.simple_list_item_1, cursor,
//					new String[] { ClassicoDatabase.KEY_COMPOSER },
//					new int[] { android.R.id.text1 });
//
//			getListView().setAdapter(listAdapter);
//			getListView().setFastScrollEnabled(true);
//			getListView().setOnItemClickListener(new OnItemClickListener() {
//
//				@Override
//				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
//					Log.d(TAG, "onItemClick(): " + arg3);
//					final Intent scoresIntent = new Intent(getApplicationContext(),
//							CompositionList.class);
//					scoresIntent.putExtra("composer", ((TextView) arg1).getText());
//					scoresIntent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, "Scores");
//					startActivity(scoresIntent);
//				}
//			});
//			waitingDialog.dismiss();
//		}
//	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity(), ClassicoProvider.COMPOSER_URI, null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		mListAdapter.swapCursor(arg1);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mListAdapter.swapCursor(null);

	}
}
