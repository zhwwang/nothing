package cn.com.aboobear.spam;

import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.mail.Session;

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
	
	/*
	 * return -1 if met error, return 0 if no spam and virus, return 1 if spam or virus.
	 */
	private int SpamAndClam(EmlItem item){
		Engine.getEngineLogger().log(Level.INFO, "start to porcess item:" + item.getFullEmlpath());
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
				Engine.getEngineLogger().log(Level.INFO, "start clamav scan:" + spamRes);
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
					spamClient.close();
				} else {
					spamClient.close();
					return -1;
				}
			} else {
				//TODO it is spam, do next.
				return 1;
			}
		} else {
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
				
				int ret = SpamAndClam(item);
				if(ret == -1)
					ret = SpamAndClam(item);
				
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
