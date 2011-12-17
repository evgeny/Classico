package de.evgeny.classico;

import greendroid.app.GDApplication;
import dalvik.system.VMRuntime;

public class ClassicoApplication extends GDApplication {

	public final static String PREF_FILE_NAME = "classico_pref"; 
	public final static String RECENTLY_SHOWED_SCORES = "recently_showed";

	@Override
	public void onCreate() {
		super.onCreate();

		VMRuntime runtime = VMRuntime.getRuntime(); 
		runtime.setMinimumHeapSize(8000000); 
		runtime.setTargetHeapUtilization(0.9f);
	}

	@Override
	public Class<?> getHomeActivityClass() {
		return Dashboard.class;
	}
}
