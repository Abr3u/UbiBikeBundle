package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class User implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8619665750420244431L;
	private int _score;
	private String _username, _password;
	private Bike _currentBike;
	private List<Trajectory> _trajectories;
	private SecretKey _SecretKey;

	public int getScore() {
		return _score;
	}

	User(String username, String password) {
		_score = 0;
		_username = username;
		_password = password;
		_currentBike = null;
		_trajectories = new ArrayList<>();
		generateSecret();
	}
	
	public SecretKey getSecretKey(){
		return _SecretKey;
	}
	
	public String getSecretString64(){
		return Base64.getEncoder().encodeToString(_SecretKey.getEncoded());
	}

	public void generateSecret() {
		KeyGenerator keyGenerator;
		try {
			keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256);
			_SecretKey = keyGenerator.generateKey();
			storeSecret(_SecretKey);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	public String readFromFile(String path) {
		BufferedReader br = null;
		String everything = null;
		try {
			br = new BufferedReader(new FileReader(path));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			everything = sb.toString();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return everything;
	}
	
	private SecretKey getSecretFromFile() {
		
		String KeyStr = readFromFile(_username+".txt");
		byte[] KeyBytes = toDecodedBase64ByteArray(KeyStr.getBytes());
		SecretKey Key = new SecretKeySpec(KeyBytes, "AES");
		return Key;
	}
	
	private static byte[] toDecodedBase64ByteArray(byte[] base64EncodedByteArray) {
		return DatatypeConverter.parseBase64Binary(new String(base64EncodedByteArray, Charset.forName("UTF-8")));
	}
	
	private void storeSecret(SecretKey key) {
		Writer writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(_username+".txt"), "utf-8");
			writer = new BufferedWriter(writer);
			writer.write(Base64.getEncoder().encodeToString(key.getEncoded()));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	boolean hasBookedBike() {
		return _currentBike != null;
	}

	boolean checkUsername(String username) {
		return _username.equals(username);
	}

	boolean checkPassword(String password) {
		return _password.equals(password);
	}

	String getPassword() {
		return _password;
	}

	User incrementPoints(Integer points) {
		_score += points;
		return this;
	}

	User decrementPoints(Integer points) {
		_score -= points;
		return this;
	}

	Bike checkAndRemoveCurrentBike(String beaconId) throws CannotFindBikeException {
		if (!_currentBike.getBeaconID().equals(beaconId)) {
			throw new CannotFindBikeException();
		}
		return _currentBike;
	}

	Bike removeCurrentBike() {
		Bike bike = _currentBike;
		_currentBike = null;
		return bike;
	}

	Bike getCurrentBike() {
		return _currentBike;
	}

	User setCurrentBike(Bike currentBike) {
		_currentBike = currentBike;
		return this;
	}

	User addTrajectory(Trajectory trajectory) {
		_trajectories.add(trajectory);
		try {
			new FilesManager().writeIntoTrajectoryFile(_trajectories);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return this;
	}

	JSONObject toJSON(UserInformationType userInformationType) throws JSONException, InvalidRequestException {
		switch (userInformationType) {
		case CURRENT_SCORE:
			return new JSONObject().put(UserJsonKey.USER_CURRENT_SCORE.toString(), _score);
		case MOST_RECENT_TRAJECTORY:
			return getMostRecentTrajectoryJson(new JSONObject());
		case PAST_TRAJECTORIES:
			return getPastTrajectoriesJson(new JSONObject());
		case TRAJECTORIES:
			JSONObject user = new JSONObject();

			getMostRecentTrajectoryJson(user);
			return getPastTrajectoriesJson(user);
		default:
			throw new InvalidRequestException();
		}
	}

	private JSONObject getPastTrajectoriesJson(JSONObject user) {
		if (_trajectories.size() < 2) {
			return user;
		}

		return user.put(UserJsonKey.USER_PAST_TRAJECTORIES.toString(),
				new JSONArray(_trajectories.subList(0, _trajectories.size() - 1).parallelStream()
						.map(trajectory -> trajectory.toJSON()).collect(Collectors.toList())));
	}

	private JSONObject getMostRecentTrajectoryJson(JSONObject user) {
		if (_trajectories.isEmpty()) {
			return user;
		}

		return user.put(UserJsonKey.USER_MOST_RECENT_TRAJECTORY.toString(), _trajectories.get(_trajectories.size() - 1).toJSON());
	}

	public boolean hasTrajectory() {
		if(_trajectories.isEmpty()){
			return false;
		}
		return true;
	}
}
