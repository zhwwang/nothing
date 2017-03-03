package cn.com.aboobear.spam;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import cn.com.aboobear.mailrelay.misc.BaseThread;
import cn.com.aboobear.mailrelay.misc.SocketClient;

/**
 * @author tjuwzw
 *
 */
/**
 * @author tjuwzw
 *
 */
@SuppressWarnings("unused")
public class SpamThread extends BaseThread {
	private BlockingQueue<EmlItem> taskItems = null;
	private long workingItemId = 0;
	private Session mailSession = null;
	public static Date lastRestarttime = null;
	private SpamMgrThread MgrThread = null;
	
	public SpamThread(int index, BlockingQueue<EmlItem> taskItems) {
		this.threadId = "SpamThread_" + index;
		this.taskItems = taskItems;
		Properties props = System.getProperties();
		props.put("mail.host", "mail.maildata.cn");
		props.put("mail.transport.protocol", "smtp");
		this.mailSession = Session.getDefaultInstance(props, null);
	}

	public void setMgrThread(SpamMgrThread pMgrThread) {
		this.MgrThread = pMgrThread;
	}
	
	public void close() {
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (SQLException e) {
			}
			this.connection = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		this.close();
	}

	public long getWorkingItemId() {
		return this.workingItemId;
	}
	
	public void Info(String msg) {
		Engine.getEngineLogger().log(Level.INFO, this.threadId + msg);
	}

	public void Info(String msg, Exception ex) {
		Engine.getEngineLogger().log(Level.INFO, this.threadId + msg, ex);
	}

	public void Warn(String msg, Exception ex) {
		Engine.getEngineLogger().log(Level.WARNING, this.threadId + msg, ex);
	}
	
	public static String getFilenameSuffix(String filename) {
		String suffix = "";
		String[] tmp = filename.split("\\.");
		if (tmp.length > 1) {
			String last_str = tmp[tmp.length - 1];
			if (last_str.equalsIgnoreCase("gz")
					|| last_str.equalsIgnoreCase("bz2")) {
				if (tmp.length > 2) {
					String pre_part = tmp[tmp.length - 2];
					if (pre_part.equalsIgnoreCase("tar")) {
						suffix = "tar." + last_str;
					}
				}
			} else {
				suffix = last_str;
			}
		}
		return suffix;
	}
	
