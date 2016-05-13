package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.IOException;

class UbiServer {
	public static void main(String[] args)
			throws ClassNotFoundException, IOException {
		new Server().start();
	}
}
