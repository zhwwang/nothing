package cn.com.aboobear.mailrelay.misc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.StringEscapeUtils;

public abstract class Item {
	public long id = 0;
	public long tid = 0;
	public int retries = 0;
	public int retry_round = 0;
	public int status = 0;
	public long mailsize = 0;
	public String sendfrom = null;
	private int returncode = 0;

	// equals forward
	public String sendtoStr = null;
	public String sendto = null;

	public boolean isOverseas = false;
	public int mailtype = 0;
	public String domain = null;
	public String todomain = null;
	public String maildate = null;
	public String reason = null;
	public String title = null;
	public String msgid = null;
	public String failedaddress = "";

	public StringBuffer log = new StringBuffer();

	public DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public HashMap<String, MailAddress> sendtoMap = new HashMap<String, MailAddress>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getTid() {
		return tid;
	}

	public void setTid(long tid) {
		this.tid = tid;
	}

	public boolean isReturn() {
		return (mailtype == 1);
	}

	public boolean isLocalMail() {
		return (mailtype > 0);
	}

	public void setMailType(int type) {
		this.mailtype = type;
	}

	public boolean isOverseas() {
		return isOverseas;
	}

	public void setOverseas(boolean isOverseas) {
		this.isOverseas = isOverseas;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public void addRetries() {
		this.retries++;
	}

	public int getStatus() {
		return status;
	}

	public void updateStatus(int status) {
		this.status = status;
	}

	public long getMailsize() {
		return mailsize;
	}

	public void setMailsize(long mailsize) {
		this.mailsize = mailsize;
	}

	public int getRetryRound() {
		return retry_round;
	}

	public void setRetryRound(int retry_round) {
		this.retry_round = retry_round;
	}

	public boolean needRetry(Domain domain) {
		if (this.status == Constants.RETRY) {
			return true;
		} else {
			return false;
		}
	}

	public String getSendfrom() {
		return sendfrom;
	}

	public void setSendfrom(String sendfrom) {
		this.sendfrom = sendfrom;
	}

	public String getSendtoStr() {
		return sendtoStr;
	}

	public String getSendto() {
		return sendto;
	}

	public void setSendto(String sendtoStr) {
		this.sendtoStr = sendtoStr;
		this.sendto = this.sendtoStr.replaceAll(":0", "").replaceAll(":1", "");
		this.sendtoMap.clear();
		String[] sendto_array = this.sendtoStr.split(";");
		for (int i = 0; i < sendto_array.length; i++) {
			try {
				String[] sendto_array_split = sendto_array[i].split(":");
				int sendto_status = 0;
				if (sendto_array_split.length == 0) {
					continue;
				} else if (sendto_array_split.length > 1) {
					sendto_status = Integer.parseInt(sendto_array_split[1]);
				}
				MailAddress item = new MailAddress(new InternetAddress(
						sendto_array_split[0], false));
				item.setStatus(sendto_status);
				this.sendtoMap.put(sendto_array_split[0], item);
			} catch (Exception e) {
			}
		}
	}

	private void refreshSentoStr() {
		StringBuffer sendtobf = new StringBuffer();
		Iterator<Entry<String, MailAddress>> iter = this.sendtoMap.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Entry<String, MailAddress> entry = iter.next();
			if (sendtobf.length() == 0) {
				sendtobf.append(entry.getKey()).append(":")
						.append(entry.getValue().getStatus());
			} else {
				sendtobf.append(";").append(entry.getKey()).append(":")
						.append(entry.getValue().getStatus());
			}
		}
		this.sendtoStr = sendtobf.toString();
		this.sendto = this.sendtoStr.replaceAll(":0", "").replaceAll(":1", "");
	}

	public void updateSendtoList(Address[] addresses, int status) {
		MailAddress item = null;
		for (int i = 0; i < addresses.length; i++) {
			item = this.sendtoMap.get(addresses[i].toString());
			if (item != null) {
				item.setStatus(status);
			}
		}
		this.refreshSentoStr();
	}

	public boolean existFailedAddress() {
		boolean result = false;
		Iterator<Entry<String, MailAddress>> iter = this.sendtoMap.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Entry<String, MailAddress> entry = iter.next();
			MailAddress paddr = entry.getValue();
			if (paddr.getStatus() != 1) {
				result = true;
				break;
			}
		}
		return result;
	}

	public String getFailedaddress() {
		StringBuffer result = new StringBuffer();
		Iterator<Entry<String, MailAddress>> iter = this.sendtoMap.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Entry<String, MailAddress> entry = iter.next();
			MailAddress paddr = entry.getValue();
			if (paddr.getStatus() != 1) {
				if (result.length() == 0) {
					result.append(paddr.toString());
				} else {
					result.append(";").append(paddr.toString());
				}
			}
		}
		this.failedaddress = result.toString();
		return this.failedaddress;
	}

	public void setSuccessOnSendtoList() {
		Iterator<Entry<String, MailAddress>> iter = this.sendtoMap.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Entry<String, MailAddress> entry = iter.next();
			entry.getValue().setStatus(1);
		}
		this.refreshSentoStr();
	}

	public Address[] getNeededSentoAddr() {
		List<InternetAddress> results = new ArrayList<InternetAddress>();
		Iterator<Entry<String, MailAddress>> iter = this.sendtoMap.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Entry<String, MailAddress> entry = iter.next();
			MailAddress item = entry.getValue();
			if (item.getStatus() == 0) {
				results.add(item.toInternetAddress());
			}
		}
		return results.toArray(new Address[0]);
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getTodomain() {
		return todomain;
	}

	public void setTodomain(String todomain) {
		this.todomain = todomain;
	}

	public String getMaildate() {
		return maildate;
	}

	public void setMaildate(String maildate) {
		this.maildate = maildate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMsgid() {
		return msgid;
	}

	public void setMsgid(String msgid) {
		this.msgid = msgid;
	}

	public String getReason() {
		if (reason == null) {
			return "";
		} else {
			return reason;
		}
	}

	public void setReason(String reason, int returncode) {
		this.reason = reason;
		this.appendLog(this.reason);
		this.setReturncode(returncode);
	}

	public void appendLog(String log) {
		this.log.append(this.dateFormat.format(new Date()) + " -- " + log
				+ "\n");
	}

	public void appendLogWithFormat(String log) {
		this.log.append(log).append("\n");
	}

	public String getLog() {
		return this.log.toString();
	}

	public String getEsacpeLog() {
		String escape_log = null;
		if (this.log != null) {
			try {
				escape_log = StringEscapeUtils.escapeSql(this.log.toString());
				int escape_log_len = escape_log.length();
				int dec = escape_log_len - 5100;
				if (dec > 0) {
					escape_log = escape_log.substring(dec, 5100);
					this.log.setLength(0);
					this.log.append(escape_log);
				}
			} catch (Exception ex) {
				escape_log = "";
				this.log.setLength(0);
			}
			return escape_log;
		} else {
			return "";
		}
	}

	public void setLog(String log) {
		this.log.setLength(0);
		if (log != null) {
			this.log.append(log);
			this.log.append("\n");
		}
	}

	public int getReturncode() {
		return returncode;
	}

	public void setReturncode(int returncode) {
		this.returncode = returncode;
	}

	public boolean is500Error() {
		if (this.returncode > 499 && this.returncode < 600) {
			return true;
		}
		return false;
	}
}
