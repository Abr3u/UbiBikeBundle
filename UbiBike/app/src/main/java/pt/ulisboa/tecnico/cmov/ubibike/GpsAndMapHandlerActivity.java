package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import android.location.LocationListener;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

abstract class GpsAndMapHandlerActivity extends NetworkHandlerActivity
        implements OnMapReadyCallback,
        LocationListener {

    //Google Map API object
    protected GoogleMap mGoogleMap;

    protected Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLastLocation = null;

        // Setup Location manager and receiver
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, this);
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
        mGoogleMap = googleMap;

        //Clear map
        mGoogleMap.clear();
        try {
            mGoogleMap.setMyLocationEnabled(true);
        } catch (SecurityException exception) {
            exitApp(GpsAndMapHandlerActivity.this, exception.getMessage() + ".");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            mLastLocation = location;

            if (mLastLocation != null) {
                onLocationChange(mLastLocation);
            }
        } catch (SecurityException exception) {
            exitApp(GpsAndMapHandlerActivity.this, exception.getMessage() + ".");
        }
    }

    protected void onLocationChange(Location location) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}