package snapchatproto.servers.managers;

public class RaftNode {

	public static NodeState currentState=NodeState.Follower;
	
	public static NodeState getCurrentState() {
		return currentState;
	}

	public static void setCurrentState(NodeState currState) {
		currentState = currState;
	}

	public static enum NodeState
	{
		Leader,Follower,Candidate;
	}
	
	
}
