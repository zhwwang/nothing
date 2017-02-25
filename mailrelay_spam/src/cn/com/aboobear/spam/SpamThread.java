package cn.com.aboobear.spam;

import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;

import javax.mail.Session;

import cn.com.aboobear.mailrelay.misc.BaseThread;
import cn.com.aboobear.mailrelay.misc.SocketClient;

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
	
	private void SpamAndClam(EmlItem item){
		String clamHost = new StringBuilder(Configuration.SPAM_HOST).append(":").append(Configuration.CLAMD_PORT).toString();
		SocketClient spamClient = new SocketClient(Configuration.SPAM_HOST, Configuration.SPAM_PORT);
		if(spamClient.connect()) {
			try {
				spamClient.send(new StringBuilder("score ").append(/*item.getFullEmlpath()).toString()*/ "/home/23/fd01a91c-4024-46c9-b404-3fb141545130").toString(), "utf8");
				Engine.getEngineLogger().log(Level.INFO, "send out command to spam" + Configuration.SPAM_HOST + ":" + Configuration.CLAMD_PORT);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				String res = spamClient.receive("utf8");
				Engine.getEngineLogger().log(Level.INFO, "response from spam:" + res);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Engine.getEngineLogger().log(Level.INFO, "error from spam");
			}
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
						this.threadId + " -- Started");
			}

			while (this.running) {
				EmlItem item = null;
				this.workingItemId = 0;
				try {
					item = this.taskItems.take();
					SpamAndClam(item);
				} catch (InterruptedException ex) {
					Engine.getEngineLogger()
							.log(Level.INFO,
									this.threadId
											+ " -- meet InterruptedException",
									ex);

					if (!this.running) {
						break;
					}
				} catch (Exception ex) {
					Engine.getEngineLogger()
							.log(Level.WARNING,
									this.threadId
											+ " -- Fail to take item from basket",
									ex);
				}
				Engine.getEngineLogger().log(Level.INFO,
						this.threadId + " start working on " + item.getId());

				this.workingItemId = item.getId();

				String emlpath_str = item.getEmlpath();

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
