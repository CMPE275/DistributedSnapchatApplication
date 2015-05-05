package snapchatproto.servers.logging;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import snapchatproto.servers.logging.LogData;

public class LogList {

	protected static AtomicReference<LogList> instance = new AtomicReference<LogList>();
	ArrayList<LogData> logListEntries;
	int latestMyTerm = -1;
	int latestMyIndex = -1;
	
	public int getLatestMyTerm() {
		return latestMyTerm;
	}

	public void setLatestMyTerm(int latestMyTerm) {
		this.latestMyTerm = latestMyTerm;
	}

	public int getLatestMyIndex() {
		return latestMyIndex;
	}

	public void setLatestMyIndex(int latestMyIndex) {
		this.latestMyIndex = latestMyIndex;
	}


	public ArrayList<LogData> getLogListEntries() {
		return logListEntries;
	}

	public void setLogListEntries(ArrayList<LogData> logListEntries) {
		this.logListEntries = logListEntries;
	}

	public LogList() {
		logListEntries = new ArrayList<LogData>();
	}

	public static LogList initManager() {
		instance.compareAndSet(null, new LogList());
		return instance.get();
	}

	public static LogList getInstance() {
		return instance.get();
	}

	public void clear() {
		logListEntries.clear();
	}

}
