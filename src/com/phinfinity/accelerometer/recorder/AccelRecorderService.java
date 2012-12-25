package com.phinfinity.accelerometer.recorder;

import android.app.IntentService;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class AccelRecorderService extends IntentService {

	private SensorManager mSmgr;
	private Sensor mSensor;
	private boolean running = false;
	private String file_name;
	private int rec_freq;
	private long start_time;
	private long no_datapoints;
	Thread some_woork;

	public AccelRecorderService() {
		super("Accelerator Recorder Service");
	}

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		AccelRecorderService getService() {
			return AccelRecorderService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("accel_service", "Bindinig to serivce");
		return mBinder;
	}

	@Override
	public void onDestroy() {
		Log.d("accelrecservice", "Destroying Service");
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mSmgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensor = mSmgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		running = false;
		Intent i;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (!running) {
			Bundle data = intent.getExtras();
			if (data.containsKey("file_name") && data.containsKey("rec_freq")) {
				file_name = data.getString("file_name");
				rec_freq = data.getInt("rec_freq");
				running = true;
				start_time = System.currentTimeMillis();
				no_datapoints = 0;
				// start data collection;

				some_woork = new Thread(new Runnable() {
					public void run() {
						while (true) {
							try {
								Thread.sleep(400);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								break;
							}
							no_datapoints++;
							Log.d("accel_service", "dooing dummy work");

						}
					}
				});
				some_woork.start();

				Log.d("accel_service", "STarting to record...");
			} else {
				Log.e("accel_service", "No file name or frequency specified");
			}
		} else {
			Log.e("accel_service", "Requested second recording while recording");
		}
		try {
			synchronized (this) {
				this.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stop_recording() {
		if (running) {
			running = false;
			Log.d("accel_service", "Stopped recording...");
			// stop recording;
			some_woork.interrupt();
			synchronized (this) {
				this.notify();	
			}
		}
	}

	public boolean is_running() {
		return running;
	}

	public long get_no_points() {
		return no_datapoints;
	}

}
