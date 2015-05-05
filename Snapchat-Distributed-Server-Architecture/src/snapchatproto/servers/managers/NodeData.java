package snapchatproto.servers.managers;

public class NodeData {
	private RaftStatus nodeStatus = RaftStatus.FOLLOWER;
	private String nodeId;
	private String votedFor;
	private int currentTerm;
	private long lastBeatReceivedFromLeader;
	
	public long getLastBeatReceivedFromLeader() {
		return lastBeatReceivedFromLeader;
	}

	public void setLastBeatReceivedFromLeader(long lastBeatReceivedFromLeader) {
		this.lastBeatReceivedFromLeader = lastBeatReceivedFromLeader;
	}

	public RaftStatus getNodeStatus() {
		return nodeStatus;
	}

	public void setNodeStatus(RaftStatus nodeStatus) {
		this.nodeStatus = nodeStatus;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getVotedFor() {
		return votedFor;
	}

	public void setVotedFor(String votedFor) {
		this.votedFor = votedFor;
	}

	public int getCurrentTerm() {
		return currentTerm;
	}

	public void setCurrentTerm(int currentTerm) {
		this.currentTerm = currentTerm;
	}

	public enum RaftStatus {
		LEADER, FOLLOWER, CANDIDATE
	}
}
