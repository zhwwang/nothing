package cn.com.aboobear.spam;

import java.io.File;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import cn.com.aboobear.mailrelay.misc.SystemUtils;

public class Engine {
	private static Logger ENGINELOGGER = null;
	private static Logger SPAMLOGGER = null;
	// mysql connection
	public static Connection connection;

	public static Thread socketThread = null;
	public static ServerSocket server = null;
	
	public static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	public static DateFormat dateFormat_dateonly = new SimpleDateFormat(
			"yyyy_MM_dd");
	public static DateFormat dateFormat_dir = new SimpleDateFormat("yyyyMM//dd");
	public static String dateFormat_dir_now = SystemUtils.Format(
			Engine.dateFormat_dir, new Date());
	public static String dateFormat_dateonly_now = SystemUtils.Format(
			Engine.dateFormat_dateonly, new Date());
	
	public static String LOG_ROOT_DIR = "/home/log/mrspam/";

	public static boolean STOP = false;

	public static boolean prepareLogDir() {
		String dir = Engine.LOG_ROOT_DIR + Engine.dateFormat_dir_now;
		boolean result = false;
		File dirfile = new File(dir);
		if (!dirfile.exists()) {
			try {
				dirfile.mkdirs();
			} catch (Exception e) {
				System.out.println("mrspam fail to create log dir:" + dir);
				e.printStackTrace();
				return result;
			}
		}
		if (!dirfile.canWrite()) {
			try {
				String shell = "sudo chmod -R 777 " + dir;
				Runtime rt = Runtime.getRuntime();
				Process proc = rt.exec(shell);
				proc.exitValue();
				// dirfile.setWritable(true, false);
			} catch (Exception e) {
				System.out.println("mrspam fail to change log dir Writable:"
						+ dir);
				e.printStackTrace();
			}
			try {
				Thread.sleep(3000);
			} catch (Exception e) {
			}
			result = true;
		} else {
			result = true;
		}
		return result;
	}

	public static Logger getEngineLogger() {
		if (ENGINELOGGER == null) {
			try {
				ENGINELOGGER = Logger.getLogger("engine");
				ENGINELOGGER.setUseParentHandlers(false);
				FileHandler fileHandler = new FileHandler(Engine.LOG_ROOT_DIR
						+ Engine.dateFormat_dir_now + "/mrspam_engine_"
						+ Engine.dateFormat_dateonly_now + "_%g.log", 5242880,
						5, true);
				fileHandler.setLevel(Configuration.LOGLEVEL);
				SimpleFormatter fter = new SimpleFormatter();
				fileHandler.setFormatter(fter);
				ENGINELOGGER.addHandler(fileHandler);
			} catch (Exception e) {
				e.printStackTrace();
				ENGINELOGGER = null;
			}
		}
		return ENGINELOGGER;
	}

	public static Logger getSpamLogger() {
		if (SPAMLOGGER == null) {
			try {
				SPAMLOGGER = Logger.getLogger("spam");
				SPAMLOGGER.setUseParentHandlers(false);
				FileHandler fileHandler = new FileHandler(Engine.LOG_ROOT_DIR
						+ Engine.dateFormat_dir_now + "/mrspam_spam_"
						+ Engine.dateFormat_dateonly_now + "_%g.log", 5242880,
						5, true);
				fileHandler.setLevel(Configuration.SPAM_LOGLEVEL);
				SimpleFormatter fter = new SimpleFormatter();
				fileHandler.setFormatter(fter);
				SPAMLOGGER.addHandler(fileHandler);
			} catch (Exception e) {
				e.printStackTrace();
				SPAMLOGGER = null;
			}
		}
		return SPAMLOGGER;
	}

	public static String FormatDateStr() {
		return SystemUtils.Format(Engine.dateFormat, new Date());
	}

	public static boolean EstablishMysqlConnection(boolean force) {
		if (Engine.connection == null || force) {
			if (Engine.connection != null && force) {
				terminateMysqlConnection();
			}

			String url = Configuration.DBURL;
			String username = Configuration.DBUSERNAME;
			String password = Configuration.DBPASSWORD;
			try {
				Class.forName("org.gjt.mm.mysql.Driver");
				Engine.connection = DriverManager.getConnection(url, username,
						password);
			} catch (Exception sqlex) {
				Engine.getEngineLogger().log(Level.SEVERE,
						"mrspam EstablishMysqlConnection error", sqlex);
				Engine.connection = null;
			}
		}
		if (Engine.connection == null) {
			return false;
		} else {
			return true;
		}
	}

