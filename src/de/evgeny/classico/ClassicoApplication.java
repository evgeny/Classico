package de.evgeny.classico;

import greendroid.app.GDApplication;
import dalvik.system.VMRuntime;

public class ClassicoApplication extends GDApplication{
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		VMRuntime runtime = VMRuntime.getRuntime(); 
		runtime.setMinimumHeapSize(8000000); 
		runtime.setTargetHeapUtilization(0.9f); 
	}
	
    @Override
    public Class<?> getHomeActivityClass() {
        return MainActivity.class;
    }
}
