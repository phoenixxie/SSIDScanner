package com.phoenixxie.utils.wireless;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.phoenixxie.utils.wireless.R;
import com.phoenixxie.utils.wireless.BluetoothUpdater.Result;

import android.app.Activity;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;

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
		Button btnRecord;
		Button btnStopRecord;
		Switch switchScan;
		CheckBox cbBonded;
		SimpleAdapter ssidAdapter;
		SimpleAdapter bhAdapter;

		SSIDUpdater ssidUpdater;
		BluetoothUpdater bhUpdater;

		TextRecorder recorder;
		ArrayList<HashMap<String, String>> ssids = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> bluetooths = new ArrayList<HashMap<String, String>>();

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);

			lvSSID = (ListView) rootView.findViewById(R.id.listViewSSID);
			btnRecord = (Button) rootView.findViewById(R.id.buttonRecord);
			btnStopRecord = (Button) rootView
					.findViewById(R.id.buttonStopRecord);
			switchScan = (Switch) rootView.findViewById(R.id.switchScan);
			cbBonded = (CheckBox) rootView.findViewById(R.id.checkBoxBonded);

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

			switchScan.setChecked(true);
			switchScan
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							if (isChecked) {
								lvSSID.setAdapter(ssidAdapter);
								cbBonded.setVisibility(View.INVISIBLE);
							} else {
								lvSSID.setAdapter(bhAdapter);
								cbBonded.setVisibility(View.VISIBLE);
							}
						}
					});
			cbBonded.setChecked(false);
			cbBonded.setVisibility(View.INVISIBLE);

			ssidAdapter = new SimpleAdapter(getActivity(), ssids,
					R.layout.ssid_grid,
					new String[] { "key", "level", "freq" }, new int[] {
							R.id.ssid_name, R.id.ssid_level, R.id.ssid_freq });
			
			bhAdapter = new SimpleAdapter(getActivity(), bluetooths,
					R.layout.ssid_grid,
					new String[] { "key", "level", "freq" }, new int[] {
							R.id.ssid_name, R.id.ssid_level, R.id.ssid_freq });
			lvSSID.setAdapter(this.ssidAdapter);

			recorder = new TextRecorder(getActivity(), "/mnt/sdcard");

			bhUpdater = new BluetoothUpdater(getActivity());
			bhUpdater.init();
			bhUpdater
					.setListener(new BluetoothUpdater.SortedBluetoothUpdateAdapter(
							new BluetoothUpdater.BluetoothUpdateListener() {

								@Override
								public void update(List<Result> results) {
									bluetooths.clear();
									boolean showBonded = cbBonded.isChecked();

									for (BluetoothUpdater.Result result : results) {
										if (showBonded && !result.bonded) {
											continue;
										}
										HashMap<String, String> item = new HashMap<String, String>();
										item.put("key", result.name);
										item.put("freq", result.address);
										item.put("level", "" + result.rssi);
										bluetooths.add(item);
									}
									bhAdapter.notifyDataSetChanged();
								}
							}));

			ssidUpdater = new SSIDUpdater(getActivity());
			ssidUpdater.init();
			ssidUpdater.setListener(new SSIDUpdater.SortedSSIDUpdateAdapter(
					new SSIDUpdater.SSIDUpdateListener() {

						@Override
						public void update(List<ScanResult> results) {
							ssids.clear();
							int ssidCount = results.size();

							ssidCount = ssidCount - 1;

							long unixTime = System.currentTimeMillis() / 1000L;
							while (ssidCount >= 0) {
								HashMap<String, String> item = new HashMap<String, String>();
								item.put("key", results.get(ssidCount).SSID
										+ "(" + results.get(ssidCount).BSSID
										+ ")");
								item.put("freq", ""
										+ results.get(ssidCount).frequency);
								item.put("level", ""
										+ results.get(ssidCount).level);

								StringBuilder builder = new StringBuilder();
								builder.append(unixTime)
										.append(",")
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

								ssidAdapter.notifyDataSetChanged();
							}

						}

					}));

			bhUpdater.start();
			ssidUpdater.start();

			return rootView;
		}
	}

}
