package de.evgeny.classico;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.webkit.CacheManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.CacheManager.CacheResult;

public class CompositionView extends Activity {
	private static final String TAG = CompositionView.class.getSimpleName();
	
	WebView mWebView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.webview_layout);

	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setBuiltInZoomControls(true);
	    WebSettings webSettings = mWebView.getSettings();
	    webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
	    
	    saveRemoteImageToCache("http://members.home.nl/yourdesktop/highresolution/9.jpg");
	    
	    //mWebView.loadUrl("http://members.home.nl/yourdesktop/highresolution/9.jpg");
	    //mWebView.setBackgroundDrawable(Drawable.crea);
	}
	
//	private Bitmap getRemoteImage(String imageUrl) {
//        URL aURL = null;
//        URLConnection conn = null;
//        Bitmap bmp = null;
//
//        CacheResult cache_result = CacheManager.getCacheFile(imageUrl, new HashMap());
//
//        if (cache_result == null) {
//            try {
//                aURL = new URL(imageUrl);
//                conn = aURL.openConnection();
//                conn.connect();
//                InputStream is = conn.getInputStream();
//
//                cache_result = new CacheManager.CacheResult();
//                copyStream(is, cache_result.getOutputStream());
//                CacheManager.saveCacheFile(imageUrl, cache_result);
//            } catch (Exception e) {
//                return null;
//            }
//        }
//
//        bmp = BitmapFactory.decodeStream(cache_result.getInputStream());
//        return bmp;
//    }
	
	private void saveRemoteImageToCache(String imageUrl) {
		URL url;
		try {
			url = new URL(imageUrl);
			URLConnection ucon = url.openConnection();
			InputStream is = ucon.getInputStream();
			
			File dir = getExternalCacheDir();
			File file = new File(dir, "1.jpg");
			
			BufferedInputStream bis = new BufferedInputStream(is);
			FileOutputStream fout = new FileOutputStream(file);
			
			int current = 0;
			while ((current = bis.read()) != -1) {
				fout.write(current);
			}
			bis.close();
			fout.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}            	
	}
}
