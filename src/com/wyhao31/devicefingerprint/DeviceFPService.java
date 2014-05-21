package com.wyhao31.devicefingerprint;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class DeviceFPService extends Service {

	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		PollingUtils.startPollingService(this, 3 * 60 * 60, AutoCollect.class);
		return START_REDELIVER_INTENT;
	}
	
    @Override
    public void onDestroy() {
    	PollingUtils.stopPollingService(this, AutoCollect.class);
    }

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