	public boolean handleAttachments(Part part,
			List<AttachmentItem> attachments)
			throws Exception {
		String fileName = "";
		boolean isEncrypted = false;
		Engine.getEngineLogger().log(Level.INFO,
				"auditor handleAttachments start to get attachment");
		if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart mpart = mp.getBodyPart(i);
				String disposition = mpart.getDisposition();
				if ((disposition != null)
						&& disposition.equals(Part.ATTACHMENT)) {
					fileName = mpart.getFileName();
					if (fileName != null) {
						fileName = MimeUtility.decodeText(fileName);
					}
					String filenameSuffix = getFilenameSuffix(fileName);
					long filesize = mpart.getSize();
					
					AttachmentItem item = new AttachmentItem();
					item.setAttachmentnames(fileName);
					item.setAttachmenttype(filenameSuffix);
					if (filesize > 0)
						item.setActn_len(filesize);

					attachments.add(item);
				}/* else if (mpart.isMimeType("multipart/*")) {
					isEncrypted = handleAttachments(mpart, attachments,
							filenameInZip);
				}*/
			}
		} else if (part.isMimeType("message/rfc822")) {
			isEncrypted = handleAttachments((Part) part.getContent(),
					attachments);
		}
		return isEncrypted;
	}
	
	public boolean getAttachments(String mail_emlpath,
			List<AttachmentItem> attachments) {
		InputStream source = null;
		Engine.getEngineLogger().log(
				Level.INFO,
				"auditor getAttachments start to get attachment from :"
						+ mail_emlpath);
		try {
			source = new FileInputStream(mail_emlpath);
			MimeMessage message = new MimeMessage(mailSession, source);
			try {
				handleAttachments(message, attachments);
			} catch (Exception e) {
				Engine.getEngineLogger().log(Level.WARNING,
								"auditor getAttachments-handleAttachments Exception",
								e);
			}
		} catch (OutOfMemoryError e) {
			Engine.getEngineLogger().log(Level.WARNING,
					"auditor getAttachments OutOfMemoryError", e);
			System.exit(99);
		} catch (Exception e) {
			Engine.getEngineLogger().log(Level.WARNING,
					"auditor getAttachments Exception", e);
		} finally {
			if (source != null) {
				try {
					source.close();
				} catch (Exception e) {
				}
			}
		}
		return true;
	}
	
	private String getHeader(String mail_emlpath) {
		InputStream source = null;
		Enumeration headlines = null;
		StringBuffer sb = new StringBuffer();
		try {
			source = new FileInputStream(mail_emlpath);
			MimeMessage message = new MimeMessage(mailSession, source);
			headlines = message.getAllHeaderLines();
			while (headlines.hasMoreElements())
				sb.append((String) headlines.nextElement());
		} catch (OutOfMemoryError e) {
			Engine.getEngineLogger().log(Level.WARNING,
					"auditor getContents OutOfMemoryError", e);
			System.exit(99);
		} catch (Exception e) {
			Engine.getEngineLogger().log(Level.WARNING,
					"auditor getContents Exception", e);
		} finally {
			if (source != null) {
				try {
					source.close();
				} catch (Exception e) {
				}
			}
		}
		return sb.toString();
	}
	
	public static void getMailContent(Part part, StringBuffer bodytext)
			throws Exception {
		if (part.isMimeType("text/plain")) {
			bodytext.append((String) part.getContent());
		} else if (part.isMimeType("text/html")) {
			bodytext.append((String) part.getContent());
		} else if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			int counts = multipart.getCount();
			for (int i = 0; i < counts; i++) {
				getMailContent(multipart.getBodyPart(i), bodytext);
			}
		} else if (part.isMimeType("message/rfc822")) {
			getMailContent((Part) part.getContent(), bodytext);
		}
	}
	
	public String getContents(String mail_emlpath) {
		String mail_content = null;
		InputStream source = null;
		try {
			source = new FileInputStream(mail_emlpath);
			MimeMessage message = new MimeMessage(mailSession, source);

			StringBuffer bodytext = new StringBuffer();

			try {
				getMailContent(message, bodytext);
				mail_content = bodytext.toString();
			} catch (Exception e) {
				Engine.getEngineLogger().log(Level.WARNING,
						"auditor getContents-getMailContent Exception", e);
			}
		} catch (OutOfMemoryError e) {
			Engine.getEngineLogger().log(Level.WARNING,
					"auditor getContents OutOfMemoryError", e);
			System.exit(99);
		} catch (Exception e) {
			Engine.getEngineLogger().log(Level.WARNING,
					"auditor getContents Exception", e);
		} finally {
			if (source != null) {
				try {
					source.close();
				} catch (Exception e) {
				}
			}
		}
		return mail_content;
	}
	
	private boolean checkCondition(String mail_data, String value, String match) {
		String new_rule_str = null;
		boolean status = false;
		this.Info("checking the rule for string match - rule's content is:"
				+ value);

		if (mail_data != null && mail_data.length() > 0) {
			if (match.equalsIgnoreCase("not_contain") || match.equalsIgnoreCase("not_match")) {
				status = (mail_data.indexOf(value) < 0);
			} else if (match.equalsIgnoreCase("contain") || match.equalsIgnoreCase("part_match")) {
				status = (mail_data.indexOf(value) >= 0);
			} else if (match.equalsIgnoreCase("full_match")) {
				status = (mail_data.equalsIgnoreCase(value));
			}
		}
		this.Info("checking the rule for string match - match result:" + status);
		return status;
	}

	private boolean checkCondition(long size, String value, String match) {
		this.Info("checking the rule for size match - rule's size is :" + value
				+ "targert size:" + size);
		boolean status = false;
		double size_value = Double.parseDouble(value) * 1024 * 1024;// it is MB,
																	// but
		// don't know
		// the real mail
		// size from
		// stmpitem.

		if (size > 0) {
			if (match.equalsIgnoreCase("gte")) {
				status = (size >= size_value);
			} else if (match.equalsIgnoreCase("lte")) {
				status = (size <= size_value);
			}
		}

		this.Info("checking the rule for size match - match result:" + status);
		return status;
	}
	
	private String generateAuditLog(Rule rule) {
		StringBuffer audit_log = new StringBuffer();

		audit_log.append("该邮件命中规则的名称：").append(rule.getName())
				.append("。执行动作：");
		int action = rule.getActions();
		switch (action) {
		case Configuration.ACTION_LEVEL_ADD_TO_FILTER_QUEUE:
			audit_log.append("添加到过滤队列");
			break;
		case Configuration.ACTION_LEVEL_FORWARD:
			// 1. update status to(INPROGRESS = 1). 2. copy
			// mr_smtp_task_achieve, status(10). 3. copy from folder relay to
			// audit.
			String email = rule.getForward();
			int flag = rule.getFowardanddeliver();
			audit_log.append("转发到邮箱："+email);
			if(flag == 0){
				audit_log.append(" 不再投递");
			} else if(flag == 1){
				audit_log.append(" 并继续投递");
			}
			break;
		case Configuration.ACTION_LEVEL_ROMOVE:
			audit_log.append("丢弃");
			break;
		case Configuration.ACTION_LEVEL_REPORT:
			audit_log.append("上报管理中心");
			break;
		case Configuration.ACTION_LEVEL_SEND:
			audit_log.append("放行");
			break;
		default:
			audit_log.append("等待审核");
			break;
		}
		return audit_log.toString();

	}

	private void updateMailStatus(int status, Rule rule, String ruledesc,
			long tid, EmlItem smtpitem) {
		String p_ruledesc = "";
		long ruleid = rule.getId();
		int action = rule.getActions();
		String rulename = rule.getName();
		
		String update_sql = "update " + Configuration.MR_DB_TABLE_PREFIX
				+ "smtp_task set status=" + status + ", ruleid=" + ruleid
				+ ", action=" + action  + "' where id=" + tid;

		this.Update(Configuration.DBCONFIGURATION, update_sql);

		this.Info("auditor updateMailStatus:" + update_sql);
	}
	
	private int filter(EmlItem item){
		int result = 0;
		String mail_from = item.getSendfrom();
		String mail_to = item.getSendTo();
		String mail_domain = item.getDomain();
		String mail_todomain = item.getTodomain();
		String mail_title = item.getTitle();
		long mail_size = item.getMailsize();
		int hasAttachment = item.getHasattachment();
		String mail_path = item.getFullEmlpath();
		
		List<AttachmentItem> attachments = new ArrayList<AttachmentItem>();
		
		boolean got_mail_content = false;
		boolean got_mail_attachments = false;
		boolean got_mail_header = false;
		String mail_content = null;
		String mail_header = null;

		String matched_rule_str = null;
		Rule rule = null;
		boolean status = false;
		List<Rule> ruleList = Configuration.RuleList;

		Engine.getEngineLogger().log(Level.INFO,
				this.threadId + " -- process smtp item - " + item.getId()
						+ " - " + item.getSendfrom() + " - "
						+ item.getSendTo() + " - " + ruleList.size());
		String fieldName = null;
		String matchName = null;
		String valueName = null;
		for (int i = 0; i < ruleList.size(); i++) {
			rule = ruleList.get(i);
			String rulecontent = (rule == null) ? null : rule.getCondition();
			String[] condition_pieces = (rulecontent == null) ? null : rulecontent.split("[#]");
			boolean p_matched = false;

			Engine.getEngineLogger().log(Level.INFO,
					this.threadId + " -- process smtp item - " + item.getId()
							+ " - " + rulecontent);
			String field = null;
			String match = null;
			String value = null;
			if(condition_pieces != null) {
				if(condition_pieces.length == 2 && condition_pieces[0].equalsIgnoreCase("attachtype")) {
					field = condition_pieces[0];
					match = "full_match";
					value = condition_pieces[1];
				} else if(condition_pieces.length == 3 && !condition_pieces[0].equalsIgnoreCase("attachtype")) {
					field = condition_pieces[0];
					match = condition_pieces[1];
					value = condition_pieces[2];
				} else {
					Engine.getEngineLogger().log(Level.INFO,
							this.threadId + " -- failed to parser rule ");
					continue;
				}
			}
			valueName = value;
			if(match.equalsIgnoreCase("contain") || match.equalsIgnoreCase("part_match")) {
				matchName =  "包含";
			} else if(match.equalsIgnoreCase("not_contain") || match.equalsIgnoreCase("not_match")) {
				matchName =  "不包含";
			} else if(match.equalsIgnoreCase("full_match")) {
				matchName =  "等于";
			} else if(match.equalsIgnoreCase("gte")) {
				matchName =  "大于或者等于";
			} else if(match.equalsIgnoreCase("lte")) {
				matchName =  "小于或者等于";
			}
			if(field.equalsIgnoreCase("sender")) {
				status = checkCondition(mail_from, match, value);
				fieldName = "发信人";
			} else if(field.equalsIgnoreCase("reveiver")) {
				status = checkCondition(mail_to, match, value);
				fieldName = "收信人";
			} else if(field.equalsIgnoreCase("senddomain")) {
				status = checkCondition(mail_domain, match, value);
				fieldName = "发送人所在域";
			} else if(field.equalsIgnoreCase("recedomain")) {
				status = checkCondition(mail_todomain, match, value);
				fieldName = "收信人所在域";
			} else if(field.equalsIgnoreCase("subject")) {
				status = checkCondition(mail_title, match, value);
				fieldName = "信件标题";
			} else if(field.equalsIgnoreCase("mailheader")) {
				if(!got_mail_header) {
					mail_header = getHeader(mail_path);
					got_mail_header = true;
				}
				status = checkCondition(mail_header, match, value);
				fieldName = "信件头";
			} else if(field.equalsIgnoreCase("fulltext")) {
				status = checkCondition(mail_title, match, value);
				if(!status) {
					if(!got_mail_header) {
						mail_header = getHeader(mail_path);
						got_mail_header = true;
					}
					status = checkCondition(mail_header, match, value);
				}
				if(!status) {
					if(!got_mail_content) {
						mail_content = getContents(mail_path);
						got_mail_content = true;
					}
					status = checkCondition(mail_content, match, value);
				}
				if(!status && hasAttachment == 1) {
					if(!got_mail_attachments) {
						got_mail_attachments = true;
						got_mail_attachments = this.getAttachments(
								mail_path, attachments);
					}
					for(int j = 0; j < attachments.size(); j++) {
						String name = attachments.get(j).getAttachmentnames();
						status = checkCondition(name, match, value);
						if(status) {
							break;
						}
					}
				}
			} else if(field.equalsIgnoreCase("content")) {
				if(!got_mail_content) {
					mail_content = getContents(mail_path);
					got_mail_content = true;
				}
				status = checkCondition(mail_content, match, value);
				fieldName = "信件内容";
			} else if(field.equalsIgnoreCase("mailsize")) {
				status = checkCondition(mail_size, match, value);
				fieldName = "信件大小";
			} else if(field.equalsIgnoreCase("attachname") && hasAttachment == 1) {
				if(!got_mail_attachments) {
					got_mail_attachments = true;
					got_mail_attachments = this.getAttachments(
							mail_path, attachments);
				}
				for(int j = 0; j < attachments.size(); j++) {
					String name = attachments.get(j).getAttachmentnames();
					status = checkCondition(name, match, value);
					if(status) {
						break;
					}
				}
				fieldName = "附件名";
			} else if(field.equalsIgnoreCase("attachsize") && hasAttachment == 1) {
				if(!got_mail_attachments) {
					got_mail_attachments = true;
					got_mail_attachments = this.getAttachments(
							mail_path, attachments);
				}
				for(int j = 0; j < attachments.size(); j++) {
					long size = attachments.get(j).getActn_len();
					status = checkCondition(size, match, value);
					if(status) {
						break;
					}
				}
				fieldName = "附件大小";
			} else if(field.equalsIgnoreCase("attachtype") && hasAttachment == 1) {
				if(!got_mail_attachments) {
					got_mail_attachments = true;
					got_mail_attachments = this.getAttachments(
							mail_path, attachments);
				}
				for(int j = 0; j < attachments.size(); j++) {
					String type = attachments.get(j).getAttachmenttype();
					status = checkCondition(type, match, value);
					if(status) {
						break;
					}
				}
				fieldName = "附件类型";
			}
			if(status)
				break;
		}
		if(status) {
			Engine.getEngineLogger().log(Level.INFO, "item id:" + item.getId() + "match the rule:" + rule.getId());
			StringBuilder sb = new StringBuilder();
			String log = sb.append("该邮件命中规则:").append(rule.getId()).append(" 因为").append(fieldName).append(matchName).append(valueName).toString();
			int action = rule.getActions();
			result = 1;
		}
		return result;
	}
	
	/*
	 * return -1 if met error, return 0 if no spam and virus, return 1 if spam or virus.
	 */
	private int spam(EmlItem item){
		Engine.getEngineLogger().log(Level.INFO, "start to spam item:" + item.getId());
		String toDomain = item.getTodomain();
		Policy policy = null;
		if(toDomain != null) {
			policy = Configuration.PolicyMap.get(toDomain);
			if(policy == null) {
				policy = Configuration.PolicyMap.get("*");
			}
		}
		
		if(policy == null) {
			Engine.getEngineLogger().log(Level.INFO, "no spam policy found for item:" + item.getId());
			return 0;
		}
		
		SocketClient spamClient = new SocketClient(Configuration.SPAM_HOST, Configuration.LGSPAM_PORT);
		String spamRes = null;
		String clamavRes = null;
		if(spamClient.connect()) {
			try {
				String command = new StringBuilder("score ").append(item.getFullEmlpath()).append("\n").toString();
				spamClient.send(command, "utf8");
				Engine.getEngineLogger().log(Level.INFO, "send out command to "+ Configuration.SPAM_HOST + Configuration.LGSPAM_PORT +" with command:" + command);
			} catch (Exception e) {
				Engine.getEngineLogger()
				.log(Level.INFO, this.threadId + " -- meet error when send command to spam", e);
				spamClient.close();
				return -1;
			}
			try {
				spamRes = spamClient.receive("utf8");
				Engine.getEngineLogger().log(Level.INFO, "response from spam:" + spamRes);
			} catch (Exception e) {
				Engine.getEngineLogger()
				.log(Level.INFO,this.threadId+ " -- meet error when get response from spam",e);
				spamClient.close();
				return -1;
			}
			try {
				spamClient.send("quit\n", "utf8");
				Engine.getEngineLogger().log(Level.INFO, "send quit to spam");
			} catch (Exception e) {
				Engine.getEngineLogger()
				.log(Level.INFO,this.threadId + " -- meet error when send quit command to spam",e);
			}
			spamClient.close();
		} else {
			spamClient.close();
			return -1;
		}
		
		if(spamRes != null) {
			if(spamRes.toLowerCase().contains("good")) {
				return 0;
			} else {
				//TODO it is spam, do next.
				return 1;
			}
		} else {
			return -1;
		}
	}
	
	private int clamav(EmlItem item) {
		Engine.getEngineLogger().log(Level.INFO, "start clamav scan:" + item.getId());
		String toDomain = item.getTodomain();
		Policy policy = null;
		if(toDomain != null) {
			policy = Configuration.PolicyMap.get(toDomain);
			if(policy == null) {
				policy = Configuration.PolicyMap.get("*");
			}
		}
		
		if(policy == null) {
			Engine.getEngineLogger().log(Level.INFO, "no clamav policy found for item:" + item.getId());
			return 0;
		}
		String clamavRes = null;
		SocketClient clamavClient = new SocketClient(Configuration.SPAM_HOST, Configuration.CLAMD_PORT);
		if(clamavClient.connect()) {
			String command = new StringBuilder("SCAN ").append(item.getFullEmlpath()).append("\n").toString(); 
			try {
				clamavClient.send(command, "utf8");
			} catch (Exception e) {
				Engine.getEngineLogger()
				.log(Level.INFO, this.threadId + " -- meet error when send command to clamav",e);
				clamavClient.close();
				return -1;
			}
			try {
				clamavRes = clamavClient.receive("utf8");
				Engine.getEngineLogger().log(Level.INFO, "response from clamav:" + clamavRes);
			} catch (Exception e) {
				Engine.getEngineLogger()
				.log(Level.INFO,this.threadId + " -- meet error when get response from clamav",e);
				clamavClient.close();
				return -1;
			}
			clamavClient.close();
		} else {
			clamavClient.close();
			return -1;
		}
		
		if(clamavRes != null) {
			if(clamavRes.toLowerCase().contains("ok")) {
				return 0;
			} else {
				//TODO find virus by clamav. do next
				return 1;
			}
		} else {
			return -1;
		}
	}

	@Override
	public void run() {
		try {
			boolean result = this.EstablishMysqlConnection(
					Configuration.DBCONFIGURATION, false);
			if (!result) {
				Engine.getEngineLogger().log(Level.WARNING,
						this.threadId + " -- Fail to connect mysql database");
				return;
			} else {
				Engine.getEngineLogger().log(Level.INFO,
						this.threadId + " -- Started. the status is:" + this.running);
			}

			while (this.running) {
				EmlItem item = null;
				this.workingItemId = 0;
				try {
					item = this.taskItems.take();
				} catch (InterruptedException ex) {
					Engine.getEngineLogger()
							.log(Level.INFO, this.threadId + " -- meet InterruptedException", ex);

					if (!this.running) {
						break;
					}
				} catch (Exception ex) {
					Engine.getEngineLogger()
					.log(Level.WARNING, this.threadId + " -- Fail to take item from basket", ex);
				}
				Engine.getEngineLogger().log(Level.INFO,
						this.threadId + " start working on " + item.getId());

				this.workingItemId = item.getId();

				String emlpath_str = item.getEmlpath();
				
				String[] order = Configuration.process_order;
				int ret = 0;
				for(String value : order){
					if(ret == 1)
						break;
					if(value.contentEquals("spam")) {
						ret = spam(item);
						if(ret == -1)
							ret = spam(item);
					} else if(value.contentEquals("clamav")) {
						ret = clamav(item);
						if(ret == -1)
							ret = clamav(item);
					} else if(value.contentEquals("spam")) {
						ret  = filter(item);
					}
				}
				Engine.getEngineLogger().log(Level.INFO,
						this.threadId + " stop working on " + item.getId());

				if (Configuration.THREADS_INTERVAL > 0) {
					try {
						Thread.sleep(Configuration.THREADS_INTERVAL);
					} catch (Exception e) {
						Engine.getEngineLogger().log(Level.INFO,
								this.threadId + " -- sleep interval error", e);
					}
				}
			}

			this.TerminateMysqlConnection(Configuration.DBCONFIGURATION);

			Engine.getEngineLogger().log(Level.INFO,
					this.threadId + " -- Stopped");

		} catch (Error err) {
			Engine.getEngineLogger().log(Level.WARNING,
					this.threadId + " -- FtiBuildThread error", err);
			try {
				Thread.sleep(3000);
			} catch (Exception e) {
			}
			Engine.STOP = true;
		}
	}

	@Override
	public void RestartTasks() {
		// TODO Auto-generated method stub
		
	}
}
