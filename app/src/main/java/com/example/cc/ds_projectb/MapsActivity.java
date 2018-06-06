package com.example.cc.ds_projectb;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private TextView finalResult;
    private EditText ipAddress, userID;

    private Socket requestSocket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        finalResult = findViewById(R.id.textView);

        ipAddress = findViewById(R.id.editText_ip);
        userID = findViewById(R.id.editText_user_id);

//        Get current user location
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        assert locationManager != null;
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        Log.d("LAT_LON", "LAT: " + latitude + " LON: " + longitude);

        Button btn = findViewById(R.id.button_connect);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean execute_socketConnect = true;
                if (userID.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.editText_userID_empty, Toast.LENGTH_SHORT).show();
                    execute_socketConnect = false;
                }

                if (ipAddress.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.editText_ip_empty, Toast.LENGTH_SHORT).show();
                    execute_socketConnect = false;
                }

                if (execute_socketConnect) {
                    AsyncTaskSocketConnect socketConnect = new AsyncTaskSocketConnect();
                    socketConnect.execute();
                }
            }
        });
    }

//    AsyncTask<doInBackground parameter type, onProgressUpdate parameter type, onPostExecute parameter type>
//    Connect to host via Socket using the ip given
    private class AsyncTaskSocketConnect extends AsyncTask<String, String, String> {

        private String ip;
        private int id;
        private ProgressDialog progressDialog;

        private boolean connected = false;

        @Override
        protected String doInBackground(String... params) {
            String result = null;

            publishProgress("Waiting for host..."); // Calls onProgressUpdate()
            try {
                requestSocket = new Socket(ip, 4321);

                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                result = "Successfully connected to " + ip + " as user " + id + '.';
                connected = true;
            } catch (UnknownHostException unknownHost) {
                result = "Couldn't connect to " + ip;
            } catch (ConnectException connectionException) {
                result = "Connection timed out.";
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onProgressUpdate(String... text) {
            progressDialog.setMessage(text[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            progressDialog.dismiss();
            finalResult.setText(result);
            String s = "connected: " + connected;
            Log.d("FINISHED_ASYNC_TASK", s);

            if (connected) {
                AsyncTaskSocketSendInfo sendInfo = new AsyncTaskSocketSendInfo();
                sendInfo.execute();
            }
        }


        @Override
        protected void onPreExecute() {
            ip = ipAddress.getText().toString();
            id = Integer.parseInt(userID.getText().toString());
            progressDialog = ProgressDialog.show(
                    MapsActivity.this,
                    "ProgressDialogSocketConnect",
                    "Waiting for " + ip + " to respond..."
            );
        }

    }

//    Send some information via Socket
    private class AsyncTaskSocketSendInfo extends AsyncTask<String, String, String> {

        private ProgressDialog progressDialog;

        @Override
        protected String doInBackground(String... params) {

            String result;

            int cores = Runtime.getRuntime().availableProcessors();
            long memory = Runtime.getRuntime().freeMemory();

            String s = "cores: " + cores + " memory: " + memory;
            Log.d("cores and memory", s);

            try {
                publishProgress("Sending number of cores...");
                out.writeInt(cores);
                out.flush();
                Log.d("CORES_SENT", "Number of cores successfully sent.");

                publishProgress("Sending available memory...");
                out.writeLong(memory);
                out.flush();
                Log.d("MEMORY_SENT", "Available memory successfully sent.");

                result = "Successfully sent system info to host.";

            } catch (IOException e) {
                e.printStackTrace();
                result = "IOException occurred while sending info to host.";
            }

            return result;

        }

        @Override
        protected void onProgressUpdate(String... text) {
            progressDialog.setMessage(text[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            progressDialog.dismiss();
            finalResult.setText(result);
        }


        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(
                    MapsActivity.this,
                    "ProgressDialogSendInfo",
                    "Sending information to host..."
            );
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Turn on "find location" button
        enableMyLocation();

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/

        LatLng athens = new LatLng(37.9908997, 23.7033199);
        mMap.addMarker(new MarkerOptions().position(athens).title("Αθήνα!!!").snippet("Γεια! :) Είμαι η Αθήνα! :)"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(athens));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 1000, null);
        mMap.setTrafficEnabled(true);

    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
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
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 177) {
            if (grantResults[0] == grantResults[1] && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        } else {
            Toast.makeText(getApplicationContext(), "You have to allow this permission in order to use the application.", Toast.LENGTH_LONG).show();
            finish();
            System.exit(0);
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        177
                );

            }

        } else {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
