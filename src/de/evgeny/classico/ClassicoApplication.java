package de.evgeny.classico;

import dalvik.system.VMRuntime;
import greendroid.app.GDApplication;

public class ClassicoApplication extends GDApplication{
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		VMRuntime runtime = VMRuntime.getRuntime(); 
		runtime.setMinimumHeapSize(8000000); 
		runtime.setTargetHeapUtilization(0.9f); 
	}
}
