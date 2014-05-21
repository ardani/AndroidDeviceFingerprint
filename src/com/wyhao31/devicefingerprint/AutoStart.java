package com.wyhao31.devicefingerprint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			PollingUtils.startPollingService(context, 3 * 60 * 60, AutoCollect.class);
		}
	}
}
