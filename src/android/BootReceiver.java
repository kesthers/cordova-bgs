package com.kesthers.plugins.bgs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String[] serviceList = PropertyHelper.getBootServices(context);
		
		if (serviceList != null) {
			for (int i = 0; i < serviceList.length; i++) {
				Class<?> serviceClass = ReflectionHelper.LoadClass(serviceList[i]);
				Intent serviceIntent = new Intent(context, serviceClass);
				context.startService(serviceIntent);
			}
		}
	}
}