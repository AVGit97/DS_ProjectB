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

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import test.ds_project.poi.Poi;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private TextView finalResult;
    private EditText ipAddress, userID;

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        assert locationManager != null;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }

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
                } else if (!isValidIP(ipAddress.getText().toString())) {
                    Toast.makeText(getApplicationContext(), "The IP address you entered is not valid.", Toast.LENGTH_SHORT).show();
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
                Socket requestSocket = new Socket(ip, 4321);

                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                result = "Successfully connected to " + ip + " as user " + id + '.';
                connected = true;
            } catch (UnknownHostException unknownHost) {
                result = "Couldn't connect to " + ip;
            } catch (ConnectException connectionException) {
                result = "Connection timed out.";
            } catch (NoRouteToHostException routeException) {
                result = "Couldn't find route to host.";
            } catch (SocketException socketException) {
                result = "Network is unreachable.";
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
            Log.d("FINISHED_ASYNC_TASK", "connected: " + connected);

            if (connected) {
                AsyncTaskSocketSendReceiveInfo sendInfo = new AsyncTaskSocketSendReceiveInfo();
                sendInfo.execute(String.valueOf(id));
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
    private class AsyncTaskSocketSendReceiveInfo extends AsyncTask<String, String, String> {

        private ProgressDialog progressDialog;

        private ArrayList<Marker> markers = new ArrayList<>();
        private ArrayList<Poi> listFromServer;

        private int k;
        private double range;

        @Override
        protected String doInBackground(String... params) {

            String result;
            int id = Integer.parseInt(params[0]);

            try {
                publishProgress("Sending ID...");
                out.writeInt(id);
                out.flush();
                Log.d("ID_SENT", "ID successfully sent.");

                publishProgress("Sending latitude...");
                out.writeDouble(latitude);
                out.flush();
                Log.d("LATITUDE_SENT", "Latitude successfully sent.");

                publishProgress("Sending longitude...");
                out.writeDouble(longitude);
                out.flush();
                Log.d("LONGITUDE_SENT", "longitude successfully sent.");

                publishProgress("Successfully sent client info to host.");

                // wait for response
                publishProgress("Receiving k...");
                k = in.readInt();
                Log.d("K_RECEIVED", "k successfully received. (k = " + k + ')');

                publishProgress("Receiving range...");
                range = in.readDouble();
                Log.d("RANGE_RECEIVED", "range successfully received. (range = " + range + ')');

                publishProgress("Receiving POI list...");
                listFromServer = (ArrayList<Poi>) in.readObject();
                Log.d("POI_LIST_RECEIVED", "POI list successfully received.");

                if (listFromServer != null) {
                    result = "Here are the best " + listFromServer.size() + " local pois in a " + range + "km range:";
                } else {
                    result = "There is nothing interesting here for you!";
                }

            } catch (IOException e) {
                e.printStackTrace();
                result = "IOException occurred while sending info to host.";
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                result = "ClassNotFoundException occurred while receiving info from host.";
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

            ArrayList<Poi> bestLocalPois;
            if (listFromServer != null) {

                bestLocalPois = new ArrayList<>(listFromServer);
                MarkerOptions options = new MarkerOptions();
                LatLng currentPoint;

                for (Poi p:
                        bestLocalPois) {
                    Log.d("POI_INFO", "Poi " + p.getId() + ": at " + p.getLatitude() + ", " + p.getLongitude());

                    currentPoint = new LatLng(p.getLatitude(), p.getLongitude());
                    options.position(currentPoint);
                    options.title(p.getName() + " (#" + p.getId() + ')');
                    options.snippet("Category: " + p.getCategory());
                    markers.add(mMap.addMarker(options));
                }

//                Move camera in order to show all markers
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : markers) {
                    builder.include(marker.getPosition());
                }
                LatLngBounds bounds = builder.build();

                int padding = 100; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                mMap.animateCamera(cu);

            } else {
                Log.d("POI_LIST_EMPTY", "listFromServer is null.");
            }
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

        /*LatLng athens = new LatLng(37.9908997, 23.7033199);
        mMap.addMarker(new MarkerOptions().position(athens).title("Αθήνα"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(athens));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 1000, null);*/
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

    private static boolean isValidIP(String ip) {
        try {

            if (ip == null || ip.isEmpty()) {
                return false;
            }

            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }

            if (ip.endsWith(".")) {
                return false;
            }

            return true;

        } catch (NumberFormatException nfe) {

            return false;

        }

    }

}
