package shao.robopack;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class act extends Activity {

	private static final int REQUEST_CONNECT_DEVICE = 1000;
	private static final int REQUEST_ENABLE_BT = 2000;
	
	private final static int STATE_FIND_BALL = 91;
	private final static int STATE_TRACK_BALL = 92;
	
	private final static float MAX_MOTOR_SPEED = 100; //degrees
	private final static float HORIZONTAL_FIELD_OF_VIEW = 40; //degrees
	private final static float VERTICAL_FIELD_OF_VIEW = 50; //degrees
	private final static float HORIZONTAL_DEGREES_PER_PIXEL = 144/HORIZONTAL_FIELD_OF_VIEW;
	private final static float VERTICAL_DEGREES_PER_PIXEL = 144/VERTICAL_FIELD_OF_VIEW;

	LinearLayout mainView;
	public static int callBackFrameCount;
	public static CommsView commsbox;
	public static CamView cambox;
	public static FilteredView finalbox;
	public static FilterConfigView filterconfigbox;
	public static TextView dataview;
	public static Hashtable<String, String> dataTable;
	public static TextView statsview;


	//Compass
	private SensorManager mSensorManager;
	private Sensor mSensor;    
	private float[] mValues;


	private Toast reusableToast;
	private boolean connected = false;
	boolean pairing;
	
	UltrasonicSensor sonic;

	private Timer myTimer;
	int prevFrameCount = 0;
	int secondsToAverage = 3;
	int[] workingFrameCount = new int[secondsToAverage];
	int ticks = 0;


	private final SensorEventListener mListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent event) {
			Log.d("roboeyes", "sensorChanged (" + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + ")");
			mValues = event.values;
			addDataString("compass", Integer.toString((int)event.values[0]));
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {

		dataTable = new Hashtable<String, String>();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mainView = (LinearLayout)findViewById(R.id.mainview);
		commsbox = (CommsView)findViewById(R.id.commsview);
		cambox =  (CamView)findViewById(R.id.camview);
		finalbox =  (FilteredView)findViewById(R.id.finalview);
		dataview = (TextView)findViewById(R.id.dataview);
		statsview = (TextView)findViewById(R.id.statsView);
		filterconfigbox = (FilterConfigView)findViewById(R.id.filterconfigview);
		finalbox.setAct(this);
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

		commsbox.startComms("1");
		//selectNXT();
		sonic = new UltrasonicSensor(SensorPort.S4);
		connected = true;
		myTimer = new Timer();
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}

		}, 3000, 1000);

	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public void pulse() {
		try{
			int cogx = (Integer.parseInt(act.getDataString("COG_X")));			
			int midpoint = finalbox.outwidth/2;
			int displacement = midpoint - cogx;
			float angle_to_ball = displacement * HORIZONTAL_DEGREES_PER_PIXEL;
			int motspeed = Math.abs((int)((MAX_MOTOR_SPEED/midpoint * ((float)midpoint - (float)cogx))));

			if ((displacement > midpoint - 10) && (displacement < midpoint + 10)) {
				Motor.A.stop();
				
			} else if (cogx < midpoint) {				
				Motor.A.setSpeed(motspeed);
				Motor.A.forward();				
			} else {
				
				Motor.A.setSpeed(motspeed);
				Motor.A.backward();				
			}

		} catch (Exception e) {
			
		}
	}

    private void showToast(String textToShow) {
		reusableToast.setText(textToShow);
		reusableToast.show();
	}

	private void TimerMethod()
	{

		this.runOnUiThread(StatsUpdater);
	}

	private Runnable StatsUpdater = new Runnable() {
		public void run() {

			//This method runs in the same thread as the UI.			
			ticks++;
			if(ticks == secondsToAverage) ticks = 0;
			workingFrameCount[ticks] = callBackFrameCount - prevFrameCount;
			prevFrameCount = callBackFrameCount;
			float curFPS = 0;
			for(int i = 0; i < secondsToAverage; i++){
				curFPS += workingFrameCount[i];				
			}
			curFPS /= (float)secondsToAverage;
			statsview.setText("FPS: " + ((Float.toString(curFPS).length() > 4) ? Float.toString(curFPS).subSequence(0,4) : Float.toString(curFPS)));
			statsview.append("\n");
			statsview.append("COG: " + getDataString("COG_X") + "," + getDataString("COG_Y"));
			statsview.append("\n");
			//statsview.append("US:" + sonic.getDistance());

		}
	};


	@Override
	protected void onPause() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor filterPrefs = preferences.edit();
		filterPrefs.putInt("MinRed", finalbox.minRed);
		filterPrefs.putInt("MaxRed", finalbox.maxRed);
		filterPrefs.putInt("MinGreen", finalbox.minGreen);
		filterPrefs.putInt("MaxGreen", finalbox.maxGreen);
		filterPrefs.putInt("MinBlue", finalbox.minBlue);
		filterPrefs.putInt("MaxBlue", finalbox.maxBlue);
		filterPrefs.putInt("MinIntensity", finalbox.minIntensity);
		filterPrefs.putInt("MaxIntensity", finalbox.maxIntensity);
		//filterPrefs.putInt("Threshold", finalbox.pixelThreshold);
		filterPrefs.commit();
		mSensorManager.unregisterListener(mListener, mSensor);
		commsbox.kill();
		myTimer.cancel();
		super.onPause();
	}

	@Override
	protected void onResume() {		
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		finalbox.minRed = preferences.getInt("MinRed", 0);
		finalbox.maxRed = preferences.getInt("MaxRed", 0);
		finalbox.minGreen = preferences.getInt("MinGreen", 0);
		finalbox.maxGreen = preferences.getInt("MaxGreen", 0);
		finalbox.minBlue = preferences.getInt("MinBlue", 0);
		finalbox.maxBlue = preferences.getInt("MaxBlue", 0);
		finalbox.minIntensity = preferences.getInt("MinIntensity", 0);
		finalbox.maxIntensity = preferences.getInt("MaxIntensity", 0);
		//finalbox.pixelThreshold = preferences.getInt("Threshold", 0);
		filterconfigbox.init();
		mSensorManager.registerListener(mListener, mSensor, SensorManager.SENSOR_DELAY_UI);

		super.onResume();
	}

	public static void addDataString(String name, String value) {
		try {
			dataTable.put(name, value);
		} catch (Exception e) {
		}
	}

	public static void removeDataString(String name) {
		try {
			dataTable.remove(name);
		} catch (Exception e) {
		}
	}

	public static String getDataString(String name) {
		try {
			return dataTable.get(name);
		} catch (Exception e) {
			return null;
		}
	}

}



