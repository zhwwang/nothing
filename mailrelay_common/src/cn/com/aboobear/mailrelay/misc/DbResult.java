package cn.com.aboobear.mailrelay.misc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbResult {
	public Statement statement = null;
	public ResultSet resultSet = null;

	public DbResult(Statement pstatement, ResultSet presultSet) {
		this.statement = pstatement;
		this.resultSet = presultSet;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public void setResultSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	public void close() {
		if (this.resultSet != null) {
			try {
				this.resultSet.close();
			} catch (SQLException e) {
				// e.printStackTrace();
			}
		}

		if (this.statement != null) {
			try {
				this.statement.close();
			} catch (SQLException e) {
				// e.printStackTrace();
			}
		}
	}
}
