package cn.com.aboobear.mailrelay.misc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;

public abstract class BaseThread implements Runnable {

	public Connection connection = null;
	public List<BaseThread> subThreadItems = null;
	public boolean running = true;
	public String threadId = "thread";

	public void Info(String msg) {
	}

	public void Warn(String msg, Exception ex) {
	}

	// Establish MYSQL connection
	public boolean EstablishMysqlConnection(DBConfiguration config,
			boolean force) {
		if (this.connection == null || force) {
			if (this.connection != null && force) {
				this.TerminateMysqlConnection(config);
			}

			if (force) {
				config.getLogger().log(Level.WARNING,
						this.threadId + " -- Re-establish mysql connection");
			}

			try {
				Class.forName("org.gjt.mm.mysql.Driver");
				this.connection = DriverManager.getConnection(
						config.getDBURL(), config.getDBUSERNAME(),
						config.getDBPASSWORD());
			} catch (Exception sqlex) {
				config.getLogger().log(Level.SEVERE,
						this.threadId + " -- Establish mysql connection error",
						sqlex);
				this.connection = null;
			}
		}
		if (this.connection == null) {
			return false;
		} else {
			return true;
		}
	}

	// Update
	public void Update(DBConfiguration config, String sql) {
		try {
			this.UpdateWithException(config, sql);
		} catch (Exception e) {
			config.getLogger().log(Level.WARNING,
					threadId + " -- Execute mysql update error, will retry");

			this.EstablishMysqlConnection(config, true);
			try {
				this.UpdateWithException(config, sql);
			} catch (Exception ex) {
				config.getLogger()
						.log(Level.WARNING,
								threadId
										+ " -- Execute mysql update error, fail again",
								ex);
			}
		}
	}

	public void UpdateWithException(DBConfiguration config, String sql)
			throws Exception {
		Statement statement = null;

		if (this.connection == null) {
			throw new Exception("communication error");
		}

		try {
			statement = this.connection.createStatement();
			statement.execute(sql);
			// config.getLogger().log(Level.INFO, threadId +
			// " -- Execute mysql update: " + sql);
		} catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException ex1) {
			throw ex1;
		} catch (com.mysql.jdbc.CommunicationsException ex2) {
			throw ex2;
		} catch (Exception e) {
			if (e.getMessage().indexOf("Duplicate entry") >= 0) {
				config.getLogger()
						.log(Level.WARNING,
								threadId
										+ " -- Execute mysql update Duplicate entry error: "
										+ sql);
			} else {
				config.getLogger().log(Level.WARNING,
						threadId + " -- Execute mysql update error: " + sql, e);
			}

		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					// e.printStackTrace();
				}
			}
		}
	}

	public DbResult Query(DBConfiguration config, String sql) {
		DbResult dbresult = null;
		try {
			dbresult = this.QueryWithException(config, sql);
		} catch (Exception e) {
			config.getLogger().log(Level.WARNING,
					threadId + " -- Execute mysql query error, will retry");

			this.EstablishMysqlConnection(config, true);
			try {
				dbresult = this.QueryWithException(config, sql);
			} catch (Exception ex) {
				dbresult = null;
				config.getLogger().log(Level.WARNING,
						threadId + " -- Execute mysql query error, fail again",
						ex);
			}
		}
		return dbresult;
	}

	public DbResult QueryWithException(DBConfiguration config, String sql)
			throws Exception {
		Statement statement = null;
		ResultSet resultSet = null;
		DbResult dbresult = null;
		if (sql == null || sql.length() <= 0) {
			return null;
		}

		if (this.connection == null) {
			throw new Exception("communication error");
		}

		try {
			statement = this.connection.createStatement();
			resultSet = statement.executeQuery(sql);
			dbresult = new DbResult(statement, resultSet);
			// config.getLogger().log(Level.INFO, threadId +
			// " -- Execute mysql query: " + sql);
		} catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException ex1) {
			throw ex1;
		} catch (com.mysql.jdbc.CommunicationsException ex2) {
			throw ex2;
		} catch (Exception e) {
			config.getLogger().log(Level.WARNING,
					threadId + " -- Execute mysql query error: " + sql, e);
		}
		return dbresult;
	}

	// Batch Update
	public void BatchUpdate(DBConfiguration config, List<String> sql_list) {
		try {
			this.BatchUpdateWithException(config, sql_list);
		} catch (Exception e) {
			config.getLogger()
					.log(Level.WARNING,
							threadId
									+ " -- Execute mysql batch update error, will retry");

			this.EstablishMysqlConnection(config, true);
			try {
				this.BatchUpdateWithException(config, sql_list);
			} catch (Exception ex) {
				config.getLogger()
						.log(Level.WARNING,
								threadId
										+ " -- Execute mysql batch update error, fail again",
								ex);
			}
		}
	}

	public void BatchUpdateWithException(DBConfiguration config,
			List<String> sql_list) throws Exception {
		Statement statement = null;
		int[] results = null;
		String sql_piece = null;
		StringBuffer sqlbuf = new StringBuffer();

		if (sql_list == null || sql_list.size() <= 0) {
			return;
		}

		if (this.connection == null) {
			throw new Exception("communication error");
		}

		try {
			statement = this.connection.createStatement();
			for (int i = 0; i < sql_list.size(); i++) {
				sql_piece = sql_list.get(i);
				if (sql_piece != null && sql_piece.length() > 0) {
					statement.addBatch(sql_piece);
					if (config.getLoglevel() == Level.INFO) {
						sqlbuf.append(sql_piece).append("\r\n");
					}
				}
			}
			results = statement.executeBatch();
			if (config.getLoglevel() == Level.INFO) {
				sqlbuf.append("Results: ");
				if (results != null) {
					for (int i = 0; i < results.length; i++) {
						sqlbuf.append(i).append("|");
					}
				}
				sqlbuf.append("\r\n");
				// config.getLogger().log(Level.INFO, this.threadId +
				// " -- Batch sql execution -- sql: " + sqlbuf.toString());
			}
		} catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException ex1) {
			throw ex1;
		} catch (com.mysql.jdbc.CommunicationsException ex2) {
			throw ex2;
		} catch (Exception e) {
			config.getLogger().log(
					Level.WARNING,
					threadId + " -- Execute mysql update error: "
							+ sqlbuf.toString(), e);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					// e.printStackTrace();
				}
			}
		}
	}

	// Terminate MYSQL connection
	public void TerminateMysqlConnection(DBConfiguration config) {
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (SQLException sqlex) {
				config.getLogger().log(Level.SEVERE,
						this.threadId + " -- Terminate mysql connection error",
						sqlex);
			}
			this.connection = null;
		}
	}

	public abstract void RestartTasks();

	// stop all sub threads
	public void stopSubThreads() {
		if (this.subThreadItems != null) {
			BaseThread subthread = null;

			for (int i = 0; i < this.subThreadItems.size(); i++) {
				subthread = this.subThreadItems.get(i);
				subthread.setRunning(false);
			}
			this.subThreadItems.clear();
		}
	}

	// notify sub threads to quit
	public void setRunning(boolean running) {
		this.running = running;
	}

	// check remaining threads number
	public int countRemainItems() {
		return 0;
	}

	// check whether all sub threads had quit or not
	public int countRemainingTasks() {
		int remain_tasks_count = 0;
		for (int i = 0; i < this.subThreadItems.size(); i++) {
			BaseThread item = this.subThreadItems.get(i);
			int remains = item.countRemainItems();
			remain_tasks_count += remains;
		}
		return remain_tasks_count;
	}

	public Connection getConnection() {
		return this.connection;
	}
}
