package com.wyhao31.devicefingerprint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoCollect extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v("AutoCollect", "Receive broadcast and start asynctask");
		new DeviceFPCollect().execute(context);
	}
}
