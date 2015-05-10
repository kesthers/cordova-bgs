function BackgroundServiceFactory() { }
BackgroundServiceFactory.prototype.create = function (serviceName) {
	var exec = require("cordova/exec");
	var BackgroundService = function (serviceName) {
		var ServiceName = serviceName;
		this.getServiceName = function() {
			return ServiceName;
		};
	};
	
	BackgroundService.prototype.startService = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'startService', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.stopService = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'stopService', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.enableTimer = function(milliseconds, successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'enableTimer', [this.getServiceName(), milliseconds]);
	};
	
	BackgroundService.prototype.disableTimer = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'disableTimer', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.setConfiguration = function(configuration, successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'setConfiguration', [this.getServiceName(), configuration]);
	};
	
	BackgroundService.prototype.registerForBootStart = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'registerForBootStart', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.deregisterForBootStart = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'deregisterForBootStart', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.isRegisteredForBootStart = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'isRegisteredForBootStart', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.getStatus = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'getStatus', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.runOnce = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'runOnce', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.registerForUpdates = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'registerForUpdates', [this.getServiceName()]);
	};
	
	BackgroundService.prototype.deregisterForUpdates = function(successCallback, failureCallback) {
		return exec(	successCallback, failureCallback, 'BackgroundServicePlugin', 'deregisterForUpdates', [this.getServiceName()]);
	};
	
	var backgroundService = new BackgroundService(serviceName);
	return backgroundService;
};

module.exports = new BackgroundServiceFactory();