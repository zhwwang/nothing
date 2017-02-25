package cn.com.aboobear.mailrelay.misc;

import java.util.ArrayList;
import java.util.List;

public class Domain {
	long id;
	String domain;
	int timeout;
	int reconnect;
	int sipmaximum;
	long smailsize;
	long smailattchsize;
	List<String> unavailableips = new ArrayList<String>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getReconnect() {
		return reconnect;
	}

	public void setReconnect(int reconnect) {
		this.reconnect = reconnect;
	}

	public int getSipmaximum() {
		return sipmaximum;
	}

	public void setSipmaximum(int sipmaximum) {
		this.sipmaximum = sipmaximum;
	}

	public long getSmailsize() {
		return smailsize;
	}

	public void setSmailsize(long smailsize) {
		this.smailsize = smailsize;
	}

	public long getSmailattchsize() {
		return smailattchsize;
	}

	public void setSmailattchsize(long smailattchsize) {
		this.smailattchsize = smailattchsize;
	}

	public boolean isUnavailableip(String unavailableip) {
		return this.unavailableips.contains(unavailableip);
	}

	public void addUnavailableip(String unavailableip) {
		this.unavailableips.add(unavailableip);
	}

	public void cleanUnavailableip() {
		this.unavailableips.clear();
	}
}
