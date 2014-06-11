package com.phoenixxie.utils.wireless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

public class BluetoothUpdater {
	public static class Result {
		public String name;
		public String address;
		public int rssi;
		public boolean bonded;
	}

	Activity activity;

	BluetoothUpdateListener listener;
	int timeout = 200; // milliseconds
	boolean running = false;

	BluetoothAdapter bluetooth;
	ArrayList<Result> scanResult;

	Semaphore lock = new Semaphore(1, false);

	public BluetoothUpdater(Activity context) {
		this.activity = context;
		this.scanResult = new ArrayList<Result>();
	}

	public void setListener(BluetoothUpdateListener listener) {
		this.listener = listener;
	}

	public void setInterval(int milliseconds) {
		this.timeout = milliseconds;
	}

	public void init() {
		running = false;

		bluetooth = BluetoothAdapter.getDefaultAdapter();
		if (bluetooth == null) {
			Toast.makeText(activity, "No bluetooth on this device.",
					Toast.LENGTH_LONG).show();
		}
		if (!bluetooth.isEnabled()) {
			Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBT, 0xDEADBEEF);
		}

		BluetoothBroadcastReceiver receiver = new BluetoothBroadcastReceiver();
		activity.registerReceiver(receiver, new IntentFilter(
				BluetoothDevice.ACTION_FOUND));
		activity.registerReceiver(receiver, new IntentFilter(
				BluetoothAdapter.ACTION_DISCOVERY_STARTED));
		activity.registerReceiver(receiver, new IntentFilter(
				BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

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

	class BluetoothBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				Result result = new Result();
				result.address = device.getAddress();
				result.name = device.getName();
				result.bonded = (device.getBondState() == BluetoothDevice.BOND_BONDED);
				result.rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,
						Short.MIN_VALUE);
				scanResult.add(result);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				scanResult.clear();
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				lock.release();
				listener.update(scanResult);
			}
		}
	}

	class SSIDUpdaterRunnable implements Runnable {

		@Override
		public void run() {
			while (running) {

				if (bluetooth.isDiscovering()) {
					bluetooth.cancelDiscovery();
				}

				bluetooth.startDiscovery();

				try {
					lock.acquire();
					Thread.sleep(timeout);
				} catch (Exception e) {
				}
			}
		}

	}

	public static interface BluetoothUpdateListener {
		public void update(final List<Result> results);
	}

	public static class SortedBluetoothUpdateAdapter implements
			BluetoothUpdateListener {

		BluetoothUpdateListener inner;

		public SortedBluetoothUpdateAdapter(BluetoothUpdateListener listener) {
			this.inner = listener;
		}

		@Override
		public void update(List<Result> results) {
			Collections.sort(results, new Comparator<Result>() {

				@Override
				public int compare(Result lhs, Result rhs) {
					return lhs.rssi - rhs.rssi;
				}

			});
			inner.update(results);
		}

	}
}
