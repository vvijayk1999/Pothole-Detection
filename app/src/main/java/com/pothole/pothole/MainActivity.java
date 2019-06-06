package com.pothole.pothole;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Timer;

import static android.content.ContentValues.TAG;
import static com.pothole.pothole.R.color.grayTranslucent;
import static com.pothole.pothole.R.color.greenTranslucent;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnMapReadyCallback, LocationListener, PermissionsListener, LocationEngineListener {

    //////////////////////////MQTT Connect options/////////////////////////////
    String URL = "tcp://m16.cloudmqtt.com";
    String portNumber = "13941";
    String userName = "vbqcvuri";
    String password = "tgt22oGl7EwE";
    String callback;
    ///////////////////////////////////////////////////////////////////////////

    ///////////// XML resources //////////////////////////////////////////////
    TextView acc, gps_view, textView;
    ToggleButton button;
    Button show_potholes, showallpotholes, sync;
    ProgressBar progressBar;
    //////////////////////////////////////////////////////////////////////////

    ////////// MapBox declarations ///////////////////////////////////////////
    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;
    private com.mapbox.geojson.Point pothole;
    private Marker pothole_marker;
    /////////////////////////////////////////////////////////////////////////

    /////////////////////Database Helper/////////////////////////////////////
    DatabaseHelper myDb;
    int pothole_number=0;
    /////////////////////////////////////////////////////////////////////////

    private static final String TAG = "file_op";
    double ax, ay, az, longitude, latitude;
    boolean button_state = false, detection_state = false;
    private Timer myTimer;
    String uname;
    int maxValue;
    int progressValue;
    private SharedPreferences mPrefs;
    private static final String PREFS_NAME = "PrefsFile";

    private CoordinatorLayout coordinatorLayout;
    MQTTHelper mqttHelper;

    String clientId = MqttClient.generateClientId();
    MqttAndroidClient client;

    Dialog myDialogue;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!Permissons.Check_STORAGE(MainActivity.this))
        {
            //if not permisson granted so request permisson with request code
            Permissons.Request_STORAGE(MainActivity.this,22);
        }
        if(!Permissons.Check_FINE_LOCATION(MainActivity.this))
        {
            //if not permisson granted so request permisson with request code
            Permissons.Request_FINE_LOCATION(MainActivity.this,22);
        }
        if(!Permissons.Check_PHONE_STATE(MainActivity.this))
        {
            //if not permisson granted so request permisson with request code
            Permissons.Request_PHONE_STATE(MainActivity.this,22);
        }

        ////////////////////////////////  MQTT //////////////////////////////////////////////////////////////
        client = new MqttAndroidClient(this.getApplicationContext(), URL + ":" + portNumber, clientId);
        mqttHelper = new MQTTHelper();
        mqttHelper.connect(client, userName, password);

        ////////////////////////////////////////////////////////////////////////////////////////////////////


        myDb = new DatabaseHelper(this);

        if(!getLoginStatus(this)){
            Intent myIntent = new Intent(this, Login.class);
            startActivityForResult(myIntent,0);
        }


        acc = (TextView) findViewById(R.id.acc);
        textView = (TextView) findViewById(R.id.textView);


        //////////////////////////// READ FILE ////////////////////////////////////////////////////////////
        mPrefs=getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
        //////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////  Accelerometer //////////////////////////////////////////
        SensorManager sensorManager;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        //////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////  MapBOX  //////////////////////////////////////////////////
        Mapbox.getInstance(this, "pk.eyJ1Ijoic2FtcGF0aHNhbSIsImEiOiJjanNybDU2ZHAwN3d2NDNwNGUzdzRqNXZ5In0.D4gRrSnSPep_O9Txw55fkQ");
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        ///////////////////////////////////////////////////////////////////////////////////////////

        /////////////////////////// Pothole time detection ////////////////////////////////////////
        HandlerThread handlerThread = new HandlerThread("UIHandlerThread") {
            public void run() {
                while (true) {
                    while (detection_state) {
                        long startTime, endTime, elapsedMilliSeconds = 0;
                        boolean pothole_time;
                        if (az <= -4.0|| ax <= -4.0 || ay<=-4.0) {
                            pothole_time = true;
                            startTime = SystemClock.elapsedRealtime();
                            while (az <= -4.0|| ax <= -4.0 || ay<=-4.0 ) {
                            }
                            endTime = SystemClock.elapsedRealtime();
                            elapsedMilliSeconds = endTime - startTime;
                        }
                        if (elapsedMilliSeconds <= 350 && elapsedMilliSeconds != 0) {
                            Log.d(TAG, "elapsed time =" + elapsedMilliSeconds);
                            showToast();

                        }
                    }
                }

            }
        };
        //////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////// Current Coordinates  //////////////////////////////////////////
        final Thread getGPS = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    getCurrentLocation();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        getGPS.start();
        if (!handlerThread.isAlive())
            handlerThread.start();

        /////////////////////////////////////////////////////////////////////////////////////////
        button = (ToggleButton) findViewById(R.id.button);
        show_potholes = (Button) findViewById(R.id.show_potholes);
        showallpotholes = (Button) findViewById(R.id.showallpotholes);
        sync = (Button) findViewById(R.id.sync);
        progressBar=(ProgressBar) findViewById(R.id.progressBar);
         maxValue=progressBar.getMax();
        progressValue=progressBar.getProgress();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean on = ((ToggleButton) v).isChecked();

                if (on) {
                    button.setBackgroundColor(getResources().getColor(greenTranslucent));
                    Toast.makeText(MainActivity.this, "Started", Toast.LENGTH_LONG).show();
                    detection_state = true;
                    Log.d(TAG, "start trip");

                } else {
                    button.setBackgroundColor(getResources().getColor(grayTranslucent));
                    Toast.makeText(MainActivity.this, "Stopped", Toast.LENGTH_LONG).show();
                    detection_state = false;
                    Log.d(TAG, "stop trip");

                }

            }
        });

        show_potholes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Cursor res = myDb.getAllData();
                if (res.getCount() == 0) {
                    // show message
                    Toast.makeText(MainActivity.this, "No Pothole data found.", Toast.LENGTH_LONG).show();
                    return;
                }

                StringBuffer buffer = new StringBuffer();
                while (res.moveToNext()) {

                    pothole_marker = map.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(res.getString(2)), Double.parseDouble(res.getString(3)))));

                }

            }
        });
        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setProgress(100);
                Cursor res = myDb.getAllData();
                if (res.getCount() == 0) {
                    // show message
                    Toast.makeText(MainActivity.this, "No Pothole data found.", Toast.LENGTH_LONG).show();
                    return;
                }

                StringBuffer buffer = new StringBuffer();
                while (res.moveToNext()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mqttHelper.publish(client, "Client", "inslatlong,"+getUname(MainActivity.this) + "," + res.getString(1) + "," + res.getString(2) + "," + res.getString(3));
                }
                progressBar.setProgress(100);
            }
        });
        showallpotholes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttHelper.subscribe(client);
                mqttHelper.publish(client,"Client","getpotholes,"+getUname(MainActivity.this));

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {

                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        callback = new String(message.getPayload());
                        String[] values = callback.split(",");
                        //Log.d(TAG, callback);
                        Log.d(TAG, values[0]+values[1]+values[2]);
                        if(values[0].equals("potholes")&& values[1].equals(getUname(MainActivity.this)))
                            Log.d(TAG, values[0]+values[1]+values[2]);
                            pothole_marker = map.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(values[2]), Double.parseDouble(values[3]))));

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        });

    }
    public String getUname(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("uname_key",null);
    }
    public Boolean getLoginStatus(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("login_key",false);
    }
    public void showToast() {

        boolean isInserted = myDb.insertData(Integer.toString(pothole_number),Double.toString(latitude), Double.toString(longitude));
        if (isInserted == true) {
//            Toast.makeText(MainActivity.this, "Pothole Data Inserted", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Data inserted");
        } else {
//            Toast.makeText(MainActivity.this, "Pothole Data not Inserted", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Data not inserted");
        }
        pothole_number++;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main_menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.logout:
                setLoginStatusFalse(this);
                Intent myIntent = new Intent(this, Login.class);
                startActivityForResult(myIntent,0);

                Intent intent = getIntent();
                finish();
                startActivity(intent);

                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
    public void setLoginStatusFalse( Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean("login_key",false);
        prefsEditor.apply();
    }
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        enableLocation();
    }

    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
            initializeLocationLayer();

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.BALANCED_POWER_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void initializeLocationLayer() {
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING_COMPASS);
        locationLayerPlugin.setRenderMode(RenderMode.COMPASS);

    }

    private void setCameraPosition(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()), 17.0));
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocation();
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onConnected() {
        locationEngine.removeLocationUpdates();

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            originLocation = location;
            setCameraPosition(location);
        }

    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        if (locationEngine != null)
            locationEngine.requestLocationUpdates();
        if (locationLayerPlugin != null)
            locationLayerPlugin.onStart();

        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationEngine != null)
            locationEngine.removeLocationUpdates();
        if (locationLayerPlugin != null)
            locationLayerPlugin.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationEngine != null)
            locationEngine.deactivate();
        mapView.onDestroy();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            acc = (TextView) findViewById(R.id.acc);

            ax = event.values[0];
            ay = event.values[1];
            az = event.values[2];

            acc.setText(Double.toString(longitude) + "," + Double.toString(latitude));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    public void getCurrentLocation() {
        LocationManager locationManager;
        String context = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) getSystemService(context);
        Criteria crta = new Criteria();
        crta.setAccuracy(Criteria.ACCURACY_FINE);
        crta.setAltitudeRequired(false);
        crta.setBearingRequired(false);
        crta.setCostAllowed(true);
        crta.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(crta, true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Permissons.Request_STORAGE(MainActivity.this,22);
            return;
        }
        runOnUiThread(() ->
                locationManager.requestLocationUpdates(provider, 1000, 0,
                        new LocationListener() {
                            @Override
                            public void onStatusChanged(String provider, int status,
                                                        Bundle extras) {
                            }

                            @Override
                            public void onProviderEnabled(String provider) {
                            }

                            @Override
                            public void onProviderDisabled(String provider) {
                            }

                            @Override
                            public void onLocationChanged(Location location) {
                                if (location != null) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                }
                            }
                        }));
    }
}


