package com.example.nehal.googleplacesapidemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity
        implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, OnMapReadyCallback{
    private GoogleMap mMap;
    ArrayList<LatLng> markerPoints;
    private GoogleApiClient mGoogleApiClient;
    int PLACE_PICKER_REQUEST = 1;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 2;
    private Button button1, button2,getDirections;
    private int placeOneOrTwo;
    private Api api;
    Place place1,place2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        markerPoints = new ArrayList<LatLng>();
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        getDirections = (Button)findViewById(R.id.getDirection);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        getDirections.setOnClickListener(this);
        api = AppUtils.getRetrofitApi(BuildConfig.SERVER_URL, this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){
        markerPoints.clear();
        mMap.clear();
        markerPoints.add(origin);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
        MarkerOptions options = new MarkerOptions();
        options.position(dest);
        mMap.addMarker(options);
        //api.getDirections(origin.latitude+","+origin.longitude,dest.latitude+","+dest.longitude,"true");
        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        String str_dest = "destination="+dest.latitude+","+dest.longitude;
        String sensor = "sensor=false";
        String alternatives="alternatives=true";
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+alternatives;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
        return url;
    }

    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        }catch(Exception e){
            Log.d("Exception download url", e.toString());
        }finally{
            try {
                iStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String>{

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }


    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> > {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.endCap(new RoundCap());
                lineOptions.width(8);
                if(i == 0)
                    lineOptions.color(Color.BLACK);
                else if(i == 1)
                    lineOptions.color(Color.RED);
                else
                    lineOptions.color(Color.YELLOW);
                lineOptions.jointType(JointType.ROUND);
                mMap.addPolyline(lineOptions);

            }

            // Drawing polyline in the Google Map for the i-th route

        }
    }

    public void findPlace() {
        try {
//            Intent intent =
//                    new PlaceAutocomplete.IntentBuilder();
//                            .setFilter(typeFilter)
//                            .build(this);
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    // A place has been received; use requestCode to track the request.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
//                Place place = PlaceAutocomplete.getPlace(this, data);
//                Log.i("Place: ", place.getName().toString());
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                //Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                if(placeOneOrTwo == 1){
                    button1.setText(toastMsg);
                     place1 = place;
                }

                else{
                    button2.setText(toastMsg);
                     place2 = place;
                }

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("Status", status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED  || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    mMap.setMyLocationEnabled(true);
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                    if (location != null){
                        moveCameraToCurrentLocation(location);
                    }
                }

            }
        }
    }

    @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.button1:
                    placeOneOrTwo = 1;
                    findPlace();
                    break;
                case R.id.button2:
                    placeOneOrTwo = 2;
                    findPlace();
                    break;
                case R.id.getDirection:
                    if(place1 != null && place2 != null){
                        String url = getDirectionsUrl(place1.getLatLng(), place2.getLatLng());
                        //new
//                        markerPoints.clear();
//                        mMap.clear();
//                        markerPoints.add(place1.getLatLng());
//                        mMap.moveCamera(CameraUpdateFactory.newLatLng(place1.getLatLng()));
//                        MarkerOptions options = new MarkerOptions();
//                        options.position(place2.getLatLng());
//                        mMap.addMarker(options);
//                        Call<Object> call = api.getDirections(place1.getLatLng().latitude+","+place1.getLatLng().longitude,place2.getLatLng().latitude+","+place2.getLatLng().longitude,"false","true","AIzaSyDqZrd1vh4hTRLrLRNSoijnOozVnhRf4CA");
//                        call.enqueue(new Callback<Object>() {
//                            @Override
//                            public void onResponse(Call<Object> call, Response<Object> response) {
//                                ParserTask parserTask = new ParserTask();
//
//                                // Invokes the thread for parsing the JSON data
//                                parserTask.execute(response.body().toString());
//                            }
//
//                            @Override
//                            public void onFailure(Call<Object> call, Throwable t) {
//
//                            }
//                        });
                        //new

                        //old
                        DownloadTask downloadTask = new DownloadTask();
                        downloadTask.execute(url);
                        //old
                    }else{
                        Toast.makeText(this, "Enter 2 places", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        int locationPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);

            return;
        }else{
            mMap.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();

            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            if (location != null){
                moveCameraToCurrentLocation(location);
            }

        }
    }

    private void moveCameraToCurrentLocation(Location location) {

        {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .bearing(360)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }


}
