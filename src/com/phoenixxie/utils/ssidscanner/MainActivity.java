package com.phoenixxie.utils.ssidscanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.os.Build;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		ListView lvSSID;
		Button btnScan;
		Button btnStopScan;
		Button btnRecord;
		Button btnStopRecord;
		SimpleAdapter adapter;

		SSIDUpdater updater;
		TextRecorder recorder;
		int ssidCount = 0;
		ArrayList<HashMap<String, String>> ssids = new ArrayList<HashMap<String, String>>();

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);

			lvSSID = (ListView) rootView.findViewById(R.id.listViewSSID);
			btnScan = (Button) rootView.findViewById(R.id.buttonScan);
			btnStopScan = (Button) rootView.findViewById(R.id.buttonStopScan);
			btnRecord = (Button) rootView.findViewById(R.id.buttonRecord);
			btnStopRecord = (Button) rootView
					.findViewById(R.id.buttonStopRecord);

			btnScan.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View btn) {
					btnScan.setEnabled(false);
					updater.start();
					btnStopScan.setEnabled(true);
				}
			});

			btnStopScan.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					updater.stop();
					btnScan.setEnabled(true);
					btnStopScan.setEnabled(false);
				}
			});

			btnRecord.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					recorder.start();
					btnStopRecord.setEnabled(true);
					btnRecord.setEnabled(false);
				}
			});

			btnStopRecord.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					recorder.stop();
					btnRecord.setEnabled(true);
					btnStopRecord.setEnabled(false);
				}
			});

			adapter = new SimpleAdapter(getActivity(), ssids,
					R.layout.ssid_grid,
					new String[] { "key", "level", "freq" }, new int[] {
							R.id.ssid_name, R.id.ssid_level, R.id.ssid_freq });
			lvSSID.setAdapter(this.adapter);

			recorder = new TextRecorder(getActivity(), "/mnt/sdcard");

			updater = new SSIDUpdater(getActivity());
			updater.init();
			updater.setListener(new SSIDUpdater.SSIDUpdateListener() {

				@Override
				public void update(List<ScanResult> results) {
					ssids.clear();
					ssidCount = results.size();

					ssidCount = ssidCount - 1;

					long unixTime = System.currentTimeMillis() / 1000L;
					while (ssidCount >= 0) {
						HashMap<String, String> item = new HashMap<String, String>();
						item.put("key", results.get(ssidCount).SSID + "  "
								+ results.get(ssidCount).capabilities);
						item.put("freq", "" + results.get(ssidCount).frequency);
						item.put("level", "" + results.get(ssidCount).level);

						StringBuilder builder = new StringBuilder();
						builder.append(unixTime).append(",")
								.append(results.get(ssidCount).SSID)
								.append(",")
								.append(results.get(ssidCount).capabilities)
								.append(",")
								.append(results.get(ssidCount).level)
								.append(",")
								.append(results.get(ssidCount).frequency);

						recorder.writeLine(builder.toString());

						ssids.add(item);
						ssidCount--;

						adapter.notifyDataSetChanged();
					}

				}

			});

			return rootView;
		}
	}

}
