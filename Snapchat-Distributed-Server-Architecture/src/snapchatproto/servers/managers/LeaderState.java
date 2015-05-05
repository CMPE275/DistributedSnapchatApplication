package snapchatproto.servers.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.core.RaftMgmt.AcknowledgementPayload;
import snapchatproto.core.RaftMgmt.RaftHeader;
import snapchatproto.core.RaftMgmt.RaftMessagePayload;
import snapchatproto.core.RaftMgmt.ServerCommunicationMessage;
import snapchatproto.core.RaftMgmt.AcknowledgementPayload.ResponseAction;
import snapchatproto.core.RaftMgmt.RaftMessagePayload.RaftAction;
import snapchatproto.servers.managers.NodeData.RaftStatus;

public class LeaderState implements NodeState {
	
	protected static Logger logger = LoggerFactory.getLogger("node");

	private static int countAckAppend = 0;
	int totalConnections = ConnectionManager.getNumMgmtConnections();
	
	public LeaderState()
	{
		RaftElectionManager.getInstance();
	}

	@SuppressWarnings("static-access")
	@Override
	public void repondToVoteRequest(ServerCommunicationMessage mgmt) {
		
		RaftMessagePayload rp = mgmt.getRaftMessagePayload();
		
		ServerCommunicationMessage.Builder response = ServerCommunicationMessage.newBuilder();
		
		if (rp.getAction() == RaftAction.REQUESTVOTE && RaftElectionManager.getInstance().isElectionInProgress() == false) 		
		{
			RaftElectionManager.getInstance().setElectionInProgress(true);
					
			String votedFor = NodeDataManager.getInstance().getNodeData().getVotedFor();
			int currentTerm = NodeDataManager.getInstance().getNodeData().getCurrentTerm();
			int myNodeId = Integer.parseInt(NodeDataManager.getInstance().getNodeData().getNodeId());
			String orginatorId = String.valueOf(mgmt.getRaftHeader().getOriginatorNodeId());
			RaftHeader.Builder responseHeader = RaftHeader.newBuilder();
			responseHeader.setOriginatorNodeId(myNodeId);
			AcknowledgementPayload.Builder ackPayload = AcknowledgementPayload.newBuilder();		
			logger.info("---> Voting for node : "+ orginatorId);
			NodeDataManager.getNodeData().setCurrentTerm(rp.getTerm());
			
			ackPayload.setTerm(rp.getTerm());
				
			if(mgmt.getRaftMessagePayload().getTerm() > currentTerm && votedFor.isEmpty()){
				ackPayload.setResponse("YES");
				NodeDataManager.getInstance().getNodeData().setVotedFor(orginatorId);
			}else{
				
				ackPayload.setResponse("NO");
			}
				
			RaftElectionManager.getInstance().concludeWith(false, -1);
			
			ackPayload.setAction(ResponseAction.CASTVOTE);
			response.setRaftHeader(responseHeader.build());
			response.setAcknowledgementPayload(ackPayload.build());
					
			ConnectionManager.getConnection(mgmt.getRaftHeader().getOriginatorNodeId(), true).writeAndFlush(response.build());
		}
		
	}

	@Override
	public void repondToCastVote(ServerCommunicationMessage msg) {
	}

	@Override
	public void respondToLeaderElected(ServerCommunicationMessage msg) {	
		RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().followerState);
		RaftElectionManager.getInstance().concludeWith(true, msg.getRaftHeader().getOriginatorNodeId());
		RaftElectionManager.getInstance().setElectionInProgress(false);
		NodeDataManager.getNodeData().setNodeStatus(RaftStatus.FOLLOWER);
	}

	@Override
	public void respondToTheLeaderIs(ServerCommunicationMessage msg) {
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

	
	// call when ack_append msg is processed.
	@Override
	public void processAckAppendMessage(ServerCommunicationMessage msg) {
		
		logger.info("Ack Append received ...");
		if(countAckAppend > (totalConnections+1)/2){
			countAckAppend = 0;
			//give response to client
		} else {
			countAckAppend++;
		}
		
		
	}

}
