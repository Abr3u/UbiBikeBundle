package pt.ulisboa.tecnico.cmov.ubibike.server;

final class ExistantUserException extends UbiServerException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -762431495378343759L;

	ExistantUserException(String username) {
		super("User \"" + username + "\" already used.");
	}
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.ALREADY_EXISTS;
	}
}
