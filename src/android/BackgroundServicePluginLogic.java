package com.kesthers.plugins.bgs;

import java.util.Enumeration;
import java.util.Hashtable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.content.ServiceConnection;

public class BackgroundServicePluginLogic {
	public static final String TAG = BackgroundServicePluginLogic.class.getSimpleName();
	public static final String ACTION_START_SERVICE = "startService";
	public static final String ACTION_STOP_SERVICE = "stopService";
	public static final String ACTION_ENABLE_TIMER = "enableTimer";
	public static final String ACTION_DISABLE_TIMER = "disableTimer";
	public static final String ACTION_SET_CONFIGURATION = "setConfiguration";
	public static final String ACTION_REGISTER_FOR_BOOTSTART = "registerForBootStart";
	public static final String ACTION_DEREGISTER_FOR_BOOTSTART = "deregisterForBootStart";
	public static final String ACTION_GET_STATUS = "getStatus";
	public static final String ACTION_RUN_ONCE = "runOnce";
	public static final String ACTION_REGISTER_FOR_UPDATES = "registerForUpdates";
	public static final String ACTION_DEREGISTER_FOR_UPDATES = "deregisterForUpdates";
	public static final int ERROR_NONE_CODE = 0;
	public static final String ERROR_NONE_MSG = "";
	public static final int ERROR_PLUGIN_ACTION_NOT_SUPPORTED_CODE = -1;
	public static final String ERROR_PLUGIN_ACTION_NOT_SUPPORTED_MSG = "Passed action not supported by Plugin";
	public static final int ERROR_INIT_NOT_YET_CALLED_CODE = -2;
	public static final String ERROR_INIT_NOT_YET_CALLED_MSG = "Please call init prior any other action";
	public static final int ERROR_SERVICE_NOT_RUNNING_CODE = -3;
	public static final String ERROR_SERVICE_NOT_RUNNING_MSG = "Sevice not currently running";
	public static final int ERROR_UNABLE_TO_BIND_TO_BACKGROUND_SERVICE_CODE = -4;
	public static final String ERROR_UNABLE_TO_BIND_TO_BACKGROUND_SERVICE_MSG ="Plugin unable to bind to background service";
	public static final int ERROR_UNABLE_TO_RETRIEVE_LAST_RESULT_CODE = -5;
	public static final String ERROR_UNABLE_TO_RETRIEVE_LAST_RESULT_MSG = "Unable to retrieve latest result (reason unknown)";
	public static final int ERROR_LISTENER_ALREADY_REGISTERED_CODE = -6;
	public static final String ERROR_LISTENER_ALREADY_REGISTERED_MSG = "Listener already registered";
	public static final int ERROR_LISTENER_NOT_REGISTERED_CODE = -7;
	public static final String ERROR_LISTENER_NOT_REGISTERED_MSG = "Listener not registered";
	public static final int ERROR_UNABLE_TO_CLOSED_LISTENER_CODE = -8;
	public static final String ERROR_UNABLE_TO_CLOSED_LISTENER_MSG = "Unable to close listener";
	public static final int ERROR_ACTION_NOT_SUPPORTED__IN_PLUGIN_VERSION_CODE = -9;
	public static final String ERROR_ACTION_NOT_SUPPORTED__IN_PLUGIN_VERSION_MSG = "Action is not supported in this version of the plugin";
	public static final int ERROR_EXCEPTION_CODE = -99;
	
	private Context mContext;
	private Hashtable<String, ServiceDetails> mServices = new Hashtable<String, ServiceDetails>();
	
	public BackgroundServicePluginLogic(Context pContext) {
		this.mContext = pContext;
	}
	
	public boolean isActionValid(String action) {
		boolean result = false;
		
		if(ACTION_START_SERVICE.equals(action)) result = true;
		if(ACTION_STOP_SERVICE.equals(action)) result = true;
		if(ACTION_ENABLE_TIMER.equals(action)) result = true;
		if(ACTION_DISABLE_TIMER.equals(action)) result = true;
		if(ACTION_SET_CONFIGURATION.equals(action)) result = true;
		if(ACTION_REGISTER_FOR_BOOTSTART.equals(action)) result = true;
		if(ACTION_DEREGISTER_FOR_BOOTSTART.equals(action)) result = true;
		if(ACTION_GET_STATUS.equals(action)) result = true;
		if(ACTION_RUN_ONCE.equals(action)) result = true;
		if(ACTION_REGISTER_FOR_UPDATES.equals(action)) result = true;
		if(ACTION_DEREGISTER_FOR_UPDATES.equals(action)) result = true;
		return result;
	}
	
