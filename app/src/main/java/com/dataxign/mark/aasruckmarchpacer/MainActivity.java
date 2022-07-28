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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.dataxign.mark.aasruckmarchpacer.functions.HelperStuff;
import com.dataxign.mark.aasruckmarchpacer.geo.LocationPoint;
import com.dataxign.mark.aasruckmarchpacer.geo.Route;
import com.dataxign.mark.aasruckmarchpacer.geo.Segment;
import com.dataxign.mark.aasruckmarchpacer.mdp.DataManager;
import com.dataxign.mark.aasruckmarchpacer.geo.DataSmoother;
import com.dataxign.mark.aasruckmarchpacer.mdp.ObanSensor;

import java.util.List;

public class MainActivity extends Activity {

    private final int UI_UPDATE_TIME_MILLIS = 1000;
    private final int SMOOTHING_INTERVAL_MILLIS = 6000;

    private Handler handler = null;
    private Activity act = this;

    private boolean started = false;

    //Widgets for user interface
    private TextView lat_raw, lon_raw, easting, northing, updateFreq;
    private TextView speedinst, speedave, headinginst, headingave;
    private TextView segmentnum, segmenthead, segmentdist;
    private EditText roster_num_input;
    private TextView heart_rate, battery;
    private Button button_start, button_mode, button_pair, button_policy, button_route;
    private MapChart_CustomView map;
    private int mapDisplayMode = 0; //0=Map, 1=Pacing Display

    // For location logic
    private LocationManager locationManager;
    private MainLocationListener locationListener;
    private Location location;
    private LocationPoint currentLocation;
    private long updateTime = 0, updateInterval = 0, lastSmoothUpdate = 0;
    private DataSmoother moveData;

    private double guidance = 0; // The guidance speed
    private double speed; // The subjects current speed
    private int HR; // The subjects current heart rate
    private int Battery; // The current sensor battery

    public ObanSensor sensor;

    // Data manager
    public DataManager dm;
    Route route = null;

