package com.phinfinity.accelerometer.recorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.phinfinity.accelerometer.recorder.AccelRecorderService.LocalBinder;

public class MainActivity extends Activity {
	View screen1, screen2;
	EditText mFileName;
	TextView mNoDatapoints;
	Spinner mFreq;
	boolean mBound = false;
	AccelRecorderService mService;
	Handler mHandler;

	Runnable status_updater = new Runnable() {
		public void run() {
			if(mService == null)
				Log.d("accel_activity","No service launched yet...");
			if (mService != null && mService.is_running()) {
				is_running();
			} else {
				is_stopped();
			}
			mHandler.postDelayed(status_updater, 1000); // Update every 1 second
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		screen1 = findViewById(R.id.screen1);
		screen2 = findViewById(R.id.screen2);
		mFileName = (EditText) findViewById(R.id.rec_name);
		mFreq = (Spinner) findViewById(R.id.spinner_rec_freq);
		mNoDatapoints = (TextView) findViewById(R.id.no_datapoints);
		mHandler = new Handler();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		mHandler.removeCallbacks(status_updater);
	}

	@Override
	protected void onResume() {
		super.onResume();
		screen1.setVisibility(View.GONE);
		screen2.setVisibility(View.GONE);
		Intent intent = new Intent(this, AccelRecorderService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		mHandler.post(status_updater);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			if (mService.is_running())
				is_running();
			else
				is_stopped();
		}

		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			finish();
		}
	};

	public void start_recording(View v) {
		String file_name = mFileName.getText().toString();
		int freq = mFreq.getSelectedItemPosition();
		if (freq == 0)
			freq = SensorManager.SENSOR_DELAY_FASTEST;
		else if (freq == 1)
			freq = SensorManager.SENSOR_DELAY_GAME;
		else if (freq == 2)
			freq = SensorManager.SENSOR_DELAY_UI;
		else if (freq == 3)
			freq = SensorManager.SENSOR_DELAY_NORMAL;
		else
			freq = -1;
		Log.d("accel_activity", "Selected File name : " + file_name);
		Log.d("accel_activity", "Selected Item Id : " + freq);
		Intent intent = new Intent(this, AccelRecorderService.class);
		intent.putExtra("file_name", file_name);
		intent.putExtra("rec_freq", freq);
		startService(intent);
		is_running();
	}

	public void is_running() {
		Log.d("accel_activity","Updating UI running status");
		screen2.setVisibility(View.VISIBLE);
		screen1.setVisibility(View.GONE);
		mNoDatapoints.setText(Long.toString(mService.get_no_points()));
	}

	public void is_stopped() {
		Log.d("accel_activity","Updating UI stopped status");
		screen1.setVisibility(View.VISIBLE);
		screen2.setVisibility(View.GONE);
	}

	public void stop_recording(View v) {
		mService.stop_recording();
		is_stopped();
	}
}
