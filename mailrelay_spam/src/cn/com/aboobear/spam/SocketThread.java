package cn.com.aboobear.spam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;

public class SocketThread implements Runnable {

	@Override
	public void run() {
		Engine.getEngineLogger().log(Level.INFO, "mrspam SocketThread start");

		Socket request = null;
		while (!Engine.STOP) {
			if (Engine.server == null) {
				try {
					Engine.server = new ServerSocket(Configuration.SPAM_PORT);
				} catch (Exception e) {
					Engine.server = null;
					Engine.getEngineLogger().log(
							Level.WARNING,
							"mrspam create ServerSocket("
									+ Configuration.SPAM_PORT + ") error", e);
					Engine.STOP = true;
					break;
				}
			}
			if (Engine.server != null) {
				try {
					request = Engine.server.accept();
					BufferedReader buff = new BufferedReader(
							new InputStreamReader(request.getInputStream()));
					String command = buff.readLine();
					if (command != null) {
						Engine.getEngineLogger().log(
								Level.INFO,
								"mrspam SocketThread - command:" + command
										+ "-" + command.length());
						if (command.equals("stop")) {
							request.close();
							request = null;
							Engine.STOP = true;
							break;
						} else if (command.startsWith("c")
								&& (command.length() == 5)) {
							try {
								Configuration.readConfigConf();
							} catch (Exception e) {
								Engine.getEngineLogger()
										.log(Level.WARNING,
												"mrspam reload configuration file error",
												e);
							}
							try {
								Configuration.loadConfigFromDB();
							} catch (Exception e) {
								Engine.getEngineLogger()
										.log(Level.WARNING,
												"mrspam reload configuration from DB error",
												e);
							}
							Engine.getEngineLogger().log(Level.WARNING,
									"mrspam reload configuration done");
						}
					}
					request.close();
					request = null;
				} catch (SocketException e) {
					if (e.getMessage().equalsIgnoreCase("Socket closed")) {
						Engine.getEngineLogger().log(Level.INFO,
								"mrspam SocketThread meet Socket closed", e);
					} else {
						Engine.getEngineLogger().log(Level.WARNING,
								"mrspam SocketThread error", e);
					}
				} catch (Exception e) {
					Engine.getEngineLogger().log(Level.WARNING,
							"mrspam SocketThread error", e);
				}
			} else {
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
				}
			}
		}

		Engine.getEngineLogger().log(Level.INFO, "mrspam SocketThread quit");
	}

}
