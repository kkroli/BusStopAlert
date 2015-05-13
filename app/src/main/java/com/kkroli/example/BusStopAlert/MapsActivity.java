package com.kkroli.example.BusStopAlert;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    public static final String TAG = MapsActivity.class.getSimpleName();

    private static final long LOCATION_ITERATION_PAUSE_TIME = 1000;
    private static final int NUMBER_OF_LOCATION_ITERATIONS = 10;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Boolean lastLocationSet = false;
    private List<Geofence> geofenceList = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;
    private PendingIntent geofencePendingIntent;
    private UpdateLocationRunnable updateLocationRunnable;
    private LocationManager locationManager;

    List<Step> previousMarkers = new ArrayList<>();
    private int markerCount = 0;
    private MarkerOptions destination;
    private String title;
    public Animation bottomUp;
    public Animation bottomDown;

    private EditText mapSearchBox;
    private Button mapGetDirections;
    private Location mLastLocation;
    private Address address;
    private List<Entry> routes = null;
    private RelativeLayout addressInfo;
    private TextView addressLine1;
    private TextView addressLine2;
    private TextView getRoutes;
    private ImageButton myLocationBtn;
    private ListView directionsList;
    private RelativeLayout transitDetails;
    private RelativeLayout listPanel;

    private int transitPosition = 0;
    private List<Step> stepsPicked = new ArrayList<>();
    private int stage = 0;
    private boolean searched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        myLocationBtn = (ImageButton) findViewById(R.id.myLocationView);
        myLocationBtn.setOnClickListener(this);

        bottomUp = AnimationUtils.loadAnimation(this, R.anim.bottom_up);
        bottomDown = AnimationUtils.loadAnimation(this, R.anim.bottom_down);

        directionsList = (ListView) findViewById(R.id.directions);
        addressLine1 = (TextView) findViewById(R.id.addressLine1);
        addressLine2 = (TextView) findViewById(R.id.addressLine2);
        getRoutes = (TextView) findViewById(R.id.getRoutes);

        listPanel = (RelativeLayout) findViewById(R.id.listPanel);

        transitDetails = (RelativeLayout) findViewById(R.id.transitDetails);
        transitDetails.setOnClickListener(new View.OnClickListener(){

            /*
             * This event occurs when the route is already picked, and the route details want to be
             * seen or not, by toggling the textview panel at the bottom.
             */
            @Override
            public void onClick(View v){

                if (searched && stage == 3) {
                    RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) transitDetails.getLayoutParams();

                    TextView time = (TextView) findViewById(R.id.timesingle);
                    time.setTextColor(Color.WHITE);

                    TextView duration = (TextView) findViewById(R.id.durationsingle);
                    duration.setTextColor(Color.WHITE);

                    TextView route = (TextView) findViewById(R.id.routesingle);
                    route.setTextColor(Color.WHITE);

                    transitDetails.startAnimation(bottomUp);
                    transitDetails.setVisibility(View.VISIBLE);
                    transitDetails.setBackgroundColor(Color.BLUE);

                    p.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                    transitDetails.setLayoutParams(p);

                    RelativeLayout myLocation = (RelativeLayout) findViewById(R.id.defaultlocation);
                    RelativeLayout.LayoutParams myLocationParams = (RelativeLayout.LayoutParams) myLocation.getLayoutParams();
                    myLocation.setVisibility(View.VISIBLE);
                    myLocation.setBackgroundColor(Color.WHITE);

                    myLocationParams.addRule(RelativeLayout.BELOW, R.id.transitDetails);
                    myLocation.setLayoutParams(myLocationParams);

                    TextView time1 = (TextView) findViewById(R.id.time1);
                    time1.setText(routes.get(transitPosition).getTimeLength().substring(0,6));
                    time1.setTextColor(Color.BLACK);

                    TextView location = (TextView) findViewById(R.id.location1);
                    location.setText("Your Location");
                    location.setTextColor(Color.BLACK);

                    directionsList.startAnimation(bottomUp);
                    directionsList.setBackgroundColor(Color.WHITE);
                    directionsList.setVisibility(View.VISIBLE);

                    stage = 2;
                } else if (searched && stage == 2){
                    RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) transitDetails.getLayoutParams();

                    TextView time = (TextView) findViewById(R.id.timesingle);
                    time.setTextColor(Color.BLACK);

                    TextView duration = (TextView) findViewById(R.id.durationsingle);
                    duration.setTextColor(Color.BLACK);

                    TextView route = (TextView) findViewById(R.id.routesingle);
                    route.setTextColor(Color.BLACK);

                    //transitDetails.startAnimation(bottomUp);
                    transitDetails.setBackgroundColor(Color.WHITE);

                    p.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                    p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    transitDetails.setLayoutParams(p);

                    /*directionsList.startAnimation(bottomDown);
                    directionsList.setBackgroundColor(Color.WHITE);
                    directionsList.setVisibility(View.VISIBLE);
                    */
                    stage = 3;
                }
            }
        });

        addressInfo = (RelativeLayout) findViewById(R.id.AddressInfo);
        addressInfo.setOnClickListener(new View.OnClickListener() {

            /*
             * This event occurs when an address has already been searched and a route to the
             * destination needs to be picked.
             */
            @Override
            public void onClick(View v){

                if (searched) {
                    RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) addressInfo.getLayoutParams();
                    RelativeLayout.LayoutParams dl = (RelativeLayout.LayoutParams) listPanel.getLayoutParams();

                    switch (stage) {
                        case 0: // AddressInfo panel was clicked when it was on bottom
                            addressLine1.setTextColor(Color.WHITE);
                            addressLine2.setTextColor(Color.WHITE);
                            getRoutes.setTextColor(Color.WHITE);
                            addressInfo.startAnimation(bottomUp);
                            addressInfo.setVisibility(View.VISIBLE);
                            addressInfo.setBackgroundColor(Color.BLUE);

                            p.addRule(RelativeLayout.BELOW, R.id.block);
                            p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                            addressInfo.setLayoutParams(p);

                            listPanel.setVisibility(View.VISIBLE);
                            dl.addRule(RelativeLayout.BELOW, R.id.AddressInfo);
                            listPanel.setLayoutParams(dl);

                            directionsList.startAnimation(bottomUp);
                            directionsList.setBackgroundColor(Color.WHITE);
                            directionsList.setVisibility(View.VISIBLE);

                            stage = 1;
                            break;

                        case 1: //AddressInfo panel was clicked while it was popped up to show routes
                            addressLine1.setTextColor(Color.BLACK);
                            addressLine2.setTextColor(Color.BLACK);
                            getRoutes.setTextColor(Color.BLACK);
                            addressInfo.setVisibility(View.VISIBLE);
                            addressInfo.setBackgroundColor(Color.WHITE);

                            p.addRule(RelativeLayout.BELOW, 0);
                            p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
                            addressInfo.setLayoutParams(p);

                            directionsList.setVisibility(View.INVISIBLE);

                            stage = 0;
                            break;

                        case 2: // a route was chosen to display
                            addressInfo.setVisibility(View.INVISIBLE);
                            p.addRule(RelativeLayout.BELOW, 0);
                            p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                            addressInfo.setLayoutParams(p);
                            directionsList.setVisibility(View.INVISIBLE);

                            listPanel.setVisibility(View.VISIBLE);
                            dl.addRule(RelativeLayout.BELOW, R.id.transitDetails);
                            listPanel.setLayoutParams(dl);

                            transitDetails.setVisibility(View.VISIBLE);
                            TextView time = (TextView) findViewById(R.id.timesingle);
                            time.setText(routes.get(transitPosition).getTimeLength());
                            time.setTextColor(Color.BLACK);
                            TextView duration = (TextView) findViewById(R.id.durationsingle);
                            duration.setText(routes.get(transitPosition).getDuration());
                            duration.setTextColor(Color.BLACK);
                            TextView route = (TextView) findViewById(R.id.routesingle);
                            List<String> lines = routes.get(transitPosition).getTransitLine();
                            String lineOut = "";
                            for (int i = 0; i < lines.size(); i++) {
                                lineOut = lineOut + lines.get(i) + " ";
                            }
                            route.setText(lineOut);
                            route.setTextColor(Color.BLACK);

                            stage = 3;
                            break;
                    }
                }
            }
        });

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(7000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mapSearchBox = (EditText) findViewById(R.id.et_location);
        mapGetDirections = (Button) findViewById(R.id.btn_find);

        mapGetDirections.setOnClickListener(new View.OnClickListener() {
            // Gets triggered when user clicks find button to find address
            @Override
            public void onClick(View view) {
                if (!mapSearchBox.getText().toString().equals("")) getDirections();
            }
        });

        // same as on top but instead of clicking button, the done button on keypad was pressed
        mapSearchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

                    // hide virtual keyboard
                    if (!mapSearchBox.getText().toString().equals("")) getDirections();
                    return true;
                }
                return false;
            }
        });

        setUpMapIfNeeded();

        // If there was an orientation change on the phone, the following reloads what was previously searched
        if (savedInstanceState != null){
            markerCount = savedInstanceState.getInt("markerCount");

            if (markerCount > 0){
                for (int i = 0; i < markerCount; i++){
                    LatLng firstNode = new LatLng(savedInstanceState.getDouble("start step " + i + " lat"), savedInstanceState.getDouble("start step " + i + " long"));
                    LatLng lastNode = new LatLng(savedInstanceState.getDouble("end step " + i + " lat"), savedInstanceState.getDouble("end step " + i + " long"));

                    mMap.addPolyline(new PolylineOptions()
                            .add(firstNode, lastNode)
                            .width(5)
                            .color(Color.RED));

                    previousMarkers.add(new Step(firstNode, lastNode, savedInstanceState.getInt("step " + i + " mode"), savedInstanceState.getString("step " + i + " direction"), savedInstanceState.getString("step " + i + " distance"),savedInstanceState.getString("step " + i + " duration"), savedInstanceState.getString("step " + i + " stopLocation"), savedInstanceState.getString("step " + i + " departure_time"), savedInstanceState.getString("step " + i + " arrival_time")));
                }

                for (int i = 0; i < markerCount; i++){
                    if (previousMarkers.get(i).getMode() == 2){
                        Geofence fence = new Geofence.Builder()
                                .setRequestId(previousMarkers.get(i).getStopLocation())
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                                .setCircularRegion(previousMarkers.get(i).getEndLocation().latitude, previousMarkers.get(i).getEndLocation().longitude, 200)
                                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                .build();
                        geofenceList.add(fence);

                        CircleOptions circleOpt = new CircleOptions()
                                .center(previousMarkers.get(i).getEndLocation())
                                .radius(200)
                                .fillColor(Color.RED)
                                .strokeColor(Color.TRANSPARENT)
                                .strokeWidth(2);
                        mMap.addCircle(circleOpt);
                    }
                }

                transitPosition = savedInstanceState.getInt("transitPosition");

                destination = new MarkerOptions()
                        .position(previousMarkers.get(markerCount-1).getEndLocation())
                        .title(savedInstanceState.getString("title"))
                        .visible(true);
                mMap.addMarker(destination);

                title = savedInstanceState.getString("address");
                addressLine1.setText(savedInstanceState.getString("AddressLine1"));
                addressLine1.setTextColor(Color.BLACK);
                addressLine2.setText(savedInstanceState.getString("AddressLine2"));
                addressLine2.setTextColor(Color.BLACK);
                addressInfo.startAnimation(bottomUp);
                addressInfo.setVisibility(View.VISIBLE);
                addressInfo.setBackgroundColor(Color.WHITE);
                transitDetails.setVisibility(View.INVISIBLE);
                stage = 0;
            }
        }
    }

    //We need to store all that is necessary in order to recreate the event before orientation was changed
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (markerCount > 0){
            searched = true;
            for (int i = 0; i < markerCount; i++){
                outState.putDouble("start step " + i + " lat", previousMarkers.get(i).getStartLocation().latitude);
                outState.putDouble("start step " + i + " long", previousMarkers.get(i).getStartLocation().longitude);
                outState.putDouble("end step " + i + " lat", previousMarkers.get(i).getEndLocation().latitude);
                outState.putDouble("end step " + i + " long", previousMarkers.get(i).getEndLocation().longitude);
                outState.putInt("step " + i + " mode", previousMarkers.get(i).getMode());
                outState.putString("step " + i + " direction", previousMarkers.get(i).getDirection());
                outState.putString("step " + i + " distance", previousMarkers.get(i).getDistance());
                outState.putString("step " + i + " duration", previousMarkers.get(i).getDuration());
                outState.putString("step " + i + " stopLocation", previousMarkers.get(i).getStopLocation());
                outState.putString("step " + i + " departure_time", previousMarkers.get(i).getDepartureTime());
                outState.putString("step " + i + " arrival_time", previousMarkers.get(i).getArrivalTime());
            }
            outState.putString("title", destination.getTitle());
        }
        outState.putString("address", title);
        outState.putInt("markerCount", markerCount);
        outState.putInt("transitPosition", transitPosition);

        if (address != null) {
            outState.putString("AddressLine1", address.getAddressLine(0));
            outState.putString("AddressLine2", address.getAddressLine(1));
        }
    }

    /*
     * This method takes what was in the search box and tries to find the address on the map. Out
     * of all the locations on map it gets back, it checks which of them is closer to user and uses
     * that one to then create a marker, zooms in to that part of the map and then opens up a connection
     * to get the directions to that location. Different alternatives will be presented in a listview manner.
     */
    private void getDirections(){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mapSearchBox.getWindowToken(), 0);

        // gets all possible locations from the submitted address
        Geocoder geocoder = new Geocoder(getApplicationContext());
        List<Address> results = null;
        try {
            results = geocoder.getFromLocationName(mapSearchBox.getText().toString(), 5);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (results == null){
                Toast.makeText(this, "Couldn't connect to network.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //Gets the closest location to the user's current location
        List<Location> locationResults = new ArrayList<>();
        for(int i = 0; i < results.size(); i++){
            Location toAdd = new Location("result " + i);
            toAdd.setLatitude(results.get(i).getLatitude());
            toAdd.setLongitude(results.get(i).getLongitude());
            locationResults.add(toAdd);
        }

        int pick = 0;
        int index = 0;
        float distanceToBeat = 0;
        if (lastLocationSet){
            do{
                float tmpDist = mLastLocation.distanceTo(locationResults.get(index));
                if (index == 0)
                    distanceToBeat = tmpDist;
                else if (distanceToBeat > tmpDist){
                    distanceToBeat = tmpDist;
                    pick = index;}
                index++;
            } while (index < locationResults.size());
        }

        searched = true;
        address = results.get(pick);

        final LatLng zoomInfo = new LatLng(address.getLatitude(), address.getLongitude());

        title = "";
        title = title + address.getAddressLine(0) + "\n" + address.getAddressLine(1);

        //Displays the address information on the bottom panel
        addressLine1.setText(address.getAddressLine(0));
        addressLine2.setText(address.getAddressLine(1));
        addressLine1.setTextColor(Color.BLACK);
        addressLine2.setTextColor(Color.BLACK);
        addressInfo.startAnimation(bottomUp);
        addressInfo.setVisibility(View.VISIBLE);
        addressInfo.setBackgroundColor(Color.WHITE);
        transitDetails.setVisibility(View.INVISIBLE);
        stage = 0;

        ((ViewGroup.MarginLayoutParams) myLocationBtn.getLayoutParams()).bottomMargin = 25 + addressInfo.getHeight();

        mMap.clear();
        if (geofencePendingIntent != null) LocationServices.GeofencingApi.removeGeofences(
                mGoogleApiClient,
                // This is the same pending intent that was used in addGeofences().
                geofencePendingIntent
        ).setResultCallback(this); // Result processed in onResult().

        destination = new MarkerOptions()
                .position(new LatLng(address.getLatitude(), address.getLongitude()))
                .title(title)
                .visible(true);

        mMap.addMarker(destination);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(zoomInfo, 15));

        mapSearchBox.setText("", TextView.BufferType.EDITABLE);

        // Connection is opened to the Directions API
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String url = "https://maps.googleapis.com/maps/api/directions/xml?"
                    + "origin=" + mLastLocation.getLatitude() + "," + mLastLocation.getLongitude()
                    + "&destination=" + address.getLatitude() + "," + address.getLongitude()
                    + "&sensor=false&alternatives=true&units=metric&mode=transit"
                    + "&key=AIzaSyB9n2RpnOIzBkaMmN-33FUj1jSKPv5p3-E";
            try {
                routes = new GMapV2Direction().execute(url).get();
                populateList();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        directionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            // The following is triggered when a route is picked among the list. Geofences are then created.
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (stage < 2) {
                    markerCount = routes.get(position).stepsSize();
                    previousMarkers = routes.get(position).getSteps();

                    mMap.clear();
                    mMap.addMarker(destination);

                    for (int i = 0; i < routes.get(position).stepsSize(); i++) {
                        mMap.addPolyline(new PolylineOptions()
                                .add(routes.get(position).getStartPoint(i), routes.get(position).getEndPoint(i))
                                .width(5)
                                .color(Color.RED));
                    }
                    for (int i = 0; i < routes.get(position).stepsSize(); i++) {
                        if (routes.get(position).getMode(i) == 2) {
                            Geofence fence = new Geofence.Builder()
                                    .setRequestId(routes.get(position).getStopLocation(i))
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                                    .setCircularRegion(routes.get(position).getEndPoint(i).latitude, routes.get(position).getEndPoint(i).longitude, 200)
                                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                    .build();
                            geofenceList.add(fence);

                            Color circleColor = new Color();

                            CircleOptions circleOpt = new CircleOptions()
                                    .center(routes.get(position).getEndPoint(i))
                                    .radius(200)
                                    .fillColor(circleColor.argb(75,255,0,0))
                                    .strokeColor(Color.TRANSPARENT)
                                    .strokeWidth(2);
                            mMap.addCircle(circleOpt);
                        }
                    }

                    monitorFences();

                    transitPosition = position;
                    stepsPicked = routes.get(position).getSteps();

                    stage = 2;
                    addressInfo.performClick();
                    populateList();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpMapIfNeeded();

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // The following is mostly for testing purposes.
        Log.i(TAG, "Setup MOCK Location Providers");
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        Log.i(TAG, "GPS Provider");
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, false, false, false, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        Log.i(TAG, "Network Provider");
        locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false, true, false, false, false, false, Criteria.POWER_MEDIUM, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
    }

    @Override
    protected void onPause() {
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Interrupt our runnable if we're going into the background or exiting
        if (updateLocationRunnable != null) {
            updateLocationRunnable.interrupt();
        }

        Log.i(TAG, "Cleanup Our Fields");
        locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
        locationManager = null;
        updateLocationRunnable = null;

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            if (geofencePendingIntent != null) LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    geofencePendingIntent
            ).setResultCallback(this); // Result processed in onResult().
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * Some pre built-in google map features are enabled here.
     */
    private void setUpMap() {
        mMap.setBuildingsEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        monitorFences();

        // This is for testing purposes, where a user can change its location by clicking anywhere on the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (updateLocationRunnable != null && updateLocationRunnable.isAlive() && !updateLocationRunnable.isInterrupted()) {
                    updateLocationRunnable.interrupt();
                }
                updateLocationRunnable = new UpdateLocationRunnable(locationManager, latLng);
                updateLocationRunnable.start();
            }
        });
    }

    /**
     * Connect our GoogleApiClient so we can begin monitoring our fences.
     */
    private void monitorFences() {

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (!geofenceList.isEmpty()) {
            geofencePendingIntent = getRequestPendingIntent();
            PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofenceList, geofencePendingIntent);
            result.setResultCallback(this);
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            lastLocationSet = true;
            if (destination == null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng (mLastLocation.getLatitude(), mLastLocation.getLongitude()), 12));
            else {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(destination.getPosition());
                builder.include(new LatLng (mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                LatLngBounds bounds = builder.build();
                int padding = 0; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                mMap.animateCamera(cu);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "GoogleApiClient Connection Suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "GoogleApiClient Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResult(Status status) {
        String toastMessage;
        if (status.isSuccess()) {
            toastMessage = "Success: We Are Monitoring Our Fences";
        } else {
            toastMessage = "Error: We Are NOT Monitoring Our Fences";
        }
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(final View v) {

        Location location = this.mMap.getMyLocation();

        if (location != null) {

            LatLng target = new LatLng(location.getLatitude(), location.getLongitude());

            CameraPosition.Builder builder = new CameraPosition.Builder();
            builder.zoom(15);
            builder.target(target);

            this.mMap.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));

        }
    }

    /**
     * Returns the current PendingIntent to the caller.
     *
     * @return The PendingIntent used to create the current set of geofences
     */
    public PendingIntent getRequestPendingIntent() {
        return createRequestPendingIntent();
    }

    /**
     * Get a PendingIntent to send with the request to add Geofences. Location
     * Services issues the Intent inside this PendingIntent whenever a geofence
     * transition occurs for the current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence
     * transitions.
     */
    private PendingIntent createRequestPendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        } else {
            Intent intent = new Intent(this, GeofenceTransitionReceiver.class);
            intent.setAction("geofence_transition_action");
            return PendingIntent.getBroadcast(this, R.id.geofence_transition_intent, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    // The proper adapter is picked to set up the list of routes or the list of the routes details
    private void populateList(){

        if (stage < 2) {
            ArrayAdapter<Entry> adapter = new DirectionsListAdapter();
            directionsList.setAdapter(adapter);
        } else {
            ArrayAdapter<Step> adapter = new TransitDetailsListAdapter();
            directionsList.setAdapter(adapter);
        }
    }

    // This is the adapter for the transit details
    private class TransitDetailsListAdapter extends ArrayAdapter<Step> {

        public TransitDetailsListAdapter(){
            super(MapsActivity.this, R.layout.transitdetailsview, stepsPicked);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent){
            if (view == null)
                view = getLayoutInflater().inflate(R.layout.transitdetailsview, parent,false);

            RelativeLayout panel = (RelativeLayout) view.findViewById(R.id.firstPanel);
            panel.setVisibility(View.VISIBLE);
            panel = (RelativeLayout) view.findViewById(R.id.secondPanel);
            panel.setVisibility(View.VISIBLE);
            panel.setBackgroundColor(Color.GRAY);

            TextView transitInfo = (TextView) view.findViewById(R.id.transitInfo);
            transitInfo.setText(stepsPicked.get(position).getDirection());
            transitInfo.setTextColor(Color.BLACK);

            TextView duration = (TextView) view.findViewById(R.id.duration);
            duration.setText(stepsPicked.get(position).getDistance() + " (" + stepsPicked.get(position).getDuration() + ")");
            duration.setTextColor(Color.BLACK);

            TextView time = (TextView) view.findViewById(R.id.time);
            time.setText(stepsPicked.get(position).getDepartureTime());
            time.setTextColor(Color.BLACK);

            TextView location = (TextView) view.findViewById(R.id.location);
            String stopLocation = stepsPicked.get(position).getStopLocation();
            if (stopLocation.equals("departure stop needed")) location.setText(title);
            else location.setText(stopLocation);
            location.setTextColor(Color.BLACK);

            return view;
        }
    }

    // This is the adapter for the list of routes
    private class DirectionsListAdapter extends ArrayAdapter<Entry> {

        public DirectionsListAdapter() {
            super(MapsActivity.this, R.layout.directionslistview_item, routes);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent){
            if(view == null)
                view = getLayoutInflater().inflate(R.layout.directionslistview_item, parent, false);

            if (stage < 2) {
                Entry currentRoute = routes.get(position);

                TextView time = (TextView) view.findViewById(R.id.time);
                time.setText(currentRoute.getTimeLength());
                time.setTextColor(Color.BLACK);
                TextView duration = (TextView) view.findViewById(R.id.duration);
                duration.setText(currentRoute.getDuration());
                duration.setTextColor(Color.BLACK);
                TextView route = (TextView) view.findViewById(R.id.route);
                List<String> lines = currentRoute.getTransitLine();
                String lineOut = "";
                for (int i = 0; i < lines.size(); i++) {
                    lineOut = lineOut + lines.get(i) + " ";
                }
                route.setText(lineOut);
                route.setTextColor(Color.BLACK);
            }
            return view;
        }
    }


    /*
     * The following code is for TESTING purposes. A thread is created to create a mock location
     * in order to see if the geofences are notifying the user properly when entered. The code was
     * also not entirely implemented by me, credits go to androidfu. For more info, please check
     * https://github.com/androidfu/GeofenceExample/blob/master/app/src/main/java/com/androidfu/example/geofences/MapsActivity.java
     */

    // /////////////////////////////////////////////////////////////////////////////////////////
    // // UpdateLocationRunnable                                                              //
    // /////////////////////////////////////////////////////////////////////////////////////////

    class UpdateLocationRunnable extends Thread {

        private final LocationManager locMgr;
        private final LatLng latlng;
        Location mockGpsLocation;
        Location mockNetworkLocation;

        UpdateLocationRunnable(LocationManager locMgr, LatLng latlng) {
            this.locMgr = locMgr;
            this.latlng = latlng;
        }

        /**
         * Starts executing the active part of the class' code. This method is
         * called when a thread is started that has been created with a class which
         * implements {@code Runnable}.
         */
        @Override
        public void run() {
            try {
                Log.i(TAG, String.format("Setting Mock Location to: %1$s, %2$s", latlng.latitude, latlng.longitude));
                /*
                    Location can be finicky.  Iterate over our desired location every second for
                    NUMBER_OF_LOCATION_ITERATIONS seconds to help it figure out where we want it to
                    be.
                 */
                for (int i = 0; !isInterrupted() && i <= NUMBER_OF_LOCATION_ITERATIONS; i++) {
                    mockGpsLocation = createMockLocation(LocationManager.GPS_PROVIDER, latlng.latitude, latlng.longitude);
                    locMgr.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockGpsLocation);
                    mockNetworkLocation = createMockLocation(LocationManager.NETWORK_PROVIDER, latlng.latitude, latlng.longitude);
                    locMgr.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, mockNetworkLocation);
                    Thread.sleep(LOCATION_ITERATION_PAUSE_TIME);
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "Interrupted.");
                // Do nothing.  We expect this to happen when location is successfully updated.
            } finally {
                Log.i(TAG, "Done moving location.");
            }
        }
    }


    // /////////////////////////////////////////////////////////////////////////////////////////
    // // CreateMockLocation                                                                  //
    // /////////////////////////////////////////////////////////////////////////////////////////

    private Location createMockLocation(String locationProvider, double latitude, double longitude) {
        Location location = new Location(locationProvider);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(1.0f);
        location.setTime(System.currentTimeMillis());
        /*
            setElapsedRealtimeNanos() was added in API 17
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        try {
            Method locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
            if (locationJellyBeanFixMethod != null) {
                locationJellyBeanFixMethod.invoke(location);
            }
        } catch (Exception e) {
            // There's no action to take here.  This is a fix for Jelly Bean and no reason to report a failure.
        }
        return location;
    }
}
