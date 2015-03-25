package org.cody.gpstrack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.example.gpstrack.R;

import org.cody.gpstrack.CSVWriter;
import org.cody.gpstrack.GPSSensor;
import org.cody.gpstrack.GPSSensor.GPSListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GPSTrackActivity extends Activity implements GPSListener {

	private static GPSSensor gpsSensor;
	private static Handler handler = null;
	private static final String LOGNAME = "GPSTrackActivity";
	
	private static TextView timeView;
	private static TextView longView;
	private static TextView latView;
	private static TextView altView;
	private static TextView bearingView;
	private static Button startStop;
	
	private long mSleep = 1000;
	private long prevTime, curTime;
	
	private String mTimeCreateFile;
	
	private boolean running;
	private Timer timer;
	
	private CSVWriter writer;
	private File file;
	private String time;
	private String latitude;
	private String longitude;
	private String altitude;
	
	private ArrayList<String> dataSample;
	/**
	 * 
	 */
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}
	
	/**
	 * 
	 */
	protected void onPause()
	{
		super.onPause();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gpstrack);
		
		handler = new Handler();
		gpsSensor = new GPSSensor(GPSTrackActivity.this, handler);
		gpsSensor.setGPSListener(GPSTrackActivity.this);
		gpsSensor.start();
		
		timeView = (TextView) findViewById(R.id.timeValue);
        latView = (TextView) findViewById(R.id.LatValue);
        longView = (TextView) findViewById(R.id.LongValue);
        altView = (TextView) findViewById(R.id.altitudeValue);
        startStop = (Button) findViewById(R.id.startstopbutton);
        
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               running = !running;
               if(running)
               {
            	   startStop.setText("Stop Logging");
            	   writeTextFile();
               }
               else
               {
            	   startStop.setText("Start Logging");
        		   try
        		   {
        			   writer.close();
        			   writer = null;
        		   }
        		   catch(Exception e)
        		   {
            		   Log.e(LOGNAME, "ERROR: Writer wasn't on " + e.toString());
        		   }

               }
            }
        });
        

        timer = new Timer();
	    timer.scheduleAtFixedRate(new TimerTask() {
	    	@Override
			public void run() {
	    		if(prevTime == 0)
	    			prevTime = System.currentTimeMillis();
	    		final String strTime;
	    		strTime=getTimeString();
	   
				try {
					handler.post(new Runnable() {
						public void run(){
							try {
								timeView.setText(strTime);
								time = strTime;
								//date.setText(getDateString());
							} 
							catch (Exception e) {
								Log.e(LOGNAME, "Run Error");
								e.printStackTrace();
							}
						}
					});
				} 
				catch (Exception e) 
				{
					Log.e(LOGNAME, "ERROR in run");
				}
				
				
				dataSample = new ArrayList<String>();
				dataSample.add(time);
				dataSample.add(latitude);
				dataSample.add(longitude);
				dataSample.add(altitude);
				if(writer != null)
				{
					try
					{
						writer.writeNext(dataSample);
					}
					catch(Exception e)
					{
						Log.e(LOGNAME, "Exception writing data", e);
					}
				}
				curTime=System.currentTimeMillis();
				
				mSleep=(prevTime+1000-curTime);
				if(mSleep>1000)
					mSleep=1000;
				prevTime=curTime;
				
			}
	}, 0, mSleep);
	 ArrayList<String> list = new ArrayList<String>();
	 list.add(time);
	 list.add(latitude);
	 list.add(longitude);
	 list.add(altitude);
	 if(writer != null)
	 {
		 writer.writeNext(list);
	 }
	//list=new ArrayList<String>();
} 


        
    

	@Override
	public void onCompassChange(float mOrientation, float mOrientation2,
			String mOrientAccuracy) {
		Log.i(LOGNAME, "Compass direction changed");
		return;
	}

	@Override
	public void onLocationChange(double Lat, double Lng, double Alt,
			double speed) {
		Log.i(LOGNAME, "GPS Location Changed");
		latView.setText(""+Lat);
		longView.setText(""+Lng);
		altView.setText(""+Alt+"m");
		
		latitude = ""+Lat;
		longitude = ""+Lng;
		altitude = ""+Alt;
		
	}

	@Override
	public void onStatusChange(String sat) {
		return;
	}
	
	/**
	 * 
	 * 
	 */
	private void initHeaderCSV()
	{
		ArrayList<String> header = new ArrayList<String>();
		header.add("Time");
		header.add("Latitude");
		header.add("Longitude");
		header.add("Altitude");
		try
		{
			Log.i(LOGNAME, "Writing header");
			writer.writeNext(header);
		}
		catch(Exception e)
		{
			Log.e(LOGNAME, "Error writing header: " + e.toString());
		}		 
	}
	
	/**
	 * 
	 * @return
	 */
	public static String getFileTime() 
	{
		String result = null;
		//SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy_HHmmss");
		SimpleDateFormat formatter = new SimpleDateFormat("MMddyyyy_HHmmss"); // Changed 1/27/2014
		Date cal = Calendar.getInstance().getTime();
		result=formatter.format(cal);
		return result;
	}
	
	/**
	 * 
	 * @return
	 */
	public static String getTimeString()
	{
		String result = "";
		try 
		{
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss",Locale.US);
			Date cal = Calendar.getInstance().getTime();
			result=formatter.format(cal);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 
	 * @return
	 */
	private static String getDateString() 
	{
		String result = "";
		try 
		{
			SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
			Date cal = Calendar.getInstance().getTime();
			result=formatter.format(cal);
		} 
		catch (Exception e) {}
		return result;
	}
	
	//direction parameter

	/**
	 * 
	 * 
	 * @return
	 */
	private String getSavePath()
	{
		String path = String.format(Environment.getExternalStorageDirectory() + "/GPSTrack/");
		Log.i(LOGNAME, "Save Path:"+path);
		return path;
	}
	
	private void prepareSdCard() 
	{
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) 
		{
				File sDir = new File(getSavePath());
				if (!sDir.exists()) 
				{
					sDir.mkdirs();
				}
		} 
	}
	/**
	 * 
	 * 
	 */
	 private void writeTextFile()
	 {
		 prepareSdCard();
	 	if(mTimeCreateFile==null)
	 		mTimeCreateFile=getTimeString();
 		
	 	String mTimeDateCreateFile=getFileTime();
	 	//String mFileName= mTimeDateCreateFile;			
	 	file= new File(getSavePath() + "/" + mTimeDateCreateFile  + ".csv");
		try
		{
			file.getParentFile().mkdirs();
			file.createNewFile();
			Log.i(LOGNAME, "New file created");
		} 
		catch (IOException e1) {
			Log.e(LOGNAME, "ERROR creating new file", e1);
		}
		try
		{
			writer = new CSVWriter(file);
		} 
		catch (IOException e) 
		{
			Log.e(LOGNAME, "Exception opening file writer",e);
		}
		initHeaderCSV(); 
	 }
	
} // End of class


