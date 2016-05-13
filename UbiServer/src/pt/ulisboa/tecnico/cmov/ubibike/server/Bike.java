package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

final class Bike implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1530111632267803079L;
	private int _id;
	private String _lastStationName,
	 			_beaconID;
	
	Bike(int id, int beaconID) {
		_id = id;
		_beaconID =  "B"+String.format("%04d", beaconID);//B0000-B9999
		//_beaconID = "B0000";
	}
	
	boolean checkLastStationName(String lastStationName) {
		return _lastStationName.equals(lastStationName);
	}
	
	int getId() {
		return _id;
	}
	
	String getBeaconID() {
		return _beaconID;
	}
	
	Integer getBeaconIdNumbers(){
		return Integer.parseInt(_beaconID.substring(1));
	}
	
	Bike setLastStationName(Station station) {
		_lastStationName = station.getName();
		return this;
	}
	
	Bike setLastStationName(String stationName) {
		_lastStationName = stationName;
		return this;
	}
	
	String getLastStationName() {
		return _lastStationName;
	}

	JSONObject toJSON()
			throws JSONException {
		return new JSONObject()
				.put(UserJsonKey.BIKE_BEACON_ID.toString(), _beaconID)
		        .put(UserJsonKey.BIKE_LAST_STATION_NAME.toString(), _lastStationName);

	}
}
