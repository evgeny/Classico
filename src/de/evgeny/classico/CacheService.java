package de.evgeny.classico;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class CacheService extends Service {

	private static final String TAG = CacheService.class.getSimpleName();

	private final IBinder mBinder = new CacheServiceBinder();
	private Handler mHandler;
	
	private boolean mRestartCacheThread;

	public HashMap<Integer, SoftReference<Bitmap>> cache;
	private final int cacheSize = 4; //use even numbers
	//private int cacheOffset;
	private int mCurrentPageNumber;
	private int lastPageNumber = 1000; //bad decision
	private File imslpDir;
	private final static String WEB_SERVER = "http://scorelocator.appspot.com/image?sid=IMSLP";
	private String mImslp;
	private int mDisplayHeight;

	@Override
	public void onCreate() {	
		super.onCreate();
		lastPageNumber = 1000;
		mCurrentPageNumber = 1;
		//cacheOffset = 0;
		cache = new HashMap<Integer, SoftReference<Bitmap>>();

		//Find the dir to save cached images
		imslpDir = new File(Environment.getExternalStorageDirectory(),"Classico/" + mImslp);			
		if(!imslpDir.exists()) {
			imslpDir.mkdirs();
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	public class CacheServiceBinder extends Binder {

		public void setHandler(final Handler handler) {
			mHandler = handler;
		}

		public void setImslp(final String imslp) {
			mImslp = imslp;
		}

		public void setDisplayHeight(int height) {
			mDisplayHeight = height;
		}

		public void setCurrentPage(int page) {
			mCurrentPageNumber = page;
			mRestartCacheThread = true;
		}

		public Bitmap getPage() {
			return null;
		}
	}

	private String getPageLink(final int pageNumber) {		
		final String url = WEB_SERVER + mImslp
		+ "&h=" + mDisplayHeight
		+ "&page=" + pageNumber; 
		Log.i(TAG, "load page "+ pageNumber +" from url: " + url);
		return url;
	}

	private void saveToCache(final Bitmap bitmap, final int pageNumber) {
		Log.d(TAG, "save page " + pageNumber + " to cache");
		cache.put(pageNumber, new SoftReference<Bitmap>(bitmap));
	}

	private void saveToFile(final Bitmap bitmap, final int pageNumber) {
		File file = new File(imslpDir, pageNumber + ".jpg");
		if (!file.exists()) {
			Log.d(TAG, "save page " + pageNumber + " to file");
			try {
				bitmap.compress(CompressFormat.JPEG, 100, new FileOutputStream(file));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		saveToCache(bitmap, pageNumber);
	}

	private boolean loadFromServer(final int pageNumber) {
		try {
			Log.d(TAG, "load page " + pageNumber + " from server");
			URL url = new URL(getPageLink(pageNumber));

			URLConnection ucon = url.openConnection();
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);

			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}					

			bis.close();
			if (TextUtils.equals(ucon.getURL().getPath(), "/Noimage.svg")) {
				return false;
			}

			final Bitmap bitmap = 
				BitmapFactory.decodeByteArray(baf.toByteArray(), 0, baf.length());
			saveToFile(bitmap, pageNumber);
			return true;
		} catch (IOException e) {
			Log.e(TAG, "Partiture load failed", e);
			return false;
		}
	}

	private boolean loadFromFile(final int pageNumber) {
		final File file = new File(imslpDir, pageNumber + ".jpg");
		if (file.exists()) {
			try {
				Log.d(TAG, "load page " + pageNumber + " from file");
				final Bitmap softBitmap = BitmapFactory.decodeStream(new FileInputStream(file));
				saveToCache(softBitmap, pageNumber);

			} catch (FileNotFoundException e) {			
				e.printStackTrace();
				return false;
			}
			return true;
		} 
		return false;
	}

	private boolean isBitmapInCache(final int pageNumber) {
		Log.d(TAG, "isBitmapInCache " + pageNumber);
		return cache.containsKey(pageNumber);
	}
}
