package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.Application;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by ist172904 on 5/10/16.
 */
public class UbiBikeApplication extends Application {
    //Service state parameter
    private UbiBikeServiceState mUbiBikeState = UbiBikeServiceState.NONE;
    private String  mUsername = "",
            mPassword = "",
            mLastStationStation = "",
            mBeaconId = "";

    private JSONArray mStations = null;
    private JSONObject mLastTrajectory = null;

    private int mCurrentScore = 0;

    public UbiBikeServiceState getUbiBikeState() {
        return mUbiBikeState;
    }
    public String getUsername() {
        return mUsername;
    }
    public String getPassword() {
        return mPassword;
    }
    public String getLastStationStation() {
        return mLastStationStation;
    }
    public String getBeaconId() {
        return mBeaconId;
    }
    public JSONArray getStations() {
        return mStations;
    }
    public JSONObject getLastTrajectory() {
        return mLastTrajectory;
    }
    public int getCurrentScore() {
        return mCurrentScore;
    }

    public void setUbiBikeState(UbiBikeServiceState ubiBikeState) {
        mUbiBikeState = ubiBikeState;
    }
    public void setUsername(String username) {
        mUsername = username;
    }
    public void setPassword(String password) {
        mPassword = password;
    }
    public void setLastStationStation(String lastStationStation) {
        mLastStationStation = lastStationStation;
    }
    public void setBeaconId(String beaconId) {
        mBeaconId = beaconId;
    }
    public void setStations(JSONArray stations) {
        mStations = stations;
    }
    public void setLastTrajectory(JSONObject lastTrajectory) {
        mLastTrajectory = lastTrajectory;
    }
    public void setCurrentScore(int currentScore) {
        mCurrentScore = currentScore;
    }
}
