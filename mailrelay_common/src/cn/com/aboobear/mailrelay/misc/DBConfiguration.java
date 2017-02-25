package cn.com.aboobear.mailrelay.misc;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConfiguration {
	public String DBURL = null;
	public String DBUSERNAME = null;
	public String DBPASSWORD = null;
	public String DBDATABASE = null;
	public Logger logger = null;
	public Level loglevel = Level.WARNING;
	

	public String getDBURL() {
		return DBURL;
	}

	public void setDBURL(String dBURL) {
		DBURL = dBURL;
	}

	public String getDBUSERNAME() {
		return DBUSERNAME;
	}

	public void setDBUSERNAME(String dBUSERNAME) {
		DBUSERNAME = dBUSERNAME;
	}

	public String getDBPASSWORD() {
		return DBPASSWORD;
	}

	public void setDBPASSWORD(String dBPASSWORD) {
		DBPASSWORD = dBPASSWORD;
	}

	public String getDBDATABASE() {
		return DBDATABASE;
	}

	public void setDBDATABASE(String dBDATABASE) {
		DBDATABASE = dBDATABASE;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public Level getLoglevel() {
		return loglevel;
	}

	public void setLoglevel(Level loglevel) {
		this.loglevel = loglevel;
	}
}