	public static void terminateMysqlConnection() {
		if (Engine.connection != null) {
			try {
				Engine.connection.close();
			} catch (SQLException e) {
				if (Engine.getEngineLogger() != null) {
					Engine.getEngineLogger().log(Level.WARNING,
							"mrspam close mysql connection error", e);
				}
			}
			Engine.connection = null;
		}
	}

	public static boolean StartupServerSocket() {
		boolean status = false;
		try {
			Engine.server = new ServerSocket(Configuration.SPAM_PORT);
		} catch (Exception e) {
			Engine.server = null;
			if (Engine.getEngineLogger() != null) {
				Engine.getEngineLogger().log(Level.WARNING,
						"mrspam StartupServerSocket Error", e);
			}
		}

		if (Engine.server != null) {
			try {
				SocketThread st = new SocketThread();
				Engine.socketThread = new Thread(st);
				Engine.socketThread.start();
			} catch (Exception e) {
				Engine.socketThread = null;
				if (Engine.getEngineLogger() != null) {
					Engine.getEngineLogger().log(Level.WARNING,
							"mrspam StartupServerSocketThread Error", e);
				}
			}
		}

		if (Engine.server != null && Engine.socketThread != null) {
			status = true;
		}

		return status;
	}

	public static void TerminateServerSocket() {
		Engine.STOP = true;
		try {
			if (Engine.server != null) {
				Engine.server.close();
			}
			Engine.server = null;
		} catch (Exception e) {
			if (Engine.getEngineLogger() != null) {
				Engine.getEngineLogger().log(Level.WARNING,
						"mrspam terminate ServerSocket Error", e);
			}
		}
		try {
			if (Engine.socketThread != null) {
				Engine.socketThread.interrupt();
				Engine.getEngineLogger().log(Level.INFO,
						"mrspam TerminateServerSocket thread interrupt");
			}
			Engine.socketThread = null;
		} catch (Exception e) {
			if (Engine.getEngineLogger() != null) {
				Engine.getEngineLogger().log(Level.WARNING,
						"mrspam terminate socketThread Error", e);
			}
		}
	}

	public static void TerminateSystem(int exitcode) {
		Engine.terminateMysqlConnection();
		Engine.TerminateServerSocket();

		if (exitcode != 0) {
			System.exit(exitcode);
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		int status = 0;

		boolean dirstatus = Engine.prepareLogDir();
		if (!dirstatus) {
			if (Engine.getEngineLogger() != null) {
				getEngineLogger().log(Level.SEVERE,
						"mrspam fail to prepare log dir. Exit...");
			}
			System.out.println("mrspam fail to prepare log dir. Exit...");
			Engine.TerminateSystem(1);
		}

		try {
			Engine mengine = new Engine();
		} catch (Exception e) {
			if (Engine.getEngineLogger() != null) {
				getEngineLogger().log(Level.SEVERE,
						"mrspam initialized error. Exit...", e);
			}
			System.out.println("mrspam initialized error: [" + e.getMessage()
					+ "]. Exit...");
			Engine.TerminateSystem(2);
		}

		if (!Engine.StartupServerSocket()) {
			if (Engine.getEngineLogger() != null) {
				getEngineLogger().log(Level.WARNING,
						"There is already a mrspam running. Exit...");
			}
			System.out.println("There is already a mrspam running. Exit...");
			Engine.TerminateSystem(3);
		}

		getEngineLogger().log(Level.WARNING, "mrspam launch successfully...");

		while (!Engine.STOP) {
			try {
				Thread.sleep(Configuration.FETCH_TASK_NUMBER);
			} catch (Exception e) {
			}
		}

		try {
			Thread.sleep(2000);
		} catch (Exception e) {
		}

		Engine.TerminateSystem(0);

		getEngineLogger().log(Level.WARNING, "mrspam quit successfully...");

		System.out.println("mrspam quit successfully...");
	}
}