	public ExecuteResult execute(String action, JSONArray data) {
		return execute(action, data, null, null);
	}
	
	public ExecuteResult execute(String action, JSONArray data, IUpdateListener listener, Object[] listenerExtras) {
		ExecuteResult result = null;
		
		try {
			if ((data != null) && (!data.isNull(0)) && (data.get(0) instanceof String) && (data.getString(0).length() > 0)) {
				String serviceName = data.getString(0);
				ServiceDetails service = null;
				
				if (this.mServices.containsKey(serviceName)) {
					service = this.mServices.get(serviceName);
				} else {
					service = new ServiceDetails(this.mContext, serviceName);
					this.mServices.put(serviceName, service);
				}
				
				if (!service.isInitialised()) service.initialise();
				if (ACTION_GET_STATUS.equals(action)) result = service.getStatus();
				if (ACTION_START_SERVICE.equals(action)) result = service.startService();
				if (ACTION_REGISTER_FOR_BOOTSTART.equals(action)) result = service.registerForBootStart();
				if (ACTION_DEREGISTER_FOR_BOOTSTART.equals(action)) result = service.deregisterForBootStart();
				if (ACTION_REGISTER_FOR_UPDATES.equals(action)) result = service.registerForUpdates(listener, listenerExtras);
				if (ACTION_DEREGISTER_FOR_UPDATES.equals(action)) result = service.deregisterForUpdates();
				if (result == null) {
					if (service != null && service.isServiceRunning()) {
						if (ACTION_STOP_SERVICE.equals(action)) result = service.stopService();
						if (ACTION_ENABLE_TIMER.equals(action)) result = service.enableTimer(data);
						if (ACTION_DISABLE_TIMER.equals(action)) result = service.disableTimer();
						if (ACTION_SET_CONFIGURATION.equals(action)) result = service.setConfiguration(data);
						if (ACTION_RUN_ONCE.equals(action)) result = service.runOnce();
					} else {
						result = new ExecuteResult(ExecuteStatus.INVALID_ACTION);
					}
				}
				
				if (result == null) result = new ExecuteResult(ExecuteStatus.INVALID_ACTION);
			} else {
				result = new ExecuteResult(ExecuteStatus.ERROR);
				Log.d(TAG, "ERROR - no servicename");
			}
		} catch (Exception ex) {
			result = new ExecuteResult(ExecuteStatus.ERROR);
			Log.d(TAG, "Exception - " + ex.getMessage());
		}
		
		return result;
	}
	
	public void onDestroy() {
		try {
			if (this.mServices != null && this.mServices.size() > 0 ) {
				Enumeration<String> keys = this.mServices.keys();
				while( keys.hasMoreElements() ) {
					String key = keys.nextElement();
					ServiceDetails service = this.mServices.get(key);
					service.close();
				}
			}
		} catch (Throwable t) {
			Log.d(TAG, "Error has occurred while trying to close services", t);
		}
		
		this.mServices = null;
	}
	
	protected class ServiceDetails {
		public final String LOCALTAG = BackgroundServicePluginLogic.ServiceDetails.class.getSimpleName();
		private String mServiceName = "";
		private Context mContext;
		private BackgroundServiceApi mApi;
		private String mUniqueID = java.util.UUID.randomUUID().toString();
		private boolean mInitialised = false;
		private Intent mService = null;
		private Object mServiceConnectedLock = new Object();
		private Boolean mServiceConnected = null;
		private IUpdateListener mListener = null;
		private Object[] mListenerExtras = null;
		
		public ServiceDetails(Context context, String serviceName) {
			this.mContext = context;
			this.mServiceName = serviceName;
		}
		
		public void initialise() {
			this.mInitialised = true;
			if (this.isServiceRunning()) {
				startService();
			}
		}
		
		public boolean isInitialised() {
			return mInitialised;
		}
		
