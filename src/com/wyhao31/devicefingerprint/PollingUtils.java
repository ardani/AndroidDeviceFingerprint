package com.wyhao31.devicefingerprint;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class PollingUtils {

	public static boolean isPollingServiceExist(Context context, Class<?> cls) {
		Intent intent = new Intent(context, cls);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
		/*
		if (pendingIntent != null)
			Log.v("pendingIntent", pendingIntent.toString());
		*/
		Log.v("FPAlarm", pendingIntent != null ? "Exist" : "Not exist");
		return pendingIntent != null;
	}

	// 开启轮询服务
	public static void startPollingService(Context context, int seconds, Class<?> cls) {
		// 获取AlarmManager系统服务
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		// 包装需要执行Service的Intent
		Intent intent = new Intent(context, cls);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// 触发服务的起始时间
		long triggerAtTime = SystemClock.elapsedRealtime();

		// 使用AlarmManger的setRepeating方法设置定期执行的时间间隔（seconds秒）和需要执行的Service
		// manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
		// triggerAtTime + 30 * 1000, seconds * 1000, pendingIntent);
		manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime + 30 * 1000, seconds * 1000, pendingIntent);
		SharedPreferences prefs = context.getSharedPreferences("com.wyhao31.devicefingerprint", Context.MODE_PRIVATE);
		prefs.edit().putBoolean("running", true).commit();
		Log.v("FPAlarm", "Start");
	}

	// 停止轮询服务
	public static void stopPollingService(Context context, Class<?> cls) {
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, cls);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
		// 取消正在执行的服务
		manager.cancel(pendingIntent);
		SharedPreferences prefs = context.getSharedPreferences("com.wyhao31.devicefingerprint", Context.MODE_PRIVATE);
		prefs.edit().putBoolean("running", false).commit();
		Log.v("FPAlarm", "Cancel");
	}
}