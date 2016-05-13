package pt.ulisboa.tecnico.cmov.ubibike.server;

final class InvalidCiphertextException extends UbiServerException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7344421164918063749L;
	private static final String message = "Invalid Ciphertext.";

	InvalidCiphertextException() {
		super(message);
	}

	InvalidCiphertextException(Throwable cause) {
		super(message, cause);
	}
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.TECHNICAL_FAILURE;
	}
}
