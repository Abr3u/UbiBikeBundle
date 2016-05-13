package pt.ulisboa.tecnico.cmov.ubibike.server;

final class PointsMissmatchException extends UbiServerException {
	
	PointsMissmatchException() {
		super("Points at User didn't match points at Server");
	}

	private static final long serialVersionUID = 7487730687022314764L;
	
	@Override
	UserReplyType getUserReplyType() {
		return UserReplyType.POINTS_MISSMATCH;
	}
}