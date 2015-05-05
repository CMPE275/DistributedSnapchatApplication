package snapchatproto.servers.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.core.RaftMgmt.AcknowledgementPayload;
import snapchatproto.core.RaftMgmt.RaftHeader;
import snapchatproto.core.RaftMgmt.RaftMessagePayload;
import snapchatproto.core.RaftMgmt.ServerCommunicationMessage;
import snapchatproto.core.RaftMgmt.AcknowledgementPayload.ResponseAction;
import snapchatproto.core.RaftMgmt.RaftMessagePayload.RaftAction;

public class FollowerState implements NodeState {
	protected static Logger logger = LoggerFactory.getLogger("node");
	
	private RaftElectionManager electionManager;

	public FollowerState()
	{
		this.electionManager = RaftElectionManager.getInstance();
	}

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
			logger.info("Node "+myNodeId+" casted a vote to node "+ orginatorId);

			ConnectionManager.getConnection(mgmt.getRaftHeader().getOriginatorNodeId(), true).writeAndFlush(response.build());

		}
		
	}

	@Override
	public void repondToCastVote(ServerCommunicationMessage msg) {
		
	}

	@Override
	public void respondToLeaderElected(ServerCommunicationMessage msg) {
		RaftElectionManager.getInstance().concludeWith(true,msg.getRaftHeader().getOriginatorNodeId());
		RaftElectionManager.getInstance().setElectionInProgress(false);
		RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().followerState);
	}

	@Override
	public void respondToTheLeaderIs(ServerCommunicationMessage msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void respondToWhoIsTheLeader(ServerCommunicationMessage msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void appendLogs(ServerCommunicationMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void replicateLogs(ServerCommunicationMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processAckAppendMessage(ServerCommunicationMessage msg) {
		// TODO Auto-generated method stub
		
	}


}
