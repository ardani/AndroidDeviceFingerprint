package com.wyhao31.devicefingerprint.db;

public class updatehistoryItem {
	public int id;
	public String time;
	public long size;
	public String result;

	public updatehistoryItem() {

	}

	public updatehistoryItem(String time, long size, String result) {
		this.time = time;
		this.size = size;
		this.result = result;
	}
}
