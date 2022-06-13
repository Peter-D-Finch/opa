/**
 * Edited 6/1/2022 by Peter Finch
 *
 * The program loads a Route.
 * A route is a set of contiguous segments.
 * A segment is made up of two LocationPoints.
 * A LocationPoint is a pair of coordinates (GPS: lat / long OR UTM: northing / easting)
 */

package com.dataxign.mark.aasruckmarchpacer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.File;

public class MainActivity extends Activity {

    private final int UI_UPDATE_TIME_MILLIS = 1000;
    private final int SMOOTHING_INTERVAL_MILLIS = 6000;
    private Handler handler = null;

    //Widgets
    private TextView lat_raw, lon_raw, easting, northing, updateFreq, speedinst, speedave, headinginst, headingave, segmentnum, segmenthead, segmentdist;
    private Button button_start, button_mode;
    private MapChart_CustomView map;
    private int mapDisplayMode = 0; //0=Map, 1=Pacing Display

    // For location logic
    private LocationManager locationManager;
    private MainLocationListener locationListener;
    private Location location, lastLocation;
    private boolean isGPSEnabled, isCellularEnabled;
    private LocationPoint currentLocation;
    private long updateTime = 0, updateInterval = 0, lastSmoothUpdate = 0;
    private DataSmoother moveData;
    private double desiredSpeed = 2.5; // mph

    // Data manager and sensor stuff
    public ObanSensor oban;
    public DataManager dm;

    // Route stuff
    Route route = null;
    State state = null;

