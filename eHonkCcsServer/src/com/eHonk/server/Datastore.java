package com.eHonk.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/* Singleton DataStore should be promoted to Factory pattern */
public final class Datastore {

	public class DriverRecord {
		private final String gcmRegId;
		private final String licensePlate;
		private final int car_color;
		private final String phoneNumber;

		public DriverRecord() {
			this.gcmRegId = null;
			this.licensePlate = null;
			this.car_color = 0;
			this.phoneNumber = null;
		}

		public DriverRecord(String gcmRegId, String licensePlate) {
			this.gcmRegId = gcmRegId;
			this.licensePlate = licensePlate;
			this.car_color = 0;
			this.phoneNumber = null;
		}

		public DriverRecord(String gcmRegId, String licensePlate, int car_color) {
			this.gcmRegId = gcmRegId;
			this.licensePlate = licensePlate;
			this.car_color = car_color;
			this.phoneNumber = null;
		}

		public DriverRecord(String gcmRegId, String licensePlate,
				int car_color, String phoneNumber) {
			this.gcmRegId = gcmRegId;
			this.licensePlate = licensePlate;
			this.car_color = car_color;
			this.phoneNumber = phoneNumber;
		}

		public String getGcmRegId() {
			return gcmRegId;
		}

		public String getLicensePlate() {
			return licensePlate;
		}

		public int getCarColor() {
			return car_color;
		}

		public String getPhoneNumber() {
			return phoneNumber;
		}
	}

	private static final Logger logger = Logger.getLogger(Datastore.class
			.getName());

	/* Singleton pattern */
	private static Datastore instance = null;

	private Connection connect = null;

	private PreparedStatement insertStatement = null;
	private PreparedStatement deleteStatement = null;
	private PreparedStatement updateStatement = null;
	private PreparedStatement selectStatement = null;
	private PreparedStatement countStatement = null;

	private Datastore() throws Exception {
		// this will load the MySQL driver, each DB has its own driver
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			logger.info("Class com.mysql.jdbc.Driver not found");
			throw e;
		}
		// setup the connection with the DB.
		try {
			connect = DriverManager
					.getConnection("jdbc:mysql://localhost/eHonk?"
							+ "user=eHonkUser&password=eHonkUser123");
		} catch (SQLException e) {
			logger.info("Unable to connect to jdbc:mysql://localhost/eHonk");
			close();
			throw e;
		}

		try {
			insertStatement = connect
					.prepareStatement("insert into  eHonk.DRIVER_REGISTER(ID,LICENSE_PLATE,COLOR,PHONE_NUMBER,REGISTER_ID,TS)" +
							" values (default, ?, ?, ?, ?, default) ON DUPLICATE KEY UPDATE LICENSE_PLATE=VALUES(LICENSE_PLATE)," +
							" COLOR=VALUES(COLOR), PHONE_NUMBER=VALUES(PHONE_NUMBER)");
		} catch (SQLException e) {
			logger.info("Unable to prepare insertStatement");
			close();
			throw e;
		}

		try {
			deleteStatement = connect
					.prepareStatement("delete from  eHonk.DRIVER_REGISTER where REGISTER_ID=?");
		} catch (SQLException e) {
			logger.info("Unable to prepare deleteStatement");
			close();
			throw e;
		}

		try {
			updateStatement = connect
					.prepareStatement("update eHonk.DRIVER_REGISTER set LICENSE_PLATE=?,COLOR=?,PHONE_NUMBER=? where REGISTER_ID=?");
		} catch (SQLException e) {
			logger.info("Unable to prepare updateStatement");
			close();
			throw e;
		}

		try {
			selectStatement = connect
					.prepareStatement("SELECT LICENSE_PLATE, COLOR, PHONE_NUMBER, REGISTER_ID from eHonk.DRIVER_REGISTER where LICENSE_PLATE=?");
		} catch (SQLException e) {
			logger.info("Unable to prepare selectStatement");
			close();
			throw e;
		}

