package com.phoenixxie.utils.wireless;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.Toast;

public class SSIDUpdater {

	Activity activity;

	SSIDUpdateListener listener;
	int timeout = 200; // milliseconds
	boolean running = false;

	WifiManager wifi = null;
	List<ScanResult> results;

	Semaphore lock = new Semaphore(1, false);

	public SSIDUpdater(Activity context) {
		this.activity = context;
	}

	public void setListener(SSIDUpdateListener listener) {
		this.listener = listener;
	}

	public void setInterval(int milliseconds) {
		this.timeout = milliseconds;
	}

	public void init() {
		running = false;

		wifi = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
		if (!wifi.isWifiEnabled()) {
			Toast.makeText(activity, "Wifi is disabled. I will enable it.",
					Toast.LENGTH_LONG).show();
			wifi.setWifiEnabled(true);
		}

		activity.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				lock.release();
				listener.update(wifi.getScanResults());
			}
		}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

	}

	public void start() {
		if (running) {
			return;
		}
		running = true;

		lock.drainPermits();

		(new Thread(new SSIDUpdaterRunnable())).start();
	}

	public void stop() {
		running = false;
	}

	class SSIDUpdaterRunnable implements Runnable {

		@Override
		public void run() {
			while (running) {

				wifi.startScan();

				try {
					lock.acquire();
					Thread.sleep(timeout);
				} catch (Exception e) {
				}
			}
		}

	}

	public static interface SSIDUpdateListener {
		public void update(final List<ScanResult> results);
	}

	public static class SortedSSIDUpdateAdapter implements SSIDUpdateListener {
		
		SSIDUpdateListener inner;
		
		public SortedSSIDUpdateAdapter(SSIDUpdateListener listener) {
			this.inner = listener;
		}
		
		@Override
		public void update(List<ScanResult> results) {
			Collections.sort(results, new Comparator<ScanResult> () {

				@Override
				public int compare(ScanResult lhs, ScanResult rhs) {
					return lhs.level - rhs.level;
				}
				
			});
			inner.update(results);
		}

	}
}
