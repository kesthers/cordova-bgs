package com.kesthers.plugins.bgs;   

import com.kesthers.plugins.bgs.BackgroundServiceListener;

interface BackgroundServiceApi {  
	String getLatestResult();     
	
	void addListener(BackgroundServiceListener listener);     
	
	void removeListener(BackgroundServiceListener listener); 
	
	boolean isTimerEnabled();
	
	void enableTimer(int milliseconds);
	
	void disableTimer();
	
	String getConfiguration();
	
	void setConfiguration(String configuration);
	
	int getTimerMilliseconds();
	
	void run();
} 
