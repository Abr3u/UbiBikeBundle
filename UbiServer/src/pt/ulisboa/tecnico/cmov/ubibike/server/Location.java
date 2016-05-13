package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

class Location implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4959579837238175383L;
	private Date _date;
	private double _latitude;
	private double _longitude;
	
	Location(Date date, double latitude, double longitude) {
		_date = date;
		_latitude = latitude;
		_longitude = longitude;
	}

	Date getDate() {
		return _date;
	}
	
	double getLatitude() {
		return _latitude;
	}

	double getLongitude() {
		return _longitude;
	}

	JSONObject toJSON()
			throws JSONException {
		return new JSONObject()
				.put(UserJsonKey.LOCATION_DATE.toString(), new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(this.getDate()))
				.put(UserJsonKey.LOCATION_LATITUDE.toString(), this.getLatitude())
				.put(UserJsonKey.LOCATION_LONGITUDE.toString(), this.getLongitude());
	}
}
