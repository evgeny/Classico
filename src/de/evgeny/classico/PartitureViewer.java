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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ZoomControls;
import de.evgeny.classico.CacheService.CacheServiceBinder;

public class PartitureViewer extends Activity implements OnTouchListener, AnimationListener {

	private static final String TAG = PartitureViewer.class.getSimpleName();

	private final static String WEB_SERVER = "http://scorelocator.appspot.com/image?sid=IMSLP";
	private String mImslp;
	private Dialog mDialog;
	private boolean restartTask = false;
	private boolean taskRunned = false;

	//navigation
	private ImageButton mNext;
	private ImageButton mPrev;
	private Animation mFadeOutAnimation;
	private ZoomControls mZoomControls;

	private Bitmap mOriginBitmap;
	private float[] mOriginMatrixValues = new float[9];
	private float[] mCurrentMatrixValues = new float[9];
	private float mBitmapWidth; 
	private float mBitmapHeight;

	private FrameLayout mFrameLayout;
	private boolean mFirstTouch = true;

	public HashMap<Integer, Bitmap> cache;
	private final int cacheSize = 4; //use even numbers
	//private int cacheOffset;
	private int currentPageNumber;
	//private int loadingPage;
	private int lastPageNumber = 1000; //bad decision
	private File imslpDir;

	//private FillCacheTask cacheTask;

	private ImageView mImageView;

	// These matrices will be used to scale points of the image
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();

	Bitmap originImage = null;

