package snapchatproto.servers.cluster;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.comm.App.JoinMessage;
import snapchatproto.comm.App.Request;
import snapchatproto.servers.cluster.ClusterConfList.ClusterConf;
import snapchatproto.servers.conf.NodeDesc;
import snapchatproto.servers.data.DataInitializer;
import snapchatproto.servers.managers.NodeDataManager;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ClusterConnectionManager extends Thread {
	
	private Map<Integer, Channel> connMap = new HashMap<Integer, Channel>();
	private static Map<Integer, ClusterConf> clusterMap;
	private boolean isLeader = false;

	protected static AtomicReference<ClusterConnectionManager> instance = new AtomicReference<ClusterConnectionManager>();
	protected static Logger logger = LoggerFactory.getLogger("ClusterConnectionManager");
	
	public static ClusterConnectionManager initManager(ClusterConfList clusterConf) {
		clusterMap = clusterConf.getClusters();
		instance.compareAndSet(null, new ClusterConnectionManager());
		return instance.get();
	}
	
	public static ClusterConnectionManager getInstance() {
		return instance.get();
	}
	
	public ClusterConnectionManager() {
	
	}

	public void registerConnection(int nodeId, Channel channel) {
		connMap.put(nodeId, channel);
	}

	public ChannelFuture connect(String host, int port) {

		ChannelFuture channel = null;
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			logger.info("Attempting to  connect to : "+host+" : "+port);
			Bootstrap b = new Bootstrap();
			b.group(workerGroup).channel(NioSocketChannel.class)
			.handler(new DataInitializer(false));

			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			channel = b.connect(host, port).syncUninterruptibly();
		} catch (Exception e) {
			return null;
		}

		return channel;
	}

	public Request createClusterJoinMessage(int fromCluster, int fromNode,
			int toCluster, int toNode) {
		Request.Builder req = Request.newBuilder();

		JoinMessage.Builder jm = JoinMessage.newBuilder();
		jm.setFromClusterId(fromCluster);
		jm.setFromNodeId(fromNode);
		jm.setToClusterId(toCluster);
		jm.setToNodeId(toNode);

		req.setJoinMessage(jm.build());
		return req.build();

	}

	@Override
	public void run() {
		Iterator<Integer> it = clusterMap.keySet().iterator();
		int myNodeId = Integer.parseInt(NodeDataManager.getNodeData().getNodeId()); 
		while (true) {
			if(isLeader){
				try {
					int key = it.next();
					if (!connMap.containsKey(key)) {
						ClusterConf cc = clusterMap.get(key);
						List<NodeDesc> nodes = cc.getClusterNodes();
						for (NodeDesc n : nodes) {
							String host = n.getHost();
							int port = n.getPort();

							ChannelFuture channel = connect(host, port);
							Request req = createClusterJoinMessage(35325,
									myNodeId, key, n.getNodeId());
							if (channel != null) {
								channel = channel.channel().writeAndFlush(req);
								if (channel.channel().isWritable()) {
									registerConnection(key,
											channel.channel());
									logger.info("Connection to cluster " + key
											+ " added");
									break;
								}
							}
						}
					}
				} catch (NoSuchElementException e) {
					it = clusterMap.keySet().iterator();
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {
						logger.error("Exception " + e1);
					}
				}
			} else {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}


	public static class ClusterLostListener implements ChannelFutureListener {
		ClusterConnectionManager ccm;

		public ClusterLostListener(ClusterConnectionManager ccm) {
			this.ccm = ccm;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
		}
	}


	public void processRequest(Request req, Channel channel) {
		if(isLeader){
			int nodeId = req.getHeader().getOriginator();
			registerConnection(nodeId, channel);
		}
	}
}