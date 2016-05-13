package pt.ulisboa.tecnico.cmov.ubibike.server;

enum UserJsonKey {
	//Type keys
	REQUEST_TYPE,
	REPLY_TYPE,

	//Request keys
	USERNAME,
	PASSWORD,
	STATION,
	RECEIVER,
	TRAJECTORY,
	TRADED_POINTS,
	BEACON_ID,
	DATE,
	SIGNATURE,
	IV,
	
	//Reply keys
	USER,
	BIKE,
	STATIONS,
	LOCATIONS,
	SECRET_KEY,
	
	//User keys
	USER_CURRENT_SCORE,
	USER_MOST_RECENT_TRAJECTORY,
	USER_PAST_TRAJECTORIES,
	USER_TRAJECTORIES,
	
	//Bike keys
	BIKE_BEACON_ID,
	BIKE_LAST_STATION_NAME,
	
	//Location keys
	LOCATION_DATE,
	LOCATION_LATITUDE,
	LOCATION_LONGITUDE,
	
	//Station keys
	STATION_NAME,
	STATION_LATITUDE,
	STATION_LONGITUDE;

	public String toString() {
		return Integer.toString(this.ordinal());
	}
}
