package com.csc354cde335.kuselftour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * Created by Steven on 2/28/2015.
 * This class performs displaying of overlaid content
 * using the onDraw method of our view.
 * This class also implements a LocationListener,
 * using the device's GPS to find its location.
 */
public class OverlayView extends View implements SensorEventListener, LocationListener{

    // Begin Class Fields --------------------------------------- C

    /**
     * The Log.e's Debug tag
     */
    public static final String DEBUG_TAG = "OverlayView Log";

    // End Class Fields --------------------------------------- C


    // Begin SensorEvent Fields --------------------------------------- S

    /**
     * String containing last known value of device Accelerometer.
     */
    String accelData = "Accelerometer Data";

    /**
     * String containing last known value of device Compass.
     */
    String compassData = "Compass Data";

    /**
     * String containing last known value of device Gyroscope.
     */
    String gyroData = "Gyro Data";

    /**
     * These objects represent each hardware object and their software manager
     */
    SensorManager sensors; // Sensor Manager
    Sensor accelSensor; // measured in m/s^2 ( ex. Earth's gravity is 9.81 m/s^2 )
    Sensor compassSensor; // measured in micro-Teslas (x, y, z) Determine magnetic north
    Sensor gyroSensor; // measured in rotations around each axis in radians per second

    /**
     * Values for calculating device orientation
     */
    float[] lastAccelerometer;
    float[] lastCompass;

    /**
     * Availability tests for each hardware object
     */
    boolean isAccelAvailable;
    boolean isCompassAvailable;
    boolean isGyroAvailable;

    // End SensorEvent Fields --------------------------------------- S


    // Begin Location Fields --------------------------------------- L

    /**
     * This field holds the last known location by device hardware
     */
    private Location lastLocation = null;

    /**
     * This location manager manages listening updates
     */
    private LocationManager locationManager = null;

    /**
     * This is a test location for Mt. Washington to test the application
     * and its associated static class to set definitions
     */
    private final static Location mountWashington = new Location("manual");
    // Static class for packaging convenience with locations
    static {
        // Mount Washington is just a test value
        mountWashington.setLatitude(44.27179d);
        mountWashington.setLongitude(-71.3039d);
        mountWashington.setAltitude(1916.5d);
    }

    /**
     * Another test value, the exact coordinates of my house
     */
    private final static Location StevesHouse = new Location("manual");
    static {
        StevesHouse.setLatitude(40.1185573);
        StevesHouse.setLongitude(-75.48192219999999);
        StevesHouse.setAltitude(51.5112);
    }

    // End Location Fields --------------------------------------- L


    // Begin Class Methods --------------------------------------- C

    /**
     * Standard Constructor
     * Assign values to some initial fields
     * Register listeners with sensor objects
     * @param context - current context
     */
    public OverlayView(Context context) {
        super(context);

        // Assign values to the Sensor objects
        sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Assign listeners to sensor objects
        isAccelAvailable = sensors.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isCompassAvailable = sensors.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isGyroAvailable = sensors.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Set parameters (Criteria) for GPS hardware
        locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

        // Select a provider and retrieve any location available
        lastLocation = getLocation();
    }


    // End Class Methods --------------------------------------- C


    // Begin SensorEvent Methods --------------------------------------- S

