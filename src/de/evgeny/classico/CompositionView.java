package de.evgeny.classico;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class CompositionView extends Activity {
	WebView mWebView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.webview_layout);

	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setBuiltInZoomControls(true);
	    mWebView.loadUrl("http://members.home.nl/yourdesktop/highresolution/9.jpg");
	}

}
