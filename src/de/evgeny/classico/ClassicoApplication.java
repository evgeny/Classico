package de.evgeny.classico;

import greendroid.app.GDApplication;
import dalvik.system.VMRuntime;

public class ClassicoApplication extends GDApplication {

	public final static String PREF_FILE_NAME = "classico_pref"; 
	public final static String RECENTLY_SHOWED_SCORES = "recently_showed";

//	private LinkedList<String> recentlyScores; 

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

//	public LinkedList<String> getScoresHistory() {
//		if (recentlyScores == null) {
//			recentlyScores = new LinkedList<String>();
//
//			SharedPreferences prefs = getSharedPreferences(PREF_FILE_NAME, 0);
//			if (prefs.contains(RECENTLY_SHOWED_SCORES)) {
//				final String listString = prefs.getString(ClassicoApplication.RECENTLY_SHOWED_SCORES, "");
//				final String[] arr = 
//					listString.substring(1, listString.length() - 2).split(",");
//				recentlyScores.addAll(Arrays.asList(arr));
//			}
//		}
//		return recentlyScores;
//	}

//	public void addScoreToHistory(final String scoreUri) {
//		final LinkedList<String> scores = getScoresHistory();
//		if (!scores.isEmpty()) {
//			if (scores.contains(scoreUri)) {
//				//TODO: reorganize list, move to first
//				return;
//			} else {
//				if (scores.size() >= 10) scores.removeLast();
//				scores.addFirst(scoreUri);
//			}
//		} else {
//			scores.add(scoreUri);
//		}
//		SharedPreferences settings = getSharedPreferences(PREF_FILE_NAME, 0);
//		SharedPreferences.Editor editor = settings.edit();
//		editor.putString(RECENTLY_SHOWED_SCORES, scores.toString());
//
//		editor.commit();
//	}
}
