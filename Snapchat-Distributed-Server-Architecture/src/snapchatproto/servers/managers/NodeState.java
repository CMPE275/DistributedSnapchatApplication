package snapchatproto.servers.managers;

import snapchatproto.core.RaftMgmt.ServerCommunicationMessage;


public interface NodeState {

	public void repondToVoteRequest(ServerCommunicationMessage msg);
	
	public void repondToCastVote(ServerCommunicationMessage msg);
	
	public void respondToLeaderElected(ServerCommunicationMessage msg);
	
	public void appendLogs(ServerCommunicationMessage msg);
	
	public void replicateLogs(ServerCommunicationMessage msg);
	
	public void respondToTheLeaderIs(ServerCommunicationMessage msg);
	
	public void respondToWhoIsTheLeader(ServerCommunicationMessage msg);
	
	public void processAckAppendMessage(ServerCommunicationMessage msg);
	
}
