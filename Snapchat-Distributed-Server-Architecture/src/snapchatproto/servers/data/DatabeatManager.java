package snapchatproto.servers.data;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.comm.App.Header;
import snapchatproto.comm.App.Payload;
import snapchatproto.comm.App.Ping;
import snapchatproto.comm.App.Request;
import snapchatproto.constants.Constants;
import snapchatproto.servers.conf.ServerConf;
import snapchatproto.servers.conf.ServerConf.AdjacentConf;
import snapchatproto.servers.data.AppData.BeatStatus;
import snapchatproto.servers.managers.NodeDataManager;

import com.google.protobuf.GeneratedMessage;

/**
 * The DatabeatManager is used to setup a channel between different 
 * nodes of the subnet at the application level
 * 
 * The channels will be used for transmission and receiving the actual data
 * bytes over this channel
 * 
 */

public class DatabeatManager extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("DatabeatManager");
	protected static AtomicReference<DatabeatManager> instance = new AtomicReference<DatabeatManager>();

	// frequency that heartbeats are checked
	static final int sHeartRate = 5000; // msec

	private static ServerConf conf;
	boolean forever = true;

	ConcurrentHashMap<Channel, AppData> outgoingHB = new ConcurrentHashMap<Channel, AppData>();
	ConcurrentHashMap<Integer, AppData> incomingHB = new ConcurrentHashMap<Integer, AppData>();

	public static DatabeatManager initManager(ServerConf conf) {
		DatabeatManager.conf = conf;
		instance.compareAndSet(null, new DatabeatManager());
		return instance.get();
	}

	public static DatabeatManager getInstance() {
		return instance.get();
	}

	/**
	 * initialize the heartbeatMgr for this server
	 * 
	 * @param nodeId
	 *            The server's (this) ID
	 */
	protected DatabeatManager() {
	}

	/**
	 * create/register expected connections that this node will make. These
	 * edges are connections this node is responsible for monitoring.
	 * 
	 * @param edges
	 */
	public void initNetwork(AdjacentConf edges) {
	}

	/**
	 * update information on a node we monitor
	 * 
	 * @param nodeId
	 */
	public void processRequest(Request appReq) {
		if (appReq == null)
			return;

		AppData hd = incomingHB.get(appReq.getHeader().getOriginator());
		if (hd == null) {
			//logger.error("Unknown heartbeat received from node ", appReq.getHeader().getOriginator());
			return;
		} else {
			//if (logger.isDebugEnabled())
			logger.info("AppHeartbeatManager.processRequest() HB received from " + appReq.getHeader().getOriginator());

			hd.setFailures(0);
			long now = System.currentTimeMillis();
			hd.setLastBeat(now);
			//RaftTimer.setLastBeatTime(now);
			//DataChannelManager.getInstance().getNodeData().setLastBeatReceivedFromLeader(now);
		}
	}

	/**
	 * This is called by the HeartbeatPusher when this node requests a
	 * connection to a node, this is called to register interest in creating a
	 * connection/edge.
	 * 
	 * @param node
	 */
	protected void addAdjacentNode(AppData node) {
		if (node == null || node.getHost() == null || node.getPort() == null) {
			logger.error("Registration of data channel missing data");
			return;
		}

		if (!incomingHB.containsKey(node.getNodeId())) {
			logger.info("Expects to connect to node " + node.getNodeId() + " (" + node.getHost() + ", "
					+ node.getPort() + ")");

			// ensure if we reuse node instances that it is not dirty.
			node.clearAll();
			node.setInitTime(System.currentTimeMillis());
			node.setStatus(BeatStatus.Init);
			incomingHB.put(node.getNodeId(), node);
		}
	}

	/**
	 * add an INCOMING endpoint (receive HB from). This is called when this node
	 * actually establishes the connection to the node. Prior to this call, the
	 * system will register an inactive/pending node through addIncomingNode().
	 * 
	 * @param nodeId
	 * @param ch
	 * @param sa
	 */
	public void addAdjacentNodeChannel(int nodeId, Channel ch, SocketAddress sa) {
		AppData hd = incomingHB.get(nodeId);
		if (hd != null) {
			hd.setConnection(ch, sa, nodeId);
			hd.setStatus(BeatStatus.Active);

			ch.closeFuture().addListener(new CloseHeartListener(hd));
		} else {
			logger.error("Received data beat from unknown node  ", nodeId);
		}
	}

	/**
	 * send a OUTGOING heartbeatMgr to a node. This is called when a
	 * client/server makes a request to receive heartbeats.
	 * 
	 * @param nodeId
	 * @param ch
	 * @param sa
	 */
	public void addOutgoingChannel(int nodeId, String host, int port, Channel ch, SocketAddress sa) {
		if (!outgoingHB.containsKey(ch)) {
			AppData heart = new AppData(nodeId, host, port, null);
			heart.setConnection(ch, sa, nodeId);
			outgoingHB.put(ch, heart);

			// when the channel closes, remove it from the outgoingHB
			ch.closeFuture().addListener(new CloseHeartListener(heart));
		} else {
			logger.error("Received data beat from unknown node ", nodeId);
		}
	}

	public void release() {
		forever = true;
	}

	private Request generateHB() {
		int myNodeId = Integer.parseInt(NodeDataManager.getInstance().getNodeData().getNodeId());

		Ping.Builder pb = Ping.newBuilder();
		pb.setTag(Constants.SERVER_JOIN_TAG);
		pb.setNumber(Constants.SERVER_JOIN_NUMBER);

		Payload.Builder payB = Payload.newBuilder();
		payB.setPing(pb.build());

		Request.Builder b= Request.newBuilder();
		b.setBody(payB.build());

		Header.Builder h = Header.newBuilder();

		h.setOriginator(myNodeId);
		h.setTag(Constants.SERVER_JOIN_TAG);
		h.setTime(System.currentTimeMillis());
		h.setRoutingId(Header.Routing.PING);
		b.setHeader(h.build());

		return b.build();
	}

	@Override
	public void run() {
		logger.info("Starting databeat manager");
		int count = 0;

		try{
			while (forever) {
				try {
					Thread.sleep(sHeartRate);

					if (outgoingHB.size() > 0) {
						GeneratedMessage msg = null;

						count++;
						for (AppData hd : outgoingHB.values()) {
							if (hd.getFailuresOnSend() > AppData.sFailureToSendThresholdDefault) {
								continue;
							}

							if (msg == null)
								msg = generateHB();

							try {
								if (logger.isDebugEnabled())
									logger.debug("Sending databeat to " + hd.getNodeId());
								hd.getChannel().writeAndFlush(msg);
								hd.setLastBeatSent(System.currentTimeMillis());
								hd.setFailuresOnSend(0);
								if (logger.isDebugEnabled())
									logger.debug("beat (" + DatabeatManager.conf.getNodeId() + ") sent to "
											+ hd.getNodeId() + " at " + hd.getHost());
							} catch (Exception e) {
								hd.incrementFailuresOnSend();
								logger.error("Failed " + hd.getFailures() + " times to send databeat for " + hd.getNodeId()
										+ " at " + hd.getHost(), e);
							}
						}

						if(outgoingHB.size() == 2 && count == 2){
							logger.info("Network is set and node is connected to adjacent data nodes..");
						}

					}
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					logger.error("Unexpected appdata communcation failure", e);
					break;
				}
			}
		} catch(Exception e) {
			logger.error("Unexpected error", e);
		}

		if (!forever)
			logger.info("appdata outbound queue closing");
		else
			logger.info("unexpected closing of appdata HB manager");

	}

	public class CloseHeartListener implements ChannelFutureListener {
		private AppData heart;

		public CloseHeartListener(AppData heart) {
			this.heart = heart;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (outgoingHB.containsValue(heart)) {
				logger.warn("databeat outgoing channel closing for node '" + heart.getNodeId() + "' at " + heart.getHost());
				outgoingHB.remove(future.channel());
			} else if (incomingHB.containsValue(heart)) {
				logger.warn("databeat incoming channel closing for node '" + heart.getNodeId() + "' at " + heart.getHost());
				incomingHB.remove(future.channel());
			}
		}
	}
}
