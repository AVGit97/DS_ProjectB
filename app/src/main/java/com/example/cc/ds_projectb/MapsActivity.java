package com.example.cc.ds_projectb;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private TextView finalResult;
    private EditText ipAddress, userID;

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

        Button btn = findViewById(R.id.button_connect);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean execute_runner = true;
                if (userID.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.editText_userID_empty, Toast.LENGTH_SHORT).show();
                    execute_runner = false;
                }

                if (ipAddress.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.editText_ip_empty, Toast.LENGTH_SHORT).show();
                    execute_runner = false;
                }

                if (execute_runner) {
                    AsyncTaskRunner runner = new AsyncTaskRunner();
                    runner.execute();
                }
            }
        });
    }

    // AsyncTask<doInBackground parameter type, onProgressUpdate parameter type, onPostExecute parameter type>
    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private String ip, result;
        private int id;
        ProgressDialog progressDialog;

        @Override
        protected String doInBackground(String... params) {
            publishProgress("Waiting for host..."); // Calls onProgressUpdate()
            // TODO : Sockets here
            sleep(5000);
            /*if (host unreachable)*/ result = "Couldn't connect to " + ip;
            /*else*/ result = "Successfully connected to " + ip + " as user " + id;
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
            ip = ipAddress.getText().toString();
            id = Integer.parseInt(userID.getText().toString());
            progressDialog = ProgressDialog.show(MapsActivity.this,
                    "ProgressDialog",
                    "Waiting for " + ip + " to respond...");
        }

        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

}
