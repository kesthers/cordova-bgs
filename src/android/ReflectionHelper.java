package com.kesthers.plugins.bgs;

import android.util.Log;

public class ReflectionHelper {
	public static final String TAG = ReflectionHelper.class.getSimpleName();
	public static Class<?> LoadClass(String className) {
		Class<?> result = null;
		ClassLoader classLoader = ReflectionHelper.class.getClassLoader();
		try {
			result = classLoader.loadClass(className);
		} catch (ClassNotFoundException ex) {
			Log.d(TAG, "Class failed to load");
			Log.d(TAG, ex.getMessage());
		}
		
		return result;
	}
}