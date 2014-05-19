package com.wyhao31.devicefingerprint;

import com.wyhao31.devicefingerprint.db.DBManager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

@SuppressLint("Wakelock")
public class AutoCollect extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v("FPAlarm", "Collect");
		new DeviceFPCollect().execute(context);
	}
}
