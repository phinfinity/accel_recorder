package com.phinfinity.accelerometer.recorder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AccelRecorderService extends IntentService implements
		SensorEventListener {

	private SensorManager mSmgr;
	private boolean running = false;
	private String file_name;
	private int rec_freq;
	private long start_time;
	private long no_datapoints;
	FileOutputStream mOutputFileStream;
	BufferedOutputStream mOutputBufferedStream;
	DataOutputStream mOutputDataStream;
	PowerManager.WakeLock wl;
	private static int MAX_ENCODING_TYPES = 8; // maximum number of different
												// sensors being encoded

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
		running = false;
		Intent i;
		PowerManager pm = (PowerManager) getApplicationContext()
				.getSystemService(POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"accel_recorder_wakelock");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (!running) {
			Bundle data = intent.getExtras();
			if (data.containsKey("file_name") && data.containsKey("rec_freq")) {
				file_name = data.getString("file_name");
				rec_freq = data.getInt("rec_freq");
				int[] sensorList = data.getIntArray("sensor_list");
				if (sensorList == null) {
					sensorList = new int[1];
					sensorList[0] = Sensor.TYPE_ACCELEROMETER;
				}
				running = true;
				wl.acquire();
				start_time = System.currentTimeMillis();
				no_datapoints = 0;
				// start data collection;

				File output_folder = new File(
						Environment.getExternalStorageDirectory(),
						"accel_recordings");
				output_folder.mkdirs();
				File output_file = new File(output_folder, file_name);
				int total_sensors_in_use = 0;
				for (int sensor_type : sensorList) {
					Sensor mSensor = mSmgr.getDefaultSensor(sensor_type);
					if (mSensor != null) {
						total_sensors_in_use++;
						mSmgr.registerListener(this, mSensor, rec_freq);
					} else {
						Log.w("accel_service", "Sensor of type " + sensor_type + " was not available");
					}
				}
				if(total_sensors_in_use == 0) {
					Log.e("accel_service", "No valid sensors available not starting...");
					return;
				}
				try {
					mOutputFileStream = new FileOutputStream(output_file);
				} catch (FileNotFoundException e) {
					stop_recording();
				}
				mOutputBufferedStream = new BufferedOutputStream(
						mOutputFileStream);
				mOutputDataStream = new DataOutputStream(mOutputBufferedStream);
				Log.d("accel_service", "STarting to record...");
			} else {
				Log.e("accel_service", "No file name or frequency specified");
			}
		} else {
			Log.e("accel_service", "Requested second recording while recording");
		}
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this);
		mBuilder.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Recording Accelerometer Data")
				.setContentText(
						"Accelerometer Data is currently being recorded")
				.setTicker("Starting Accelerometer Recording...")
				.setContentIntent(pendingIntent);

		startForeground(1, mBuilder.build());

		// Don't let the function end or IntentService might destroy you are add
		// other recordings
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
			wl.release();
			Log.d("accel_service", "Stopped recording...");
			// stop recording;
			mSmgr.unregisterListener(this);
			try {
				mOutputDataStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stopForeground(true);
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

	public long get_start_time() {
		return start_time;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public synchronized void write_data(SensorEvent event) {
		no_datapoints++;
		try {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				int id = 0;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
			} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
				int id = 1;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
			} else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
				int id = 2;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
			} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
				int id = 3;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
				//mOutputDataStream.writeFloat(event.values[3]); // this one is not always available
			} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				int id = 4;
				mOutputDataStream.writeLong(System.currentTimeMillis()
						* MAX_ENCODING_TYPES + id);
				mOutputDataStream.writeFloat(event.values[0]);
				mOutputDataStream.writeFloat(event.values[1]);
				mOutputDataStream.writeFloat(event.values[2]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onSensorChanged(SensorEvent event) {
		write_data(event);
	}

}
