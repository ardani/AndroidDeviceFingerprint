package com.wyhao31.devicefingerprint;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.Adler32;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import com.wyhao31.devicefingerprint.db.DBManager;
import com.wyhao31.devicefingerprint.db.updatehistoryItem;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

public class DeviceFPCollect extends AsyncTask<Context, Void, Void> {

	private WakeLock wl = null;

	public DeviceFPCollect() {
		this.wl = null;
	}

	public DeviceFPCollect(WakeLock wl) {
		this.wl = wl;
	}

	private String runShellCMD(String cmdline) throws IOException {
		Process process = Runtime.getRuntime().exec(cmdline);
		InputStream is = process.getInputStream();
		SystemClock.sleep(2000);
		byte[] buff = new byte[4096];
		String result = "";
		while (is.available() > 0) {
			int readed = is.read(buff);
			if (readed <= 0)
				break;
			String seg = new String(buff, 0, readed);
			result += seg;
		}
		if (result == "")
			result = "unknown";
		return result;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private String getSerialNum() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
			return android.os.Build.SERIAL;
		else
			return "";
	}

	public boolean isDeviceRooted() {
		return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
	}

	public boolean checkRootMethod1() {
		String buildTags = android.os.Build.TAGS;
		return buildTags != null && buildTags.contains("test-keys");
	}

	public boolean checkRootMethod2() {
		try {
			File file = new File("/system/app/Superuser.apk");
			return file.exists();
		} catch (Exception e) {
			return false;
		}
	}