	// The 3 states (events) which the user is trying to perform
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// these PointF objects are used to record the point(s) the user is touching
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist = 1f;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//layout
		final Bundle extras = getIntent().getExtras();
		mImslp = extras.getString("imslp");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.viewer);

		//Find the dir to save cached images
		imslpDir = new File(Environment.getExternalStorageDirectory(),"Classico/" + mImslp);			
		if(!imslpDir.exists()) {
			imslpDir.mkdirs();
		}

		mFrameLayout = (FrameLayout) findViewById(R.id.mainLayout);
		mImageView = (ImageView) findViewById(R.id.image_view);

		//cache
		//cacheOffset = 0;
		cache = new HashMap<Integer, Bitmap>();
		currentPageNumber = 1;
		//start fill cache
		doBindService();
		setBitmap2();
		//		cacheTask = new FillCacheTask();
		//		cacheTask.execute();

		//navigation
		mZoomControls = (ZoomControls) findViewById(R.id.zoomControls);
		mZoomControls.setOnZoomInClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mImageView.setScaleType(ImageView.ScaleType.MATRIX);
				matrix.set(mImageView.getImageMatrix());
				matrix.postScale(1.5f, 1.5f);
				mImageView.setImageMatrix(correctBorder(matrix)); // display the transformation on screen
			}
		});

		mZoomControls.setOnZoomOutClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mImageView.setScaleType(ImageView.ScaleType.MATRIX);
				matrix.set(mImageView.getImageMatrix());
				matrix.postScale(0.5f, 0.5f);				
				mImageView.setImageMatrix(correctBorder(matrix)); // display the transformation on screen

			}
		});

		mImageView.setOnTouchListener(this);

		mNext = (ImageButton) findViewById(R.id.next);
		mPrev = (ImageButton) findViewById(R.id.prev);

		mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);		

		mFadeOutAnimation.setFillAfter(true);
		mFadeOutAnimation.setFillEnabled(true);
		mFadeOutAnimation.setAnimationListener(this);

		//		mNext.startAnimation(mFadeOutAnimation);		
		//		mPrev.startAnimation(mFadeOutAnimation);
		//		mZoomControls.startAnimation(mFadeOutAnimation);
		controllNavigationAnimation();

	}

	private void controllNavigationAnimation() {	
		Log.d(TAG, "controllNavigationAnimation");
		mZoomControls.clearAnimation();
		if (currentPageNumber == 0) {
			mNext.clearAnimation();					
			mNext.startAnimation(mFadeOutAnimation);
			mPrev.startAnimation(mFadeOutAnimation);
		} else if (currentPageNumber == lastPageNumber) {
			mPrev.clearAnimation();
			mPrev.startAnimation(mFadeOutAnimation);
			//mNext.startAnimation(mFadeOutAnimation);
		} else {
			mNext.clearAnimation();		
			mPrev.clearAnimation();
			mNext.startAnimation(mFadeOutAnimation);
			mPrev.startAnimation(mFadeOutAnimation);
		}
		mZoomControls.startAnimation(mFadeOutAnimation);		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.w(TAG, "onTouch(): " + v.toString());

		ImageView view = (ImageView) v;
		view.setScaleType(ImageView.ScaleType.MATRIX);
		float scale;

		if (mFirstTouch) {
			view.getImageMatrix().getValues(mOriginMatrixValues);
			mFirstTouch = false;
		}

		// all touch events are handled through this switch statement
		switch (event.getAction() & MotionEvent.ACTION_MASK) {


		case MotionEvent.ACTION_DOWN: //first finger down only
			savedMatrix.set(view.getImageMatrix());
			start.set(event.getX(), event.getY());
			Log.d(TAG, "mode=DRAG" ); //write to LogCat
			mode = DRAG;


			//			mNext.clearAnimation();		
			//			mPrev.clearAnimation();
			//			mZoomControls.clearAnimation();
			//			mNext.startAnimation(mFadeOutAnimation);
			//			mPrev.startAnimation(mFadeOutAnimation);
			//			mZoomControls.startAnimation(mFadeOutAnimation);
			break;
		case MotionEvent.ACTION_UP: //first finger lifted
		case MotionEvent.ACTION_POINTER_UP: //second finger lifted
			mode = NONE;
			Log.d(TAG, "mode=NONE" );
			controllNavigationAnimation();
			break;

		case MotionEvent.ACTION_POINTER_DOWN: //first and second finger down
			oldDist = spacing(event);
			Log.d(TAG, "oldDist=" + oldDist);
			if (oldDist > 5f) {
				savedMatrix.set(view.getImageMatrix());
				//savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
				Log.d(TAG, "mode=ZOOM" );
			}
			break;

		case MotionEvent.ACTION_MOVE:

			if (mode == DRAG) { //movement of first finger
				Log.d(TAG, "mode=DRAG => DRAG");

				matrix.set(savedMatrix);
				matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); //create the transformation in the matrix of points
			}
			else if (mode == ZOOM) { //pinch zooming
				float newDist = spacing(event);
				Log.d(TAG, "newDist=" + newDist);
				if (newDist > 5f) {
					matrix.set(savedMatrix);

					scale = newDist / oldDist; //setting the scaling of the matrix...if scale > 1 means zoom in...if scale < 1 means zoom out
					matrix.postScale(scale, scale, mid.x, mid.y);
				}
			}

			view.setImageMatrix(correctBorder(matrix)); // display the transformation on screen
			break;
		}
		return true; // indicate event was handled
	} 

	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	/**
	 * don't let the image scroll over visibly range
	 * 
	 * @param matrix
	 * @return
	 */
	private Matrix correctBorder(Matrix matrix) {
		matrix.getValues(mCurrentMatrixValues);

		//		Log.w(TAG, mCurrentMatrixValues[0] + " " + mCurrentMatrixValues[1] + " " + mCurrentMatrixValues[2] + " " +
		//				mCurrentMatrixValues[3] + " " + mCurrentMatrixValues[4] + " " + mCurrentMatrixValues[5] + " " +
		//				mCurrentMatrixValues[6] + " " + mCurrentMatrixValues[7] + " " + mCurrentMatrixValues[8]);

		if (mCurrentMatrixValues[0] < mOriginMatrixValues[0]) {
			mCurrentMatrixValues[0] = mOriginMatrixValues[0];
			mCurrentMatrixValues[4] = mOriginMatrixValues[4];
		} else if (mCurrentMatrixValues[0] > 1.0f) {
			mCurrentMatrixValues[0] = 1.0f;
			mCurrentMatrixValues[4] = 1.0f;
		}

		final float y = (mFrameLayout.getHeight()-(mBitmapHeight*mCurrentMatrixValues[0]))/2;
		final float x = (mFrameLayout.getWidth()-(mBitmapWidth*mCurrentMatrixValues[0]))/2;

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

	public void next(View v) {
		Log.d(TAG, "Next pressed");
		if (currentPageNumber == lastPageNumber) {
			return;
		}
		currentPageNumber++;
		setBitmap2();
	}

	public void prev(View v) {
		Log.d(TAG, "Prev pressed");
		if (currentPageNumber == 1) {
			return;
		}
		currentPageNumber--;
		setBitmap2();
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
							return false;
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
				mDialog = ProgressDialog.show(PartitureViewer.this, "", 
						"Loading. Please wait...", true);
			} else {
				mDialog.dismiss();
				Log.d(TAG, "pages in cache " + cache.size());
				Log.d(TAG, "show page number " + currentPageNumber);
				setBitmap(cache.get(currentPageNumber));
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

	@Override
	public void onAnimationEnd(Animation animation) {
		mNext.setVisibility(View.GONE);
		mPrev.setVisibility(View.GONE);		
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void onAnimationStart(Animation animation) {
		mNext.setVisibility(View.VISIBLE);
		mPrev.setVisibility(View.VISIBLE);

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
		mOriginBitmap = bitmap;
		mBitmapHeight = mOriginBitmap.getHeight();
		mBitmapWidth = mOriginBitmap.getWidth();
		mImageView.setImageBitmap(mOriginBitmap);	
		mImageView.setScaleType(ScaleType.FIT_CENTER);

		mFirstTouch = true;		
	}

	private void setBitmap2() {
		if (isBitmapInCache(currentPageNumber)) {
			setBitmap(cache.get(currentPageNumber));
		} 
		if (!taskRunned) {
			mDialog = ProgressDialog.show(PartitureViewer.this, "", 
					"Loading. Please wait...", true);
			new FillCacheTask().execute(currentPageNumber);
		} else {
			restartTask = true;
		}
	}

	//RESEARCH DISTRICT
	static final int TAKE_PAGE = 0;
	private class ImageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TAKE_PAGE:
				//TODO: dismiss dialog
				mCacheServiceBinder.getPage();
				break;
			default:
				super.handleMessage(msg);
			}			
		}
	}

	private CacheServiceBinder mCacheServiceBinder;

	private ServiceConnection mCacheServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mCacheServiceBinder = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mCacheServiceBinder = (CacheServiceBinder)service;
			mCacheServiceBinder.setHandler(new ImageHandler());
			mCacheServiceBinder.setDisplayHeight(800);
			mCacheServiceBinder.setImslp(mImslp);
		}
	};

	private void doBindService() {
		bindService(new Intent(getApplicationContext(), CacheService.class), 
				mCacheServiceConnection, Context.BIND_AUTO_CREATE);
	}
}