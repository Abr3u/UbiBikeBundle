package pt.ulisboa.tecnico.cmov.ubibike;

import android.graphics.Color;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserJsonKey;

public class TrajectoryInformationActivity extends GpsAndMapHandlerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);

        //Check intent parameters
        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(IntentKey.TRAJECTORY.toString())) {
            exitApp(TrajectoryInformationActivity.this, "Intent has no trajectories.");
            return;
        }

        try {
            //Get json object from checked intent
            final JSONObject trajectory = new JSONObject(extras.getString(IntentKey.TRAJECTORY.toString()));
            final ArrayList<LatLng> positions = new ArrayList<>();
            final LatLngBounds.Builder builder = new LatLngBounds.Builder();

            //Add position markers
            final JSONArray locations = getArrayFromJson(trajectory, UserJsonKey.LOCATIONS);
            LatLng position;
            JSONObject location;
            for(int index = 0; index < locations.length(); ++index) {
                location = locations.getJSONObject(index);

                position = new LatLng(
                        getDoubleFromJson(location, UserJsonKey.LOCATION_LATITUDE),
                        getDoubleFromJson(location, UserJsonKey.LOCATION_LONGITUDE));
                builder.include(mGoogleMap.addMarker(
                        new MarkerOptions()
                                .position(position)
                                .title(getStringFromJson(location, UserJsonKey.LOCATION_DATE))).getPosition());
                positions.add(position);
            }

            //Add poly lines into google map
            mGoogleMap.addPolyline(
                    new PolylineOptions()
                            .addAll(positions)
                            .width(5)
                            .color(Color.RED));

            //Move camera to start position
            mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    mGoogleMap.moveCamera(
                            CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
                    // Remove listener to prevent position reset on camera move.
                    mGoogleMap.setOnCameraChangeListener(null);
                }
            });
        } catch (JSONException |
                 InvalidReplyException exception) {
            exitApp(TrajectoryInformationActivity.this, exception.getMessage() + ".");
        }
    }
}