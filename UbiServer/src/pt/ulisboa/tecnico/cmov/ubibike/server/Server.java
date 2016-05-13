package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Server {

	void start() throws IOException, ClassNotFoundException {
		// Initialise lists and maps
		final List<User> users = new ArrayList<>();
		final List<TradedPoint> tradedPoints = new ArrayList<>();
		final List<Trajectory> trajectories = new ArrayList<>();
		final List<Station> stations = new ArrayList<>();
		int stationID = 0;
		int bikeID = 0;
		int beaconID = 0;

		/*
		 * final FilesManager filesManager = new FilesManager()
		 * .readFromUserFile(users) .readFromTradedPointFile(tradedPoints)
		 * .readFromTrajectoryFile(trajectories) .readFromStationFile(stations)
		 * .readFromBookedBikeFile(bookedBikes);
		 */

		// TODO: erase code after developing UbiBike

		User user1 = new User("ricardo", "p");
		User user2 = new User("abreu", "p");
		user1.incrementPoints(10);
		user2.incrementPoints(10);
		users.add(user2);
		users.add(user1);

		List<Location> trajectoryLocations = new ArrayList<>();
		trajectoryLocations.add(new Location(new Date(), 10, 10));
		trajectoryLocations.add(new Location(new Date(), 11, 11));
		trajectoryLocations.add(new Location(new Date(), 12, 12));
		trajectoryLocations.add(new Location(new Date(), 13, 13));
		trajectoryLocations.add(new Location(new Date(), 14, 14));
		Trajectory trajectory = new Trajectory(trajectoryLocations);
		trajectories.add(trajectory);
		user2.addTrajectory(trajectory);
		
		Date d = new Date();
		d.setHours(d.getHours()+1);
		
		trajectoryLocations = new ArrayList<>();
		trajectoryLocations.add(new Location(d, 0, 0));
		trajectoryLocations.add(new Location(d, 0, 1));
		trajectoryLocations.add(new Location(d, 0, 2));
		trajectoryLocations.add(new Location(d, 0, 3));
		trajectoryLocations.add(new Location(d, 0, 4));
		trajectory = new Trajectory(trajectoryLocations);
		trajectories.add(trajectory);
		user2.addTrajectory(trajectory);
		
		// user1.addTrajectory(trajectory);

		TradedPoint point1 = new TradedPoint("abreu", "ricardo", 5);
		TradedPoint point2 = new TradedPoint("ricardo", "abreu", 1);
		tradedPoints.add(point1);
		tradedPoints.add(point2);

		Station station1 = new Station(stationID++, 50, 50);
		Station station2 = new Station(stationID++, 60, 60);
		stations.add(station1);
		stations.add(station2);

		Bike bike1 = new Bike(bikeID++, beaconID++);
		Bike bike2 = new Bike(bikeID++, beaconID++);
		Bike bike3 = new Bike(bikeID++, beaconID++);
		station1.addBike(bike1);
		station1.addBike(bike2);
		station2.addBike(bike3);

		/*
		 * filesManager .writeIntoUserFile(users)
		 * .writeIntoTradedPointFile(tradedPoints)
		 * .writeIntoTrajectoryFile(trajectories)
		 * .writeIntoStationFile(stations)
		 * .writeIntoBookedBikeFile(bookedBikes);
		 */

		System.out.println("Users -> " + users.size());
		System.out.println("TradedPoints -> " + tradedPoints.size());
		System.out.println("Trajectories -> " + trajectories.size());
		System.out.println("Stations -> " + stations.size());
		System.out.println("Bikes -> " + stations.parallelStream()
				.flatMap(station -> station.getBikes().parallelStream()).collect(Collectors.toList()).size());

		final int serverPort = 9999;
		final ServerSocket serverSocket = new ServerSocket(serverPort);
		System.out.println("Server listening on port " + serverPort);

		while (!Thread.currentThread().isInterrupted()) {
			new Thread(new ServerThread(serverSocket.accept(), users, tradedPoints, stations)).start();
		}

		System.out.println("Server thread interrupted");
		serverSocket.close();
		System.out.println("Server closed");
	}

	private class ServerThread implements Runnable {

		private Socket _socket;
		private List<User> _users;
		private List<TradedPoint> _tradedPoints;
		private List<Station> _stations;

		private ServerThread(Socket socket, List<User> users, List<TradedPoint> tradedPoints, List<Station> stations) {
			_socket = socket;
			_users = users;
			_tradedPoints = tradedPoints;
			_stations = stations;
		}

		@Override
		public void run() {
			byte[] message = null;
			JSONObject request, reply;
			try {
				message = receiveBytesSocket();
				assert (message != null);

				try {
					try {
						request = new JSONObject(new String(message));
						log("Request: " + new String(message));
						int requestTypeString = getIntFromJson(request, UserJsonKey.REQUEST_TYPE);
						UserRequestType userRequestType = UserRequestType.values()[requestTypeString];

						try {
							switch (userRequestType) {
							case LOGIN_USER:
								reply = loginUser(userRequestType, request);
								break;
							case SIGN_UP_USER:
								reply = signUpUser(userRequestType, request);
								break;
							case SEND_POINTS:
								reply = sendPoints(userRequestType, request);
								break;
							case CHECK_POINTS:
								reply = checkPoints(userRequestType, request);
								break;
							case GET_USER_MOST_RECENT_TRAJECTORY:
								reply = getUserMostRecentTrajectory(userRequestType, request);
								break;
							case GET_USER_PAST_TRAJECTORIES:
								reply = getUserPastTrajectories(userRequestType, request);
								break;
							case GET_USER_TRAJECTORIES:
								reply = getUserTrajectories(userRequestType, request);
								break;
							case BOOK_BIKE_STATION:
								reply = bookBikeStation(userRequestType, request);
								break;
							case PICK_UP_BIKE:
								reply = pickUpBike(userRequestType, request);
								break;
							case DROP_OFF_BIKE:
								reply = dropOffBike(userRequestType, request);
								break;
							case RECEIVE_NEW_TRAJECTORY:
								reply = receiveNewTrajectory(userRequestType, request);
								break;
							default:
								reply = getError(userRequestType, UserReplyType.INVALID_ENUM);
								break;
							}
						} catch (UbiServerException exception) {
							logError(exception.getMessage());
							reply = getError(userRequestType, exception.getUserReplyType());
						}
					} catch (InvalidRequestException exception) {
						logError(exception.getMessage());
						reply = getError(UserRequestType.INVALID_ENUM, exception.getUserReplyType());
					}

					// Send reply
					sendBytesSocket(reply.toString().getBytes());
					log("Reply: " + reply.toString());
				} catch (JSONException | ClassNotFoundException | InvalidKeyException | NoSuchPaddingException
						| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
						| NoSuchAlgorithmException | ParseException | InvalidKeySpecException exception) {

					// Send failure reply
					logError(exception.getMessage());
					sendBytesSocket(UserReplyType.TECHNICAL_FAILURE.toString().getBytes());
				}
			} catch (IOException exception) {
				logError(exception.getMessage());
			}
		}

		private JSONObject checkPoints(UserRequestType userRequestType, JSONObject request)
				throws InvalidRequestException, MissingUserException, JSONException, PointsMissmatchException {
			checkJsonSize(request, UserJsonKey.USER_CURRENT_SCORE, UserJsonKey.USERNAME);

			int userPoints = getIntFromJson(request, UserJsonKey.USER_CURRENT_SCORE);
			String username = getStringFromJson(request, UserJsonKey.USERNAME);

			if (getUserByUsername(username).getScore() == userPoints) {
				return getSuccess(userRequestType);
			}
			throw new PointsMissmatchException();
		}

		private void log(String message) {
			System.out.println("[" + LocalDateTime.now().toString() + "] " + message);
		}

		private void logError(String message) {
			System.err.println("[" + LocalDateTime.now().toString() + "] " + message);
		}

		private JSONObject getError(UserRequestType userRequestType, UserReplyType userReplyType) throws JSONException {
			JSONObject reply = new JSONObject();
			reply.put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal());
			reply.put(UserJsonKey.REPLY_TYPE.toString(), userReplyType.ordinal());

			return reply;
		}

		private void checkJsonSize(JSONObject request, UserJsonKey... keys) throws InvalidRequestException {
			if (request.keySet().size() != keys.length + 1) {
				throw new InvalidRequestException();
			}
		}

		private void checkJsonKeyExistence(JSONObject request, UserJsonKey key) throws InvalidRequestException {
			if (!request.has(key.toString())) {
				throw new InvalidRequestException();
			}
		}

		private String getStringFromJson(JSONObject request, UserJsonKey key)
				throws InvalidRequestException, JSONException {
			checkJsonKeyExistence(request, key);
			return request.getString(key.toString());
		}

		private byte[] getByteArrayFromJson(JSONObject request, UserJsonKey key)
				throws InvalidRequestException, JSONException {
			String jsonString = getStringFromJson(request, key);
			String correctString = jsonString.substring(0, jsonString.length() - 1);
			return Base64.getDecoder().decode(correctString);
		}

		protected JSONArray getArrayFromJson(JSONObject request, UserJsonKey key)
				throws InvalidRequestException, JSONException {
			checkJsonKeyExistence(request, key);
			return request.getJSONArray(key.toString());
		}

		private double getDoubleFromJson(JSONObject request, UserJsonKey key)
				throws InvalidRequestException, JSONException {
			checkJsonKeyExistence(request, key);
			return request.getDouble(key.toString());
		}

		private Date getDateFromJson(JSONObject request, UserJsonKey key)
				throws InvalidRequestException, JSONException, ParseException {
			return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(getStringFromJson(request, key));
		}

		private int getIntFromJson(JSONObject request, UserJsonKey key) throws InvalidRequestException, JSONException {
			checkJsonKeyExistence(request, key);
			return request.getInt(key.toString());
		}

		private JSONObject sendPoints(UserRequestType userRequestType, JSONObject request)
				throws MissingUserException, InvalidCiphertextException, InvalidRequestException, JSONException,
				IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException,
				NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
				BadPaddingException, InvalidKeySpecException {
			// Verify remaining JSON keys
			checkJsonSize(request, UserJsonKey.USERNAME, UserJsonKey.RECEIVER, UserJsonKey.TRADED_POINTS,
					UserJsonKey.DATE, UserJsonKey.SIGNATURE);

			// Get request arguments
			final String username = getStringFromJson(request, UserJsonKey.USERNAME),
					receiver = getStringFromJson(request, UserJsonKey.RECEIVER),
					points = getStringFromJson(request, UserJsonKey.TRADED_POINTS),
					date = getStringFromJson(request, UserJsonKey.DATE);

			// Verify if received hash matches generated hash
			if (!Arrays.equals(
					// Generated hash
					getHash((username + receiver + points + date).getBytes()),
					// Received hash
					checkData(getByteArrayFromJson(request, UserJsonKey.SIGNATURE), getUserByUsername(username).getSecretKey()))) {
				throw new InvalidCiphertextException();
			}

			TradedPoint tradedPoint = new TradedPoint(username, receiver, Integer.parseInt(points));
			_tradedPoints.add(tradedPoint);
			updateUsersPoints(tradedPoint);
			FilesManager filesManager = new FilesManager();
			filesManager.writeIntoTradedPointFile(_tradedPoints);
			filesManager.writeIntoUserFile(_users);
			

			// Log event
			log("[" + username + "]: has sent " + points + " points to user \"" + receiver + "\" in " + date + ".");

			// Set and return reply message
			return getSuccess(userRequestType).put(UserJsonKey.USER_CURRENT_SCORE.toString(),
					getUserByUsername(username).getScore());
		}


		private JSONObject loginUser(UserRequestType userRequestType, JSONObject request)
				throws InvalidRequestException, JSONException, InvalidPasswordException, MissingUserException {
			// Verify remaining JSON keys
			checkJsonSize(request, UserJsonKey.USERNAME, UserJsonKey.PASSWORD);

			// Get request arguments
			String username = getStringFromJson(request, UserJsonKey.USERNAME),
					password = getStringFromJson(request, UserJsonKey.PASSWORD);

			for (User user : _users) {
				if (user.checkUsername(username)) {
					if (user.getPassword().equals(password)) {
						// Log event
						log("[" + username + "]: has logged in using password \"" + password + "\".");

						// Set and return reply message
						return getUserCurrentScore(getSuccess(userRequestType), user);
					} else {
						throw new InvalidPasswordException(username, password);
					}
				}
			}
			throw new MissingUserException(username);
		}

		private JSONObject getSuccess(UserRequestType userRequestType) throws JSONException {
			return new JSONObject().put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal())
					.put(UserJsonKey.REPLY_TYPE.toString(), UserReplyType.SUCCESS.ordinal());
		}

		private JSONObject signUpUser(UserRequestType userRequestType, JSONObject request)
				throws InvalidRequestException, JSONException, ExistantUserException, ClassNotFoundException,
				IOException, MissingUserException {
			// Verify remaining JSON keys
			checkJsonSize(request, UserJsonKey.USERNAME, UserJsonKey.PASSWORD);

			// Get request arguments
			String username = getStringFromJson(request, UserJsonKey.USERNAME),
					password = getStringFromJson(request, UserJsonKey.PASSWORD);

			for (User user : _users) {
				if (user.checkUsername(username)) {
					if (user.checkPassword(password)) {
						throw new ExistantUserException(username);
					}
				}
			}
			User newUser = new User(username, password);
			newUser.incrementPoints(20);
			_users.add(newUser);
			new FilesManager().writeIntoUserFile(_users);
			

			// Log event
			log("[" + username + "]: has signed up using password \"" + password + "\".");

			// Set and return reply message
			return getUserCurrentScore(getSuccess(userRequestType), newUser)
					.put(UserJsonKey.SECRET_KEY.toString(), newUser.getSecretString64());
		}

		private JSONObject getUserCurrentScore(JSONObject reply, User user)
				throws InvalidRequestException, JSONException, MissingUserException {		
			// User information (in JSON)
			reply.put(UserJsonKey.USER.toString(),
					user.toJSON(UserInformationType.CURRENT_SCORE))
			.put(UserJsonKey.SECRET_KEY.toString(), user.getSecretString64())
			.put(UserJsonKey.STATIONS.toString(), new JSONArray(_stations.parallelStream()
					.map(station -> station.toJSON()).collect(Collectors.toList())));
			
			if(user.hasBookedBike()) {
				reply.put(UserJsonKey.BIKE.toString(), user.getCurrentBike().toJSON());
			}

			if (user.hasTrajectory()) {
				reply.put(UserJsonKey.TRAJECTORY.toString(), user.toJSON(UserInformationType.MOST_RECENT_TRAJECTORY));
			}
			
			return reply;
		}

		private JSONObject getUserMostRecentTrajectory(UserRequestType userRequestType, JSONObject request)
				throws InvalidRequestException, JSONException, MissingUserException {
			return getUserInformation(userRequestType, request, UserInformationType.MOST_RECENT_TRAJECTORY);
		}

		private JSONObject getUserPastTrajectories(UserRequestType userRequestType, JSONObject request)
				throws InvalidRequestException, JSONException, MissingUserException {
			return getUserInformation(userRequestType, request, UserInformationType.PAST_TRAJECTORIES);
		}

		private JSONObject getUserTrajectories(UserRequestType userRequestType, JSONObject request)
				throws InvalidRequestException, JSONException, MissingUserException {
			return getUserInformation(userRequestType, request, UserInformationType.TRAJECTORIES);
		}

		private JSONObject getUserInformation(UserRequestType userRequestType, JSONObject request,
				UserInformationType userInformationType)
				throws InvalidRequestException, JSONException, MissingUserException {
			// Verify remaining JSON keys
			checkJsonSize(request, UserJsonKey.USERNAME);

			String username = getStringFromJson(request, UserJsonKey.USERNAME);

			// Set reply message
			JSONObject reply = getSuccess(userRequestType)
					// User information (in JSON)
					.put(UserJsonKey.USER.toString(), getUserByUsername(username).toJSON(userInformationType));

			// Log event
			log("[" + username + "]: Has got user trajectory");

			// Set and return reply message
			return reply;
		}

		private JSONObject receiveNewTrajectory(UserRequestType userRequestType, JSONObject request)
				throws MissingUserException, NumberFormatException, InvalidRequestException, JSONException,
				ClassNotFoundException, IOException, ParseException, CannotFindBikeException {
			// Verify remaining JSON keys
			checkJsonSize(request, UserJsonKey.USERNAME, UserJsonKey.TRAJECTORY);

			// Get request arguments
			String username = getStringFromJson(request, UserJsonKey.USERNAME);

			// Get user information from database
			User user = getUserByUsername(username);

			// Add trajectory
			JSONArray locations = getArrayFromJson(request, UserJsonKey.TRAJECTORY);
			JSONObject location;
			List<Location> positions = new ArrayList<>();
			for (int index = 0; index < locations.length(); ++index) {
				location = locations.getJSONObject(index);

				positions.add(new Location(getDateFromJson(location, UserJsonKey.LOCATION_DATE),
						getDoubleFromJson(location, UserJsonKey.LOCATION_LATITUDE),
						getDoubleFromJson(location, UserJsonKey.LOCATION_LONGITUDE)));
			}
			user.addTrajectory(new Trajectory(positions));

			// Log event
			log("[" + username + "]: has sent trajectory.");

			// Set and return reply message
			return getSuccess(userRequestType);
		}

		private Station getStationByUser(User user) throws CannotFindStationException {
			String name = user.getCurrentBike().getLastStationName();

			return _stations.parallelStream().filter(station -> station.checkName(name)).findAny()
					.orElseThrow(() -> new CannotFindStationException(name));
		}

		private JSONObject pickUpBike(UserRequestType userRequestType, JSONObject request)
				throws MissingUserException, NumberFormatException, NotBookedBikeException, InvalidRequestException,
				JSONException, IOException, ClassNotFoundException, CannotFindStationException {
			// Verify remaining JSON keys
			checkJsonSize(request, UserJsonKey.USERNAME, UserJsonKey.STATION);

			// Get request arguments
			String username = getStringFromJson(request, UserJsonKey.USERNAME);

			// Verify user from database
			User user = getUserByUsername(username);

			// Submit pick up bike event
			Bike bike = getStationByUser(user).pickUpBike(user);

			// Log event
			log("[" + username + "]: has picked up bike " + bike.getBeaconID() + ".");

			// Set and return reply message
			return getSuccess(userRequestType);
		}
		
		private Station getStationByName(String stationName) throws CannotFindStationException {
			return _stations.parallelStream().filter(station -> station.checkName(stationName)).findAny()
					.orElseThrow(() -> new CannotFindStationException(stationName));
		}

		private JSONObject dropOffBike(UserRequestType userRequestType, JSONObject request)
				throws MissingUserException, NumberFormatException, NotBookedBikeException, InvalidRequestException,
				JSONException, IOException, ClassNotFoundException, CannotFindStationException, ParseException {
			// Verify remaining JSON keys
			checkJsonSize(request, UserJsonKey.USERNAME, UserJsonKey.STATION, UserJsonKey.TRAJECTORY);

			// Get request arguments
			String username = getStringFromJson(request, UserJsonKey.USERNAME);
			String stationName = getStringFromJson(request, UserJsonKey.STATION).split("=")[0];

			// Verify user from database
			User user = getUserByUsername(username);

			// Submit pick up bike event
			Bike bike = getStationByName(stationName).dropOffBike(user);

			// Add trajectory
			JSONArray locations = getArrayFromJson(request, UserJsonKey.TRAJECTORY);
			double pointsGained = calculatePointsGained(locations);
			user.incrementPoints((int) pointsGained);
			JSONObject location;
			List<Location> positions = new ArrayList<>();
			for (int index = 0; index < locations.length(); ++index) {
				location = locations.getJSONObject(index);

				positions.add(new Location(getDateFromJson(location, UserJsonKey.LOCATION_DATE),
						getDoubleFromJson(location, UserJsonKey.LOCATION_LATITUDE),
						getDoubleFromJson(location, UserJsonKey.LOCATION_LONGITUDE)));
			}
			user.addTrajectory(new Trajectory(positions));

			// Log event
			log("[" + username + "]: has dropped off bike " + bike.getBeaconID() + ".");

			// Set and return reply message
			return getSuccess(userRequestType);
		}

		private double calculatePointsGained(JSONArray locations) {
			if (locations != null && !locations.isNull(0) && (locations.length() != 1)) {
				try {
					Double lat1 = locations.getJSONObject(0).getDouble(UserJsonKey.LOCATION_LATITUDE.toString());
					Double longi1 = locations.getJSONObject(0).getDouble(UserJsonKey.LOCATION_LONGITUDE.toString());

					Double lat2 = locations.getJSONObject(locations.length() - 1)
							.getDouble(UserJsonKey.LOCATION_LATITUDE.toString());
					Double longi2 = locations.getJSONObject(locations.length() - 1)
							.getDouble(UserJsonKey.LOCATION_LONGITUDE.toString());

					double result = distanceBetween(lat1, longi1, lat2, longi2);
					return Math.floor(result / 1000);// 1 km = 1 point
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return 0;
		}

		private double distanceBetween(Double lat1, Double longi1, Double lat2, Double longi2) {
			float pk = (float) (180 / 3.14169);

			float a1 = (float) (lat1 / pk);
			float a2 = (float) (longi1 / pk);
			float b1 = (float) (lat2 / pk);
			float b2 = (float) (longi2 / pk);

			double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
			double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
			double t3 = Math.sin(a1) * Math.sin(b1);
			double tt = Math.acos(t1 + t2 + t3);

			return 6366000 * tt;
		}

		private JSONObject bookBikeStation(UserRequestType userRequestType, JSONObject request)
				throws MissingUserException, InvalidRequestException, JSONException, CannotFindStationException,
				CannotFindBikeException, ClassNotFoundException, IOException {
			// Verify remaining JSON keys
			checkJsonSize(request, UserJsonKey.USERNAME, UserJsonKey.STATION);

			// Get request arguments
			String username = getStringFromJson(request, UserJsonKey.USERNAME),
					stationName = getStringFromJson(request, UserJsonKey.STATION);

			// Verify user from database
			User user = getUserByUsername(username);

			if (user.hasBookedBike()) {
				// Get bike information
				Bike previousBookedBike = user.removeCurrentBike();
				String previousStationName = previousBookedBike.getLastStationName();

				// Unbook bike
				_stations.parallelStream().filter(station -> previousBookedBike.checkLastStationName(station.getName()))
						.findAny().orElseThrow(() -> new CannotFindStationException(previousStationName))
						.addBike(previousBookedBike);

				// Log event
				log("[" + username + "]: has unbooked bike " + previousBookedBike.getBeaconID() + " from station "
						+ previousStationName + ".");

				if (previousBookedBike.checkLastStationName(stationName)) {
					// Set and return reply message
					return getError(userRequestType, UserReplyType.ALREADY_EXISTS);
				}
			}

			// Book bike
			Bike bike = _stations.parallelStream().filter(station -> station.checkName(stationName)).findAny()
					.orElseThrow(() -> new CannotFindStationException(stationName)).bookBike(user);

			// Log event
			log("[" + username + "]: has booked bike from station " + stationName + ".");

			// Return reply
			return getSuccess(userRequestType).put(UserJsonKey.BIKE.toString(), bike.toJSON());
		}

		private User getUserByUsername(String username) throws MissingUserException {
			return _users.parallelStream().filter(user -> user.checkUsername(username)).findAny()
					.orElseThrow(() -> new MissingUserException(username));
		}

		private void updateUsersPoints(TradedPoint tradedPoint) throws ClassNotFoundException, IOException {
			int points = tradedPoint.getAmount();
			for (User user : _users) {
				if (user.checkUsername(tradedPoint.getSender())) {
					user.decrementPoints(points);
				}
				if (user.checkUsername(tradedPoint.getReceiver())) {
					user.incrementPoints(points);
				}
			}
		}

		private byte[] receiveBytesSocket() throws IOException {
			DataInputStream dIn;
			byte[] message = null;

			dIn = new DataInputStream(_socket.getInputStream());

			int length = dIn.readInt(); // read length of incoming message
			if (length > 0) {
				message = new byte[length];
				dIn.readFully(message, 0, message.length); // read message
			}

			return message;
		}

		private void sendBytesSocket(byte[] message) throws IOException {
			DataOutputStream dOut = new DataOutputStream(_socket.getOutputStream());
			dOut.writeInt(message.length); // write length of the message
			dOut.write(message);
		}

		private byte[] checkData(byte[] data, SecretKey secret)
				throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
				InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, secret);
			return cipher.doFinal(data);
		}

		private byte[] getHash(byte[] data) throws NoSuchAlgorithmException {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data);

			byte[] digest = md.digest();
			return digest;
		}
	}
}
