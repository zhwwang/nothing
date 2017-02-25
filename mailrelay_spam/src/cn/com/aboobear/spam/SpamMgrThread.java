package cn.com.aboobear.spam;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import cn.com.aboobear.mailrelay.misc.BaseThread;
import cn.com.aboobear.mailrelay.misc.Constants;
import cn.com.aboobear.mailrelay.misc.DbResult;

public class SpamMgrThread extends BaseThread {
	private int threadNumber = 10;
	private ExecutorService taskService = null;
	private BlockingQueue<EmlItem> taskItems = null;
	private long[] workingItemList = null;

	public static String RETRIEVE_TASK_SQL = "select id from mr_smtp_task where status=" + Constants.MAIL_WAIT_SCAN;
	
	public SpamMgrThread() throws Exception {
		this.threadId = "SpamMgrThread";
		this.threadNumber = Configuration.THREADS_NUMBER;
		
		this.taskService = Executors.newFixedThreadPool(this.threadNumber);
		this.taskItems = new LinkedBlockingQueue<EmlItem>(
				2 * Configuration.FETCH_TASK_NUMBER);
		
		this.subThreadItems = new ArrayList<BaseThread>();

		this.workingItemList = new long[this.threadNumber];

		for (int ip_num = 0; ip_num < this.threadNumber; ip_num++) {
			this.workingItemList[ip_num] = 0;
		}
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

	public void StartTasks() {
		this.stopSubThreads();
		int tindex = 0;

		for (int j = 1; j <= this.threadNumber; j++) {
			tindex = j;
			SpamThread pt = new SpamThread(tindex, this.taskItems);
			pt.setMgrThread(this);
			this.taskService.submit(pt);
			this.subThreadItems.add(pt);
		}
	}

	public void StopTasks() {
		this.stopSubThreads();
		this.taskService.shutdownNow();
		for (int i=0; i < this.subThreadItems.size(); i++) {
			SpamThread pt = (SpamThread)this.subThreadItems.get(i);
			if (pt != null) {
				pt.close();
			}
		}
		this.close();
		this.taskItems.clear();
	}

	private void loadWorkingItemIds() {
		if (this.subThreadItems != null) {
			SpamThread subthread = null;
			for (int i = 0; i < this.subThreadItems.size(); i++) {
				subthread = (SpamThread) this.subThreadItems.get(i);
				this.workingItemList[i] = subthread.getWorkingItemId();
			}
		}
	}

	private boolean searchInWorkingItemIds(long pid) {
		for (int i = 0; i < this.workingItemList.length; i++) {
			if (this.workingItemList[i] != 0
					&& (this.workingItemList[i] == pid)) {
				return true;
			}
		}
		return false;
	}
	
	public void retrieveTasks(int tcount) {
		try {
			int index = 0;
			int skip = 0;
			boolean used = false;
			long pid = 0;
			String sql = null;

			sql = RETRIEVE_TASK_SQL;

			this.loadWorkingItemIds();

			DbResult dbresult = this.Query(Configuration.DBCONFIGURATION, sql);

			if (dbresult == null) {
				return;
			}

			ResultSet resultSet = dbresult.getResultSet();

			while (resultSet != null && resultSet.next()) {
				pid = resultSet.getLong("id");
				used = this.searchInWorkingItemIds(pid);

				if (!used) {
					EmlItem nitem = new EmlItem();
					nitem.setId(pid);

					this.taskItems.put(nitem);
				} else {
					skip++;
				}

				index++;
			}

			if (Configuration.LOGLEVEL == Level.INFO) {
				Engine.getEngineLogger().log(
						Level.INFO,
						this.threadId + " -- RETRIEVE_TASK_SQL: " + sql
								+ ", count: " + index + ", skip: " + skip);
			}

			if (dbresult != null) {
				dbresult.close();
			}
		} catch (Exception ex) {
			Engine.getEngineLogger().log(Level.WARNING,
					this.threadId + " -- retrieveTasks -- Exception", ex);
		}
	}
	
	@Override
	public void run() {
		int tcount = 0;
		int term = 0;
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

		Engine.getEngineLogger().log(Level.WARNING,
				this.threadId + " -- RETRIEVE_TASK_SQL:" + RETRIEVE_TASK_SQL);

		this.StartTasks();

		int rcount = (Configuration.ALIVE_DURATION * 60 * 1000)
				/ Configuration.FETCH_TASK_INTERVAL;

		while (this.running) {
			tcount = this.taskItems.size();
			if (tcount == 0) {
				this.retrieveTasks(tcount);
			}
			try {
				Thread.sleep(Configuration.FETCH_TASK_INTERVAL);
			} catch (Exception e) {
			}
			
			term++;
			if (term > rcount) {
				break;
			}
		}

		this.running = false;

		this.StopTasks();

		Engine.STOP = true;

		this.TerminateMysqlConnection(Configuration.DBCONFIGURATION);

		Engine.getEngineLogger().log(Level.INFO, this.threadId + " -- Stopped");
	}

	@Override
	public void RestartTasks() {
		// TODO Auto-generated method stub
		
	}

}
