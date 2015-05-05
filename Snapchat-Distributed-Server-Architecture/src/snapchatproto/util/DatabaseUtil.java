package snapchatproto.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.core.RaftMgmt.LogList;
import snapchatproto.servers.logging.LogData;

public class DatabaseUtil {

	protected static Logger logger = LoggerFactory.getLogger("FileUtil");
	private static Connection connect;
	private static Statement statement;

	public DatabaseUtil() {
	}

	public static Connection getConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			logger.debug("driver attached");
			connect = DriverManager.getConnection("jdbc:mysql://localhost/raftLog?"+ "user=root&password=root");
			logger.debug("connection established");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return connect;
	}


	@SuppressWarnings("null")
	public static ArrayList<LogData> getAllLogs() {
		ArrayList<LogData> logListEntries = null;
		try {
			statement = connect.createStatement();
			ResultSet rs = statement.executeQuery("select * from raftData;");

			if(rs!=null){
				while(rs.next())
				{
					LogData logData = new LogData( rs.getInt("term_entry"), rs.getInt("index_entry") , "add");
					logListEntries.add(logData);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return logListEntries;
	}
	
	
	public static void replaceAllLogs(LogList incominglogList) {
		try {
			statement = connect.createStatement();	
			statement.execute("delete from raftData;" );
			
			logger.info("*********delete query executed");

			logger.info("incominglogList.getLogEntryCount() in replicateDB: "+incominglogList.getLogEntryCount());

			for(int i=0; i < incominglogList.getLogEntryCount(); i++){
				logger.info("*********Inserting data");
				statement.execute("insert into raftData values ( " + incominglogList.getLogEntry(i).getTerm() + "," + incominglogList.getLogEntry(i).getLogIndex() + ", 'add' );" );
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

