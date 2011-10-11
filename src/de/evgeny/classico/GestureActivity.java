package de.evgeny.classico;

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

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

public class GestureActivity extends Activity {

	private static final String TAG = GestureActivity.class.getSimpleName();
	
	private final static String WEB_SERVER = "http://www.peachnote.com/rest/api/v0/image?sid=IMSLP";
	private final static float maxScaleFactor = 3.0f;
	private String mImslp;
	
	private ProgressDialog mDialog;
	
	private boolean restartTask = false;
	private boolean taskRunned = false;
	
//	private String pageNumberText;

	private Bitmap mOriginBitmap;
	private float[] mOriginMatrixValues = new float[9];
	private float[] mCurrentMatrixValues = new float[9];
	private float mBitmapWidth; 
	private float mBitmapHeight;

	private boolean mFirstTouch = true;
	private float oldScale = 0;

	private HashMap<Integer, SoftReference<Bitmap>> cache; 		
	
	private final int cacheSize = 4; //use even numbers
	private int currentPageNumber;
	private int lastPageNumber;// = 1000; //bad decision, but it's work for now
	private File imslpDir;

	private ImageView mImageView;

	// These matrixes will be used to scale points of the image
	Matrix currentMatrix = new Matrix();

	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;

	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 1000;
    
    private static View sDecorView;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Bundle extras = getIntent().getExtras();
		mImslp = extras.getString("imslp");
		lastPageNumber = extras.getInt("pages");
		if (lastPageNumber == 0) {
			lastPageNumber = 1000;
		}
		
		if (savedInstanceState != null) {
			currentPageNumber = savedInstanceState.getInt("currentpage", 1);
		} else {
			currentPageNumber = 1;
		}
		setContentView(R.layout.gesture_layout);
		
		Log.d(TAG, "current page=" + currentPageNumber);

		mDialog = new ProgressDialog(this);
		mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mDialog.setMessage("Loading next page...");


		//Find the dir to save cached images
		imslpDir = new File(Environment.getExternalStorageDirectory(),"Classico/" + mImslp);			
		if(!imslpDir.exists()) {
			imslpDir.mkdirs();
		}
		
		sDecorView = getWindow().getDecorView();

		mGestureDetector = new GestureDetector(this, new GestureListener(), null, true);
		mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

		mImageView = (ImageView) findViewById(R.id.image_view);
		
		//cache init
		cache = new HashMap<Integer, SoftReference<Bitmap>>();
		
		//show current page number
		Toast.makeText(this, currentPageNumber + "/" + lastPageNumber, Toast.LENGTH_SHORT).show();
		
