package snapchatproto.servers.managers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.core.RaftMgmt;
import snapchatproto.core.RaftMgmt.LogList;
import snapchatproto.servers.logging.LogData;
import snapchatproto.util.DatabaseUtil;


public class LogListManager {

	protected static Logger logger = LoggerFactory.getLogger("Log manager");

	private Connection connect = null;
	private Statement statement = null;

	protected static AtomicReference<LogListManager> instance = new AtomicReference<LogListManager>();

	public static LogListManager initManager() {
		instance.compareAndSet(null, new LogListManager());
		return instance.get();
	}

	public static LogListManager getInstance() {
		return instance.get();
	}

	public void replicateLogs(LogList incominglogList){
		snapchatproto.servers.logging.LogList mylogList = snapchatproto.servers.logging.LogList.getInstance();

		if(incominglogList.getLogEntryCount() != mylogList.getLogListEntries().size()){
			mylogList.clear();

			// delete all entries from LogList.getInstance the add new data
			for(int i=0; i < incominglogList.getLogEntryCount(); i++){
				LogData logData = new LogData(incominglogList.getLogEntry(i).getTerm(),incominglogList.getLogEntry(i).getLogIndex(), "ADD");
				snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().add(logData);	
			}

			replicateDB(incominglogList);

			logger.info("Inconsistency of logs removed..");
			printLogs();
			//call append_ack method

		}else {
			logger.info("Logs are consistent..");
		}

	}

	public RaftMgmt.LogList.Builder embedLogs(){

		logger.info("Size of loglist in embed logs: "+snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().size());

		RaftMgmt.LogEntry.Builder logEntrybuilder = RaftMgmt.LogEntry.newBuilder();
		RaftMgmt.LogList.Builder logListbuilder = RaftMgmt.LogList.newBuilder();

		for(int i=0; i < snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().size(); i++){
			logEntrybuilder.setLogIndex(snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().get(i).getIndex());
			logEntrybuilder.setTerm(snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().get(i).getTerm());

			logListbuilder.addLogEntry(logEntrybuilder.build());
		}

		logger.info("********Msg Builder data while sending logs***********");
		int j = logListbuilder.getLogEntryCount();
		while(j > 0){
			logger.info("Index: "+logListbuilder.getLogEntry(--j).getLogIndex()+" || "+"Term: "+logListbuilder.getLogEntry(j).getTerm());
		}

		return logListbuilder;

	}

	public void printLogs(){
		ArrayList<LogData> logList = snapchatproto.servers.logging.LogList.getInstance().getLogListEntries();	

		Iterator<LogData> itr =logList.iterator();

		logger.info("********** Logs replicated with follower **********");
		while(itr.hasNext()){	
			LogData logData = itr.next();
			logger.info("Index: "+ logData.getIndex() + " || Term: "+ logData.getTerm());
		}

	}


	// call when new request comes from client
	@SuppressWarnings({ "static-access"})
	public void addNewLogEntry()
	{

		logger.info("*************************in add new log entry  term: "+NodeDataManager.getInstance().getNodeData().getCurrentTerm()+"    index: "+snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().size());
		LogData logData = new LogData(NodeDataManager.getInstance().getNodeData().getCurrentTerm() ,
				snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().size() , "ADD");

		//snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().add(logData);


		ArrayList<LogData> logListEntries = snapchatproto.servers.logging.LogList.getInstance().getLogListEntries();

		logListEntries.add(logData);
		snapchatproto.servers.logging.LogList.getInstance().setLogListEntries(logListEntries);



		updateDB(NodeDataManager.getInstance().getNodeData().getCurrentTerm() , 
				snapchatproto.servers.logging.LogList.getInstance().getLogListEntries().size()-1);

	}


	public void updateDB(int term , int index)
	{

		try {
			logger.info("****** in update db **********");
			Class.forName("com.mysql.jdbc.Driver");
			logger.info("driver attached");
			connect = DriverManager.getConnection("jdbc:mysql://localhost/raftLog?"+ "user=root&password=root");
			logger.info("connection established");

			statement = connect.createStatement();
			logger.info("statement created");
			int insert;
			insert= statement.executeUpdate("insert into raftData values ( " + term + "," + index + ", 'add' );" );

			if(insert!=0){
				logger.info("query executed   insert into raftData values ( " + term + "," + index + ", 'add' );");
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				connect.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	public void replicateDB(LogList incominglogList)
	{
		try {
			connect = DatabaseUtil.getConnection();
			DatabaseUtil.replaceAllLogs(incominglogList);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				connect.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	// called when node goes down and comes up 
	public void getLogsFromDB()
	{

		try {
			connect = DatabaseUtil.getConnection();
			snapchatproto.servers.logging.LogList.getInstance().setLogListEntries(DatabaseUtil.getAllLogs());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
