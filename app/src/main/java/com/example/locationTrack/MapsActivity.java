package com.example.locationTrack;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;


import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends Activity implements OnMapReadyCallback {

    private Button btn_start, btn_stop;
    private LinearLayout linearLayout;
    private TextView timeTV;
    private BroadcastReceiver broadcastReceiver;
    Calendar mCalendar;

    private GoogleMap mGoogleMap;
    private ArrayList<LatLng> points; //added

    @Override
    protected void onResume() {
        super.onResume();
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    double latitude = intent.getExtras().getDouble("Lat");
                    double longitude = intent.getExtras().getDouble("Lng");
                    LatLng latLng = new LatLng(latitude, longitude); //you already have this

                    points.add(latLng);
                    if (points!= null && points.size()==1) {
                        addMarker(new LatLng(latitude, longitude), "SRC");
                    }
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        btn_start = (Button) findViewById(R.id.button);
        btn_stop = (Button) findViewById(R.id.button2);
        linearLayout = (LinearLayout) findViewById(R.id.linerLyt);
        timeTV = (TextView) findViewById(R.id.shift_time_tv);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        points = new ArrayList<LatLng>(); //added

        if (!runtime_permissions())
            enable_buttons();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
    }


    private void enable_buttons() {

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCalendar = Calendar.getInstance();
                linearLayout.setVisibility(View.GONE);
                btn_start.setVisibility(View.GONE);
                btn_stop.setVisibility(View.VISIBLE);

                points.clear();

                Intent i = new Intent(getApplicationContext(), GPS_Service.class);
                startService(i);
                if (mGoogleMap != null)
                    mGoogleMap.clear();  //clears all Markers and Polylines
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar end = Calendar.getInstance();

                long diff = end.getTimeInMillis()-mCalendar.getTimeInMillis();
                long hour = TimeUnit.MILLISECONDS.toMinutes(diff);
                long hours = hour / 60;
                long minutes = hour % 60;

                linearLayout.setVisibility(View.VISIBLE);
                timeTV.setText(hours+"h "+minutes+"m");

                mCalendar.clear();
                btn_start.setVisibility(View.VISIBLE);
                btn_stop.setVisibility(View.GONE);

                Intent i = new Intent(getApplicationContext(), GPS_Service.class);
                stopService(i);
                redrawLine();
            }
        });

    }

    private void redrawLine() {

        PolylineOptions options = new PolylineOptions().width(4).color(Color.BLUE).geodesic(true);
        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }

        //TODO - uncomment below 3 lines if you want to test for static location
//        LatLng dest = new LatLng(12.90, 77.6319);
//        addMarker(dest, "DES");
//        options.add(dest);

        mGoogleMap.addPolyline(options); //add Polyline
    }

    private void addMarker(LatLng points, String title) {
        LatLng source = new LatLng(points.latitude, points.longitude);
        mGoogleMap.addMarker(new MarkerOptions().position(source).title(title));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(source));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(points).zoom(16).build();
        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private boolean runtime_permissions() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

            return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enable_buttons();
            } else {
                runtime_permissions();
            }
        }
    }

}
