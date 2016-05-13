package pt.ulisboa.tecnico.cmov.ubibike;


import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public class UbiBikeService extends Service
        implements SimWifiP2pManager.PeerListListener,
        SimWifiP2pManager.GroupInfoListener,
        LocationListener {

    //Book Bike listener
    private BookBikeBroadcastReceiver mBookBikeReceiver;

    //wifiDirect listener
    private SimWifiP2pBroadcastReceiver mSimWifiReceiver;

    //network listener
    private BroadcastReceiver mNetworkBroadcastReceiver;

    //histories
    private ArrayList<JSONObject> trajectoryHistory;
    private ArrayList<JSONObject> tradedPointsHistory;
    private ArrayList<String> msgHistory;


    public void addNewTradedPoints(JSONObject json) {
        tradedPointsHistory.add(json);
    }

    public void addMsgToHistory(String value) {
        msgHistory.add(value);
    }

    public ArrayList<String> getMsgHistory() {
        return msgHistory;
    }

    //Wifi direct parameters
    private MainActivity mActivity;
    private SimWifiP2pSocketServer mSrvSocket = null;
    private PeopleNearNewMessageActivity nearMsgActivity;
    private PeopleNearSendPointsActivity nearPointsActivity;
    private IBinder mBinder = new WifiBinder();
    private SimWifiP2pManager mManager = null;
    private SimWifiP2pManager.Channel mChannel = null;
    private Messenger mService = null;
    protected boolean mBound = false;

    private String inRangeString = "";
    private String groupInfoString = "";
    public boolean msgBound = false;
    public boolean pointsBound = false;

    /*------------------------
    getter & setters for WifiDirect
     -------------------------*/

    public void setSrvSocket(SimWifiP2pSocketServer s) {
        mSrvSocket = s;
    }

    public SimWifiP2pSocketServer getServerSocket() {
        return mSrvSocket;
    }

    public MainActivity getMainActivity() {
        return mActivity;

    }

    public PeopleNearNewMessageActivity getNearMsgActivity() {
        return nearMsgActivity;
    }

    public PeopleNearSendPointsActivity getNearPointsActivity() {
        return nearPointsActivity;
    }

    public void setMainActivity(MainActivity a) {
        mActivity = a;
    }

    public void setNearMsgActivity(PeopleNearNewMessageActivity a) {
        msgBound = true;
        nearMsgActivity = a;
    }

    public void setNearPointsActivity(PeopleNearSendPointsActivity a) {
        pointsBound = true;
        nearPointsActivity = a;
    }

    public void setGroupInfoString(String s) {
        this.groupInfoString = s;
    }

    public String getInRangeString() {
        return inRangeString;
    }

    public void setInRangeString(String inRangeString) {
        this.inRangeString = inRangeString;
    }


    //Notification parameter
    protected NotificationManager mNotificationManager;

    //Stations parameters
    protected Map<String, LatLng> mStations;
    protected Map.Entry<String, LatLng> mStation;

    //Network parameter
    private Handler mNetworkHandler;

    //Trajectory parameters
    private String mUsername,
            mBeaconId;
    private JSONArray mLocations,
            mPastTrajectories;

    private JSONObject mMostRecentTrajectory;
    private JSONObject mRecentBackUP;
    private boolean hasMRecent;
    private boolean hasPastTrajectories;

    public void setUsername(String u) {
        mUsername = u;
    }

    public JSONArray getPastTrajectories() {
        return mPastTrajectories;
    }

    public JSONObject getMostRecentTrajectory() {
        return mMostRecentTrajectory;
    }

    public void setMostRecentTrajectory(JSONObject t) {
        mMostRecentTrajectory = t;
        hasMRecent = true;
    }

    public boolean hasMostRecentTrajectory() {
        return hasMRecent;
    }

    public void addToPastTrajectories(JSONObject t) {
        mPastTrajectories.put(t);
        hasPastTrajectories = true;
    }

    public void addToPastTrajectories(JSONArray t) {
        boolean found;
        try {
            JSONObject temp;
            for (int i = 0; i < t.length(); i++) {
                found = false;
                temp = t.getJSONObject(i);
                for (int index = 0; index < mPastTrajectories.length(); ++index) {
                    String date = temp.getString(UserJsonKey.LOCATION_DATE.toString());
                    if (mPastTrajectories.getJSONObject(index).getString(UserJsonKey.LOCATION_DATE.toString()).equals(date)) {
                        found = true;
                        break;
                    }
                }
                if(!found){
                    mPastTrajectories.put(temp);
                }
            }
        }catch(JSONException exception) {
            exitApp(exception.getMessage() + ".");
        }
    }

    public boolean hasPastTrajectories(){
        return hasMRecent;
    }


    /*-------------------------------------------------------------------
    ----------------------STATE transitions
    --------------------------------------------------------------------*/

    UbiBikeApplication mUbiBikeApplication;

    void onUnbookBike() {
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.NONE);
    }

    void onBookBike(String stationName, String beaconId) {

        mBeaconId = beaconId;
        mStation = new AbstractMap.SimpleImmutableEntry<>(stationName, mStations.get(stationName));
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.BOOK_BIKE_OUTSIDE);
    }

    void onBookBikeOutsideStation(String stationName, String beaconId) {
        onBookBike(stationName, beaconId);
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.BOOK_BIKE_OUTSIDE);
        printNotification("You are (outside) heading to " + mStation.getKey() + ".");
    }

    private void onBookBikeEnterStation() {
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.BOOK_BIKE_INSIDE);
        printNotification("You have entered station " + mStation.getKey() + ".");
    }

    private void onBookBikeExitStation() {
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.BOOK_BIKE_OUTSIDE);
        printNotification("You have exited station " + mStation.getKey() + ".");
    }


    private void onBookBikeGetBike() {
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.BOOK_BIKE_WITH_BIKE);
        printNotification("You have got bike.");
    }

    private void onBookBikeDropBike() {
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.BOOK_BIKE_OUTSIDE);
        printNotification("You have dropped bike.");
    }

    private void onPickUp() {
        try {
            //Creating request message
            UserRequestType userRequestType = UserRequestType.PICK_UP_BIKE;
            JSONObject request = new JSONObject()
                    .put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal())
                    .put(UserJsonKey.USERNAME.toString(), mUsername)
                    .put(UserJsonKey.STATION.toString(), mStation.getKey());

            //Start send thread
            new Thread(new ClientThread(
                    userRequestType,
                    mNetworkHandler,
                    NetworkMessageCode.PICK_UP_BIKE,
                    request)).start();

            mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.TRACK_OUTSIDE);
            mStation = null;
            printNotification("You have successfully picked up bike.");
        } catch (JSONException exception) {
            exitApp(exception.getMessage() + ".");
        }
    }


    private void onTrackDropBike() {
        try {
            //Creating request message
            UserRequestType userRequestType = UserRequestType.RECEIVE_NEW_TRAJECTORY;
            JSONArray locations = mLocations;

            mRecentBackUP = mMostRecentTrajectory;
            mMostRecentTrajectory = new JSONObject()
                    .put(UserJsonKey.LOCATIONS.toString(), mLocations);

            double pointsGained = calculatePointsGained();
            propagatePointsGained(pointsGained);
            mLocations = new JSONArray();
            JSONObject request = new JSONObject()
                    .put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal())
                    .put(UserJsonKey.USERNAME.toString(), mUsername)
                    .put(UserJsonKey.TRAJECTORY.toString(), locations);

            if(networkIsAvailable()) {
                //Start send thread
                new Thread(new ClientThread(
                        userRequestType,
                        mNetworkHandler,
                        NetworkMessageCode.SEND_TRAJECTORY,
                        request)).start();
            }else{
                trajectoryHistory.add(request);
            }
            mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.TRACK_NO_BIKE);
            printNotification("Waiting for bike...");
        } catch (JSONException exception) {
            exitApp(exception.getMessage() + ".");
        }
    }


    private void onTrackGetBike() {
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.TRACK_OUTSIDE);
        printNotification("You have got bike.");
    }

    private void onTrackEnterStation(Map.Entry<String, LatLng> station) {
        mStation = station;
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.TRACK_INSIDE);
        printNotification("You have entered station " + station.getKey() + ".");
    }

    private void onTrackExitStation() {
        String stationName = mStation.getKey();
        mStation = null;
        mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.TRACK_OUTSIDE);
        printNotification("You have exited station " + stationName + ".");
    }


    private void onDropOff() {
        try {
            //Creating request message
            UserRequestType userRequestType = UserRequestType.DROP_OFF_BIKE;
            JSONArray locations = mLocations;

            mRecentBackUP = mMostRecentTrajectory;
            mMostRecentTrajectory = new JSONObject()
                    .put(UserJsonKey.LOCATIONS.toString(), mLocations);

            double pointsGained = calculatePointsGained();
            propagatePointsGained(pointsGained);
            mLocations = new JSONArray();
            JSONObject request = new JSONObject()
                    .put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal())
                    .put(UserJsonKey.USERNAME.toString(), mUsername)
                    .put(UserJsonKey.STATION.toString(), mStation)
                    .put(UserJsonKey.TRAJECTORY.toString(), locations);

            if (networkIsAvailable()) {
                //Start send thread
                new Thread(new ClientThread(
                        userRequestType,
                        mNetworkHandler,
                        NetworkMessageCode.SEND_TRAJECTORY,
                        request)).start();
            } else {
                trajectoryHistory.add(request);
            }
            mStation = null;
            mUbiBikeApplication.setUbiBikeState(UbiBikeServiceState.NONE);
            onUnbookBike();
            printNotification("You have dropped off bike.");
        } catch (JSONException exception) {
            exitApp(exception.getMessage() + ".");
        }
    }

    private void propagatePointsGained(double points) {
        getMainActivity().updatePointsInc((int)points,false);

        if (msgBound) {
            getNearMsgActivity().updatePoints(""+points);
        }
        if (pointsBound) {
            getNearPointsActivity().updatePoints(""+points);
        }
    }

    private double calculatePointsGained() {
        if (mLocations != null && !mLocations.isNull(0) && (mLocations.length() != 1)) {
            try {
                Double lat1 = mLocations.getJSONObject(0).getDouble(UserJsonKey.LOCATION_LATITUDE.toString());
                Double longi1 = mLocations.getJSONObject(0).getDouble(UserJsonKey.LOCATION_LONGITUDE.toString());

                Double lat2 = mLocations.getJSONObject(mLocations.length() - 1).getDouble(UserJsonKey.LOCATION_LATITUDE.toString());
                Double longi2 = mLocations.getJSONObject(mLocations.length() - 1).getDouble(UserJsonKey.LOCATION_LONGITUDE.toString());


                double pointsGained = myDistanceBetween(lat1, longi1, lat2, longi2);
                return Math.floor(pointsGained/1000);//1 km = 1 point
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private double myDistanceBetween(Double lat1, Double longi1, Double lat2, Double longi2) {
        float pk = (float) (180/3.14169);

        float a1 = (float) (lat1 / pk);
        float a2 = (float) (longi1 / pk);
        float b1 = (float) (lat2 / pk);
        float b2 = (float) (longi2 / pk);

        double t1 = Math.cos(a1)*Math.cos(a2)*Math.cos(b1)*Math.cos(b2);
        double t2 = Math.cos(a1)*Math.sin(a2)*Math.cos(b1)*Math.sin(b2);
        double t3 = Math.sin(a1)*Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);

        return 6366000*tt;
    }

    private void printNotification(String message) {
        Toast.makeText(UbiBikeService.this, message, Toast.LENGTH_SHORT).show();
        mNotificationManager.notify(0, new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("UbiBike")
                .setContentText(message).build());
    }

    /*-------------------------------------------------------
     -------------------GPS & Network stuff
     --------------------------------------------------------*/

    private void finishSendTrajectory(Message message) {
        try {
            //Get JSON elements
            JSONObject reply = (JSONObject) message.obj;
            UserReplyType userReplyType = UserReplyType.values()[reply.getInt(UserJsonKey.REPLY_TYPE.toString())];

            switch (userReplyType) {
                case SUCCESS:
                    addToPastTrajectories(mRecentBackUP);
                    break;
                default:
                    mMostRecentTrajectory = mRecentBackUP;
                    mRecentBackUP = new JSONObject();
                    break;
            }
        } catch (JSONException exception) {
            exitApp(exception.getMessage() + ".");
            stopSelf();
        }
    }

    protected boolean networkIsAvailable() {
        NetworkInfo activeInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return (activeInfo != null && activeInfo.isConnected());
    }

    protected class NetworkThread implements Runnable {

        private Handler _mHandler;

        public NetworkThread(Handler mHandler) {
            super();
            _mHandler = mHandler;
        }

        @Override
        public void run() {
            try {
                //Repeat until app is connected
                while (!networkIsAvailable()) {
                    Thread.sleep(1000);
                }

                //Send reconnect signal
                onDataConnected();
            } catch (InterruptedException exception) {
                _mHandler.obtainMessage(NetworkMessageCode.EXIT.ordinal(), exception.getMessage() + ".").sendToTarget();
            }

        }
    }

    protected void onNetworkChange() {
        if (networkIsAvailable()) {
            onDataConnected();
        } else {
            onDataDisconnected();
        }
    }

    protected void onDataDisconnected() {
        //Start network thread
        new Thread(new NetworkThread(mNetworkHandler)).start();
    }

    protected void onDataConnected() {
        //send stuff to server if needed

        if (!trajectoryHistory.isEmpty()) {
            for (JSONObject request : trajectoryHistory) {
                new Thread(new ClientThread(
                        UserRequestType.DROP_OFF_BIKE,
                        mNetworkHandler,
                        NetworkMessageCode.DROP_OFF_BIKE,
                        request)).start();
                trajectoryHistory.remove(request);
            }
        }
        if (!tradedPointsHistory.isEmpty()) {
            for (JSONObject request : tradedPointsHistory) {
                new Thread(new ClientThread(
                        UserRequestType.SEND_POINTS,
                        mNetworkHandler,
                        NetworkMessageCode.SEND_POINTS,
                        request)).start();
                tradedPointsHistory.remove(request);
            }
        }

        //pointsToCheck?
        if (getMainActivity() != null && getMainActivity().getPointsToCheck()) {
            getMainActivity().startCheckPointsThread();
        }
    }

    protected final static int STATION_LIMITS = 20;

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            return;
        }

        UbiBikeServiceState ubiBikeState = mUbiBikeApplication.getUbiBikeState();
        float[] distance;
        LatLng position;
        switch (ubiBikeState) {
            case BOOK_BIKE_OUTSIDE:
                distance = new float[1];
                position = mStation.getValue();
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), position.latitude, position.longitude, distance);
                if (distance[0] <= STATION_LIMITS) {
                    onBookBikeEnterStation();
                }
                break;
            case BOOK_BIKE_INSIDE:
                distance = new float[1];
                position = mStation.getValue();
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), position.latitude, position.longitude, distance);
                if (distance[0] > STATION_LIMITS) {
                    onBookBikeExitStation();
                }
                break;
            case BOOK_BIKE_WITH_BIKE:
                distance = new float[1];
                position = mStation.getValue();
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), position.latitude, position.longitude, distance);
                if (distance[0] > STATION_LIMITS) {
                    onPickUp();
                }
                break;
            case TRACK_OUTSIDE:
                try {
                    double latitude = location.getLatitude(),
                            longitude = location.getLongitude();
                    mLocations.put(new JSONObject()
                            .put(UserJsonKey.LOCATION_DATE.toString(),
                                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                            .format(new GregorianCalendar().getTime()))
                            .put(UserJsonKey.LOCATION_LATITUDE.toString(), latitude)
                            .put(UserJsonKey.LOCATION_LONGITUDE.toString(), longitude));

                    distance = new float[1];
                    for (Map.Entry<String, LatLng> entry : mStations.entrySet()) {
                        position = entry.getValue();
                        Location.distanceBetween(latitude, longitude, position.latitude, position.longitude, distance);
                        if (distance[0] <= STATION_LIMITS) {
                            onTrackEnterStation(entry);
                            break;
                        }
                    }
                } catch (JSONException exception) {
                    exitApp(exception.getMessage() + ".");
                    stopSelf();
                }
                break;
            case TRACK_INSIDE:
                distance = new float[1];
                position = mStation.getValue();
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), position.latitude, position.longitude, distance);
                if (distance[0] > STATION_LIMITS) {
                    onTrackExitStation();
                }
                break;
        }
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

    /*---------------------------------------------------
     ---------------Wifi Direct stuff
     ----------------------------------------------------*/

    private ServiceConnection mConnection = new ServiceConnection() {
        // callbacks for service binding, passed to bindService()

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mManager = new SimWifiP2pManager(mService);
            mChannel = mManager.initialize(getApplication(), getMainLooper(), null);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mManager = null;
            mChannel = null;
            mBound = false;
        }
    };

    @Override
    public void onGroupInfoAvailable(SimWifiP2pDeviceList devices, SimWifiP2pInfo groupInfo) {
        // compile list of network members
        StringBuilder peersStr = new StringBuilder();
        setGroupInfoString("");

        for (String deviceName : groupInfo.getDevicesInNetwork()) {
            SimWifiP2pDevice device = devices.getByName(deviceName);
            String devstr = "" + deviceName + " (" +
                    ((device == null) ? "??" : device.getVirtIp()) + ")\n";
            peersStr.append(devstr);
        }

        if (peersStr.length() != 0) {
            setGroupInfoString(peersStr.toString());
        }

        if (msgBound) nearMsgActivity.inNetworkChanged(getInRangeString());
        if (pointsBound) nearPointsActivity.inNetworkChanged(getInRangeString());
    }

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList peers) {
        StringBuilder peersStr = new StringBuilder();
        setInRangeString("");

        // compile list of devices in range
        for (SimWifiP2pDevice device : peers.getDeviceList()) {
            String devstr = "" + device.deviceName + " (" + device.getVirtIp() + ")\n";
            peersStr.append(devstr);
        }
        if (peersStr.length() != 0) {
            setInRangeString(peersStr.toString());
        }
        handlePeerChanges(getInRangeString());
        if (msgBound) nearMsgActivity.inRangeChanged(getInRangeString());
        if (pointsBound) nearPointsActivity.inRangeChanged(getInRangeString());
    }

    private void handlePeerChanges(String inRangeString) {
        mBeaconId = "B0000";
        boolean myBeaconInRange = checkMyBeaconInRange(inRangeString);

        UbiBikeServiceState ubiBikeState = mUbiBikeApplication.getUbiBikeState();
        switch (ubiBikeState) {
            case BOOK_BIKE_INSIDE:
                if (myBeaconInRange) {
                    onBookBikeGetBike();
                }
                break;
            case BOOK_BIKE_WITH_BIKE:
                if (!myBeaconInRange) {
                    onBookBikeDropBike();
                }
                break;
            case TRACK_OUTSIDE:
                if (!myBeaconInRange) {
                    onTrackDropBike();
                }
                break;
            case TRACK_NO_BIKE:
                if (myBeaconInRange) {
                    onTrackGetBike();
                }
                break;
            case TRACK_INSIDE:
                if (!myBeaconInRange) {
                    onDropOff();
                }
                break;
        }
    }

    private boolean checkMyBeaconInRange(String inRangeString) {
        String[] peers = inRangeString.split("\\r?\\n");

        for (String s : peers) {
            String[] aux = s.split(" \\(");//name(98765)
            String peerName = aux[0];
            if (peerName.equals(mBeaconId)) {
                return true;
            }
        }
        return false;
    }

    public void startIncomingTask() {
        new IncommingCommTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class IncommingCommTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SimWifiP2pSocket sock = getServerSocket().accept();
                    try {
                        BufferedReader sockIn = new BufferedReader(
                                new InputStreamReader(sock.getInputStream()));
                        String st = sockIn.readLine();
                        if (st.startsWith("#")) {
                            sock.getOutputStream().write((mUsername + "\n").getBytes());
                        } else {
                            sock.getOutputStream().write("\n".getBytes());
                            publishProgress(st);
                        }
                    } catch (IOException e) {
                        Log.d("Error reading socket:", e.getMessage());
                    } finally {
                        sock.close();
                    }
                } catch (IOException e) {
                    Log.d("Error socket:", e.getMessage());
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            addMsgToHistory(values[0]);

            if (values[0].startsWith("Received")) {
                //received 9 points from abreu¯
                String received = values[0].substring(9, 10);

                getMainActivity().updatePointsInc(Integer.parseInt(received), true);
                getMainActivity().showAlertPointsReceived();

                if (msgBound) {
                    getNearMsgActivity().updatePoints(values[0]);
                }
                if (pointsBound) {
                    getNearPointsActivity().updatePoints(values[0]);
                }
            } else {
                String lastMsg = getMsgHistory().get(getMsgHistory().size() - 1);
                getMainActivity().showAlertMsgReceived(lastMsg);
                if (msgBound) {
                    getNearMsgActivity().updateHistoryList(getMsgHistory());
                }
                if (pointsBound) {
                    getNearPointsActivity().updateHistoryList(getMsgHistory());
                }
            }
        }
    }

    public void WDinRange() {
        if (mBound) {
            mManager.requestPeers(mChannel, UbiBikeService.this);
        }
    }

    public void WDinNetwork() {
        if (mBound) {
            mManager.requestGroupInfo(mChannel, UbiBikeService.this);
        }
    }

    /*--------------------------------------------
    register & unregister services and broadcasts
     ---------------------------------------------*/

    public void stopWifiDirectService() {
        unregisterReceiver(mSimWifiReceiver);
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void startWifiDirectService() {
        // Initialize the WDSim API
        SimWifiP2pSocketManager.Init(getApplicationContext());

        // Register sim wifi p2p broadcast receiver
        IntentFilter simWifiIntentFilter = new IntentFilter();
        simWifiIntentFilter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        simWifiIntentFilter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        simWifiIntentFilter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        simWifiIntentFilter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        mSimWifiReceiver = new SimWifiP2pBroadcastReceiver(this);
        registerReceiver(mSimWifiReceiver, simWifiIntentFilter);

        bindService(
                new Intent(UbiBikeService.this, SimWifiP2pService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);
        mBound = true;
    }

    public void registerBookBikeReceiver() {
        // Register book bike broadcast receiver
        IntentFilter bookBikeIntentFilter = new IntentFilter();
        bookBikeIntentFilter.addAction(BookBikeBroadcast.BIKE_BOOKED_ACTION);
        bookBikeIntentFilter.addAction(BookBikeBroadcast.BIKE_UNBOOKED_ACTION);
        mBookBikeReceiver = new BookBikeBroadcastReceiver(this);
        registerReceiver(mBookBikeReceiver, bookBikeIntentFilter);
    }

    public void unregisterBookBikeReceiver() {
        unregisterReceiver(mBookBikeReceiver);
    }

    private void registerNetworkReceiver() {
        IntentFilter networkIntentFilter = new IntentFilter();
        networkIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        networkIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        networkIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

        mNetworkBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onNetworkChange();
            }
        };

        registerReceiver(mNetworkBroadcastReceiver, networkIntentFilter);
    }

    private void unregisterNetworkReceiver() {
        unregisterReceiver(mNetworkBroadcastReceiver);
    }

    /*---------------------------------------------------
    ---------SERVICE METHODS
     ----------------------------------------------------*/

    public UbiBikeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        mUsername = mUbiBikeApplication.getUsername();
        mBeaconId = mUbiBikeApplication.getBeaconId();
        JSONArray stations = mUbiBikeApplication.getStations();
        String stationName = mUbiBikeApplication.getLastStationStation();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            mStations = new HashMap<>();

            JSONObject station;
            for (int index = 0; index < stations.length(); ++index) {
                station = stations.getJSONObject(index);
                String currentStationName = getStringFromJson(station, UserJsonKey.STATION_NAME);
                LatLng location = new LatLng(
                        getDoubleFromJson(station, UserJsonKey.STATION_LATITUDE),
                        getDoubleFromJson(station, UserJsonKey.STATION_LONGITUDE));


                mStations.put(currentStationName, location);

                if (stationName != null && stationName.equals(currentStationName)) {
                    onBookBike(stationName, mBeaconId);
                    mStation = new AbstractMap.SimpleEntry<>(currentStationName, location);
                }
            }
            if (stationName != null && stationName.isEmpty() && !mBeaconId.isEmpty()) {
                onTrackGetBike();
            }
        } catch (JSONException exception) {
            exitApp(exception.getMessage() + ".");
        }



        return mBinder;
    }

    @Override
    public void onCreate() {
        mUbiBikeApplication = (UbiBikeApplication) getApplicationContext();
        mLocations = new JSONArray();
        mMostRecentTrajectory = new JSONObject();
        hasMRecent = false;
        hasPastTrajectories = false;
        mRecentBackUP = new JSONObject();
        mPastTrajectories = new JSONArray();
        trajectoryHistory = new ArrayList<>();
        tradedPointsHistory = new ArrayList<>();
        msgHistory = new ArrayList<>();
        mNetworkHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                NetworkMessageCode networkMessageCode = NetworkMessageCode.values()[message.what];
                switch (networkMessageCode) {
                    case SEND_TRAJECTORY:
                        finishSendTrajectory(message);
                        break;
                    default:
                        break;
                }
            }
        };

        // Setup Location manager and receiver
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, this);

        registerNetworkReceiver();
        registerBookBikeReceiver();
        startWifiDirectService();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopWifiDirectService();
        unregisterNetworkReceiver();
        unregisterBookBikeReceiver();
    }

    public class WifiBinder extends Binder implements Serializable {

        public UbiBikeService getService() {

            return UbiBikeService.this;
        }
    }

    /*-----------------------------------------------
    ------------- helpers
     -----------------------------------------------*/

    protected void exitApp(String message) {
        mNotificationManager.notify(0, new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("UbiBike")
                .setContentText(message + " Apologies for inconvenience.").build());
        sendBroadcast(new Intent(getResources().getString(R.string.trackingIntent))
                .putExtra(IntentKey.ERROR_MESSAGE.toString(), message));
        stopSelf();
    }

    private String getStringFromJson(JSONObject request, UserJsonKey key)
            throws JSONException {
        return request.getString(key.toString());
    }

    private double getDoubleFromJson(JSONObject request, UserJsonKey key)
            throws JSONException {
        return request.getDouble(key.toString());
    }


}