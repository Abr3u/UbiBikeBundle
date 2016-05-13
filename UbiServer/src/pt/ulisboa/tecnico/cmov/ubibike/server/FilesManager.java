package pt.ulisboa.tecnico.cmov.ubibike.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class FilesManager {
	//Constants
	private static final String LOCATIONS_FILE_NAME = "locations.ser";
	private static final String USERS_FILE_NAME = "users.ser";
	private static final String TRADED_POINTS_FILE_NAME = "tradedPoints.ser";
	private static final String TRAJECTORIES_FILE_NAME = "trajectories.ser";
	private static final String STATIONS_FILE_NAME = "stations.ser";
	private static final String BOOKED_BIKES_FILE_NAME = "bookedbikes.ser";
	private ExecutorService _executor;
	
	FilesManager() {
		_executor = Executors.newSingleThreadExecutor();
	}
	
	FilesManager readFromLocationFile(List<Location> locations)
			throws ClassNotFoundException, IOException{
		return readFromSystem(LOCATIONS_FILE_NAME, locations);
	}

	FilesManager readFromUserFile(List<User> users)
			throws ClassNotFoundException, IOException{
		return readFromSystem(USERS_FILE_NAME, users);
	}

	FilesManager readFromTradedPointFile(List<TradedPoint> tradedPoints)
			throws ClassNotFoundException, IOException{
		return readFromSystem(TRADED_POINTS_FILE_NAME, tradedPoints);
	}

	FilesManager readFromTrajectoryFile(List<Trajectory> trajectories)
			throws ClassNotFoundException, IOException{
		return readFromSystem(TRAJECTORIES_FILE_NAME, trajectories);
	}

	FilesManager readFromStationFile(List<Station> stations)
			throws ClassNotFoundException, IOException{
		return readFromSystem(STATIONS_FILE_NAME, stations);
	}

	FilesManager readFromBookedBikeFile(Map<User, Bike> bookedBikes)
			throws ClassNotFoundException, IOException{
		return readFromSystem(BOOKED_BIKES_FILE_NAME, bookedBikes);
	}
	
	@SuppressWarnings("unchecked")
	private <T> FilesManager readFromSystem(String filename, T object)
			throws IOException, ClassNotFoundException {
		if (!new File(filename).exists()) {
			return this;
		}

		FileInputStream fis = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fis);
		object = (T) ois.readObject();
		ois.close();
		fis.close();
		
		return this;
	}
	
	FilesManager writeIntoLocationFile(List<Location> locations)
			throws ClassNotFoundException, IOException{
		return writeIntoSystem(LOCATIONS_FILE_NAME, locations);
	}

	FilesManager writeIntoUserFile(List<User> users)
			throws ClassNotFoundException, IOException{
		return writeIntoSystem(USERS_FILE_NAME, users);
	}

	FilesManager writeIntoTradedPointFile(List<TradedPoint> tradedPoints)
			throws ClassNotFoundException, IOException{
		return writeIntoSystem(TRADED_POINTS_FILE_NAME, tradedPoints);
	}

	FilesManager writeIntoTrajectoryFile(List<Trajectory> trajectories)
			throws ClassNotFoundException, IOException{
		return writeIntoSystem(TRAJECTORIES_FILE_NAME, trajectories);
	}

	FilesManager writeIntoStationFile(List<Station> stations)
			throws ClassNotFoundException, IOException{
		return writeIntoSystem(STATIONS_FILE_NAME, stations);
	}

	FilesManager writeIntoBookedBikeFile(Map<User,Bike> bookedBikes)
			throws ClassNotFoundException, IOException{
		return writeIntoSystem(BOOKED_BIKES_FILE_NAME, bookedBikes);
	}
	
	private <T> FilesManager writeIntoSystem(String filename, T object)
			throws IOException, ClassNotFoundException {
		_executor.execute(new WriteThread<T>(filename, object));
		
		return this;
	}
	
	private class WriteThread<T> implements Runnable {
		String _filename;
		T _object;
		
		WriteThread(String filename, T object) {
			_filename = filename;
			_object = object;
		}
		
		@Override
		public void run() {
			try {
				FileOutputStream fos = new FileOutputStream(_filename);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(_object);
				oos.close();
				fos.close();
			} catch (IOException exception) {
				
			}
		}
	}
}
