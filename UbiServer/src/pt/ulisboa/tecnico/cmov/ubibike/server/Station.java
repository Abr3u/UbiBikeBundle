package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.Serializable;
import java.util.ArrayList;


import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;


public class Station implements Serializable {

	private static final long serialVersionUID = 2118875728912390512L;
	private double _latitude;
	private double _longitude;
	private int _id;
	private String _name;
	private List<Bike> _bikes;
	private List<Bike> _bikesReserved;

	Station(int i, double lati, double longi) {
		_id = i;
		_latitude = lati;
		_longitude = longi;
		_name = "Station" + i;
		_bikes = new ArrayList<Bike>();
		_bikesReserved = new ArrayList<Bike>();
	}
	
	boolean checkName(String name) {
		return _name.equals(name);
	}

	double getLatitude() {
		return _latitude;
	}

	double getLongitude() {
		return _longitude;
	}

	int getId() {
		return _id;
	}

	List<Bike> getBikes() {
		return _bikes;
	}
	
	List<Bike> getBikesReserved() {
		return _bikesReserved;
	}

	int getFreeBikes() {
		return _bikes.size();
	}
	
	String getName() {
		return _name;
	}

	Station addBike(Bike bike) {
		_bikes.add(bike.setLastStationName(this));
		return this;
	}

	public Bike bookBike(User user) {
		Bike bike = _bikes.remove(0);
		_bikesReserved.add(bike);
		user.setCurrentBike(bike);
		
		return bike;
	}
	
	public Bike pickUpBike(User user) {
		Bike bike = user.getCurrentBike();
		_bikesReserved.remove(bike);
		
		return bike.setLastStationName("");
	}
	
	public Bike dropOffBike(User user) {
		Bike bike = user.removeCurrentBike();
		addBike(bike);
		
		return bike;
	}

	JSONObject toJSON()
			throws JSONException {
		return new JSONObject()
				.put(UserJsonKey.STATION_LATITUDE.toString(), _latitude)
				.put(UserJsonKey.STATION_LONGITUDE.toString(), _longitude)
				.put(UserJsonKey.STATION_NAME.toString(), _name);
	}
}