		//start fill cache
		getBitmap();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		Log.d(TAG, "onSaveInstanceState " + currentPageNumber);
		outState.putInt("currentpage", currentPageNumber);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mFirstTouch) {
			mImageView.getImageMatrix().getValues(mOriginMatrixValues);
			mFirstTouch = false;
		}
		
		mScaleGestureDetector.onTouchEvent(event);
		if (!mScaleGestureDetector.isInProgress()) 
			mGestureDetector.onTouchEvent(event);
		
		return true;
	}

	private Matrix correctBorder(final Matrix matrix) {
		Log.d(TAG, "correct boarder");
		matrix.getValues(mCurrentMatrixValues);

		if (mCurrentMatrixValues[0] < mOriginMatrixValues[0]) {
			mCurrentMatrixValues[0] = mOriginMatrixValues[0];
			mCurrentMatrixValues[4] = mOriginMatrixValues[4];
		} else if (mCurrentMatrixValues[0] > maxScaleFactor) {
			mCurrentMatrixValues[0] = maxScaleFactor;
			mCurrentMatrixValues[4] = maxScaleFactor;
		}

		final float y = (sDecorView.getHeight()-(mBitmapHeight*mCurrentMatrixValues[0]))/2;
		final float x = (sDecorView.getWidth()-(mBitmapWidth*mCurrentMatrixValues[0]))/2;

		if (x <= 0) {
			if (mCurrentMatrixValues[2] > 0) {
				mCurrentMatrixValues[2] = 0;
			}
			if (mCurrentMatrixValues[2] < (x*2)) {
				mCurrentMatrixValues[2] = x*2;
			}
		} else {
			mCurrentMatrixValues[2] = x;
		}

		if (y <= 0) {
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
				Log.d(TAG, "onFling(): ");
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				// right to left swipe
				if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					if (currentPageNumber == lastPageNumber) {
						return false;
					}
					currentPageNumber++;
					Toast.makeText(GestureActivity.this, currentPageNumber + "/" + lastPageNumber, Toast.LENGTH_SHORT).show();
					getBitmap();
				}  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					if (currentPageNumber == 1) {
						return false;
					}
					currentPageNumber--;
					Toast.makeText(GestureActivity.this, currentPageNumber + "/" + lastPageNumber, Toast.LENGTH_SHORT).show();
					getBitmap();
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
			currentMatrix.set(mImageView.getImageMatrix());
			currentMatrix.postTranslate(-distanceX, -distanceY);
			mImageView.setImageMatrix(correctBorder(currentMatrix));

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

	private class ScaleListener extends SimpleOnScaleGestureListener {
		
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			if (oldScale == 0)
				oldScale = detector.getScaleFactor();
			
			if (Math.abs(oldScale - detector.getScaleFactor()) < 0.03) return false;
			else oldScale = detector.getScaleFactor();						
						
			mImageView.setScaleType(ImageView.ScaleType.MATRIX);
			currentMatrix.set(mImageView.getImageMatrix());			
			currentMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(),
					detector.getFocusX(), detector.getFocusY());
			
			mImageView.setImageMatrix(correctBorder(currentMatrix));
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
			if (cacheOffset < 0) cacheOffset = 0;
			
			Log.d(TAG, "cache offset = " + cacheOffset);
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
							if (lastPageNumber == 0) {
								return false;						
							}
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
			return true;
		}

		@Override
		protected void onProgressUpdate(Boolean... values) {
			super.onProgressUpdate(values);
			if (values[0]) {
				Log.d(TAG, "show dialog");
				mDialog.show();
			} else {
				Log.d(TAG, "dismiss dialog");
				mDialog.dismiss();
				Log.d(TAG, "pages in cache " + cache.size());
				Log.d(TAG, "show page number " + currentPageNumber);
				if(isBitmapInCache(currentPageNumber)) {
					setBitmap(cache.get(currentPageNumber).get());
				} else {
					Log.e(TAG, "Unknown error: ");
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (!result) {
				mDialog.dismiss();
				Toast.makeText(getApplicationContext(), 
						"No pages for this score avaible", Toast.LENGTH_LONG).show();
			}
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
		+ "&page=" + pageNumber
		+ "&h=" + display.getHeight();
		 
		Log.d(TAG, "load page "+ pageNumber +" from url: " + url);
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
			ucon.setUseCaches(true);
			ucon.setConnectTimeout(5000);

			final int fileLength = ucon.getContentLength();
			InputStream is = ucon.getInputStream();

			if (TextUtils.equals(ucon.getURL().getPath(), "/Noimage.svg")) {
				return false;
			}
            byte[] buffer = new byte[8192];
            int bufferLength = 0;
            float downloadedSize = 0;
            
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
            while ((bufferLength = is.read(buffer)) > 0) {
            	baf.append(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                mDialog.setProgress((int)(downloadedSize/fileLength*100));
            }
            final Bitmap bitmap = BitmapFactory.decodeByteArray(baf.toByteArray(), 0, baf.length());
            if (bitmap == null) return false;
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
		return (cache.containsKey(pageNumber) && (cache.get(pageNumber).get() != null));
	}

	private void setBitmap(final Bitmap bitmap) {
		mOriginBitmap = bitmap;
		mBitmapHeight = mOriginBitmap.getHeight();
		mBitmapWidth = mOriginBitmap.getWidth();
		mImageView.setImageBitmap(bitmap);	
		mImageView.setScaleType(ScaleType.FIT_CENTER);

		mFirstTouch = true;
	}

	private void getBitmap() {
		if (isBitmapInCache(currentPageNumber)) {
			Log.d(TAG, "bitmap " + currentPageNumber + " is in cache");
			setBitmap(cache.get(currentPageNumber).get());
			return;
		} 
		Log.d(TAG, "show dialog");
		mDialog.setProgress(0);
		mDialog.show();
		if (!taskRunned) {
			Log.d(TAG, "bitmap " + currentPageNumber + " not in cache, " +
					"start asynctask");
			new FillCacheTask().execute(currentPageNumber);
		} else {
			Log.d(TAG, "bitmap " + currentPageNumber + " not in cache, " +
			"asynctask will be restarted");
			restartTask = true;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		mDialog.dismiss();
	}
}