package de.evgeny.classico;

import android.app.Application;

public class ClassicoApplication extends Application {

	public final static String PREF_FILE_NAME = "classico_pref"; 
	public final static String RECENTLY_SHOWED_SCORES = "recently_showed";

	@Override
	public void onCreate() {
		super.onCreate();

//		VMRuntime runtime = VMRuntime.getRuntime(); 
//		runtime.setMinimumHeapSize(8000000); 
//		runtime.setTargetHeapUtilization(0.9f);
	}
}
