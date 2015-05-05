package snapchatproto.servers.managers;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.servers.conf.ServerConf;
import snapchatproto.servers.managers.NodeData.RaftStatus;

public class NodeDataManager {
	private static NodeData nodeData = null; 
	protected static Logger logger = LoggerFactory.getLogger("NodeDataManager");
	protected static AtomicReference<NodeDataManager> instance = new AtomicReference<NodeDataManager>();
	

	@SuppressWarnings("static-access")
	public static NodeDataManager initManager(ServerConf conf) {
		instance.compareAndSet(null, new NodeDataManager());
		instance.get().setNodeData(0, ""+conf.getNodeId(), "", RaftStatus.FOLLOWER);
		
		return instance.get();
	}

	public static NodeDataManager getInstance() {
		return instance.get();
	}

	public static void setNodeData(int currentTerm, String nodeId, String votedFor, NodeData.RaftStatus status) {
		nodeData.setCurrentTerm(currentTerm);
		nodeData.setNodeId(nodeId);
		nodeData.setNodeStatus(status);
		nodeData.setVotedFor(votedFor);
	}
	
	public static NodeData getNodeData() {
		return nodeData;
	}

	protected NodeDataManager() {
		nodeData = new NodeData();
	}
}