    /**
     * This draw method is what draws data to the screen
     * @param canvas - what surface to draw onto
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Set text properties
        contentPaint.setTextAlign(Paint.Align.LEFT);
        contentPaint.setTextSize(28);
        contentPaint.setColor(Color.WHITE);

        // Write text data to the overlay for testing purposes
        final int left_margin = 15;
        canvas.drawText("DEBUG " +
                "Width:" + canvas.getWidth() + " " +
                "Height:" + canvas.getHeight(),
                canvas.getWidth()/left_margin,
                canvas.getHeight()/30,
                contentPaint);
        canvas.drawText(accelData,
                canvas.getWidth()/left_margin,
                canvas.getHeight()/30 + 28,
                contentPaint);
        canvas.drawText(compassData,
                canvas.getWidth()/left_margin,
                canvas.getHeight()/30 + (28*2),
                contentPaint);
        canvas.drawText(gyroData,
                canvas.getWidth()/left_margin,
                (canvas.getHeight())/30 + (28*3),
                contentPaint);
        canvas.drawText("Lat:" +
                lastLocation.getLatitude() +
                ", Long: " + lastLocation.getLongitude(),
                canvas.getWidth()/left_margin,
                (canvas.getHeight())/30 + (28*4),
                contentPaint);

        // Get and print orientation
        float[] orientation = getOrientation();

        canvas.drawText("Orientation: " +
                orientation[0] + " " +
                orientation[1] + " " +
                orientation[2],
                canvas.getWidth()/left_margin,
                (canvas.getHeight()/30 + (28*5)),
                contentPaint);

        float curBearingToMW = lastLocation.bearingTo(StevesHouse);
        canvas.drawText("Bearing for test: " + Float.toString(curBearingToMW),
                canvas.getWidth()/left_margin,
                (canvas.getHeight())/30 + (28*6),
                contentPaint);

        // Primary Test
        // use roll for screen rotation
        canvas.rotate((float)(0.0f - Math.toDegrees(orientation[2])));

        // Translate, but normalize for the FOV of the camera
        float dx = (float) ( (canvas.getWidth()/ ArDisplayView.horizontalFOV) * (Math.toDegrees(orientation[0])-curBearingToMW));
        float dy = (float) ( (canvas.getHeight()/ ArDisplayView.verticalFOV) * Math.toDegrees(orientation[1])) ;

        // wait to translate the dx so the horizon doesn't get pushed off
        canvas.translate(0.0f, 0.0f-dy);

        // Create a line big enough to draw regardless of rotation and translation
        canvas.drawLine(0f - canvas.getHeight(),
                canvas.getHeight()/2,
                canvas.getWidth()+canvas.getHeight(),
                canvas.getHeight()/2,
                contentPaint);

        // now translate the dx
        canvas.translate(0.0f-dx, 0.0f);

        // draw a point
        canvas.drawCircle(canvas.getWidth()/2, canvas.getHeight()/2, 8.0f, contentPaint);
    }

    /**
     * This function retrieves the devices orientation using hardware devices
     * @return - device orientation
     */
    protected float[] getOrientation(){
        // Compute the rotation matrix
        float rotation[] = new float[9];
        float identity[] = new float[9];
        boolean gotRotation = SensorManager.getRotationMatrix(rotation,
                identity, lastAccelerometer, lastCompass);

        // Compute orientation vector
        if (gotRotation) {
            // orientation vector
            float orientation[] = new float[3];
            SensorManager.getOrientation(rotation, orientation);
        }

        float cameraRotation[] = new float[9];
        float orientation[] = new float[3];

        // Remap the rotation matrix so that the camera is pointed along
        // the positive direction of the Y-axis
        if (gotRotation) {

            // remap such that the camera is pointing straight down the Y axis
            SensorManager.remapCoordinateSystem(rotation,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    cameraRotation);

            SensorManager.getOrientation(cameraRotation, orientation);
        }

        return orientation;
    }

    /**
     * Here we have the view react to sensor changes.
     * The class is registered for several different changes
     * so this method may be called for any sensor change.
     *
     * This class is saving the sensor data that changed and forcing
     * the view to update its content by invalidating the view and
     * forcing it to be redrawn.
     * @param event - the sensor event to be handled by function
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        // Define text values
        StringBuilder msg = new StringBuilder(event.sensor.getName()).append(" ");
        for(float value: event.values){
            msg.append("[").append(value).append("]");
        }

        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                // Get new value
                accelData = msg.toString();

                // Get Accelerometer matrix
                lastAccelerometer = event.values.clone();
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroData = msg.toString();

                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                // Get new value
                compassData = msg.toString();

                // Get Compass matrix
                lastCompass = event.values.clone();
                break;
        }

        // Force a redraw of the current view
        this.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // This method has no bearing on the application
    }

    // End SensorEvent Methods --------------------------------------- S

    // Begin Location Methods --------------------------------------- L

    /**
     * This function is appropriated from GpsTracker.java
     * 2/28/2015
     * Code was drawn from an internet tutorial by Justin Hartz
     * @return - lastLocation
     */
    public Location getLocation() {
        try {

            // The minimum distance to change Updates in meters
            final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

            // The minimum time between updates in milliseconds
           final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

            // getting GPS status
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
                Log.e("TEST", "No Provider Enabled");
            } else {
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (lastLocation == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lastLocation;
    }

    public void onLocationChanged(Location location) {
        lastLocation = location;
    }
    public void onProviderDisabled(String provider) {
        // ...
    }
    public void onProviderEnabled(String provider) {
        // ...
    }
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // ...
    }

    // End Location Methods --------------------------------------- L
}
