package pt.ulisboa.tecnico.cmov.ubibike.server;

abstract class UbiServerException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3515286922198907798L;

	UbiServerException(String message) {
		super(message);
	}
	
	UbiServerException(String message, Throwable cause) {
		super(message, cause);
	}
	
	abstract UserReplyType getUserReplyType();
}