		try {
			countStatement = connect
					.prepareStatement("SELECT COUNT(REGISTER_ID) from eHonk.DRIVER_REGISTER where LICENSE_PLATE=?");
		} catch (SQLException e) {
			logger.info("Unable to prepare countStatement");
			close();
			throw e;
		}
	}

	public boolean isClosed() {
		if (connect == null) {
			return true;
		}
		return false;
	}

	public void close() {

		if (insertStatement != null) {
			try {
				insertStatement.close();
			} catch (SQLException e) {
				logger.info("insertStatement close SQLException");
				e.printStackTrace();
			} finally {
				insertStatement = null;
			}
		}
		if (deleteStatement != null) {
			try {
				deleteStatement.close();
			} catch (SQLException e) {
				logger.info("deleteStatement close SQLException");
				e.printStackTrace();
			} finally {
				deleteStatement = null;
			}
		}
		if (updateStatement != null) {
			try {
				updateStatement.close();
			} catch (SQLException e) {
				logger.info("updateStatement close SQLException");
				e.printStackTrace();
			} finally {
				updateStatement = null;
			}
		}
		if (selectStatement != null) {
			try {
				selectStatement.close();
			} catch (SQLException e) {
				logger.info("selectStatement close SQLException");
				e.printStackTrace();
			} finally {
				selectStatement = null;
			}
		}
		if (countStatement != null) {
			try {
				countStatement.close();
			} catch (SQLException e) {
				logger.info("countStatement close SQLException");
				e.printStackTrace();
			} finally {
				countStatement = null;
			}
		}
		if (connect != null) {
			try {
				connect.close();
			} catch (SQLException e) {
				logger.info("Connect close SQLException");
				e.printStackTrace();
			} finally {
				connect = null;
			}
		}
	}

	/* Singleton pattern */
	public static Datastore getDatastore() {
		if (instance == null || instance.isClosed()) {
			try {
				instance = new Datastore();
			} catch (Exception e) {
				instance = null;
			}
		}
		return instance;
	}

	public void register(DriverRecord driver) throws SQLException {
		logger.info("Registering " + driver.getLicensePlate() + " ("
				+ driver.getGcmRegId() + ")");

		synchronized (insertStatement) {
			insertStatement.setString(1, driver.getLicensePlate());
			insertStatement.setInt(2, driver.getCarColor());
			insertStatement.setString(3, driver.getPhoneNumber());
			insertStatement.setString(4, driver.getGcmRegId());

			insertStatement.executeUpdate();
		}
	}

	public void unregister(String gcmRegId) throws SQLException {
		logger.info("UnRegistering " + gcmRegId);

		synchronized (deleteStatement) {
			deleteStatement.setString(1, gcmRegId);

			deleteStatement.executeUpdate();
		}
	}

	public void unregister(DriverRecord driver) throws SQLException {
		if (driver != null)
			unregister(driver.getGcmRegId());
	}

	/**
	 * Updates the registration id of a device.
	 * 
	 * @throws SQLException
	 */
	public void updateRegistration(DriverRecord driver) throws SQLException {
		logger.info("Updating " + driver.getLicensePlate() + " ("
				+ driver.getGcmRegId() + ")");

		synchronized (updateStatement) {
			updateStatement.setString(1, driver.getLicensePlate());
			updateStatement.setInt(2, driver.getCarColor());
			updateStatement.setString(3, driver.getPhoneNumber());
			updateStatement.setString(4, driver.getGcmRegId());

			updateStatement.executeUpdate();
		}
	}

	public List<DriverRecord> getDrivers(String license_plate) throws SQLException {
		logger.info("Retrieving drivers for license plate: " + license_plate);
		List<DriverRecord> drivers = new ArrayList<DriverRecord>();

		ResultSet resultSet = null;

		synchronized (selectStatement) {
			selectStatement.setString(1, license_plate);

			resultSet = selectStatement.executeQuery();
		}

		try {
			while (resultSet != null && resultSet.next()) {
				drivers.add(new DriverRecord(
						resultSet.getString("REGISTER_ID"), resultSet
								.getString("LICENSE_PLATE"), resultSet
								.getInt("COLOR"), resultSet
								.getString("PHONE_NUMBER")));
			}
		} catch (SQLException e) {
			logger.info("SQLException on getDrivers resultSet parsing");
		} finally {
			if (resultSet != null)
				resultSet.close();
		}

		return drivers;
	}
	
	public List<DriverRecord> getDrivers() throws SQLException {
		logger.info("Retrieving all drivers ");
		List<DriverRecord> drivers = new ArrayList<DriverRecord>();
		
		Statement statement = connect.createStatement();
		// resultSet gets the result of the SQL query
		ResultSet resultSet = statement.executeQuery("SELECT LICENSE_PLATE, COLOR, PHONE_NUMBER, REGISTER_ID from eHonk.DRIVER_REGISTER");

		try {
			while (resultSet != null && resultSet.next()) {
				drivers.add(new DriverRecord(
						resultSet.getString("REGISTER_ID"), resultSet
								.getString("LICENSE_PLATE"), resultSet
								.getInt("COLOR"), resultSet
								.getString("PHONE_NUMBER")));
			}
		} catch (SQLException e) {
			logger.info("SQLException on getDrivers resultSet parsing");
		} finally {
			if (resultSet != null)
				resultSet.close();
		}

		return drivers;
	}


	public int countDrivers(String license_plate) throws SQLException {
		logger.info("Retrieving count drivers for license plate "
				+ license_plate);

		ResultSet resultSet = null;
		int count = 0;

		synchronized (countStatement) {
			countStatement.setString(1, license_plate);
			resultSet = countStatement.executeQuery();
		}

		try {
			if (resultSet != null && resultSet.next()) {
				count = resultSet.getInt(1);
			}
		} catch (SQLException e) {
			logger.info("SQLException on countDrivers resultSet parsing");
		} finally {
			if (resultSet != null)
				resultSet.close();
		}

		return count;
	}

	public int countDrivers() throws SQLException {
		logger.info("Retrieving total count of drivers");
		int count = 0;

		Statement statement = connect.createStatement();
		// resultSet gets the result of the SQL query
		ResultSet resultSet = statement.executeQuery("select COUNT(*) from eHonk.DRIVER_REGISTER");
		
		try {
			if (resultSet != null && resultSet.next()) {
				count = resultSet.getInt(1);
			}
		} catch (SQLException e) {
			logger.info("SQLException on countDrivers resultSet parsing");
		} finally {
			if (resultSet != null)
				resultSet.close();
		}

		return count;
	}
	

	public int countDistinctDrivers() throws SQLException {
		logger.info("Retrieving total distinct count of drivers");
		int count = 0;

		Statement statement = connect.createStatement();
		// resultSet gets the result of the SQL query
		ResultSet resultSet = statement.executeQuery("select COUNT(DISTINCT LICENSE_PLATE) from eHonk.DRIVER_REGISTER");
		
		try {
			if (resultSet != null && resultSet.next()) {
				count = resultSet.getInt(1);
			}
		} catch (SQLException e) {
			logger.info("SQLException on countDrivers resultSet parsing");
		} finally {
			if (resultSet != null)
				resultSet.close();
		}

		return count;
	}
	
}
