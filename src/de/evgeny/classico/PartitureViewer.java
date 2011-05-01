package de.evgeny.classico;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ZoomControls;

public class PartitureViewer extends Activity implements OnTouchListener{

	private static final String TAG = PartitureViewer.class.getSimpleName();

	//database parameters
	private ArrayList<String> mPartiture;
	private int mPartiturePageNumber;

	//navigation
	private ImageButton mNext;
	private ImageButton mPrev;
	private Animation mFadeOutAnimation; 
	private Animation mFadeInAnimation;
	private ZoomControls mZoomControls;

	private Bitmap mOriginBitmap;
	private float[] mOriginMatrixValues = new float[9];
	private float[] mCurrentMatrixValues = new float[9];
	private float mBitmapWidth; 
	private float mBitmapHeight;
	private int mScreenWidth;
	private int mScreenHeight;
	private boolean mFirstTouch = true;

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
		Display display = getWindowManager().getDefaultDisplay(); 
		mScreenWidth = display.getWidth();
		mScreenHeight = display.getHeight();
		setContentView(R.layout.viewer);

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
		try {
			loadImage(mPartiture.get(mPartiturePageNumber));
		} catch (IOException e) {
			Log.e("TAGGGG", "Errorrr: " + e.getMessage());
		}

		mImageView.setOnTouchListener(this);

		mNext = (ImageButton) findViewById(R.id.next);
		mPrev = (ImageButton) findViewById(R.id.prev);

		mFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);

		mFadeOutAnimation.setFillAfter(true);
		mFadeOutAnimation.setFillEnabled(true);

		mNext.startAnimation(mFadeOutAnimation);		
		mPrev.startAnimation(mFadeOutAnimation);
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

		// Dump touch event to LogCat
		//dumpEvent(event);

		// all touch events are handled through this switch statement
		switch (event.getAction() & MotionEvent.ACTION_MASK) {


		case MotionEvent.ACTION_DOWN: //first finger down only
			//savedMatrix.set(matrix);
			savedMatrix.set(view.getImageMatrix());
			start.set(event.getX(), event.getY());
			Log.d(TAG, "mode=DRAG" ); //write to LogCat
			mode = DRAG;

			mNext.startAnimation(mFadeInAnimation);
			mNext.startAnimation(mFadeOutAnimation);
			mPrev.startAnimation(mFadeInAnimation);
			mPrev.startAnimation(mFadeOutAnimation);
			break;
		case MotionEvent.ACTION_UP: //first finger lifted
		case MotionEvent.ACTION_POINTER_UP: //second finger lifted
			mode = NONE;
			Log.d(TAG, "mode=NONE" );					
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

		final float y = (mScreenHeight-(mBitmapHeight*mCurrentMatrixValues[0]))/2;
		final float x = (mScreenWidth-(mBitmapWidth*mCurrentMatrixValues[0]))/2;

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
		if (mPartiturePageNumber > mPartiture.size()) return;
		mPartiturePageNumber++;
		try {
			loadImage(mPartiture.get(mPartiturePageNumber));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void prev(View v) {
		Log.d(TAG, "Prev pressed");
		if (mPartiturePageNumber == 0) return;
		mPartiturePageNumber--;
		try {
			loadImage(mPartiture.get(mPartiturePageNumber));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void loadImage(String uri) throws IOException {
		Log.d(TAG, "Load new Image");
		setProgressBarIndeterminateVisibility(true);
		URL url = new URL(uri);            

		URLConnection ucon = url.openConnection();
		InputStream is = ucon.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(is);

		ByteArrayBuffer baf = new ByteArrayBuffer(50);
		int current = 0;
		while ((current = bis.read()) != -1) {
			baf.append((byte) current);
		}

		mOriginBitmap = BitmapFactory.decodeByteArray(baf.toByteArray(), 0, baf.length());
		mBitmapHeight = mOriginBitmap.getHeight();
		mBitmapWidth = mOriginBitmap.getWidth();
		mImageView.setImageBitmap(mOriginBitmap);	

		mFirstTouch = true;
		
		setProgressBarIndeterminateVisibility(false);
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

	private void dumpEvent(MotionEvent event) {
		String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
				"POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_" ).append(names[actionCode]);
		if (actionCode == MotionEvent.ACTION_POINTER_DOWN
				|| actionCode == MotionEvent.ACTION_POINTER_UP) {
			sb.append("(pid " ).append(
					action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")" );
		}
		sb.append("[" );
		for (int i = 0; i < event.getPointerCount(); i++) {
			sb.append("#" ).append(i);
			sb.append("(pid " ).append(event.getPointerId(i));
			sb.append(")=" ).append((int) event.getX(i));
			sb.append("," ).append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";" );
		}
		sb.append("]" );
		Log.d(TAG, sb.toString());
	}

}