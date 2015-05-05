package snapchatproto.servers.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.core.RaftMgmt.AcknowledgementPayload;
import snapchatproto.core.RaftMgmt.RaftHeader;
import snapchatproto.core.RaftMgmt.RaftMessagePayload;
import snapchatproto.core.RaftMgmt.ServerCommunicationMessage;
import snapchatproto.core.RaftMgmt.RaftMessagePayload.RaftAction;
import snapchatproto.servers.managers.NodeData.RaftStatus;


public class CandidateState implements NodeState{

	protected static Logger logger = LoggerFactory.getLogger("node");
	
	int voteCount;

	public int getVoteCount() {
		return voteCount;
	}

	public void setVoteCount(int voteCount) {
		this.voteCount = voteCount;
	}

	public CandidateState() {

		RaftElectionManager.getInstance();
		voteCount = 1;
	}

	@Override
	public void repondToVoteRequest(ServerCommunicationMessage msg) {

		msg.getAcknowledgementPayload();
		ServerCommunicationMessage.newBuilder();
		System.out.println("I am already a candidate. I would not vote");
	}

	@SuppressWarnings("static-access")
	@Override
	public void repondToCastVote(ServerCommunicationMessage msg) {

		AcknowledgementPayload ap = msg.getAcknowledgementPayload();
		ServerCommunicationMessage.Builder response = ServerCommunicationMessage.newBuilder();
		int totalConnections = ConnectionManager.getNumMgmtConnections();

		logger.info("Candidate received a vote..");

		int currentTerm = NodeDataManager.getInstance().getNodeData().getCurrentTerm();

		int myNodeId = Integer.parseInt(NodeDataManager.getInstance().getNodeData().getNodeId());
		RaftHeader.Builder responseHeader = RaftHeader.newBuilder();
		responseHeader.setOriginatorNodeId(myNodeId);

		RaftMessagePayload.Builder responsePayload = RaftMessagePayload.newBuilder();

		if(ap.getResponse() == "YES"||ap.getResponse().equals("YES") )
		{
			this.setVoteCount(this.getVoteCount()+1);
		}
		
		if (((totalConnections+1)/2) <= voteCount ) {					
			logger.info("For node: "+myNodeId+", total count of votes is: "+voteCount+" and total connections: "+totalConnections);
			logger.info("This candidiate has received the majority. Node + myNodeId + has been elected as the leader..");

			responsePayload.setAction(RaftAction.LEADER);
			responsePayload.setLeaderId(myNodeId);			
			responsePayload.setTerm(currentTerm);
			
			responseHeader.setOriginatorNodeId(myNodeId);
			
			RaftElectionManager.getInstance().concludeWith(true, myNodeId);
			RaftElectionManager.getInstance().setElectionInProgress(false);
			
			NodeDataManager.getNodeData().setNodeStatus(RaftStatus.LEADER);
			
			RaftElectionManager.setLeaderNode(myNodeId);
			
			response.setRaftHeader(responseHeader.build());
			response.setRaftMessagePayload(responsePayload.build());
			
			RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().leaderState);
			
		}
		else
		{	
			System.out.println("Node: "+myNodeId+" (term: "+currentTerm+") has received vote from node "+msg.getRaftHeader().getOriginatorNodeId());
			System.out.println("Total count of votes with node: "+myNodeId+" is "+voteCount);
		}
		ConnectionManager.broadcast(response.build());

	}

	@Override
	public void respondToLeaderElected(ServerCommunicationMessage msg) {

		RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().followerState);
		//voteCount = 0;
		RaftElectionManager.getInstance().concludeWith(true, msg.getRaftHeader().getOriginatorNodeId());
		RaftElectionManager.getInstance().setElectionInProgress(false);
		System.out.println("Saving the leader state. Its is "+msg.getRaftHeader().getOriginatorNodeId());


	}

	@Override
	public void respondToTheLeaderIs(ServerCommunicationMessage msg) {

		RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().followerState);



	}

	@Override
	public void respondToWhoIsTheLeader(ServerCommunicationMessage msg) {

	}

	@Override
	public void appendLogs(ServerCommunicationMessage msg) {

	}

	@Override
	public void replicateLogs(ServerCommunicationMessage msg) {

	}

	@Override
	public void processAckAppendMessage(ServerCommunicationMessage msg) {

	}

}
