package snapchatproto.servers.managers;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.core.RaftMgmt.RaftHeader;
import snapchatproto.core.RaftMgmt.RaftMessagePayload;
import snapchatproto.core.RaftMgmt.ServerCommunicationMessage;
import snapchatproto.core.RaftMgmt.AcknowledgementPayload.ResponseAction;
import snapchatproto.core.RaftMgmt.RaftMessagePayload.RaftAction;
import snapchatproto.servers.conf.ServerConf;
import snapchatproto.servers.election.ElectionListener;


public class RaftElectionManager implements ElectionListener {
	protected static Logger logger = LoggerFactory.getLogger("RaftElectionManager");
	protected static AtomicReference<RaftElectionManager> instance = new AtomicReference<RaftElectionManager>();

	static Integer leaderNode;
	private boolean isElectionInProgress=false;

	public boolean isElectionInProgress() {
		return isElectionInProgress;
	}


	public void setElectionInProgress(boolean isElectionInProgress) {
		this.isElectionInProgress = isElectionInProgress;
	}

	public static ServerConf conf;
	private int electionCycle ;
	long lastknownBeat;

	public NodeState currentState;

	public NodeState getCurrentState() {
		return currentState;
	}


	public void setCurrentState(NodeState currentState) {
		this.currentState = currentState;
	}

	public NodeState leaderState;
	public NodeState followerState;
	public NodeState candidateState;

	public RaftElectionManager()
	{
		leaderNode = -1;
		electionCycle = -1;
		lastknownBeat = 0;
		leaderState = new LeaderState();
		followerState = new FollowerState();
		candidateState = new CandidateState();		
		currentState = followerState;
		isElectionInProgress= false;
	}


	public static RaftElectionManager initManager(ServerConf conf) 
	{
		RaftElectionManager.conf = conf;
		instance.compareAndSet(null, new RaftElectionManager());
		return instance.get();
	}

	public static Integer getLeaderNode()
	{
		return leaderNode;
	}

	public static void setLeaderNode(int leader)
	{
		leaderNode = leader;
	}


	@SuppressWarnings("static-access")
	public Integer whoIsTheLeader() {
		return this.leaderNode;
	}

	public static RaftElectionManager getInstance() {
		return instance.get();
	}

	@SuppressWarnings("static-access")
	public void startElection() {

		//clear previous election values
		leaderNode = -1;
		NodeDataManager.getInstance().getNodeData().setVotedFor("");
		
		logger.info("Node " + conf.getNodeId() + " is starting election..");
		
		RaftHeader.Builder rh = RaftHeader.newBuilder();
		rh.setOriginatorNodeId(conf.getNodeId());
		rh.setTime(System.currentTimeMillis());
		
		RaftMessagePayload.Builder rp = RaftMessagePayload.newBuilder();
		
		//increment term
		electionCycle = NodeDataManager.getInstance().getNodeData().getCurrentTerm() + 1;
		NodeDataManager.getInstance().getNodeData().setCurrentTerm(electionCycle);
		
		//ask for votes from other nodes
		rp.setAction(RaftAction.REQUESTVOTE);
		rp.setTerm(electionCycle);
		
		logger.info("Setting term of node "+conf.getNodeId()+ " to "+electionCycle);
		
		ServerCommunicationMessage.Builder b = ServerCommunicationMessage.newBuilder();
		b.setRaftHeader(rh.build());
		b.setRaftMessagePayload(rp.build());

		this.setCurrentState(candidateState);

		//broadcast vote request to all the nodes in the cluster
		ConnectionManager.broadcast(b.build());

	}

	public void processRequest(ServerCommunicationMessage mgmt) {

		//Server has received a vote request from other node
		if (mgmt.getRaftMessagePayload().getAction() == RaftAction.REQUESTVOTE) 
		{
			logger.info("I got a request to vote from "+mgmt.getRaftHeader().getOriginatorNodeId());
			currentState.repondToVoteRequest(mgmt);
		}
		//Server has received a vote from other node
		else if (mgmt.getAcknowledgementPayload().getAction() == ResponseAction.CASTVOTE) { 
			logger.info("I got a vote from "+mgmt.getRaftHeader().getOriginatorNodeId());
			currentState.repondToCastVote(mgmt);			
		} 
		//Server has received a leadership declaration from other node
		else if (mgmt.getRaftMessagePayload().getAction() == RaftAction.THELEADERIS || mgmt.getRaftMessagePayload().getAction() == RaftAction.LEADER) {
			logger.info("I got a leadership decralation from "+mgmt.getRaftHeader().getOriginatorNodeId());
			currentState.respondToLeaderElected(mgmt);
		}

	}

	@SuppressWarnings("static-access")
	@Override
	public void concludeWith(boolean success, Integer leaderID) {
		if (success) {
			logger.info("The leader is " + leaderID);
			this.leaderNode = leaderID;
		}
	}

}
