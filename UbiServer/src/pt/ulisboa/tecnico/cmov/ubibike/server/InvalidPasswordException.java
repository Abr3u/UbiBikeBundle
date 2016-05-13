package pt.ulisboa.tecnico.cmov.ubibike.server;

final class InvalidPasswordException extends UbiServerException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7487730687022314764L;

	InvalidPasswordException(String username, String password) {
		super("User \"" + username + "\" does not use password " + password + ".");
	}
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.INVALID_PASSWORD;
	}
}