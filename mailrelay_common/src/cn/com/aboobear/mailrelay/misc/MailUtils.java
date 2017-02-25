package cn.com.aboobear.mailrelay.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringEscapeUtils;

public class MailUtils {

	public static final String SEPARATOR = System.getProperty("file.separator");

	public static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public String operator = "audit";

	private static MailUtils uniqueInstance = null;
	private Session mailSession = null;

	private MailUtils() {
		Properties props = System.getProperties();
		props.put("mail.debug", "true");
		props.setProperty("mail.smtp.allow8bitmime", "false");
		mailSession = Session.getInstance(props);
	}

	public static MailUtils getInstance() {
		if (uniqueInstance == null) {
			uniqueInstance = new MailUtils();
		}
		return uniqueInstance;
	}

	public static String[] getNewEmlFilePath(String dirpath) {
		String path[] = null;
		if (dirpath == null) {
			return null;
		}
		path = new String[2];
		String uuid_file = UUID.randomUUID().toString();
		path[1] = uuid_file;
		path[0] = dirpath + MailUtils.SEPARATOR + path[1];
		return path;
	}

	public void setOperator(String oper) {
		this.operator = oper;
	}

	public boolean generateMail(String dbprefix, String from, String to,
			String subject, String body, String attachmentName,
			String attachmentPath, String dirpath, Connection connection,
			Logger logger) {
		boolean result = false;
		String tolist[] = to.split(";");
		for (int i = 0; i < tolist.length; i++) {
			if (tolist[i] == null && tolist[i].length() == 0) {
				continue;
			}
			try {
				MimeMessage new_msg = new MimeMessage(mailSession);

				new_msg.setFrom(new InternetAddress(from));
				InternetAddress[] address = { new InternetAddress(tolist[i]) };
				new_msg.setRecipients(Message.RecipientType.TO, address);
				if (subject == null) {
					subject = "ϵͳ֪ͨ";
				}

				new_msg.setSubject(subject);

				Multipart mp = new MimeMultipart();
				MimeBodyPart mbpContent = new MimeBodyPart();
				if (body == null) {
					mbpContent.setText(" ");
				} else {
					mbpContent.setText(body);
				}
				mbpContent.setHeader("Content-Type",
						"text/html;charset=\"UTF-8\"");
				mp.addBodyPart(mbpContent);
				boolean hasattachment = false;

				if (attachmentName != null && attachmentPath != null) {
					MimeBodyPart mbpFile = new MimeBodyPart();
					FileDataSource fds = new FileDataSource(attachmentPath);
					mbpFile.setDataHandler(new DataHandler(fds));
					mbpFile.setFileName(attachmentName);
					mp.addBodyPart(mbpFile);
					hasattachment = true;
				}

				Date nowdate = new Date();
				new_msg.setContent(mp);
				new_msg.setSentDate(new Date());

				String[] rmfilepath = MailUtils.getNewEmlFilePath(dirpath);
				if (rmfilepath != null) {
					File rmemlFile = new File(rmfilepath[0]);
					rmemlFile.createNewFile();

					new_msg.writeTo(new FileOutputStream(rmemlFile));
					long rm_mailsize = rmemlFile.length();

					String from_pieces[] = from.split("@");
					String from_domain = "";
					if (from_pieces.length > 1) {
						from_domain = from_pieces[1].trim();
					}
					String to_pieces[] = tolist[i].split("@");
					String to_domain = "";
					if (to_pieces.length > 1) {
						to_domain = to_pieces[1].trim();
					}

					String nowdate_str = SystemUtils.Format(
							MailUtils.dateFormat, nowdate);
					String new_msg_sql = "INSERT INTO "
							+ dbprefix
							+ "smtp_task(type, status, overseas, hasattachment, isreturn, hasreturn, inqueuetime, mailtime, runtime, mailsize, "
							+ "ruletype, ruleid, taskid, groupid, action, secaction, retries, total, domain, todomain, sendfrom, emldir, emlpath, title, srcIP, msgid, forward"
							+ ", reason, operator) VALUES (4, 1, 0, "
							+ hasattachment + ", 0, 0, '" + nowdate_str + "','"
							+ nowdate_str + "','" + nowdate_str + "',"
							+ rm_mailsize + ",0,0,0,0,0,0,0,1,'" + from_domain
							+ "','" + to_domain + "','" + from + "','"
							+ dirpath + "','" + rmfilepath[1] + "','"
							+ StringEscapeUtils.escapeSql(subject)
							+ "','127.0.0.1','','"
							+ StringEscapeUtils.escapeSql(tolist[i])
							+ ":0','','" + this.operator + "')";

					if (logger != null) {
						logger.log(Level.INFO, "MailUtils-generateMail sql:"
								+ new_msg_sql);
					} else {
						System.out.println("MailUtils-generateMail sql:"
								+ new_msg_sql);
					}

					Statement statement = null;

					try {
						statement = connection.createStatement();
						statement.execute(new_msg_sql);
						result = true;
					} catch (Exception ex) {
						if (logger != null) {
							logger.log(Level.WARNING,
									"MailUtils-generateMail DB exception - dir:"
											+ rmfilepath[0] + ";sql:"
											+ new_msg_sql, ex);
						} else {
							System.out
									.println("MailUtils-generateMail DB exception - dir:"
											+ rmfilepath[0]
											+ ";sql:"
											+ new_msg_sql);
							ex.printStackTrace();
						}
						result = false;
					}
				}
			} catch (Exception ex) {
				if (logger != null) {
					logger.log(Level.WARNING,
							"MailUtils-generateMail exception - from:" + from
									+ ";to:" + to + ";subject:" + subject, ex);
				} else {
					System.out
							.println("MailUtils-generateMail exception - from:"
									+ from + ";to:" + to + ";subject:"
									+ subject);
					ex.printStackTrace();
				}
				result = false;
			}
		}
		return result;
	}
}
