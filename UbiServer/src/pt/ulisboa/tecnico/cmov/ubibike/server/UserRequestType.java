package pt.ulisboa.tecnico.cmov.ubibike.server;

enum UserRequestType {
	LOGIN_USER,
	SIGN_UP_USER,
	SEND_POINTS,
	CHECK_POINTS,
	GET_USER_CURRENT_SCORE,
	GET_USER_MOST_RECENT_TRAJECTORY,
	GET_USER_PAST_TRAJECTORIES,
	GET_USER_TRAJECTORIES,
	GET_AVAILABLE_STATIONS,
	BOOK_BIKE_STATION,
	PICK_UP_BIKE,
	DROP_OFF_BIKE,
	RECEIVE_NEW_TRAJECTORY,
	INVALID_ENUM;
}