		public ExecuteResult startService() {
			ExecuteResult result = null;
			try {
				if (this.bindToService()) {
					result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
				} else {
					result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_UNABLE_TO_BIND_TO_BACKGROUND_SERVICE_CODE, ERROR_UNABLE_TO_BIND_TO_BACKGROUND_SERVICE_MSG));
				}
			} catch (Exception ex) {
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult stopService() {
			ExecuteResult result = null;
			try {
				this.mContext.unbindService(serviceConnection);
				result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
			} catch (Exception ex) {
				Log.d(LOCALTAG, "stopService failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult enableTimer(JSONArray data) {
			ExecuteResult result = null;
			int milliseconds = data.optInt(1, 15000);
			try {
				mApi.enableTimer(milliseconds);
				result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
			} catch (RemoteException ex) {
				Log.d(LOCALTAG, "enableTimer failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult disableTimer() {
			ExecuteResult result = null;
			try {
				mApi.disableTimer();
				result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
			} catch (RemoteException ex) {
				Log.d(LOCALTAG, "disableTimer failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult registerForBootStart() {
			ExecuteResult result = null;
			try {
				PropertyHelper.addBootService(this.mContext, this.mServiceName);
				result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
			} catch (Exception ex) {
				Log.d(LOCALTAG, "registerForBootStart failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult deregisterForBootStart() {
			ExecuteResult result = null;
			try {
				PropertyHelper.removeBootService(this.mContext, this.mServiceName);
				result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
			} catch (Exception ex) {
				Log.d(LOCALTAG, "deregisterForBootStart failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult setConfiguration(JSONArray data) {
			ExecuteResult result = null;
			try {
				if (this.isServiceRunning()) {
					Object obj;
					try {
						obj = data.get(1);
						mApi.setConfiguration(obj.toString());
						result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
					} catch (JSONException e) {
						Log.d(LOCALTAG, "Processing config JSON from background service failed", e);
						result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, e.getMessage()));
					}
				} else {
					result = new ExecuteResult(ExecuteStatus.INVALID_ACTION, createJSONResult(false, ERROR_SERVICE_NOT_RUNNING_CODE, ERROR_SERVICE_NOT_RUNNING_MSG));
				}
			} catch (RemoteException ex) {
				Log.d(LOCALTAG, "setConfiguration failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult getStatus() {
			ExecuteResult result = null;
			result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
			return result;
		}
		
		public ExecuteResult runOnce() {
			ExecuteResult result = null;
			try {
				if (this.isServiceRunning()) {
					mApi.run();
					result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
				} else {
					result = new ExecuteResult(ExecuteStatus.INVALID_ACTION, createJSONResult(false, ERROR_SERVICE_NOT_RUNNING_CODE, ERROR_SERVICE_NOT_RUNNING_MSG));
				}
			} catch (RemoteException ex) {
				Log.d(LOCALTAG, "runOnce failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult registerForUpdates(IUpdateListener listener, Object[] listenerExtras) {
			ExecuteResult result = null;
			try {
				if (listener == null) {
					result = new ExecuteResult(ExecuteStatus.INVALID_ACTION, createJSONResult(false, ERROR_ACTION_NOT_SUPPORTED__IN_PLUGIN_VERSION_CODE, ERROR_ACTION_NOT_SUPPORTED__IN_PLUGIN_VERSION_MSG));
				} else {
					if (this.isRegisteredForUpdates()) {
						this.deregisterListener();
					}
					
					this.mListener = listener;
					this.mListenerExtras = listenerExtras;
					result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG), false);
				}
			} catch (Exception ex) {
				Log.d(LOCALTAG, "regsiterForUpdates failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public ExecuteResult deregisterForUpdates() {
			ExecuteResult result = null;
			try {
				if (this.isRegisteredForUpdates()) {
					if (this.deregisterListener()) {
						result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG));
					} else {
						result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_UNABLE_TO_CLOSED_LISTENER_CODE, ERROR_UNABLE_TO_CLOSED_LISTENER_MSG));
					}
				} else {
					result = new ExecuteResult(ExecuteStatus.INVALID_ACTION, createJSONResult(false, ERROR_LISTENER_NOT_REGISTERED_CODE, ERROR_LISTENER_NOT_REGISTERED_MSG));
				}
			} catch (Exception ex) {
				Log.d(LOCALTAG, "deregsiterForUpdates failed", ex);
				result = new ExecuteResult(ExecuteStatus.ERROR, createJSONResult(false, ERROR_EXCEPTION_CODE, ex.getMessage()));
			}
			
			return result;
		}
		
		public void close() {
			try {
				this.deregisterListener();
				mApi.removeListener(serviceListener);
				this.mContext.unbindService(serviceConnection);
			} catch (Exception ex) {
				Log.d(LOCALTAG, "close failed", ex);
				Log.d(LOCALTAG, "Ignoring exception - will continue");
			}
		}
		
		private boolean deregisterListener() {
			boolean result = false;
			if (this.isRegisteredForUpdates()) {
				try {
					this.mListener.closeListener(new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG)), this.mListenerExtras);
				} catch (Exception ex) {
					Log.d("ServiceDetails", "Error occurred while closing the listener", ex);
				}
				
				this.mListener = null;
				this.mListenerExtras = null;
				result = true;
			}
			
			return result;
		}
		
		private boolean bindToService() {
			boolean result = false;
			try {
				Class<?> serviceClass = ReflectionHelper.LoadClass(this.mServiceName);
				this.mService = new Intent(this.mContext, serviceClass);
				this.mContext.startService(this.mService);
				if (this.mContext.bindService(this.mService, serviceConnection, 0)) {
					synchronized(mServiceConnectedLock) {
						while (mServiceConnected==null) {
							try {
								mServiceConnectedLock.wait();
							} catch (InterruptedException e) {
								Log.d(LOCALTAG, "Interrupt occurred while waiting for connection", e);
							}
						}
						
						result = this.mServiceConnected;
					}
				}
			} catch (Exception ex) {
				Log.d(LOCALTAG, "bindToService failed", ex);
			}
			
			return result;
		}
		
		private ServiceConnection serviceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mApi = BackgroundServiceApi.Stub.asInterface(service);
				try {
					mApi.addListener(serviceListener);
				} catch (RemoteException e) {
					Log.d(LOCALTAG, "addListener failed", e);
				}
				
				synchronized(mServiceConnectedLock) {
					mServiceConnected = true;
					mServiceConnectedLock.notify();
				}
			}
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				synchronized(mServiceConnectedLock) {
					mServiceConnected = false;
					mServiceConnectedLock.notify();
				}
			}
		};
		
		private BackgroundServiceListener.Stub serviceListener = new BackgroundServiceListener.Stub() {
			@Override
			public void handleUpdate() throws RemoteException {
				handleLatestResult();
			}
			
			@Override
			public String getUniqueID() throws RemoteException {
				return mUniqueID;
			}
		};
		
		private void handleLatestResult() {
			if (this.isRegisteredForUpdates()) {
				ExecuteResult result = new ExecuteResult(ExecuteStatus.OK, createJSONResult(true, ERROR_NONE_CODE, ERROR_NONE_MSG), false);
				try {
					this.mListener.handleUpdate(result, this.mListenerExtras);
				} catch (Exception ex) {
					Log.d("ServiceDetails", "Listener failed", ex);
					Log.d("ServiceDetails", "Disabling listener");
					this.mListener = null;
					this.mListenerExtras = null;
				}
			} else {
				Log.d("ServiceDetails", "No action performed");
			}
		}
		
		private JSONObject createJSONResult(Boolean success, int errorCode, String errorMessage) {
			JSONObject result = new JSONObject();
			try {
				result.put("Success", success);
				result.put("ErrorCode", errorCode);
				result.put("ErrorMessage", errorMessage);
			} catch (JSONException e) {
				Log.d(LOCALTAG, "Adding basic info to JSONObject failed", e);
			}
			
			if (this.mServiceConnected != null && this.mServiceConnected && this.isServiceRunning()) {
				try { result.put("ServiceRunning", true); } catch (Exception ex) {Log.d(LOCALTAG, "Adding ServiceRunning to JSONObject failed", ex);};
				try { result.put("TimerEnabled", isTimerEnabled()); } catch (Exception ex) {Log.d(LOCALTAG, "Adding TimerEnabled to JSONObject failed", ex);};
				try { result.put("Configuration", getConfiguration()); } catch (Exception ex) {Log.d(LOCALTAG, "Adding Configuration to JSONObject failed", ex);};
				try { result.put("LatestResult", getLatestResult()); } catch (Exception ex) {Log.d(LOCALTAG, "Adding LatestResult to JSONObject failed", ex);};
				try { result.put("TimerMilliseconds", getTimerMilliseconds()); } catch (Exception ex) {Log.d(LOCALTAG, "Adding TimerMilliseconds to JSONObject failed", ex);};
			} else {
				try { result.put("ServiceRunning", false); } catch (Exception ex) {Log.d(LOCALTAG, "Adding ServiceRunning to JSONObject failed", ex);};
				try { result.put("TimerEnabled", null); } catch (Exception ex) {Log.d(LOCALTAG, "Adding TimerEnabled to JSONObject failed", ex);};
				try { result.put("Configuration", null); } catch (Exception ex) {Log.d(LOCALTAG, "Adding Configuration to JSONObject failed", ex);};
				try { result.put("LatestResult", null); } catch (Exception ex) {Log.d(LOCALTAG, "Adding LatestResult to JSONObject failed", ex);};
				try { result.put("TimerMilliseconds", null); } catch (Exception ex) {Log.d(LOCALTAG, "Adding TimerMilliseconds to JSONObject failed", ex);};
			}
			
			try { result.put("RegisteredForBootStart", isRegisteredForBootStart()); } catch (Exception ex) {Log.d(LOCALTAG, "Adding RegisteredForBootStart to JSONObject failed", ex);};
			try { result.put("RegisteredForUpdates", isRegisteredForUpdates()); } catch (Exception ex) {Log.d(LOCALTAG, "Adding RegisteredForUpdates to JSONObject failed", ex);};
			return result;
		}
		
		private boolean isServiceRunning() {
			boolean result = false;
			try {
				ActivityManager manager = (ActivityManager)this.mContext.getSystemService(Context.ACTIVITY_SERVICE);
				for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
					if (this.mServiceName.equals(service.service.getClassName())) {
						result = true;
					}
				}
			} catch (Exception ex) {
				Log.d(LOCALTAG, "isServiceRunning failed", ex);
			}
			
			return result;
		}
		
		private Boolean isTimerEnabled() {
			Boolean result = false;
			try {
				result = mApi.isTimerEnabled();
			} catch (Exception ex) {
				Log.d(LOCALTAG, "isTimerEnabled failed", ex);
			}
			
			return result;
		}
		
		private Boolean isRegisteredForBootStart() {
			Boolean result = false;
			try {
				result = PropertyHelper.isBootService(this.mContext, this.mServiceName);
			} catch (Exception ex) {
				Log.d(LOCALTAG, "isRegisteredForBootStart failed", ex);
			}
			
			return result;
		}
		
		private Boolean isRegisteredForUpdates() {
			if (this.mListener == null) {
				return false;
			} else {
				return true;
			}
		}
		
		private JSONObject getConfiguration() {
			JSONObject result = null;
			try {
				String data = mApi.getConfiguration();
				result = new JSONObject(data);
			} catch (Exception ex) {
				Log.d(LOCALTAG, "getConfiguration failed", ex);
			}
			
			return result;
		}
		
		private JSONObject getLatestResult() {
			JSONObject result = null;
			try {
				String data = mApi.getLatestResult();
				result = new JSONObject(data);
			} catch (Exception ex) {
				Log.d(LOCALTAG, "getLatestResult failed", ex);
			}
			
			return result;
		}
		
		private int getTimerMilliseconds() {
			int result = -1;
			try {
				result = mApi.getTimerMilliseconds();
			} catch (Exception ex) {
				Log.d(LOCALTAG, "getTimerMilliseconds failed", ex);
			}
			
			return result;
		}
	}
	
	protected class ExecuteResult {
		private ExecuteStatus mStatus;
		private JSONObject mData;
		private boolean mFinished = true;
		
		public ExecuteStatus getStatus() {
			return this.mStatus;
		}
		
		public void setStatus(ExecuteStatus pStatus) {
			this.mStatus = pStatus;
		}
		
		public JSONObject getData() {
			return this.mData;
		}
		
		public void setData(JSONObject pData) {
			this.mData = pData;
		}
		
		public boolean isFinished() {
			return this.mFinished;
		}
		
		public void setFinished(boolean pFinished) {
			this.mFinished = pFinished;
		}
		
		public ExecuteResult(ExecuteStatus pStatus) {
			this.mStatus = pStatus;
		}
		
		public ExecuteResult(ExecuteStatus pStatus, JSONObject pData) {
			this.mStatus = pStatus;
			this.mData = pData;
		}
		
		public ExecuteResult(ExecuteStatus pStatus, JSONObject pData, boolean pFinished) {
			this.mStatus = pStatus;
			this.mData = pData;
			this.mFinished = pFinished;
		}
	}
	
	public interface IUpdateListener {
		public void handleUpdate(ExecuteResult logicResult, Object[] listenerExtras);
		public void closeListener(ExecuteResult logicResult, Object[] listenerExtras);
	}
	
	protected enum ExecuteStatus {
		OK,
		ERROR,
		INVALID_ACTION
	}
}