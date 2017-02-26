package cn.com.aboobear.spam;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.Level;

import cn.com.aboobear.mailrelay.misc.DBConfiguration;

public class Configuration {
	private static String MR_CONFIGPATH = "/opt/share/longgerconf/mrspam.conf";
	public static final String SEPARATOR = System.getProperty("file.separator");
	
	public static String SPAM_HOST = "127.0.0.1";
	public static int SPAM_PORT = 32389;
	public static int LGSPAM_PORT = 32387;
	public static int CLAMD_PORT = 32388;

	public static Level LOGLEVEL = Level.INFO;
	public static Level SPAM_LOGLEVEL = Level.INFO;
	public static String DBURL = null;
	public static String DBUSERNAME = null;
	public static String DBPASSWORD = null;
	public static String DBDATABASE = null;
	
	public static DBConfiguration DBCONFIGURATION = new DBConfiguration();

	public static int ALIVE_DURATION = 360;
	
	// storage path
	public static String MAIN_STORAGE = null;
	
	public static int THREADS_NUMBER = 10;
	public static int THREADS_INTERVAL = 0;
	public static int FETCH_TASK_NUMBER = 500;
	public static int FETCH_TASK_INTERVAL = 3000;
	
	public static SimpleDateFormat DIR_Formatter_IK = new SimpleDateFormat(
			"yyyyMMdd");

	public static String getConfigurationPath() {
		return Configuration.MR_CONFIGPATH;
	}

	public static boolean readConfigConf() {
		Properties props = new Properties();
		boolean result = true;
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(
					Configuration.getConfigurationPath()));
			props.load(in);
			String loglevel = props.getProperty("log_level");
			//if (loglevel.equals("all")) {
				Configuration.LOGLEVEL = Level.INFO;
			//} else {
			//	Configuration.LOGLEVEL = Level.WARNING;
			//}
			String tempstr = props.getProperty("spam_port");
			if (tempstr != null && tempstr.length() > 0) {
				Configuration.SPAM_PORT = Integer.parseInt(tempstr);
			}
			tempstr = props.getProperty("lgspam_port");
			if (tempstr != null && tempstr.length() > 0) {
				Configuration.LGSPAM_PORT = Integer.parseInt(tempstr);
			}
			tempstr = props.getProperty("clamd_port");
			if (tempstr != null && tempstr.length() > 0) {
				Configuration.CLAMD_PORT = Integer.parseInt(tempstr);
			}
			tempstr = props.getProperty("alive_duration");
			if (tempstr != null && tempstr.length() > 0) {
				Configuration.ALIVE_DURATION = Integer.parseInt(tempstr);
			}
			
			Configuration.MAIN_STORAGE = props.getProperty("main_storage");
			tempstr = props.getProperty("threads_number");
			if (tempstr != null && tempstr.length() > 0) {
				Configuration.THREADS_NUMBER = Integer.parseInt(tempstr);
			}
			tempstr = props.getProperty("thread_interval");
			if (tempstr != null && tempstr.length() > 0) {
				Configuration.THREADS_INTERVAL = Integer.parseInt(tempstr);
			}
			tempstr = props.getProperty("fetch_task_number");
			if (tempstr != null && tempstr.length() > 0) {
				Configuration.FETCH_TASK_NUMBER = Integer.parseInt(tempstr);
			}
			tempstr = props.getProperty("fetch_task_interval");
			if (tempstr != null && tempstr.length() > 0) {
				Configuration.FETCH_TASK_INTERVAL = Integer.parseInt(tempstr);
			}

			Configuration.DBUSERNAME = props.getProperty("db_username");
			if (Configuration.DBUSERNAME == ""
					|| Configuration.DBUSERNAME == null) {
				Configuration.DBUSERNAME = "longger";
			}
			Configuration.DBPASSWORD = props.getProperty("db_password");
			if (Configuration.DBPASSWORD == ""
					|| Configuration.DBPASSWORD == null) {
				Configuration.DBPASSWORD = "longger136";
			}
			Configuration.DBDATABASE = props.getProperty("db_database");
			if (Configuration.DBDATABASE == ""
					|| Configuration.DBDATABASE == null) {
				Configuration.DBDATABASE = "mailgateway";
			}
			tempstr = props.getProperty("db_url");
			if (tempstr == "" || tempstr == null) {
				tempstr = "127.0.0.1:3306";
			}
			Configuration.DBURL = "jdbc:mysql://"
					+ tempstr
					+ "/"
					+ Configuration.DBDATABASE
					+ "?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&autoReconnect=true&failOverReadOnly=false";

			DBCONFIGURATION.setDBURL(Configuration.DBURL);
			DBCONFIGURATION.setDBUSERNAME(Configuration.DBUSERNAME);
			DBCONFIGURATION.setDBPASSWORD(Configuration.DBPASSWORD);
			DBCONFIGURATION.setLoglevel(Configuration.LOGLEVEL);
			DBCONFIGURATION.setLogger(Engine.getEngineLogger());
			
			try {
				in.close();
			} catch (Exception e) {
			}

		} catch (Exception e) {
			result = false;
			Engine.getEngineLogger().log(Level.WARNING,
					"mrspam -- Fail to readConfigConf", e);
		}
		return result;
	}

	public static void loadConfigFromDB() {
		boolean result = loadConfigFromDBWithException();
		if (!result) {
			Engine.EstablishMysqlConnection(true);
		}
		loadConfigFromDBWithException();
	}

	public static boolean loadConfigFromDBWithException() {
		if (Engine.connection == null) {
			return false;
		}
		try {
			Configuration.loadMRConfigFromDB(Engine.connection);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static void loadMRConfigFromDB(Connection connection) throws Exception {
		Statement statement = null;
		ResultSet resultSet = null;
		String query = null;
		
		query = "select * from mr_filterrule";
		try {
			statement = connection.createStatement();
			resultSet = statement.executeQuery(query);

			while (resultSet.next()) {
				String ip = resultSet
						.getString("ip");
			}
		} catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException ex1) {
			throw ex1;
		} catch (com.mysql.jdbc.CommunicationsException ex2) {
			throw ex2;
		} catch (Exception sqlex) {
		} finally {
			if (resultSet != null) {
				resultSet.close();
			}
			if (statement != null) {
				statement.close();
			}			
		}
	}
}
