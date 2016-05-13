package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Trajectory implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7063403486350764609L;
	private List<Location> _locations;
	
	Trajectory(List<Location> locations) {
		_locations = locations;
	}

	JSONObject toJSON()
			throws JSONException {
		return new JSONObject().put(
				UserJsonKey.LOCATIONS.toString(),
				new JSONArray(_locations
						.parallelStream()
						.map(location -> location.toJSON())
						.collect(Collectors.toList())));
	}
	
}
