package de.evgeny.classico;

import android.app.IntentService;
import android.content.Intent;

public class LoadBufferAhead extends IntentService {

	private final static String TAG = LoadBufferAhead.class.getSimpleName();
	
	public LoadBufferAhead() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
	}
}
