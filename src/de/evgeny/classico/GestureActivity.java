package de.evgeny.classico;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class GestureActivity extends Activity {

	private static final String TAG = GestureDetector.class.getSimpleName();
	
	private final static String WEB_SERVER = "http://scorelocator.appspot.com/image?sid=IMSLP";
	private String mImslp;
	private Dialog mDialog;
	private boolean restartTask = false;
	private boolean taskRunned = false;

	private Bitmap mOriginBitmap;
	private float[] mOriginMatrixValues = new float[9];
	private float[] mCurrentMatrixValues = new float[9];
	private float mBitmapWidth; 
	private float mBitmapHeight;

	private boolean mFirstTouch = true;

	public HashMap<Integer, Bitmap> cache;
	private final int cacheSize = 4; //use even numbers
	private int currentPageNumber;
	private int lastPageNumber = 1000; //bad decision, but it's work
	private File imslpDir;

	private ImageView mImageView;

	// These matrices will be used to scale points of the image
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();

	Bitmap originImage = null;

	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;

	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 1000;
    
    private static Display sDisplay;
    
	Matrix mMatrix = new Matrix();
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Bundle extras = getIntent().getExtras();
		mImslp = extras.getString("imslp");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.gesture_layout);

		//Find the dir to save cached images
		imslpDir = new File(Environment.getExternalStorageDirectory(),"Classico/" + mImslp);			
		if(!imslpDir.exists()) {
			imslpDir.mkdirs();
		}
		
		sDisplay = ((WindowManager)
				getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		mGestureDetector = new GestureDetector(this, new GestureListener(), null, true);
		mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

		mImageView = (ImageView) findViewById(R.id.image_view);
		
		//cache init
		cache = new HashMap<Integer, Bitmap>();
		currentPageNumber = 1;
		//start fill cache
		setBitmap2();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mFirstTouch) {
			mImageView.getImageMatrix().getValues(mOriginMatrixValues);
			mFirstTouch = false;
		}
		
		mScaleGestureDetector.onTouchEvent(event);
		if (!mScaleGestureDetector.isInProgress()) mGestureDetector.onTouchEvent(event);
		
		mImageView.setImageMatrix(correctBorder(mImageView.getImageMatrix()));
		return true;
	}

	private Matrix correctBorder(final Matrix matrix) {
		matrix.getValues(mCurrentMatrixValues);

		if (mCurrentMatrixValues[0] < mOriginMatrixValues[0]) {
			mCurrentMatrixValues[0] = mOriginMatrixValues[0];
			mCurrentMatrixValues[4] = mOriginMatrixValues[4];
		} else if (mCurrentMatrixValues[0] > 1.0f) {
			mCurrentMatrixValues[0] = 1.0f;
			mCurrentMatrixValues[4] = 1.0f;
		}

		final float y = (sDisplay.getHeight()-(mBitmapHeight*mCurrentMatrixValues[0]))/2;
		final float x = (sDisplay.getWidth()-(mBitmapWidth*mCurrentMatrixValues[0]))/2;

		if (x < 0) {
			if (mCurrentMatrixValues[2] > 0) {
				mCurrentMatrixValues[2] = 0;
			}
			if (mCurrentMatrixValues[2] < (x*2)) {
				mCurrentMatrixValues[2] = x*2;
			}
		} else {
			mCurrentMatrixValues[2] = x;
		}

		if (y < 0) {
			if (mCurrentMatrixValues[5] > 0) {
				mCurrentMatrixValues[5] = 0;
			}
			if (mCurrentMatrixValues[5] < (y*2)) {
				mCurrentMatrixValues[5] = y*2;
			} 
		} else {
			mCurrentMatrixValues[5] = y;
		}
		matrix.setValues(mCurrentMatrixValues);
		return matrix;
	}


	private class GestureListener extends SimpleOnGestureListener {

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				// right to left swipe
				if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					if (currentPageNumber == lastPageNumber) {
						return false;
					}
					currentPageNumber++;
					setBitmap2();
				}  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					if (currentPageNumber == 1) {
						return false;
					}
					currentPageNumber--;
					setBitmap2();
				}
			} catch (Exception e) {
				// nothing
			}
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			Log.d(TAG, "onScroll");
			mMatrix.set(mImageView.getImageMatrix());
			mMatrix.postTranslate(-distanceX, -distanceY);
			mImageView.setImageMatrix(mMatrix);

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

	private class ScaleListener extends SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mImageView.setScaleType(ImageView.ScaleType.MATRIX);
			mMatrix.set(mImageView.getImageMatrix());
			mMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor());
			mImageView.setImageMatrix(mMatrix);
			Log.d(TAG, "onScale with factor " + detector.getScaleFactor());

			return super.onScale(detector);
		}
	}
	
	private class FillCacheTask extends AsyncTask<Integer, Boolean, Boolean> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			taskRunned = true;
		}

		@Override
		protected Boolean doInBackground(Integer... params) {
			Log.i(TAG, "Re/Fill cache");
			int taskCurrentPage = params[0];
			int cacheOffset = 0;
			Log.i(TAG, "current page is " + taskCurrentPage);

			//compute offset
			if (taskCurrentPage > cacheSize / 2) {
				cacheOffset = taskCurrentPage - cacheSize / 2;
			} 
			if (taskCurrentPage >= lastPageNumber - cacheSize / 2) {
				cacheOffset = taskCurrentPage - cacheSize / 2;
			}

			Log.i(TAG, "cache offset = " + cacheOffset);
			//(re)fill cache
			for (int i = cacheOffset + 1; i <= cacheOffset + cacheSize; i++) {
				if (i > lastPageNumber) {
					publishProgress(false);
					break;
				}
				if (restartTask) break;
				if (!isBitmapInCache(i)) {
					if (!loadFromFile(i)) {
						if (!loadFromServer(i)) {
							lastPageNumber = i - 1;
							Log.d(TAG, "last avaible page is " + lastPageNumber);
						}
					}
				}
				//dismiss dialog if current page was loaded
				if (i == currentPageNumber) {
					publishProgress(false);
				}			
			}
			Log.d(TAG, "background task finished");
			return null;
		}

		@Override
		protected void onProgressUpdate(Boolean... values) {
			super.onProgressUpdate(values);
			if (values[0]) {
				mDialog = ProgressDialog.show(GestureActivity.this, "", 
						"Loading. Please wait...", true);
			} else {
				mDialog.dismiss();
				Log.d(TAG, "pages in cache " + cache.size());
				Log.d(TAG, "show page number " + currentPageNumber);
				if(isBitmapInCache(currentPageNumber)) {
					setBitmap(cache.get(currentPageNumber));
				} else {
					Log.e(TAG, "Unknown error: ");
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (restartTask) {
				restartTask = false;
				new FillCacheTask().execute(currentPageNumber);
			} else {
				taskRunned = false;
			}
		}
	}

	private String getPageLink(final int pageNumber) {
		final Display display = getWindowManager().getDefaultDisplay(); 
		final String url = WEB_SERVER + mImslp
		+ "&h=" + display.getHeight()
		+ "&page=" + pageNumber; 
		Log.d(TAG, "load page "+ pageNumber +" from url: " + url);
		return url;
	}

	private void saveToCache(final Bitmap bitmap, final int pageNumber) {
		Log.d(TAG, "save page " + pageNumber + " to cache");
		cache.put(pageNumber, bitmap);
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

	private void setBitmap(final Bitmap bitmap) {
		//		if (mOriginBitmap != null) {
		//			mOriginBitmap.recycle();
		//		}
		Log.d(TAG, "pixels=" + bitmap.getRowBytes());
		mOriginBitmap = bitmap;
		mBitmapHeight = mOriginBitmap.getHeight();
		mBitmapWidth = mOriginBitmap.getWidth();
		mImageView.setImageBitmap(bitmap);	
		mImageView.setScaleType(ScaleType.FIT_CENTER);

		mFirstTouch = true;
	}

	private void setBitmap2() {
		if (isBitmapInCache(currentPageNumber)) {
			setBitmap(cache.get(currentPageNumber));
		} 
		if (!taskRunned) {
			mDialog = ProgressDialog.show(GestureActivity.this, "", 
					"Loading. Please wait...", true);
			new FillCacheTask().execute(currentPageNumber);
		} else {
			restartTask = true;
		}
	}
}