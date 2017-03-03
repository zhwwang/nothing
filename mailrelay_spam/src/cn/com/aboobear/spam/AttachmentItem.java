package cn.com.aboobear.spam;

public class AttachmentItem {

	private String attachmentcontent = null;
	private long actn_len = 0;
	private String attachmentnames = null;
	private int atn_len = 0;
	private String attachmenttype = null;
	private boolean isEncrypted = false;

	public String getAttachmentcontent() {
		return attachmentcontent;
	}

	public void setAttachmentcontent(String attachmentcontent) {
		this.attachmentcontent = attachmentcontent;
		if (this.attachmentcontent == null) {
			this.actn_len = 0;
		} else {
			try {
				this.actn_len = this.attachmentcontent.getBytes("utf8").length;
			} catch (Exception e) {
			}
		}
	}

	public long getActn_len() {
		return actn_len;
	}

	public void setActn_len(long actn_len) {
		this.actn_len = actn_len;
	}

	public String getAttachmentnames() {
		return attachmentnames;
	}

	public void setAttachmentnames(String attachmentnames) {
		this.attachmentnames = attachmentnames;
		if (this.attachmentnames == null) {
			this.atn_len = 0;
		} else {
			try {
				this.atn_len = this.attachmentnames.getBytes("utf8").length;
			} catch (Exception e) {
			}
		}
	}
	
	public String getAttachmenttype() {
		return this.attachmenttype;
	}
	
	public void setAttachmenttype(String attachmenttype) {
		this.attachmenttype = attachmenttype;
	}

	public int getAtn_len() {
		return atn_len;
	}

	public void setAtn_len(int atn_len) {
		this.atn_len = atn_len;
	}
	
	public boolean isEncrypted() {
		return isEncrypted;
	}

	public void setIsEncrypted(boolean isEncrypted) {
		this.isEncrypted = isEncrypted;
	}
}
