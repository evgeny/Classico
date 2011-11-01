package de.evgeny.classico;

import java.util.LinkedList;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


public class RecentScoresAdapter extends BaseAdapter {
	
	private static final String TAG = RecentScoresAdapter.class.getSimpleName();
	
    private LayoutInflater mInflater;
    private LinkedList<String> mData;
    private Activity mActivity;
    
    public RecentScoresAdapter(Activity activity, LinkedList<String> data) {
    	mInflater = LayoutInflater.from(activity);
    	mActivity = activity;
    	mData = data;
    }

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(android.R.layout.simple_list_item_2, parent, false);
		}
		
		final Uri uri = Uri.parse(mData.get(position));
		final Cursor cursor = mActivity.managedQuery(uri, null, null, null, null);
		
		Log.d(TAG, cursor.getString(cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSER)));
		Log.d(TAG, cursor.getString(cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION)));
		
		((TextView)convertView.findViewById(android.R.id.text1)).setText(
				cursor.getString(cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSER)));
		
		((TextView)convertView.findViewById(android.R.id.text2)).setText(
				cursor.getString(cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION)));
		return convertView;
	}
}