	public boolean checkRootMethod3() {
		try {
			return runShellCMD("/system/xbin/which su") == "unknown";
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean externalMemoryAvailable() {
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	public static String getAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return formatSize(availableBlocks * blockSize);
	}

	public static String getTotalInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return formatSize(totalBlocks * blockSize);
	}

	public static String getAvailableExternalMemorySize() {
		if (externalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();
			return formatSize(availableBlocks * blockSize);
		} else {
			return "Unavailable";
		}
	}

	public static String getTotalExternalMemorySize() {
		if (externalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long totalBlocks = stat.getBlockCount();
			return formatSize(totalBlocks * blockSize);
		} else {
			return "Unavailable";
		}
	}

	public static String formatSize(long size) {
		String suffix = null;

		if (size >= 1024) {
			suffix = "KB";
			size /= 1024;
			if (size >= 1024) {
				suffix = "MB";
				size /= 1024;
			}
		}

		StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

		int commaOffset = resultBuffer.length() - 3;
		while (commaOffset > 0) {
			// resultBuffer.insert(commaOffset, ',');
			commaOffset -= 3;
		}

		if (suffix != null)
			resultBuffer.append(suffix);
		return resultBuffer.toString();
	}

	public HashMap<String, String> collectFP(Context context) {
		// use hashmap to store (key, value) pairs
		HashMap<String, String> idpairs = new HashMap<String, String>();

		// get IMEI
		TelephonyManager mngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String IMEI = mngr.getDeviceId();
		idpairs.put("IMEI", IMEI);
		Log.v("IMEI", IMEI);

		/**
		 * 
		 * OS related information
		 * 
		 */
		// Android_ID
		String AndroidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		idpairs.put("Android_ID", AndroidID);
		Log.v("Android_ID", AndroidID);
		// Kernel
		String kversion = "";
		try {
			kversion = runShellCMD("cat /proc/version");
		} catch (IOException e) {
			// do nothing
		}
		idpairs.put("Kernel_Version", kversion);
		Log.v("Kernel version", kversion);

		// Android version
		String version = Build.VERSION.RELEASE;
		idpairs.put("Android_Ver", version);
		Log.v("Android version", version);

		// Build number
		String buildnum = Build.FINGERPRINT;
		idpairs.put("Build_Num", buildnum);
		Log.v("Build number", buildnum);

		// User Agent
		SharedPreferences prefs = context.getSharedPreferences("com.wyhao31.devicefingerprint", Context.MODE_PRIVATE);
		String UA = prefs.getString("UA", "");
		idpairs.put("User_Agent", UA);
		Log.v("User Agent", UA);

		/**
		 * 
		 * device related information
		 * 
		 */
		int wifion = -1;
		try {
			wifion = Settings.System.getInt(context.getContentResolver(), Settings.System.WIFI_ON);
		} catch (SettingNotFoundException e1) {
			// do nothing
		}
		idpairs.put("WIFI_ON", "" + wifion);
		Log.v("WIFI on", "" + wifion);

		String wifiMAC = "";
		try {
			wifiMAC = runShellCMD("cat /sys/class/net/wlan0/address");
		} catch (IOException e) {
			// do nothing
		}
		idpairs.put("WIFI_MAC", wifiMAC);
		Log.v("WIFI MAC", wifiMAC);
		String bluetoothMAC = "";
		try {
			bluetoothMAC = runShellCMD("cat /sys/class/bluetooth/address");
		} catch (IOException e) {
			// do nothing
		}
		idpairs.put("Bluetooth_MAC", bluetoothMAC);
		Log.v("Bluetooth MAC", bluetoothMAC);

		// model and manufacturer
		String deviceName = android.os.Build.MODEL;
		String deviceMan = android.os.Build.MANUFACTURER;
		String serial = getSerialNum();

		idpairs.put("Device_Model", deviceName);
		idpairs.put("Device_Manufacturer", deviceMan);
		idpairs.put("Serial", serial);
		Log.v("Device model", deviceName);
		Log.v("Device manufacturer", deviceMan);
		Log.v("Serial", serial);

		// Screen size
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		int width = metrics.widthPixels;
		int height = metrics.heightPixels;
		idpairs.put("Screen_Width", "" + width);
		idpairs.put("Screen_Height", "" + height);
		Log.v("Screen size", "width=" + width + ", height=" + height);
		int densityDpi = (int) (metrics.density * 160f);
		idpairs.put("Screen_Density", Integer.toString(densityDpi));
		Log.v("Screen density dpi", Integer.toString(densityDpi));

		/**
		 * 
		 * user operation related information
		 * 
		 */
		// Time zone
		String tz = Calendar.getInstance().getTimeZone().getDisplayName(Locale.ENGLISH);
		idpairs.put("Time_Zone", tz);
		Log.v("Time zone", tz);
		// 12 or 24 hour format
		int hourFormat;
		if (DateFormat.is24HourFormat(context))
			hourFormat = 24;
		else
			hourFormat = 12;
		idpairs.put("12_24", "" + hourFormat);
		Log.v("Hour format", "" + hourFormat);
		// date format
		char dfarray[] = DateFormat.getDateFormatOrder(context);
		String dateFormat = "";
		for (int i = 0; i < dfarray.length; i++) {
			if (i != 0)
				dateFormat += "-";
			if (dfarray[i] == DateFormat.DATE)
				dateFormat += "dd";
			else if (dfarray[i] == DateFormat.MONTH)
				dateFormat += "MM";
			else if (dfarray[i] == DateFormat.YEAR)
				dateFormat += "yyyy";
		}
		idpairs.put("Date_Format", dateFormat);
		Log.v("Date format", dateFormat);
		int autotime = Settings.System.getInt(context.getContentResolver(), Settings.System.AUTO_TIME, -1);
		idpairs.put("Auto_Time", "" + autotime);
		Log.v("Auto time", "" + autotime);
		int autotz = Settings.System.getInt(context.getContentResolver(), Settings.System.AUTO_TIME_ZONE, -1);
		idpairs.put("Auto_Timezone", "" + autotz);
		Log.v("Auto timezone", "" + autotz);

		int screentimeout = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, -1000) / 1000;
		idpairs.put("Screen_Timeout", "" + screentimeout);
		Log.v("Screen timeout", "" + screentimeout);

		int wifinotification = Settings.System.getInt(context.getContentResolver(), Settings.System.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, -1);
		idpairs.put("WIFI_Notification", "" + wifinotification);
		Log.v("WIFI notification", "" + wifinotification);
		int wifisleep = Settings.System.getInt(context.getContentResolver(), Settings.System.WIFI_SLEEP_POLICY, -1);
		idpairs.put("WIFI_Sleep", "" + wifisleep);
		Log.v("WIFI sleep", "" + wifisleep);

		// access location (wifi and gps)
		int location = 0;
		if (Settings.Secure.isLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER))
			location |= 1;
		if (Settings.Secure.isLocationProviderEnabled(context.getContentResolver(), LocationManager.NETWORK_PROVIDER))
			location |= 2;
		idpairs.put("Access_Loc", "" + location);
		Log.v("Access location", "" + location);

