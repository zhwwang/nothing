package cn.com.aboobear.spam;

public class EmlItem {
	private long id = 0;
	private String emldir = Configuration.MAIN_STORAGE;
	private String emlpath = null;
	private String sendfrom = null;
	private String sendTo = null;
	private String domain = null;
	private String todomain = null;
	private String title = null;
	private int hasattachment = -1;
	private long mailsize = -1;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getEmlpath() {
		return emlpath;
	}

	public void setEmlpath(String emlpath) {
		this.emlpath = emlpath;
	}

	public String getEmldir() {
		return emldir;
	}

	public void setEmldir(String emldir) {
		this.emldir = emldir;
	}

	public String getFullEmlpath() {
		return this.emldir + Configuration.SEPARATOR + this.emlpath;
	}

	public long getMailsize() {
		return mailsize;
	}

	public void setMailsize(long mailsize) {
		this.mailsize = mailsize;
	}

	public int getHasattachment() {
		return hasattachment;
	}

	public void setHasattachment(int hasattachment) {
		this.hasattachment = hasattachment;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTodomain() {
		return todomain;
	}

	public void setTodomain(String todomain) {
		this.todomain = todomain;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getSendfrom() {
		return sendfrom;
	}

	public void setSendfrom(String sendfrom) {
		this.sendfrom = sendfrom;
	}

	public String getSendTo() {
		return sendTo;
	}

	public void setSendTo(String sendTo) {
		this.sendTo = sendTo;
	}
}
