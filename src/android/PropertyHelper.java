package com.kesthers.plugins.bgs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PropertyHelper {
	private final static String KEY_BOOTSERVICES = "com.kesthers.plugins.bgs.BootServices";
	
	protected static String[] getBootServices(Context context) {
		String serviceList = getBootServicesString(context);
		
		if (serviceList.length() == 0) {
			return null;
		} else {
			return serviceList.split(";");
		}
	}
	
	protected static void addBootService(Context context, String serviceName) {
		String serviceList = getBootServicesString(context);
		
		if (serviceList.length() == 0) {
			serviceList = serviceName;
		} else {
			if (!serviceList.contains(serviceName)) {
				serviceList += ";" + serviceName;
			}
		}
		
		saveBootServices(context, serviceList);
	}
	
	protected static void removeBootService(Context context, String serviceName) {
		String serviceList = getBootServicesString(context);
		String newServiceList = "";
		if (serviceList.length() > 0) {
			if (serviceList.contains(serviceName)) {
				String[] services = serviceList.split(";");
				for (int i = 0; i < services.length; i++) {
					if (!services[i].contains(serviceName)) {
						newServiceList += ";" + services[i];
					}
				}
			}
		}
		
		saveBootServices(context, newServiceList);
	}
	
	protected static boolean isBootService(Context context, String serviceName) {
		String serviceList = getBootServicesString(context);
		boolean result = false;
		
		if (serviceList.length() > 0) {
			if (serviceList.contains(serviceName)) {
				result = true;
			}
		}
		
		return result;
	}
	
	private static void saveBootServices(Context context, String serviceList) {
		SharedPreferences.Editor editor = getEditor(context);
		editor.putString(KEY_BOOTSERVICES, serviceList);
		editor.commit();
	}
	
	private static String getBootServicesString(Context context) {
		SharedPreferences sharedPrefs = getSharedPreferences(context);
		return sharedPrefs.getString(KEY_BOOTSERVICES, "");
	}
	
	private static SharedPreferences getSharedPreferences(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	private static SharedPreferences.Editor getEditor(Context context) {
		SharedPreferences sharedPrefs = getSharedPreferences(context);
		return sharedPrefs.edit();
	}
}