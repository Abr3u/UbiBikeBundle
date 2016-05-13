package pt.ulisboa.tecnico.cmov.ubibike.server;

final class NotBookedBikeException extends UbiServerException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7109249740138160751L;

	NotBookedBikeException() {
		super("The bike was not booked.");
	}
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.NOT_BOOKED_BIKE;
	}
}
