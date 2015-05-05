package snapchatproto.servers.logging;

public class LogData {

	int term, index;
	String command;
	
	public LogData(){
		
	}
	
	public LogData(int term, int index, String command){
		this.term = term;
		this.index = index;
		this.command = command;
	}

	public int getTerm() {
		return term;
	}

	public void setTerm(int term) {
		this.term = term;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

}