    // Initialization functions
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Request permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_SCAN}, 1);
        boolean fine_loc_perm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean course_loc_perm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (fine_loc_perm && course_loc_perm) {return;}

        // Set orientation to landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        initUI(); // Grab UI Elements
        initLocations(); // Initialize the Location Elements for using the GPS

        handler = new Handler(); // Initialize the UI manager
        handler.post(activityUIManager); // Start the system loop
    }

    private void initUI() {
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

        roster_num_input = (EditText) findViewById(R.id.roster_num_input);
        heart_rate = (TextView) findViewById(R.id.heart_rate);
        battery = (TextView) findViewById(R.id.battery);

        map = (MapChart_CustomView) findViewById(R.id.map_chart);

        // Start button
        button_start = (Button) findViewById(R.id.buttonStart);
        button_start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dm.startSession();
            }
        });

        // Mode button
        button_mode = (Button) findViewById(R.id.buttonMode);
        button_mode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Change the state of the custom view to show the guidance graphic
                if (mapDisplayMode == MapChart_CustomView.MAP_DRAW_MAP)
                    mapDisplayMode = MapChart_CustomView.MAP_DRAW_GUIDE;
                else mapDisplayMode = MapChart_CustomView.MAP_DRAW_MAP;
                map.displayMode = mapDisplayMode;
            }
        });

        // Pair device button
        button_pair = (Button) findViewById(R.id.pair_device_button);
        button_pair.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onClick(View v) {
                String roster_num_str = roster_num_input.getText().toString();
                int roster_num = Integer.parseInt(roster_num_str);
                sensor = new ObanSensor(roster_num, getApplicationContext(), act);
            }
        });

        // Button to pick the policy file
        button_policy = (Button) findViewById(R.id.policy_button);
        button_policy.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onClick(View v) {
                openFileChooser(0);
            }
        });

        // Button to pick the route file
        button_route = (Button) findViewById(R.id.route_button);
        button_route.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onClick(View v) {
                openFileChooser(1);
            }
        });

        moveData = new DataSmoother();
    }

    public void onActivityResult(int requestcode, int resultcode, Intent data) {
        super.onActivityResult(requestcode, resultcode, data);
        if (resultcode == Activity.RESULT_OK) {
            if(data == null) {return;}
            if (requestcode == 0) {
                Context context = getApplicationContext();
                Uri uri = data.getData();
                Toast.makeText(context, uri.getPath(), Toast.LENGTH_SHORT).show();
                dm = new DataManager(uri, getApplicationContext());
            }
            if (requestcode == 1) {
                Context context = getApplicationContext();
                Uri uri = data.getData();
                Toast.makeText(context, uri.getPath(), Toast.LENGTH_SHORT).show();
                route = new Route(uri, context);
                map.updateRoute(route);
            }
        }
    }

    public void openFileChooser(int requestcode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, requestcode);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void initLocations() {
        currentLocation = new LocationPoint();
        locationListener = new MainLocationListener();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            // Checking if the app has the proper permissions
            int pg = PackageManager.PERMISSION_GRANTED;
            boolean afl_perm = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != pg;
            boolean acl_perm = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != pg;
            if (afl_perm && acl_perm) {
                return;
            }

            // Subscribing the locationManager to the locationListener
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener);
        } else {
            Log.e("Main", "GPS NOT Enabled");
        }
    }

    /**
     * This is the LocationListener class. It triggers the onLocationChanged() method when the
     * location is updated from the Android location service.
     */
    private class MainLocationListener implements LocationListener {
        public MainLocationListener() {
            Log.d("init", "Location Listener created!");
        }

        public void onLocationChanged(Location local) {
            location = local;
            Log.v("onLocationChanged", "Got location");
        }
    };

    /**
     * Retrieves the last known location from the LocationListener.
     * @return The last known location
     */
    private Location getLastKnownLocation() {
        //locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Log.v("gg","bleh");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("getLastKnownLocation", "Location permissions not granted.");
                return null;
            }
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    /**
     * Updates UI and system variables given a location
     * @param local The user's current location
     */
    private void handleCurrentLocation(Location local) {
        if (local != null) {
            speed = local.getSpeed();

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
            Log.d("MainActivity", "Current Route = " + snap.currentSegment);

            // If the LocationPoint was able to be snapped onto the route
            if (snap.currentSegment >= 0) {
                Segment s = route.route.get(snap.currentSegment);
                map.currentSeg = s;
                map.snap = snap;
                segmentnum.setText("Segment #: "+snap.currentSegment);
                segmenthead.setText("Segment Heading: "+HelperStuff.trimIt(s.heading,2));
                segmentdist.setText("Distsance Along Segment = "+HelperStuff.trimIt(snap.distanceAlongSegment,2));
            } else {
                segmentnum.setText("Segment #: None");
                segmenthead.setText("Segment Heading: None");
                segmentdist.setText("Distsance Along Segment = WHO KNOWS?");
                map.currentSeg = null;
                map.snap = null;
            }

            // Update the guidance UI to provide speed guidance based on how fast user currently moving
            if(moveData.getMovingAverageSpeed()<guidance)map.speedGraphic=MapChart_CustomView.SPEED_FASTER;
            if(moveData.getMovingAverageSpeed()>guidance)map.speedGraphic=MapChart_CustomView.SPEED_SLOWER;
            if(Math.abs(moveData.getMovingAverageSpeed() - guidance)<0.2)map.speedGraphic=MapChart_CustomView.SPEED_OK;

            map.setLocation(currentLocation);
            map.postInvalidate();
        }
        else {
            Log.e("handleLocation","Location is null.");
        }
    }

    /**
     * Updates the UI and gets data from the sensor
     */
    private void handleCurrentSensor() {
        if (sensor != null) {
            HR = sensor.getHR();
            Battery = sensor.getBattery();
            heart_rate.setText(String.valueOf(HR));
            battery.setText(String.valueOf(Battery));
        }
    }

    /**
     * This runnable is the system loop.
     */
    private Runnable activityUIManager = new Runnable() {
        public void run() {
            Log.v("MainRunnable", "Main system loop");
            long currentTime=System.currentTimeMillis(); // Get the current system time
            if(currentTime-lastSmoothUpdate >= SMOOTHING_INTERVAL_MILLIS) {
                moveData.smoothData(); // If it's time to smooth data for the UI, do that
                lastSmoothUpdate=currentTime;
            }
            Location local = getLastKnownLocation(); // Get the current location and handle the current location
            if (started) {
                handleCurrentLocation(local);
                handleCurrentSensor(); // Get the data from the sensor and update the data manager
                dm.update(HR, speed);
                guidance = dm.getCurrent(dm.GUID);
                map.postInvalidate(); // Tell Android the map needs to be redrawn ASAP
            }
            handler.postDelayed(activityUIManager, UI_UPDATE_TIME_MILLIS); // Loop the runnable
        }
    };
}