		// lock pattern enable
		int lockpattern = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCK_PATTERN_ENABLED, -1);
		idpairs.put("LockPattern", "" + lockpattern);
		Log.v("Lock pattern", "" + lockpattern);

		// lock pattern visible
		int lockpatternVisible = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCK_PATTERN_VISIBLE, -1);
		idpairs.put("Lock_Pattern_Visible", "" + lockpatternVisible);
		Log.v("Lock pattern visible", "" + lockpatternVisible);

		// lock pattern feedback vibrate
		int lockpatternVibrate = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED, -1);
		idpairs.put("Lock_Pattern_Vibrate", "" + lockpatternVibrate);
		Log.v("Lock pattern vibrate", "" + lockpatternVibrate);

		// input method list
		InputMethodManager imeManager = (InputMethodManager) context.getApplicationContext().getSystemService(context.INPUT_METHOD_SERVICE);
		List<InputMethodInfo> InputMethodsList = imeManager.getEnabledInputMethodList();
		PackageManager pm = context.getPackageManager();
		String inputMethods = "";
		for (InputMethodInfo inputMethodInfo : InputMethodsList)
			inputMethods += inputMethodInfo.loadLabel(pm).toString() + ",";
		idpairs.put("Input_Methods", inputMethods);
		Log.v("Input methods", inputMethods);

		// current language
		String lan = Locale.getDefault().getDisplayLanguage();
		idpairs.put("Language", lan);
		Log.v("Language", lan);
		// judge root?
		int isRoot = isDeviceRooted() == true ? 1 : 0;
		idpairs.put("Root", "" + isRoot);
		Log.v("Root", "" + isRoot);

		float scale = context.getResources().getConfiguration().fontScale;
		idpairs.put("Font_Size", "" + scale);
		Log.v("Font Size", "" + scale);
		String fonttypes = "";
		try {
			fonttypes = runShellCMD("ls /system/fonts -la");
		} catch (IOException e) {
			// do nothing
		}
		String[] fontarr = fonttypes.split("\n");
		String fontstr = "";
		for (int i = 0; i < fontarr.length; i++) {
			String[] tmp = fontarr[i].split("[ ]+");
			if (tmp.length == 7)
				fontstr += tmp[6] + ":" + tmp[3] + "##";
			else if (tmp.length == 8)
				fontstr += tmp[5] + ":" + tmp[7] + "##";
		}
		idpairs.put("Font_Types", fontstr);
		Log.v("Font Types", fontstr);

		// installed packages and services
		/*
		 * ActivityManager am = (ActivityManager)
		 * context.getSystemService(context.ACTIVITY_SERVICE);
		 * List<ActivityManager.RunningServiceInfo> rs =
		 * am.getRunningServices(100); String installedServices = ""; for (int i
		 * = 0; i < rs.size(); i++) { ActivityManager.RunningServiceInfo rsi =
		 * rs.get(i); installedServices += rsi.process + "," +
		 * rsi.service.getClassName() + "," + rsi.uid + "##"; //
		 * Log.v("Service", "Process [" + rsi.process + "] With component [" //
		 * + rsi.service.getClassName() + "]" + ", Uid = " + rsi.uid); }
		 * idpairs.put("Services", installedServices); Log.v("Services",
		 * installedServices);
		 */

		pm = context.getPackageManager();
		// a list of installed apps.
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		Collections.sort(packages, new PackageComparator());
		String installedUserPackages = "", installedSystemPackages = "";
		for (ApplicationInfo packageInfo : packages) {
			if (packageInfo.uid < 10000)
				continue;
			if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
				installedUserPackages += packageInfo.uid + ":" + packageInfo.packageName + "##";
			else
				installedSystemPackages += packageInfo.uid + ":" + packageInfo.packageName + "##";
			// Log.v("Installed package", packageInfo.packageName);
			// Log.v("Launch Activity", "" +
			// pm.getLaunchIntentForPackage(packageInfo.packageName));
			// Log.v("Uid", "" + packageInfo.uid);
		}
		idpairs.put("User_Packages", installedUserPackages);
		idpairs.put("System_Packages", installedSystemPackages);
		Log.v("User_Packages", installedUserPackages);
		Log.v("System_Packages", installedSystemPackages);

		String availableInt = getAvailableInternalMemorySize();
		String totalInt = getTotalInternalMemorySize();
		String availableExt = getAvailableExternalMemorySize();
		String totalExt = getTotalExternalMemorySize();

		idpairs.put("Int_Storage_A", availableInt);
		idpairs.put("Int_Storage_T", totalInt);
		idpairs.put("Ext_Storage_A", availableExt);
		idpairs.put("Ext_Storage_T", totalExt);
		Log.v("Internal storage", availableInt + "/" + totalInt);
		Log.v("External storage", availableExt + "/" + totalExt);

		// storage structure
		String SS = "";
		try {
			SS = runShellCMD("df");
		} catch (IOException e) {
			// do nothing
		}
		String[] strarr = SS.split("\n");
		Arrays.sort(strarr, new StringComparator());
		String SS1 = "";
		for (int i = 0; i < strarr.length - 1; i++) {
			String[] tmp = strarr[i].split("[ ]+");
			SS1 += tmp[0];
			if (tmp.length > 1)
				SS1 += " " + tmp[1];
			SS1 += "##";
		}
		idpairs.put("Storage_Structure", SS1);
		Log.v("Storage structure", SS1);

		// root directory
		String RDList = "";
		try {
			RDList = runShellCMD("ls");
		} catch (IOException e) {
			// do nothing
		}
		idpairs.put("Root_Dir_Structure", RDList.replaceAll("\n", "##"));
		Log.v("Root directory structure", RDList.replaceAll("\n", "##"));

		RingtoneManager manager = new RingtoneManager(context);
		manager.setType(RingtoneManager.TYPE_ALL);
		Cursor cursor = manager.getCursor();
		String allSound = "";
		while (cursor.moveToNext()) {
			String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
			allSound += title + ",";
		}
		idpairs.put("All_Sound", allSound);
		Log.v("All sound", allSound);

		// all sound and current user chosen sound
		String noteSound = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_NOTIFICATION_URI).getTitle(context);
		// noteSound = noteSound.substring(noteSound.indexOf("(")+1,
		// noteSound.indexOf(")"));
		idpairs.put("Notesound", noteSound);
		Log.v("Notification", noteSound);
		String alarmSound = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_ALARM_ALERT_URI).getTitle(context);
		// alarmSound = alarmSound.substring(alarmSound.indexOf("(")+1,
		// alarmSound.indexOf(")"));
		idpairs.put("Alarmsound", alarmSound);
		Log.v("Alarm", alarmSound);
		String ringSound = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_RINGTONE_URI).getTitle(context);
		idpairs.put("Ringsound", ringSound);
		// ringSound = ringSound.substring(ringSound.indexOf("(")+1,
		// ringSound.indexOf(")"));
		Log.v("Ring", ringSound);

		int soundEffect = Settings.System.getInt(context.getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, -1);
		idpairs.put("Sound_Effect", "" + soundEffect);
		Log.v("Sound effect", "" + soundEffect);

		int showPwd = Settings.System.getInt(context.getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD, -1);
		idpairs.put("Show_Pwd", "" + showPwd);
		Log.v("Show Pwd", "" + showPwd);

		int autobright = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
		idpairs.put("Auto_Bright", "" + autobright);
		Log.v("Auto bright", "" + autobright);

		int autorotate = Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, -1);
		idpairs.put("Auto_Rotate", "" + autorotate);
		Log.v("Auto rotate", "" + autorotate);

		// all wallpaper and current user chosen wallpaper
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		drawableToBitmap(wallpaperManager.getDrawable()).compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] bitmapBytes = baos.toByteArray();
		Adler32 md5Hash = new Adler32();
		md5Hash.update(bitmapBytes);
		long hashKey = md5Hash.getValue();
		idpairs.put("Wallpaper_md5", "" + hashKey);
		Log.v("Wallpaper md5", "" + hashKey);

		return idpairs;
	}

	private class PackageComparator implements Comparator<ApplicationInfo> {

		@Override
		public int compare(ApplicationInfo arg0, ApplicationInfo arg1) {
			if (arg0.uid < arg1.uid)
				return -1;
			else if (arg0.uid == arg1.uid)
				return 0;
			else
				return 1;
		}

	}

	private class StringComparator implements Comparator<String> {

		@Override
		public int compare(String arg0, String arg1) {
			return arg0.compareTo(arg1);
		}

	}

	public Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	public void run(Context context, DBManager mgr) {
		int TIMEOUT_CONNECTION = 10000; // = 10 seconds
		int TIMEOUT_SOCKET = 10000; // = 10 seconds
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		boolean flag = false;
		int count = 0;
		HashMap<String, String> idpairs = collectFP(context);
		JSONObject holder = new JSONObject(idpairs);
		while (!flag && count < 3) {
			String time = sdf.format(new Date());
			try {
				// Create a new HttpClient and Post Header
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost("http://wyhao.org/android/collectfp.php");
				HttpParams httpParams = httppost.getParams();
				HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_CONNECTION);
				HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_SOCKET);
				StringEntity se = new StringEntity(holder.toString(), "utf8");
				// sets the post request as the resulting string
				httppost.setEntity(se);
				// sets a request header so the page receiving the request
				// will know what to do with it
				httppost.setHeader("Accept", "application/json");
				httppost.setHeader("Content-type", "application/json");
				long size = se.getContentLength();
				HttpResponse httpResponse = httpclient.execute(httppost);
				int status = httpResponse.getStatusLine().getStatusCode();
				Log.v("Response status", "" + status);
				BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line = null; (line = reader.readLine()) != null;) {
					builder.append(line).append("\n");
				}
				JSONObject jsonRet = new JSONObject(builder.toString());
				Log.v("Return Json", jsonRet.toString());
				if (status == 200 && jsonRet.getBoolean("res")) {
					Log.v("Upload Result", "Success!");
					flag = true;
					mgr.add(new updatehistoryItem(time, size, "Success"));
				} else {
					Log.v("Upload Result", "Fail!");
					mgr.add(new updatehistoryItem(time, size, "Fail"));
				}
			} catch (JSONException e) {
				// e.printStackTrace();
				mgr.add(new updatehistoryItem(time, 0, "JSONException"));
				Log.v("Exception", "JSON exception.");
			} catch (UnsupportedEncodingException e) {
				// e.printStackTrace();
				mgr.add(new updatehistoryItem(time, 0, "UnsupportedEncodingException"));
				Log.v("Exception", "Unsupported encoding error.");
			} catch (ClientProtocolException e) {
				// e.printStackTrace();
				mgr.add(new updatehistoryItem(time, 0, "ClientProtocolException"));
				Log.v("Exception", "Client protocol error.");
			} catch (SocketTimeoutException e) {
				mgr.add(new updatehistoryItem(time, 0, "SocketTimeoutException"));
				Log.v("Exception", "Socket timeout.");
			} catch (ConnectTimeoutException e) {
				mgr.add(new updatehistoryItem(time, 0, "ConnectTimeoutException"));
				Log.v("Exception", "Connection timeout.");
			} catch (IOException e) {
				// e.printStackTrace();
				mgr.add(new updatehistoryItem(time, 0, "IOException"));
				Log.v("Exception", "I/O error (May be server down).");
			} catch (Exception e) {
				mgr.add(new updatehistoryItem(time, 0, "Exception"));
			}
			SystemClock.sleep(10000);
			count++;
		}
	}

	@Override
	protected Void doInBackground(Context... contextParam) {
		// TODO Auto-generated method stub
		if (contextParam.length != 0) {
			Context context = contextParam[0];
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			this.wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DeviceFingerprintWakeLock");
			wl.acquire();
			DBManager mgr = new DBManager(context);
			this.run(context, mgr);
			// this.collectFP(context);
		}
		Log.v("FPAlarm", "Async task done");
		if (this.wl != null) {
			this.wl.release();
			this.wl = null;
		}
		return null;
	}
}
