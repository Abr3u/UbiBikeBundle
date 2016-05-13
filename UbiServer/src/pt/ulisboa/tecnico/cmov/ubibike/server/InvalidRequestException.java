package pt.ulisboa.tecnico.cmov.ubibike.server;

final class InvalidRequestException extends UbiServerException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9063632358045508630L;

	InvalidRequestException() {
		super("Invalid request.");
	}
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.INVALID_ENUM;
	}
}
