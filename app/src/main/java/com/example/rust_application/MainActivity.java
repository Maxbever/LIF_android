package com.example.rust_application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.MessageFormat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor pressure;
    private TcpClient mTcpClient;
    private float light;
    private String ipAddress;
    LocationRequest locationRequest;

    static {
        System.loadLibrary("lif_android");
    }

    TextView textViewIpAddress;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewIpAddress = findViewById(R.id.getIPAddress);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        textViewIpAddress.setText("Your Device IP Address: " + ipAddress);

        Thread thread = new Thread() {
            public void run() {
                main(ipAddress);
            }
        };

        thread.start();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityResultLauncher<String[]> locationPermissionRequest =
                    registerForActivityResult(new ActivityResultContracts
                                    .RequestMultiplePermissions(), result -> {
                                Boolean fineLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                                Boolean coarseLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_COARSE_LOCATION, false);
                                if (fineLocationGranted != null && fineLocationGranted) {
                                    // Precise location access granted.
                                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                    // Only approximate location access granted.
                                } else {
                                    // No location access granted.
                                }
                            }
                    );
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

        locationRequest = locationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(50);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    if (locationResult == null) {
                        return;
                    }
                    //Showing the latitude, longitude and accuracy on the home screen.
                    for (Location location : locationResult.getLocations()) {
                        send_data(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };

        new ConnectTask().execute("");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * A native method that is implemented by the 'rust' native library,
     * which is packaged with this application.
     * @param ipAddress
     */
    public static native void main(String ipAddress);

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
         light = event.values[0];
    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> implements TcpClient.OnMessageReceived {

        @Override
        protected TcpClient doInBackground(String... message) {
            try {
                Thread.sleep(1 000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //we create a TCPClient object
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            }, ipAddress);
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("LOCA2", "response " + values[0]);
            //process server response here....

        }

        @Override
        public void messageReceived(String message) {
            Log.d("LOCA2", "response :" + message);
        }
    }

    private void send_data(double latitude, double longitude){
        if (mTcpClient != null) {
            mTcpClient.sendMessage("attach GPS_DATA admin");
            mTcpClient.sendMessage("out ("+latitude+","+longitude+","+light+")");
        }
    }
}