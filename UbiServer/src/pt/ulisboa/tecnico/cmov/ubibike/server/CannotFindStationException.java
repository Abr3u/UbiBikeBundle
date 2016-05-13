package pt.ulisboa.tecnico.cmov.ubibike.server;

final class CannotFindStationException extends UbiServerException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9022468292260234298L;

	CannotFindStationException(String station) {
		super("Cannot find station \"" + station + "\".");
	}
	
	CannotFindStationException(double latitude, double longitude) {
		super("Cannot find station using coordinates " + latitude + "° " + longitude + "°.");
	}
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.NOT_DROPPING_STATION;
	}
}
