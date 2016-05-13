package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserJsonKey;
//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserReplyType;
//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserRequestType;

public final class BookBikeActivity extends GpsAndMapHandlerActivity {

    private LatLng mStationPosition;
    private Polyline mPolyline;
    private String mStationName, mUsername, mBeaconId;
    private UbiBikeApplication mUbiBikeApplication;

    private class BookBikeMarkerClickListener implements GoogleMap.OnMarkerClickListener {
        @Override
        public boolean onMarkerClick(final Marker marker) {
            LogEnter(BookBikeMarkerClickListener.class.getSimpleName(), "onMarkerClick");
            final String currentStationName = marker.getTitle();

            switch(mUbiBikeApplication.getUbiBikeState()) {
                case NONE:
                    mStationName = marker.getTitle();
                    // Setting Dialog Title, Dialog Message and onClick event and show Alert message

                    new AlertDialog.Builder(BookBikeActivity.this)
                            .setTitle("Book Bike")
                            .setMessage("Do you want to book bike from " + mStationName + "?")
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                mStationPosition = marker.getPosition();

                                                //Start book bike thread
                                                new Thread(new ClientThread(
                                                        UserRequestType.BOOK_BIKE_STATION,
                                                        mNetworkHandler,
                                                        NetworkMessageCode.BOOK_BIKE,
                                                        //Creating request message
                                                        new JSONObject()
                                                                .put(UserJsonKey.REQUEST_TYPE.toString(), UserRequestType.BOOK_BIKE_STATION.ordinal())
                                                                .put(UserJsonKey.USERNAME.toString(), mUsername)
                                                                .put(UserJsonKey.STATION.toString(), mStationName)
                                                )).start();

                                                if(mLastLocation != null) {
                                                    //Add poly lines into google map
                                                    mPolyline = mGoogleMap.addPolyline(
                                                            new PolylineOptions()
                                                                    .add(mStationPosition)
                                                                    .add(new LatLng(
                                                                            mLastLocation.getLatitude(),
                                                                            mLastLocation.getLongitude()))
                                                                    .width(5)
                                                                    .color(Color.BLUE));
                                                } else if(mPolyline != null) {
                                                    mPolyline.remove();
                                                    mPolyline = null;
                                                }

                                                Toast.makeText(BookBikeActivity.this, "Booked bike from " + mStationName, Toast.LENGTH_SHORT).show();
                                            } catch (JSONException exception) {
                                                exitApp(BookBikeActivity.this, exception.getMessage() + ".");
                                            }
                                        }
                                    })
                            .setNegativeButton("No",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            mStationName = null;
                                        }
                                    })
                            .show();
                    break;
                case BOOK_BIKE_OUTSIDE:
                case BOOK_BIKE_INSIDE:
                    if(currentStationName.equals(mStationName)) {

                        // Setting Dialog Title, Dialog Message and onClick event and show Alert message
                        new AlertDialog.Builder(BookBikeActivity.this)
                                .setTitle("Unbook previous Bike")
                                .setMessage("Do you want to unbook previous bike from " + mStationName + "?")
                                .setPositiveButton("Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                try {
                                                    mGoogleMap.setOnMarkerClickListener(new BookBikeMarkerClickListener());

                                                    //Start book bike thread (unbooks previous bike)
                                                    new Thread(new ClientThread(
                                                            UserRequestType.BOOK_BIKE_STATION,
                                                            mNetworkHandler,
                                                            NetworkMessageCode.BOOK_BIKE,
                                                            //Creating request message
                                                            new JSONObject()
                                                                    .put(UserJsonKey.REQUEST_TYPE.toString(), UserRequestType.BOOK_BIKE_STATION.ordinal())
                                                                    .put(UserJsonKey.USERNAME.toString(), mUsername)
                                                                    .put(UserJsonKey.STATION.toString(), mStationName)
                                                    )).start();

                                                    Toast.makeText(BookBikeActivity.this, "Unbooked bike from " + mStationName, Toast.LENGTH_SHORT).show();

                                                    mStationPosition = null;
                                                    //Remove previous polyline
                                                    if(mPolyline != null) {
                                                        mPolyline.remove();
                                                        mPolyline = null;
                                                    }
                                                    mStationName = null;

                                                } catch (Exception exception) {
                                                    exitApp(BookBikeActivity.this, exception.getMessage() + ".");
                                                }
                                            }
                                        })
                                .setNegativeButton("No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                .show();
                    } else {

                        // Setting Dialog Title, Dialog Message and onClick event and show Alert message
                        new AlertDialog.Builder(BookBikeActivity.this)
                                .setTitle("Book new Bike")
                                .setMessage("Do you want to book new bike from " + currentStationName + ", unbooking previous bike from " + mStationName + "?")
                                .setPositiveButton("Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                try {
                                                    mStationPosition = marker.getPosition();

                                                    //Start book bike thread (also unbooks previous bike)
                                                    new Thread(new ClientThread(
                                                            UserRequestType.BOOK_BIKE_STATION,
                                                            mNetworkHandler,
                                                            NetworkMessageCode.BOOK_BIKE,
                                                            //Creating request message
                                                            new JSONObject()
                                                                    .put(UserJsonKey.REQUEST_TYPE.toString(), UserRequestType.BOOK_BIKE_STATION.ordinal())
                                                                    .put(UserJsonKey.USERNAME.toString(), mUsername)
                                                                    .put(UserJsonKey.STATION.toString(), currentStationName)
                                                    )).start();

                                                    //Remove previous polyline
                                                    if(mPolyline != null) {
                                                        mPolyline.remove();
                                                        mPolyline = null;
                                                    }
                                                    if(mLastLocation != null) {
                                                        //Add poly lines into google map
                                                        mPolyline = mGoogleMap.addPolyline(
                                                                new PolylineOptions()
                                                                        .add(mStationPosition)
                                                                        .add(new LatLng(
                                                                                mLastLocation.getLatitude(),
                                                                                mLastLocation.getLongitude()))
                                                                        .width(5)
                                                                        .color(Color.BLUE));
                                                    }

                                                    Toast.makeText(BookBikeActivity.this, "Unbooked bike from " + mStationName, Toast.LENGTH_SHORT).show();
                                                    Toast.makeText(BookBikeActivity.this, "Booked bike from " + currentStationName, Toast.LENGTH_SHORT).show();

                                                    mStationName = currentStationName;

                                                } catch (JSONException exception) {
                                                    exitApp(BookBikeActivity.this, exception.getMessage() + ".");
                                                }
                                            }
                                        })
                                .setNegativeButton("No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                .show();
                    }
                    break;
                default:
                    // Setting Dialog Title, Dialog Message and onClick event and show Alert message
                    new AlertDialog.Builder(BookBikeActivity.this)
                            .setTitle("Invalid book")
                            .setMessage("You have already a bike!")
                            .setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                            .show();
                    break;
            }
            LogExit(BookBikeMarkerClickListener.class.getSimpleName(), "onMarkerClick");
            return true;
        }
    }

    private class DisabledBookBikeMarkerClickListener implements GoogleMap.OnMarkerClickListener {
        @Override
        public boolean onMarkerClick(final Marker marker) {
            LogEnter(DisabledBookBikeMarkerClickListener.class.getSimpleName(), "onMarkerClick");
            // Setting Dialog Title, Dialog Message and onClick event and show Alert message
            new AlertDialog.Builder(BookBikeActivity.this)
                    .setTitle("No network available")
                    .setMessage(R.string.onDataDisconnected)
                    .setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .show();
            LogExit(DisabledBookBikeMarkerClickListener.class.getSimpleName(), "onMarkerClick");
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogEnter(BookBikeActivity.class.getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);

        mStationPosition = null;
        mPolyline = null;
        mStationName = null;
        mUsername = null;
        mBeaconId = null;
        mUbiBikeApplication = (UbiBikeApplication) getApplicationContext();

        mNetworkHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                NetworkMessageCode networkMessageCode = NetworkMessageCode.values()[message.what];
                switch(networkMessageCode) {
                    case BOOK_BIKE:
                        finishBookBike(message);
                }
            }
        };
        LogExit(BookBikeActivity.class.getSimpleName(), "onCreate");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogEnter(BookBikeActivity.class.getSimpleName(), "onActivityResult");
        // Check which request we're responding to
        ActivityMessageCode activityMessageCode = ActivityMessageCode.values()[requestCode];
        switch(activityMessageCode) {
            case DETECT_STATION:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    // Setting Dialog Title, Dialog Message and onClick event and show Alert message
                    new AlertDialog.Builder(BookBikeActivity.this)
                            .setTitle("Unbook previous Bike")
                            .setMessage("Do you want to unbook previous bike from " + mStationName + "?")
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                mGoogleMap.setOnMarkerClickListener(new BookBikeMarkerClickListener());

                                                //Start book bike thread (unbooks previous bike)
                                                new Thread(new ClientThread(
                                                        UserRequestType.BOOK_BIKE_STATION,
                                                        mNetworkHandler,
                                                        NetworkMessageCode.BOOK_BIKE,
                                                        //Creating request message
                                                        new JSONObject()
                                                                .put(UserJsonKey.REQUEST_TYPE.toString(), UserRequestType.BOOK_BIKE_STATION.ordinal())
                                                                .put(UserJsonKey.USERNAME.toString(), mUsername)
                                                                .put(UserJsonKey.STATION.toString(), mStationName)
                                                )).start();

                                                Toast.makeText(BookBikeActivity.this, "Unbooked bike from " + mStationName, Toast.LENGTH_SHORT).show();

                                                mStationPosition = null;
                                                //Remove previous polyline
                                                if (mPolyline != null) {
                                                    mPolyline.remove();
                                                    mPolyline = null;
                                                }
                                                mStationName = null;
                                            } catch (Exception exception) {
                                                exitApp(BookBikeActivity.this, exception.getMessage() + ".");
                                            }
                                        }
                                    })
                            .setNegativeButton("No",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                            .show();
                }
                break;
            default:
                exitApp(BookBikeActivity.this, "Invalid activity message code " + activityMessageCode.toString());
                break;
        }
        LogExit(BookBikeActivity.class.getSimpleName(), "onActivityResult");
    }

    @Override
    protected void onResume() {
        LogEnter(BookBikeActivity.class.getSimpleName(), "onResume");
        super.onResume();

        //Change activity layout if offline;
        onDataConnected();
        LogExit(BookBikeActivity.class.getSimpleName(), "onResume");
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        LogEnter(BookBikeActivity.class.getSimpleName(), "onMapReady");
        super.onMapReady(googleMap);

        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(IntentKey.USERNAME.toString()) || !extras.containsKey(IntentKey.STATIONS.toString())) {
            exitApp(BookBikeActivity.this, "Intent has no stations.");
            return;
        }

        if (extras.containsKey(IntentKey.STATION_NAME.toString())) {
            mStationName = extras.getString(IntentKey.STATION_NAME.toString());
        }

        try {
            mUsername = extras.getString(IntentKey.USERNAME.toString());
            final JSONArray json = new JSONArray(
                    extras.getString(IntentKey.STATIONS.toString()));
            final LatLngBounds.Builder builder = new LatLngBounds.Builder();

            if(json.length() <= 0) {
                // Setting Dialog Title, Dialog Message and onClick event and show Alert message
                new AlertDialog.Builder(BookBikeActivity.this)
                        .setTitle("No bikes")
                        .setMessage("There are no stations with available bikes.")
                        .setCancelable(false)
                        .setPositiveButton("Return to main menu",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                        .show();
                return;
            }

            //Put station information from json array into list
            LatLng position;
            JSONObject station;
            String stationName;
            for (int index = 0; index < json.length(); ++index) {
                station = json.getJSONObject(index);
                stationName = getStringFromJson(station, UserJsonKey.STATION_NAME);

                position = new LatLng(
                        getDoubleFromJson(station, UserJsonKey.STATION_LATITUDE),
                        getDoubleFromJson(station, UserJsonKey.STATION_LONGITUDE));
                builder.include(mGoogleMap.addMarker(
                        new MarkerOptions()
                                .position(position)
                                .title(stationName)).getPosition());

                if(mStationName != null && mStationName.equals(stationName)) {
                    mStationPosition = position;
                }
            }

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

            mGoogleMap.setOnMarkerClickListener(new BookBikeMarkerClickListener());
        } catch (JSONException |
                 InvalidReplyException exception) {
            exitApp(BookBikeActivity.this, exception.getMessage() + ".");
        }
        LogExit(BookBikeActivity.class.getSimpleName(), "onMapReady");
    }

    private void finishBookBike(Message message) {
        LogEnter(BookBikeActivity.class.getSimpleName(), "finishBookBike");
        try {
            //Get JSON elements
            JSONObject reply = (JSONObject) message.obj;
            UserReplyType userReplyType = UserReplyType.values()[getIntFromJson(reply, UserJsonKey.REPLY_TYPE)];

            switch (userReplyType) {
                case SUCCESS:
                    mBeaconId = getStringFromJson(getObjectFromJson(reply, UserJsonKey.BIKE), UserJsonKey.BIKE_BEACON_ID);
                    mStationName = getStringFromJson(getObjectFromJson(reply, UserJsonKey.BIKE), UserJsonKey.BIKE_LAST_STATION_NAME);
                    sendBroadcast(new Intent(BookBikeBroadcast.BIKE_BOOKED_ACTION)
                            .putExtra(BookBikeBroadcast.EXTRA_BEACON_ID_STATE, mBeaconId)
                            .putExtra(BookBikeBroadcast.EXTRA_STATION_STATE, mStationName));
                    break;
                case ALREADY_EXISTS:
                    mBeaconId = null;
                    mStationName = null;
                    sendBroadcast(new Intent(BookBikeBroadcast.BIKE_UNBOOKED_ACTION));
                    break;
                case INVALID_USERNAME:
                    exitApp(BookBikeActivity.this, "Invalid Username.");
                    break;
                case TECHNICAL_FAILURE:
                    exitApp(BookBikeActivity.this, "Technical Failure.");
                    break;
                case INVALID_ENUM:
                    exitApp(BookBikeActivity.this, "Invalid request.");
                    break;
                case NOT_DROPPING_STATION:
                    exitApp(BookBikeActivity.this, "Invalid booking.");
                    break;
            }
        } catch (JSONException |
                 InvalidReplyException exception) {
            exitApp(BookBikeActivity.this, exception.getMessage() + ".");
        }
        LogExit(BookBikeActivity.class.getSimpleName(), "finishBookBike");
    }

    @Override
    protected void onLocationChange(Location location)
            throws SecurityException {
        LogEnter(BookBikeActivity.class.getSimpleName(), "onLocationChange");
        mLastLocation = location;

        //Remove previous polyline
        if(mPolyline != null) {
            mPolyline.remove();
            mPolyline = null;
        }

        if(mStationPosition == null) {
            return;
        }

        //Add poly line into google map
        mPolyline = mGoogleMap.addPolyline(
                new PolylineOptions()
                        .add(new LatLng(
                                mLastLocation.getLatitude(),
                                mLastLocation.getLongitude()))
                        .add(mStationPosition)
                        .width(5)
                        .color(Color.BLUE));
        LogExit(BookBikeActivity.class.getSimpleName(), "onLocationChange");
    }

    @Override
    protected void enableButtons(boolean option) {
        if(mGoogleMap == null) {
            return;
        }

        if(option) {
            mGoogleMap.setOnMarkerClickListener(new BookBikeMarkerClickListener());
        } else {
            mGoogleMap.setOnMarkerClickListener(new DisabledBookBikeMarkerClickListener());
        }
    }

    @Override
    protected void returnToParentActivity() {
        LogEnter(BookBikeActivity.class.getSimpleName(), "returnToParentActivity");
        //Disable buttons
        enableButtons(false);

        //Send results to login activity
        setResult(RESULT_OK,
                new Intent(BookBikeActivity.this, MainActivity.class)
                        .putExtra(IntentKey.BEACON_ID.toString(), mBeaconId)
                        .putExtra(IntentKey.STATION_NAME.toString(), mStationName));
        finish();
        LogExit(BookBikeActivity.class.getSimpleName(), "returnToParentActivity");
    }
}