    /**
     * This is Android's initialization function. It runs when the app is started up.
     * @param savedInstanceState Don't know what this is...
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request permissions
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_SCAN}, 1);
        boolean fine_loc_perm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean course_loc_perm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (fine_loc_perm && course_loc_perm) { return; }

        // Set orientation to landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        //Grab UI Elements
        initUI();

        //Initialize the Location Elements for using the GPS
        initLocations();

        //Initialize the route for the app
        initRoute();

        // Initialize Sensor
        initSensor(0);

        //Initialize the UI manager
        handler = new Handler();
        handler.post(activityUIManager);
    }

    /**
     * This function initializes all of the UI objects
     */
    private void initUI() {
        /*
        * This function sets up the UI. The UI consists of the following elements:
        *  - TextViews
        *  - MapChart_CustomView
        *  - Start button
        *  - Mode button
        *  - DataSmoother
        */
        lat_raw = (TextView) findViewById(R.id.lat_raw);
        lon_raw = (TextView) findViewById(R.id.lon_raw);
        easting = (TextView) findViewById(R.id.east);
        northing = (TextView) findViewById(R.id.north);
        updateFreq = (TextView) findViewById(R.id.update_time);
        speedinst = (TextView) findViewById(R.id.speedinst);
        speedave = (TextView) findViewById(R.id.speedave);
        headinginst = (TextView) findViewById(R.id.headinginst);
        headingave = (TextView) findViewById(R.id.headingave);
        segmentnum = (TextView) findViewById(R.id.current_seg);
        segmenthead = (TextView) findViewById(R.id.heading_segment);
        segmentdist = (TextView) findViewById(R.id.distance_segment);

        map = (MapChart_CustomView) findViewById(R.id.map_chart);

        button_start = (Button) findViewById(R.id.buttonStart);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onClick(View v) {
                //change state to started route.
                //Check whether on the segment 0 if not then ignore
                //button_start.setEnabled(false);
                Log.e("MainActivity", "Start Button pressed");
                oban.start();
            }
        });

        button_mode = (Button) findViewById(R.id.buttonMode);
        button_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Change the state of the custom view to show the guidance graphic
                if (mapDisplayMode == MapChart_CustomView.MAP_DRAW_MAP)
                    mapDisplayMode = MapChart_CustomView.MAP_DRAW_GUIDE;
                else mapDisplayMode = MapChart_CustomView.MAP_DRAW_MAP;
                map.displayMode = mapDisplayMode;
            }
        });
        moveData = new DataSmoother();
    }

    /**
     * This function initializes everything that has to do with location.
     */
    @TargetApi(Build.VERSION_CODES.M)
    protected void initLocations() {
        currentLocation = new LocationPoint();
        //creating an instance of the location listener, a subclass to the main activity
        locationListener = new MainLocationListener();
        //creating an instance of the location manager, which controls the location services
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //setting the boolean isGPSEnabled to flag whether or not the location services has access to hardware
        //GPS sensor or not.  This sensor will operate without Network connectivity
        //NOTE that this sensor was not accessable until after the phone was able to talk to Google's servers for location
        //services, but only needed access once, then it worked reliablely when disconnected from the internet
        //NOTE that this also will turn up false if the application does not have permission to access the location service for "FineLocation"
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //setting the boolean isCellularEnabled to flag whether or not the location services has access to
        //location through network provider.  this requires network connection
        isCellularEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        //if GPS Sensor is accessible to the application, then complete the code block
        if (isGPSEnabled) {
            Log.e("Main", "GPS Enabled");
            //request every 100 milliseconds    TODO make that a constant at the top
            //request updates at 1 meter         // TODO: 1/5/2016 make that a constant

            // Checking if the app has the proper permissions
            int pg = PackageManager.PERMISSION_GRANTED;
            boolean afl_perm = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != pg;
            boolean acl_perm = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != pg;
            if (afl_perm && acl_perm) { return; }

            // Subscribing the locationManager to the locationListener
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, locationListener);
        }
        else{
            Log.e("Main","GPS NOT Enabled");
        }
    }

    /**
     * This function initializes the Route for the run
     */
    private void initRoute() {
        int whichRoute = 0; //Will be used to specify predefined routes
        route = new Route(getResources(), whichRoute);
        state = new State();
        map.updateRoute(route);
    }

    /**
     * This function initializes the apps OBAN sensor
     * @param device_num The configuration number of the user's OBAN sensor
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initSensor(int device_num) {
        Log.e("initSensor","OBAN sensor initialized: #"+device_num);
        oban = new ObanSensor(0, getApplicationContext());
    }

    /**
     * This function initializes the apps data manager
     */
    private void initDataManager(File fp) {
        dm = new DataManager(37.1,1.5,getResources(),fp,getApplicationContext());
    }

    /**
     * This is the LocationListener class. It triggers the onLocationChanged() method when the
     * location is updated from the Android location service.
     */
    public class MainLocationListener implements LocationListener {
        public MainLocationListener() {
            Log.v("inits", "Just Created a LocationListener");
        }
        protected void saveData(Location local) {
            //adds a datapoint to temp data with the location data
            //tempData.add(new DataPoints(DataPoints.INDICATOR_GPS, local, lastLocation));
            //adds a datapoint to GPS  data with the location data
            //gpsData.add(new DataPoints(DataPoints.INDICATOR_GPS, local, lastLocation));
        }
        @Override
        public void onLocationChanged(Location local) {
            //reassigning the global last location field
            lastLocation = location;
            //assigning the global location field
            location = local;
            //handleCurrentLocation(local);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //not implemented or used
        }
        @Override
        public void onProviderEnabled(String provider) {
            //not implemented or used
        }
        @Override
        public void onProviderDisabled(String provider) {
            //not implemented or used
        }
    };

    /**
     * Updates UI and system variables given a location
     * @param local The user's current location
     */
    private void handleCurrentLocation(Location local) {
        Log.w("LocUpdate", "Location: " + local);
        if (local != null) {
            // Update the latitude and longitude on the UI
            lat_raw.setText("" + local.getLatitude());
            lon_raw.setText("" + local.getLongitude());

            // Set the current location, speed, and bearing
            currentLocation.setLocation(local.getLatitude(), local.getLongitude());
            currentLocation.setSpeed((double) local.getSpeed());
            currentLocation.bearing = (double) local.getBearing();

            // Add the current speed and heading to the DataSmoother
            moveData.addSpeed(currentLocation.speedmph);
            moveData.addHeading(local.getBearing());

            // Update the easting and northing on the UI
            easting.setText("" + HelperStuff.trimIt(currentLocation.easting,3));
            northing.setText("" + HelperStuff.trimIt(currentLocation.northing,3));

            // Calculate the update time and update it on the UI
            long currentTime = System.currentTimeMillis();
            updateInterval = currentTime - updateTime;
            updateTime = currentTime;
            updateFreq.setText("Update Interval: " + ((float) updateInterval / 1000f) + " s");

            // Update the instant and average speed on the UI
            speedinst.setText("Speed (inst): " + HelperStuff.trimIt(currentLocation.speedmph,2));
            speedave.setText("Speed (ave): " + HelperStuff.trimIt(moveData.getMovingAverageSpeed(), 2));

            // Update the instant and average heading on the UI
            headinginst.setText("Heading (inst): " + HelperStuff.trimIt(currentLocation.bearing,2));
            headingave.setText("Heading (ave): " + HelperStuff.trimIt(moveData.getLastHeading(), 2));

            // Snapping the location point onto the route
            LocationPoint snap = route.snapToLine(currentLocation);
            Log.v("Main", "Current Route = " + snap.currentSegment);

            // If the LocationPoint was able to be snapped onto the route
            if (snap.currentSegment >= 0) {
                Segment s = route.route.get(snap.currentSegment);
                map.currentSeg = s;
                map.snap = snap;
                segmentnum.setText("Segment #: "+snap.currentSegment);
                segmenthead.setText("Segment Heading: "+HelperStuff.trimIt(s.heading,2));
                segmentdist.setText("Distsance Along Segment = "+HelperStuff.trimIt(snap.distanceAlongSegment,2));
            }
            // Else something must be very broken, let's hope this never happens
            else {
                segmentnum.setText("Segment #: None");
                segmenthead.setText("Segment Heading: None");
                segmentdist.setText("Distsance Along Segment = WHO KNOWS?");

                map.currentSeg = null;
                map.snap = null;
            }

            // Update the guidance UI to provide speed guidance based on how fast user currently moving
            if(moveData.getMovingAverageSpeed()<desiredSpeed)map.speedGraphic=MapChart_CustomView.SPEED_FASTER;
            if(moveData.getMovingAverageSpeed()>desiredSpeed)map.speedGraphic=MapChart_CustomView.SPEED_SLOWER;
            if(Math.abs(moveData.getMovingAverageSpeed() - desiredSpeed)<0.2)map.speedGraphic=MapChart_CustomView.SPEED_OK;
            //Log.e("MainAct: ","Des Speed: "+desiredSpeed+ " Actual Speed: "+moveData.getLastSpeed()+" Map Graphic: "+map.speedGraphic);

            map.setLocation(currentLocation);

            // Tell Android that the map has been changed and needs to be redrawn ASAP
            map.postInvalidate();
        }
    }

    /**
     * Retrieves the last known location from the LocationListener.
     * @return The last known location
     */
    private Location getCurrentLocation(){
        Location loc=null;
        try {
            loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        catch(SecurityException e){
            Log.e("MainAct", "GPS Security Permission Exception "+e);
        }
        return loc;
    }

    private void updateDataManager() {
        // TODO Get data from HR sensor
        // TODO Convert that HR into TC
        // TODO Get the RPM value

        // TODO Add all three to DataManager
    }

    /**
     * This runnable is the system loop.
     */
    private Runnable activityUIManager = new Runnable() {
        private boolean updateMinuteValues = true;
        public void run() {
            // Get the current system time
            long currentTime=System.currentTimeMillis();

            // If it's time for the data to be smoothed
            if(currentTime-lastSmoothUpdate>=SMOOTHING_INTERVAL_MILLIS){
                moveData.smoothData();
                lastSmoothUpdate=currentTime;
            }

            // Get the current location
            Location loc=getCurrentLocation();
            handleCurrentLocation(loc);

            // TODO Get data from the sensor

            // TODO Update the DataManager


            // Tell Android the the map has been changed and needs to be redrawn ASAP
            map.postInvalidate();

            // Call this runnable to the handler
            handler.postDelayed(activityUIManager, UI_UPDATE_TIME_MILLIS);
        }
    };
}
