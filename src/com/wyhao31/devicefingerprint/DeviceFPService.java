package com.wyhao31.devicefingerprint;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceFPService extends Service {

	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v("DeviceFPService", "Service start.");
		PollingUtils.startPollingService(this, 3 * 60 * 60, AutoCollect.class);
		return START_STICKY;
	}
	
    @Override
    public void onDestroy() {
    	Log.v("DeviceFPService", "Service stop.");
    	PollingUtils.stopPollingService(this, AutoCollect.class);
    	super.onDestroy();
    }

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
