package de.evgeny.classico;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class CompositionActivity extends Activity{
	
	private final static String TAG = CompositionActivity.class.getSimpleName();
	private String fileName = "test45.jpg";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.notesheet);

		Uri uri = getIntent().getData();
		Cursor cursor = managedQuery(uri, null, null, null, null);

		if (cursor == null) {
			finish();
		} else {
			cursor.moveToFirst();						
			int compositionId = cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION_ID);
			ImageView view = (ImageView) findViewById(R.id.sheet);
			getNotesSheet(compositionId);
		}		
	}

	public void getNotesSheet(final int id) {
		Log.d(TAG, "getNotesSheet(): ");
		ImageView view = (ImageView) findViewById(R.id.sheet);
		try {
			URL url = new URL("http://members.home.nl/yourdesktop/highresolution/9.jpg");            

			Log.d(TAG, "download begining");
			Log.d(TAG, "download url:" + url);
			Log.d(TAG, "downloaded file name:" + fileName);

			URLConnection ucon = url.openConnection();
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);

			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}
			
			view.setImageBitmap(BitmapFactory.decodeByteArray(baf.toByteArray(), 0, baf.length()));
		} catch (IOException e) {
			Log.e(TAG, "Errorrr: " + e.getMessage());
		}
	}
}
