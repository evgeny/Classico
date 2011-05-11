package de.evgeny.classico;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.FloatMath;
import android.util.Log;
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
import android.widget.ZoomControls;

public class PartitureViewer extends Activity implements OnTouchListener, AnimationListener {

	private static final String TAG = PartitureViewer.class.getSimpleName();

	//database parameters
	private ArrayList<String> mPartiture;
	private int mPartiturePageNumber;

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

	private final HashMap<String, SoftReference<Bitmap>> cache = new HashMap<String,  SoftReference<Bitmap>>();
	private File cacheDir;

	private static final float MIN_ZOOM = 1f;
	private static final float MAX_ZOOM = 2f;

	private ImageView mImageView;

	// These matrices will be used to scale points of the image
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();

	//Bitmap originImage = null;

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

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.viewer);

		mFrameLayout = (FrameLayout) findViewById(R.id.mainLayout);
		
		mImageView = (ImageView) findViewById(R.id.image_view);
		mPartiture = new ArrayList<String>();
		fillData();
		mPartiturePageNumber = 0;
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
		new DownloadPartitureTask().execute(mPartiture.get(mPartiturePageNumber));

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
		if (mPartiturePageNumber == 0) {
			mNext.clearAnimation();					
			mNext.startAnimation(mFadeOutAnimation);
			mPrev.startAnimation(mFadeOutAnimation);
		} else if (mPartiturePageNumber == (mPartiture.size() - 1)) {
			mPrev.clearAnimation();
			mPrev.startAnimation(mFadeOutAnimation);
			mNext.startAnimation(mFadeOutAnimation);
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
		if (View.VISIBLE != v.getVisibility()) return;
		if (mPartiturePageNumber > mPartiture.size()) return;
		mPartiturePageNumber++;
		new DownloadPartitureTask().execute(mPartiture.get(mPartiturePageNumber));
	}

	public void prev(View v) {
		Log.d(TAG, "Prev pressed");
		if (View.VISIBLE != mPrev.getVisibility()) return;
		if (mPartiturePageNumber == 0) return;
		mPartiturePageNumber--;
		new DownloadPartitureTask().execute(mPartiture.get(mPartiturePageNumber));
	}

	private void fillData() {
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/1.jpg");
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/2.jpg");
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/3.jpg");
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/4.jpg");
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/5.jpg");
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/6.jpg");
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/7.jpg");
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/8.jpg");
		mPartiture.add("http://members.home.nl/yourdesktop/highresolution/9.jpg");
	}

	private class DownloadPartitureTask extends AsyncTask<String, Void, Bitmap> {
		private ProgressDialog dialog;

		@Override
		protected void onPreExecute() {		
			super.onPreExecute();
			dialog = ProgressDialog.show(PartitureViewer.this, "", 
					"Loading. Please wait...", true);
		}

		@Override
		protected Bitmap doInBackground(String... params) {			
			Log.d(TAG, "Load new partiture sheet");		

			//Find the dir to save cached images
			cacheDir=new File(Environment.getExternalStorageDirectory(),"Partitures");			
			if(!cacheDir.exists()) {
				cacheDir.mkdirs();
			}

			try{
				URL url = new URL(params[0]);
				
//				if (cache.containsKey(url.toString())) {
//					final File file = new File(dir, cache.get);
//					BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
//					loadPageFromCache(url);
//				}

				URLConnection ucon = url.openConnection();
				InputStream is = ucon.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);

				ByteArrayBuffer baf = new ByteArrayBuffer(50);
				int current = 0;
				while ((current = bis.read()) != -1) {
					baf.append((byte) current);
				}			
				return BitmapFactory.decodeByteArray(baf.toByteArray(), 0, baf.length());
			} catch (IOException e) {
				Log.e(TAG, "Partiture load failed", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);

			mOriginBitmap = result;
			mBitmapHeight = mOriginBitmap.getHeight();
			mBitmapWidth = mOriginBitmap.getWidth();
			mImageView.setImageBitmap(mOriginBitmap);	

			mFirstTouch = true;	
			dialog.dismiss();
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
}