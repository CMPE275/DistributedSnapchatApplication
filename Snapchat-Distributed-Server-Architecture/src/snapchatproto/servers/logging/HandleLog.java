/*package snapchatproto.servers.logging;

import snapchatproto.core.Mgmt.Management;
import snapchatproto.servers.management.ManagementQueue;
import snapchatproto.servers.management.ManagementQueue.ManagementQueueEntry;

import java.sql.*;

public class HandleLog {

	LogList log ;
	
	  private Connection connect = null;
	  private Statement statement = null;
	  private ResultSet resultSet = null;
	
	
	
	public HandleLog()
	{
			try {
				log = new LogList();
				Class.forName("com.mysql.jdbc.Driver");
				System.out.println("driver attached");
				connect = DriverManager.getConnection("jdbc:mysql://localhost/raftLog?"+ "user=root&password=root");
				System.out.println("connection established");
				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		
	}

	// if leader election is started, leader requesting vote msg comes
	
	public boolean voteIfLogConsistent(int term, int index)
	{
		
		if(term < log.getLogList()[log.returnLatest()].getTerm() || (term == log.getLogList()[log.returnLatest()].getTerm() && index < log.getLogList()[log.returnLatest()].getIndex()))
		{
			return false;
		}
		return true;
	}

	// check log consistency.
	public boolean checkLOg(int term , int index) {
		boolean flg = true;
		try {
			
			if (term == log.getLogList()[log.returnLatest()].getTerm()
					&& index == log.getLogList()[log.returnLatest()].getIndex())

				flg = true;
			else
				flg =  false;

		} catch (Exception e) {
			
			e.printStackTrace();

		}

		return flg;
	}
	
	//update log when msg for append comes from leader
	void updateLog(int term , int index , String command)
	{
		if(checkLOg(term , index))
		{
		
		log.addNode(index, term , command);
		updateEntryInDB(index, term , command) ;
		
		}
		else
		{
			// edit this
			copyConsistentLog();
		}
		
	}
	
	//make logs consistent
	void copyConsistentLog()
	{
		
		int i=0;
		
		snapchatproto.core.Mgmt.LogList log1 =  mgmt.getLogList();
		while(log1.getLogEntry(i)!=null)
		{
			log.updateList(log1.getLogEntry(i).getLogIndex(), log1.getLogEntry(i).getTerm() , log1.getLogEntry(i).getCommand());
			
		}
	}
	
	//  while sending message
	public int getLatestIndex()
	{
		int latestIndex = log.getLogList()[log.getLatest()].getIndex();
		return latestIndex;
	}
	
	// while sending message
	public int getLatestTerm()
	{
		int latestTerm = log.getLogList()[log.getLatest()].getTerm();
		return latestTerm;
	}
	
	
	
	// update new entry in table
	
	public void updateEntryInDB(int index , int term , String command)
	{
		try{
			statement = connect.createStatement();
			System.out.println("statement created");
			boolean insert= statement.execute("insert into raftData values ( " + term + "," + index + ", '"+command+"' );" );
			System.out.println("query executed   :  "+resultSet);
	      
		} catch(Exception e)
		
		{
			e.printStackTrace();
		}
	}
	
	// change all data from table raftData when log is inconsistent.
	public void updateLogTableInDB(int index , int term , String command)
	{
		try{
			statement = connect.createStatement();
			System.out.println("statement created");
			boolean delete= statement.execute("delete from raftData;" );
			System.out.println("delete query executed");
			
			int i=0;
			boolean insert;
	
			while(log1.getLogEntry(i)!=null)
			{
					insert= statement.execute("insert into raftData values ( " + term + "," + index + ", 'add' );" );
				
			}
	      
		} catch(Exception e)
		
		{
			e.printStackTrace();
		}
	}
	
	
	
	

	
}
*/