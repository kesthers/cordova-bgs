package com.kesthers.plugins.bgs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import com.kesthers.plugins.bgs.BackgroundServiceApi;

public abstract class BackgroundService extends Service {
	private static final String TAG = BackgroundService.class.getSimpleName();
	private Boolean mServiceInitialised = false;
	private Timer mTimer;
	private final Object mResultLock = new Object();
	private JSONObject mLatestResult = null;
	private List<BackgroundServiceListener> mListeners = new ArrayList<BackgroundServiceListener>();
	private TimerTask mUpdateTask;
	private Date mPausedUntil = null;
	
	public void setPauseDuration(long pauseDuration) {
		this.mPausedUntil = new Date(new Date().getTime() + pauseDuration);
		onPause();
	}
	
	public Boolean getEnabled() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		return sharedPrefs.getBoolean(this.getClass().getName() + ".Enabled", false);
	}
	
	public void setEnabled(Boolean enabled) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putBoolean(this.getClass().getName() + ".Enabled", enabled);
		editor.commit();
	}
	
	public int getMilliseconds() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		return sharedPrefs.getInt(this.getClass().getName() + ".Milliseconds", 60000 );
	}
	
	public void setMilliseconds(int milliseconds) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putInt(this.getClass().getName() + ".Milliseconds", milliseconds);
		editor.commit();
	}
	
	protected JSONObject getLatestResult() {
		synchronized (mResultLock) {
			return mLatestResult;
		}
	}
	
	protected void setLatestResult(JSONObject value) {
		synchronized (mResultLock) {
			this.mLatestResult = value;
		}
	}
	
	public void restartTimer() {
		if (this.mUpdateTask != null) {
			this.mUpdateTask.cancel();
			this.mUpdateTask = null;
			this.mUpdateTask = getTimerTask();
			this.mTimer.schedule(this.mUpdateTask, getMilliseconds(), getMilliseconds());
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return apiEndpoint;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		initialiseService();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		cleanupService();
	}
	
	protected void runOnce() {
		doWorkWrapper();
	}
	
	private BackgroundServiceApi.Stub apiEndpoint = new BackgroundServiceApi.Stub() {
		@Override
		public String getLatestResult() throws RemoteException {
			synchronized (mResultLock) {
				if (mLatestResult == null) {
					return "{}";
				} else {
					return mLatestResult.toString();
				}
			}
		}
		
		@Override
		public void addListener(BackgroundServiceListener listener) throws RemoteException {
			synchronized (mListeners) {
				
			}
		}
		
		@Override
		public void removeListener(BackgroundServiceListener listener) throws RemoteException {
			synchronized (mListeners) {
				if (mListeners.size() > 0) {
					boolean removed = false;
					for (int i = 0; i < mListeners.size() && !removed; i++) {
						if (listener.getUniqueID().equals(mListeners.get(i).getUniqueID())) {
							mListeners.remove(i);
							removed = true;
						}
					}
				}
			}
		}
		
		@Override
		public void enableTimer(int milliseconds) throws RemoteException {
			stopTimerTask();
			setEnabled(true);
			setMilliseconds(milliseconds);
			setupTimerTask();
		}
		
		@Override
		public void disableTimer() throws RemoteException {
			setEnabled(false);
			stopTimerTask();
		}
		
		@Override
		public boolean isTimerEnabled() throws RemoteException {
			return getEnabled();
		}
		
		@Override
		public String getConfiguration() throws RemoteException {
			JSONObject array = getConfig();
			if (array == null) {
				return "";
			} else {
				return array.toString();
			}
		}
		
		@Override
		public void setConfiguration(String configuration) throws RemoteException {
			try {
				JSONObject array = null;
				if (configuration.length() > 0) {
					array = new JSONObject(configuration);
				} else {
					array = new JSONObject();
				}
				
				setConfig(array);
			} catch (Exception ex) {
				throw new RemoteException();
			}
		}
		
		@Override
		public int getTimerMilliseconds() throws RemoteException {
			return getMilliseconds();
		}
		
		@Override
		public void run() throws RemoteException {
			runOnce();
		}
	};
	
	private void initialiseService() {
		if (!this.mServiceInitialised) {
			JSONObject tmp = initialiseLatestResult();
			this.setLatestResult(tmp);
			if (getEnabled()) {
				this.setupTimerTask();
			}
			
			this.mServiceInitialised = true;
		}
	}
	
	private void cleanupService() {
		stopTimerTask();
		if (this.mTimer != null) {
			try {
				this.mTimer.cancel();
				this.mTimer = null;
			} catch (Exception ex) {
				Log.i(TAG, "Exception has occurred - " + ex.getMessage());
			}
		}
	}
	
	private void setupTimerTask () {
		if (this.mTimer == null) {
			this.mTimer = new Timer(this.getClass().getName());
		}
		
		if (this.mUpdateTask == null) {
			this.mUpdateTask = getTimerTask();
			int milliseconds = getMilliseconds();
			this.mTimer.schedule(this.mUpdateTask, 1000L, milliseconds);
		}
		
		onTimerEnabled();
	}
	
	private void stopTimerTask() {
		if (this.mUpdateTask != null) {
			this.mUpdateTask = null;
		}
		
		onTimerDisabled();
	}
	
	
	
	private TimerTask getTimerTask() {
		return new TimerTask() {

			@Override    
			public void run() {       
				Log.i(TAG, "Timer task starting work");

				Log.d(TAG, "Is the service paused?");
				Boolean paused = false;
				if (mPausedUntil != null) {
					Log.d(TAG, "Service is paused until " + (new SimpleDateFormat("dd/MM/yyyy hh:mm:ss")).format(mPausedUntil));
					Date current = new Date();
					Log.d(TAG, "Current is " + (new SimpleDateFormat("dd/MM/yyyy hh:mm:ss")).format(current));
					if (mPausedUntil.after(current)) {
						Log.d(TAG, "Service should be paused");
						paused = true;					// Still paused
					} else {
						Log.d(TAG, "Service should not be paused");
						mPausedUntil = null;				// Paused time has past so we can clear the pause
						onPauseComplete();
					}
				}

				if (paused) {
					Log.d(TAG, "Service is paused");
				} else {
					Log.d(TAG, "Service is not paused");
					
					// Runs the doWork 
					// Sets the last result & updates the listeners
					doWorkWrapper();
				}

				Log.i(TAG, "Timer task completing work");
			}   
		};

	}
	
	// Seperated out to allow the doWork to be called from timer and adhoc (via run method)
	private void doWorkWrapper() {
		JSONObject tmp = null;
		
		try {
			tmp = doWork();
		} catch (Exception ex) {
			Log.i(TAG, "Exception occurred during doWork()", ex);
		}

		Log.i(TAG, "Syncing result");
		setLatestResult(tmp);
		
		// Now call the listeners
		Log.i(TAG, "Sending to all listeners");
		for (int i = 0; i < mListeners.size(); i++)
		{
			try {
				mListeners.get(i).handleUpdate();
				Log.i(TAG, "Sent listener - " + i);
			} catch (RemoteException e) {
				Log.i(TAG, "Failed to send to listener - " + i + " - " + e.getMessage());
			}
		}
		
	}
	
	/*
	 ************************************************************************************************
	 * Methods for subclasses to override 
	 ************************************************************************************************
	 */
	protected abstract JSONObject initialiseLatestResult(); 
	protected abstract JSONObject doWork();
	protected abstract JSONObject getConfig();
	protected abstract void setConfig(JSONObject config);
	
	protected void onTimerEnabled() {
	}

	protected void onTimerDisabled() {
	}
	
	protected void onPause() {
	}
	
	protected void onPauseComplete() {
	}
}
