package de.evgeny.classico;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.WebView;

public class CompositionView extends Activity {
	private static final String TAG = CompositionView.class.getSimpleName();
	
	WebView mWebView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.webview_layout);

	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setBuiltInZoomControls(true);
	    mWebView.loadUrl("http://members.home.nl/yourdesktop/highresolution/9.jpg");
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			
			break;

		default:
			break;
		}
		Log.d(TAG, "don't touch me");
		return super.onTouchEvent(event);
	}
}
