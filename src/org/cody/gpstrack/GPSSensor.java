package org.cody.gpstrack;


import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

///Collecting GPS data class implementation


public class GPSSensor implements LocationListener {
	@SuppressWarnings("deprecation")
	
	private Location currentLocation;
	private LocationManager locationManager = null;
	private Handler handler = null;
	

	private PeriodicDisplayUpdate mPeriodicUpdate;

	//private LocationStartupRoutine mStartupRoutine; // GPS Startup routine for Android 4.4
	
	private SensorManager mSensorMan;
	private Sensor mOrientSensor;
	private OrientListener mOrientListener;
	public String mOrientAccuracy;
	public float mOrientation;
	public float mOrientation2;
	public long mLastOrientTS;
	private String mPositionOfSatellites="N/A";
	private Context context = null;
	

	private int startupCounter;
	
	private GPSListener listener = null;
	
	public static final float UNKNOWN = 999.999f;

	private ScheduledExecutorService scheduler =  Executors.newScheduledThreadPool(1);
	
	
	public GPSSensor(Context ctx, Handler handler) {
		context = ctx;
		this.handler = handler;
		
		// Get the location manager
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			Log.i("GPS","GPS: Provider not enabled");
		}
		
		//handler = new Handler(Looper.getMainLooper());
	}

	public void start() {
		locationManager.addGpsStatusListener(onGpsStatusChange);
	
		
		/*if (mSensorMan == null) {
			mSensorMan = (SensorManager) context
					.getSystemService(Context.SENSOR_SERVICE);
		}

		if (mSensorMan != null) {
			if (mOrientListener == null) {
				mOrientListener = new OrientListener();
			}

			if (mOrientSensor == null) {
				mOrientSensor = mSensorMan
						.getDefaultSensor(Sensor.TYPE_ORIENTATION);
				
				if (mOrientSensor != null) {
					mSensorMan.registerListener(mOrientListener, mOrientSensor,
							SensorManager.SENSOR_DELAY_NORMAL);
				}
			}
		}*/

		mPeriodicUpdate = new PeriodicDisplayUpdate();
		
		// Start up GPS and then hold
		//mStartupRoutine = new LocationStartupRoutine();
		//scheduler.execute(mStartupRoutine);
		
		/*try {
			Log.i("GPS","GPS: Waiting for initialization");
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		startSensor(mPeriodicUpdate);
		//mPeriodicUpdate.run();
		
		Log.i("GPS","GPS: Start");
	}

	
	private void startSensor(PeriodicDisplayUpdate runnable){
		scheduler.scheduleAtFixedRate(runnable, 0, 1000, TimeUnit.MILLISECONDS);
		//scheduler.execute(runnable);
		Log.i("GPS","Turning GPS scheduled sensor on");
		/*try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scheduler.scheduleAtFixedRate(runnable, 0, 1000, TimeUnit.MILLISECONDS);*/
		Log.i("GPS","Done with GPS initialization");
		
	}
	
	
	
	public void stop() {
		try {
			if(scheduler!=null && !scheduler.isShutdown()){
				scheduler.shutdownNow();
				scheduler = null;
				Log.i("GPS","GPS: Stopping GPS service");
			}	
				
			/*if(mSensorMan != null)
				mSensorMan.unregisterListener(mOrientListener, mOrientSensor);*/
			
			if(locationManager != null)
				locationManager.removeUpdates(this);
		} catch (Exception e) {
			Log.i("GPS","GPSSensor error:" + e.getMessage());
		}
		
		/*finally {
			//mPeriodicUpdate = null; // is stop flag.
		} */	
	}
	
	
	
	
	
	
	
	private boolean is_location_upadted = false;
	private boolean is_compass_upadted = false;
	private boolean is_satellites_upadted = false;
	
	private static Object lock_location = new Object();
	private static Object lock_compass = new Object();
	private static Object lock_sattelites = new Object();
	
	
	/// location update

	private class PeriodicDisplayUpdate implements Runnable {
		public void run() {
 		  handler.post( new Runnable() {
		  @Override
		  public void run() {
			Log.i("GPS","GPS: Running Periodic Update");
			//location
			synchronized (lock_location) {
				
		     try {
				if(locationManager != null){
					//locationManager.removeUpdates(GPSSensor.this);
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, GPSSensor.this);
					
					Log.i("GPS","GPS: Location update requested");
				}
			 } catch (Exception e) {
				Log.i("GPS","GPS: Exception in locking location: "+e.toString());
			 }
			
			 try {
				if(is_location_upadted){
					is_location_upadted = false;
					
					updateLocation();	
					Log.i("GPS","GPS: Location updated");
				}
			 } catch (Exception e) {
				Log.i("GPS","GPS: Exception in location update: "+e.toString());
			 }
			 
			 lock_location.notifyAll();
			
			}
			
			
			///compass and orientation
			/*synchronized (lock_compass) {
			 try {
				if(is_compass_upadted){
					is_compass_upadted = false;
					
					updateCompass();							
				}
			 } catch (Exception e) {
				ConsoleLog.getInstance().write(DataProbeActivity.getTimeString()+">GPSSensor:updateCompass() error:" + e.getMessage() + "\n");
			 }			
				
			 lock_compass.notifyAll();			
			}*/
						
			
			///sattelites
			synchronized (lock_sattelites) {
			 try {
				if(is_satellites_upadted){
					is_satellites_upadted = false;
					updateSatellites();		
					Log.i("GPS","GPS: Updating satellites");
				}
			 } catch (Exception e) {
				Log.i("GPS","GPS: Exception in locking satellites: "+e.toString());
			 }
			 
			 lock_sattelites.notifyAll();			
			}			 
			
		  }
		  });			
			
			//updateLocation();
			//updateCompass();
			//updateSatellites();
			
			//if(mPeriodicUpdate != null)
				//handler.postDelayed(mPeriodicUpdate, 1000 /* ms */);
		}
	}

	/*
	private class LocationStartupRoutine implements Runnable {
		public void run() {
 		  handler.post( new Runnable() {
		  @Override
		  public void run() {
			Log.i("GPS","GPS: Initializing fix");
			//location
			synchronized (lock_location) {
				
		     try {
				if(locationManager != null){
					locationManager.removeUpdates(GPSSensor.this);
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, GPSSensor.this);
					Log.i("GPS","GPS: Initial location update requested");
				}
			 } catch (Exception e) {
				ConsoleLog.getInstance().write(DataProbeActivity.getTimeString()+">GPSSensor:locationManager error:" + e.getMessage() + "\n");
				Log.i("GPS","GPS: Exception in locking initial location: "+e.toString());
			 }
			
			 try {
				if(is_location_upadted){
					is_location_upadted = false;
					
					updateLocation();	
					Log.i("GPS","GPS: Location initialized");
				}
			 } catch (Exception e) {
				ConsoleLog.getInstance().write(DataProbeActivity.getTimeString()+">GPSSensor:updateLocation() error:" + e.getMessage() + "\n");
				Log.i("GPS","GPS: Exception in location initialization: "+e.toString());
			 }
			 
			 lock_location.notifyAll();
			
			}
					
			///sattelites
			synchronized (lock_sattelites) {
			 try {
				if(is_satellites_upadted){
					is_satellites_upadted = false;
					updateSatellites();		
					Log.i("GPS","GPS: Updating satellites");
				}
			 } catch (Exception e) {
				ConsoleLog.getInstance().write(DataProbeActivity.getTimeString()+">GPSSensor:updateSatellites() error:" + e.getMessage() + "\n");
				Log.i("GPS","GPS: Exception in locking satellites: "+e.toString());
			 }
			 
			 lock_sattelites.notifyAll();			
			}			 
			
		  }
		  });			
			
		}
	}*/

	
	public void setGPSListener(GPSListener _listener) {
		listener = _listener;
	}
	
	
	private synchronized void updateLocation() {
    	if(currentLocation != null && listener != null) {
    		Log.i("GPS","GPS: Location:"+currentLocation.getLatitude()+","+currentLocation.getLongitude());
    		listener.onLocationChange(currentLocation.getLatitude(), 
    				currentLocation.getLongitude(), 
    				meters_to_feet(currentLocation.getAltitude()), meters_to_miles(currentLocation.getSpeed()));
    		// Clear out locations
    	
    	
    	}else 
    	{
    		if(listener != null)
    		{
    			Log.i("GPS","GPS: Current location is not found");
    			listener.onLocationChange(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
    		}
    		else
    		{
    			Log.i("GPS","GPS: Listener is NULL");
    		}
    	}
			
		
    }

	
	
	
	
	
	 private synchronized void updateCompass() {
		 if(listener != null)
			 listener.onCompassChange(mOrientation, mOrientation2, mOrientAccuracy);
	}
	
	
	 private synchronized void updateSatellites() {
		 if(listener != null)
			 listener.onStatusChange(mPositionOfSatellites);
	}
	 
	 
	 
	 private float meters_to_miles(float speed_mps) {
		  return (float) speed_mps * 2.23693629f ;
	}
	 
	 
	 private double meters_to_feet(double alt_meters){

			BigDecimal x = new BigDecimal(alt_meters*3.28f);
			x = x.setScale(2, BigDecimal.ROUND_HALF_UP); /* 5 - ���������� ������ ����� ������� */
			return  x.doubleValue();
			
			//return  (float)alt_meters*3.28f;
	    	  	
	    }
	
	
	 /*
	  * Listener for implementation processing captured data
	  * */
	 public interface GPSListener {
		 public void onCompassChange(float mOrientation, float mOrientation2, String mOrientAccuracy);
		 public void onLocationChange(double Lat, double Lng, double Alt, double speed);
		 public void onStatusChange(String sat);
	 }







	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		stop();
		super.finalize();
	}






	public void onLocationChanged(Location location) {
		synchronized (lock_location) {
			currentLocation=location;
		
			is_location_upadted = true;
			
			lock_location.notifyAll();
			Log.i("GPS","GPS: onLocationchanged");
		}	
		
		/*locationManager.removeUpdates(GPSSensor.this);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, GPSSensor.this);
		updateLocation(); */		
		
		//if (mPeriodicUpdate != null)
			//mPeriodicUpdate.run();
	}

	public void onProviderDisabled(String arg0) {
	}

	public void onProviderEnabled(String arg0) {
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		
	}
	 
	 
	 
	 
	// need for heading
	private class OrientListener implements SensorEventListener {

		private static final int WINDOW_SIZE = 20;
		private float[] mWindow = new float[WINDOW_SIZE];
		private int mNbWindow = -1 * WINDOW_SIZE;
		private float mWindowAvg = 0;

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// pass
		}

		public void onSensorChanged(SensorEvent event) {
		  synchronized (lock_compass) {
			if (event.sensor == mOrientSensor) {
				String accuracy = event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH ? "High"
						: event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ? "Medium"
								: event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW ? "Low"
										: event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ? "Unreliable"
												: "Unknown";

				mOrientAccuracy = accuracy;
				mLastOrientTS = System.currentTimeMillis();

				mOrientation = event.values[0];
				
				
				try {
					mOrientation2 = windowAvg(event.values[0]);
				} catch (Exception e) {
					Log.i("GPS","GPS: Exception " + e.toString());
				}
				
				is_compass_upadted = true;
				
				/* updateCompass(); */
				
				
				//if (mPeriodicUpdate != null)
					//mPeriodicUpdate.run();				
			}
			
			lock_compass.notifyAll();
		  }	
		}

		
		
		
		
		
		
		
		private float windowAvg(float v) {
			int n = mNbWindow;

			if (n < 0) {
				// fill initial values: n=-10 ... n=-1
				n++;
				// -9 => 10-9-1 = 0 ... 0 => 10+0-1 = -9
				int j = WINDOW_SIZE + n - 1;
				mWindow[j] = v;
				if (n == 1 - WINDOW_SIZE) {
					mWindowAvg = v;
				} else {
					// Before: T = Sum/n
					// After : n'= n+1
					// T'= Sum/n' + v/n'
					// T'= T*n/n' + v/n'
					// T'= (T*n+v)/n'
					mWindowAvg = (j * mWindowAvg + v) / (j + 1);
				}
			} else {
				// n >= 0
				// Before: T = Sum/n
				// After : T'= (Sum - value[-10] + new_value) / n
				// T'= T - v[-10]/n + v/n
				// T'= T + (v - v[-10]) / n

				float old = mWindow[n];
				mWindow[n] = v;
				n++;
				if (n == WINDOW_SIZE)
					n = 0;

				mWindowAvg += (v - old) / WINDOW_SIZE;
			}

			mNbWindow = n;
			return mWindowAvg;
		}
	}


	private Listener onGpsStatusChange=new GpsStatus.Listener() {
		
		public void onGpsStatusChanged(int event) {
		  synchronized (lock_sattelites) {
		  
			try {
				switch(event){
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					locationManager.getGpsStatus(null);
					mPositionOfSatellites=String.format("%d", countSatellites());
					
					is_satellites_upadted = true;
					
					/* updateSatellites(); */
					
					//if (mPeriodicUpdate != null)
						//mPeriodicUpdate.run();				
					
				}
			} catch (Exception e) {
				Log.i("GPS","GPS: Exception " + e.toString());
			}
		  
		    lock_sattelites.notifyAll();
		  }	
			
		}
	};

	private int countSatellites(){
		int count=0;
		GpsStatus status=locationManager.getGpsStatus(null);
		for(GpsSatellite sat:status.getSatellites())
			if(sat.usedInFix())
				count++;
		Log.i("GPS","GPS: Number of satellites = " + count);
		return count;
	}

}
