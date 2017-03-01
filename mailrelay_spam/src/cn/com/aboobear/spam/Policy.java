package cn.com.aboobear.spam;

public class Policy {
	private int id;
	private String domain;
	private int antispamPolicy;
	private int isolateMode;
	private String forwardId;
	private String markId;
	private int killVirusMode;
	private int virusProcessMode;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public int getAntispamPolicy() {
		return antispamPolicy;
	}
	public void setAntispamPolicy(int antispamPolicy) {
		this.antispamPolicy = antispamPolicy;
	}
	public int getIsolateMode() {
		return isolateMode;
	}
	public void setIsolateMode(int isolateMode) {
		this.isolateMode = isolateMode;
	}
	public String getForwardId() {
		return forwardId;
	}
	public void setForwardId(String forwardId) {
		this.forwardId = forwardId;
	}
	public String getMarkId() {
		return markId;
	}
	public void setMarkId(String markId) {
		this.markId = markId;
	}
	public int getKillVirusMode() {
		return killVirusMode;
	}
	public void setKillVirusMode(int killVirusMode) {
		this.killVirusMode = killVirusMode;
	}
	public int getVirusProcessMode() {
		return virusProcessMode;
	}
	public void setVirusProcessMode(int virusProcessMode) {
		this.virusProcessMode = virusProcessMode;
	}
}
