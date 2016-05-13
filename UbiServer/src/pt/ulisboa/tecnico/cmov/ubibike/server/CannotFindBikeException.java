package pt.ulisboa.tecnico.cmov.ubibike.server;

final class CannotFindBikeException extends UbiServerException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6916003054182053256L;

	CannotFindBikeException(String station) {
		super("Cannot find bike on station \"" + station + "\".");
	}
	
	CannotFindBikeException() {
		super("Cannot find bike.");
	}
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.NOT_DROPPING_STATION;
	}
}
