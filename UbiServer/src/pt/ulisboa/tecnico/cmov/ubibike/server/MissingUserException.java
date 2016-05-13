package pt.ulisboa.tecnico.cmov.ubibike.server;

final class MissingUserException extends UbiServerException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8520516112051806599L;

	MissingUserException(String username) {
		super("User \"" + username + "\" doesn't exist.");
	}
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.INVALID_USERNAME;
	}
}
