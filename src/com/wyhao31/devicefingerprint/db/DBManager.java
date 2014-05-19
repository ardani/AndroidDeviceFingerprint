package com.wyhao31.devicefingerprint.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBManager {
	private DBHelper helper;
	private SQLiteDatabase db;

	public DBManager(Context context) {
		helper = new DBHelper(context);
		// 因为getWritableDatabase内部调用了mContext.openOrCreateDatabase(mName, 0,
		// mFactory);
		// 所以要确保context已初始化,我们可以把实例化DBManager的步骤放在Activity的onCreate里
		db = helper.getWritableDatabase();
	}

	/**
	 * add updatehistoryItem
	 * 
	 * @param updatehistoryItem
	 */
	public void add(updatehistoryItem item) {
		db.beginTransaction(); // 开始事务
		try {
			db.execSQL("INSERT INTO updatehistory VALUES(null, ?, ?, ?)", new Object[] { item.time, item.size, item.result });
			db.setTransactionSuccessful(); // 设置事务成功完成
		} catch (SQLException e) {
			Log.v("Sqlite", "add item exception");
		} finally {
			db.endTransaction(); // 结束事务
		}
	}

	/**
	 * query all updatehistoryItems, return list
	 * 
	 * @return List<updatehistoryItem>
	 */
	public List<updatehistoryItem> query(int start, int len) {
		ArrayList<updatehistoryItem> persons = new ArrayList<updatehistoryItem>();
		Cursor c = queryTheCursor(start, len);
		while (c.moveToNext()) {
			updatehistoryItem item = new updatehistoryItem();
			item.id = c.getInt(c.getColumnIndex("id"));
			item.time = c.getString(c.getColumnIndex("time"));
			item.size = c.getInt(c.getColumnIndex("size"));
			item.result = c.getString(c.getColumnIndex("result"));
			persons.add(item);
		}
		c.close();
		return persons;
	}

	/**
	 * query all updatehistoryItems, return cursor
	 * 
	 * @return Cursor
	 */
	public Cursor queryTheCursor(int start, int len) {
		Cursor c = db.rawQuery("SELECT * FROM updatehistory ORDER BY id DESC LIMIT " + start + "," + len, null);
		return c;
	}

	/**
	 * get count()
	 * 
	 * @return return number of lines in sqlite
	 */
	public int queryCount() {
		Cursor c = db.rawQuery("SELECT COUNT(*) FROM updatehistory", null);
		c.moveToNext();
		return c.getInt(0);
	}

	/**
	 * close database
	 */
	public void closeDB() {
		db.close();
	}
}
