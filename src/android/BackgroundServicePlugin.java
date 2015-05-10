package com.kesthers.plugins.bgs;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import android.util.Log;
import org.apache.cordova.CordovaPlugin;
import com.kesthers.plugins.bgs.BackgroundServicePluginLogic.ExecuteResult;
import com.kesthers.plugins.bgs.BackgroundServicePluginLogic.ExecuteStatus;

public class BackgroundServicePlugin extends CordovaPlugin implements BackgroundServicePluginLogic.IUpdateListener {
	private static final String TAG = BackgroundServicePlugin.class.getSimpleName();
	private BackgroundServicePluginLogic mLogic = null;
	
	@Override
	public boolean execute(final String action, final JSONArray data, final CallbackContext callback) {
		boolean result = false;
		if (this.mLogic == null) {
			this.mLogic = new BackgroundServicePluginLogic(this.cordova.getActivity());
		}
		
		try {
			if (this.mLogic.isActionValid(action)) {
				final BackgroundServicePluginLogic.IUpdateListener listener = this;
				final Object[] listenerExtras = new Object[] { callback };
				
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						ExecuteResult logicResult = mLogic.execute(action, data, listener, listenerExtras);
						PluginResult pluginResult = transformResult(logicResult);
						callback.sendPluginResult(pluginResult);
					}
				});
				
				result = true;
			} else {
				result = false;
			}
		} catch (Exception ex) {
			Log.d(TAG, "Exception - " + ex.getMessage());
		}
		
		return result;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (this.mLogic != null) {
			this.mLogic.onDestroy();
			this.mLogic = null;
		}
	}
	
	public void handleUpdate(ExecuteResult logicResult, Object[] listenerExtras) {
		sendUpdateToListener(logicResult, listenerExtras);
	}
	
	public void closeListener(ExecuteResult logicResult, Object[] listenerExtras) {
		sendUpdateToListener(logicResult, listenerExtras);
	}
	
	private void sendUpdateToListener(ExecuteResult logicResult, Object[] listenerExtras) {
		try {
			if (listenerExtras != null && listenerExtras.length > 0) {
				CallbackContext callback = (CallbackContext)listenerExtras[0];
				callback.sendPluginResult(transformResult(logicResult));
			}
		} catch (Exception ex) {
			Log.d(TAG, "Sending update failed", ex);
		}
	}
	
	private PluginResult transformResult(ExecuteResult logicResult) {
		PluginResult pluginResult = null;
		if (logicResult.getStatus() == ExecuteStatus.OK) {
			if (logicResult.getData() == null) {
				pluginResult = new PluginResult(PluginResult.Status.OK);
			} else {
				pluginResult = new PluginResult(PluginResult.Status.OK, logicResult.getData());
			}
		}
		
		if (logicResult.getStatus() == ExecuteStatus.ERROR) {
			if (logicResult.getData() == null) {
				pluginResult = new PluginResult(PluginResult.Status.ERROR, "Unknown error");
			} else {
				pluginResult = new PluginResult(PluginResult.Status.ERROR, logicResult.getData());
			}
		}
		
		if (logicResult.getStatus() == ExecuteStatus.INVALID_ACTION) {
			if (logicResult.getData() == null) {
				pluginResult = new PluginResult(PluginResult.Status.INVALID_ACTION, "Unknown error");
			} else {
				pluginResult = new PluginResult(PluginResult.Status.INVALID_ACTION, logicResult.getData());
			}
		}
		
		if (!logicResult.isFinished()) {
			pluginResult.setKeepCallback(true);
		}
		
		return pluginResult;
	}
}