package com.wyhao31.devicefingerprint;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.wyhao31.devicefingerprint.R;
import com.wyhao31.devicefingerprint.db.DBManager;
import com.wyhao31.devicefingerprint.db.updatehistoryItem;

public class MainActivity extends Activity {
	private DBManager mgr;
	private int displayItems;

	public void onClickStartBtn(View v) {
		//startService(new Intent(this, DeviceFPService.class));
		PollingUtils.startPollingService(this, 3*60*60, AutoCollect.class);
		findViewById(R.id.startBtn).setEnabled(false);
		findViewById(R.id.stopBtn).setEnabled(true);
	}

	public void onClickStopBtn(View v) {
		//stopService(new Intent(this, DeviceFPService.class));
		PollingUtils.stopPollingService(this, AutoCollect.class); 
		findViewById(R.id.startBtn).setEnabled(true);
		findViewById(R.id.stopBtn).setEnabled(false);
	}

	public void onClickRefreshListBtn(View v) {
		refreshList();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// collectFP();
		WebView wv = new WebView(this);
		String UA = wv.getSettings().getUserAgentString();
		SharedPreferences prefs = this.getSharedPreferences("com.wyhao31.devicefingerprint", Context.MODE_PRIVATE);
		prefs.edit().putString("UA", UA).commit();
		
		boolean flag  = prefs.getBoolean("running", false);
		Log.v("alarm running", "" + flag);
		if (flag) {
			findViewById(R.id.startBtn).setEnabled(false);
			findViewById(R.id.stopBtn).setEnabled(true);
		} else {
			findViewById(R.id.startBtn).setEnabled(true);
			findViewById(R.id.stopBtn).setEnabled(false);
		}
		this.displayItems = 0;
		mgr = new DBManager(this);
		refreshList();
	}

	public void refreshList() {
        ListView listView = (ListView) this.findViewById(R.id.listView);
        
        //获取到集合数据
        int size = mgr.queryCount();
        List<updatehistoryItem> updatehistories = mgr.query(this.displayItems, 30);
        List<HashMap<String, Object>> data = new ArrayList<HashMap<String,Object>>();
        for(updatehistoryItem uhitem : updatehistories){
        	HashMap<String, Object> item = new HashMap<String, Object>();
        	item.put("id", uhitem.id);
        	item.put("time", uhitem.time);
        	item.put("size", uhitem.size);
        	item.put("result", uhitem.result);
        	data.add(item);
        }
       //创建SimpleAdapter适配器将数据绑定到item显示控件上
       SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.updatehistoryitem_layout, 
        		new String[]{"time", "size", "result"}, new int[]{R.id.time, R.id.size, R.id.result});
       //实现列表的显示
       listView.setAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		refreshList();
	}
}
