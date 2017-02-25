package cn.com.aboobear.spam;

public class EmlItem {
	private long id = 0;
	private String emldir = Configuration.MAIN_STORAGE;
	private String emlpath = null;

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
}
