package pt.ulisboa.tecnico.cmov.ubibike;

enum IntentKey {
    USERNAME,
    PASSWORD,
    STATIONS,
    BEACON_ID,
    STATION_NAME,
    MOST_RECENT_TRAJECTORY,
    SOME_TRAJECTORIES,
    PAST_TRAJECTORIES,
    TRAJECTORY,
    CURRENT_POINTS,
    MAIN,
    SERVICE,
    TRADED_POINT_JSON,
    MSG_HISTORY,
    PEOPLE_NEAR,
    LOGIN_MESSAGE,
    BUTTON_NAME,
    ERROR_MESSAGE;

    public String toString() {
        return Integer.toString(this.ordinal());
    }
}
