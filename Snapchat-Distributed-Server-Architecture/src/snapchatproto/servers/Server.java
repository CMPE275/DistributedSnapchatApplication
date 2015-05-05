/*
 * copyright 2014, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package snapchatproto.servers;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.servers.cluster.ClusterConfList;
import snapchatproto.servers.cluster.ClusterConnectionManager;
import snapchatproto.servers.conf.JsonUtil;
import snapchatproto.servers.conf.NodeDesc;
import snapchatproto.servers.conf.ServerConf;
import snapchatproto.servers.data.AppData;
import snapchatproto.servers.data.DataChannelManager;
import snapchatproto.servers.data.DataInitializer;
import snapchatproto.servers.data.DatabeatManager;
import snapchatproto.servers.data.DatabeatPusher;
import snapchatproto.servers.logging.LogList;
import snapchatproto.servers.management.ManagementInitializer;
import snapchatproto.servers.management.ManagementQueue;
import snapchatproto.servers.managers.HeartbeatData;
import snapchatproto.servers.managers.HeartbeatManager;
import snapchatproto.servers.managers.HeartbeatPusher;
import snapchatproto.servers.managers.JobManager;
import snapchatproto.servers.managers.LogListManager;
import snapchatproto.servers.managers.RaftElectionManager;
import snapchatproto.servers.managers.NetworkManager;
import snapchatproto.servers.managers.NodeDataManager;
import snapchatproto.servers.managers.NodeData.RaftStatus;
import snapchatproto.servers.queue.DiscreteQueue;
import snapchatproto.servers.resources.ResourceFactory;
import snapchatproto.servers.timers.*;

/**
 * Note high surges of messages can close down the channel if the handler cannot
 * process the messages fast enough. This design supports message surges that
 * exceed the processing capacity of the server through a second thread pool
 * (per connection or per server) that performs the work. Netty's boss and
 * worker threads only processes new connections and forwarding requests.
 * <p>
 * Reference Proactor pattern for additional information.
 * 
 * @author gash
 * 
 */
public class Server {
	protected static Logger logger = LoggerFactory.getLogger("server");

	protected static ChannelGroup allChannels;
	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();
	protected ServerConf conf;
	private ClusterConfList clusterConfList;

	protected JobManager jobMgr;
	protected NetworkManager networkMgr;
	protected HeartbeatManager heartbeatMgr;
	protected RaftElectionManager electionMgr;
	protected DataChannelManager dataChannelManager;
	protected DatabeatManager appHeartbeatManager;
	protected NodeDataManager nodeDataManager;
	public DiscreteQueue discreteQueue;
	protected LogList logList;
	protected LogListManager logListManager;
	protected ClusterConnectionManager clusterConnectionManager;

	/**
	 * static because we need to get a handle to the factory from the shutdown
	 * resource
	 */
	public static void shutdown() {
		try {
			if (allChannels != null) {
				ChannelGroupFuture grp = allChannels.close();
				grp.awaitUninterruptibly(5, TimeUnit.SECONDS);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		logger.info("Server shutdown");
		System.exit(0);
	}

	/**
	 * initialize the server with a configuration of it's resources
	 * 
	 * @param cfg
	 */
	public Server(File cfg, File sCfg) {
		init(cfg);
		initClusterConf(sCfg);
	}

	private void init(File cfg) {
		if (!cfg.exists())
			throw new RuntimeException(cfg.getAbsolutePath() + " not found");
		// resource initialization - how message are processed
		BufferedInputStream br = null;
		try {
			byte[] raw = new byte[(int) cfg.length()];
			br = new BufferedInputStream(new FileInputStream(cfg));
			br.read(raw);
			conf = JsonUtil.decode(new String(raw), ServerConf.class);
			if (!verifyConf(conf))
				throw new RuntimeException(
						"verification of configuration failed");
			ResourceFactory.initialize(conf);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void initClusterConf(File sCfg) {

		if (!sCfg.exists())
			throw new RuntimeException(sCfg.getAbsolutePath() + " not found");
		// resource initialization - how message are processed
		BufferedInputStream br = null;
		try {
			byte[] raw = new byte[(int) sCfg.length()];

			br=new BufferedInputStream(new FileInputStream(sCfg));
			br.read(raw);
			clusterConfList = JsonUtil.decode(new String(raw),
					ClusterConfList.class);

			ResourceFactory.initializeCluster(clusterConfList);

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private boolean verifyConf(ServerConf conf) {
		boolean rtn = true;
		if (conf == null) {
			logger.error("Null configuration");
			return false;
		} else if (conf.getNodeId() < 0) {
			logger.error("Bad node ID, negative values not allowed.");
			rtn = false;
		} else if (conf.getPort() < 1024 || conf.getMgmtPort() < 1024) {
			logger.error("Invalid port number");
			rtn = false;
		}

		return rtn;
	}

	public void release() {
		if (HeartbeatManager.getInstance() != null)
			HeartbeatManager.getInstance().release();
	}

	/**
	 * initialize the outward facing (public) interface
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartCommunication implements Runnable {
		ServerConf conf;

		public StartCommunication(ServerConf conf) {
			this.conf = conf;
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(conf.getPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new DataInitializer(compressComm));

				// Start the server.
				logger.info("Starting data server " + conf.getNodeId()
						+ ", listening on port = " + conf.getPort());
				ChannelFuture f = b.bind(conf.getPort()).syncUninterruptibly();

				// should use a future channel listener to do this step
				// allChannels.add(f.channel());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup public handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}

			// We can also accept connections from a other ports (e.g., isolate
			// read
			// and writes)
		}
	}

	/**
	 * initialize the private network/interface
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartManagement implements Runnable {
		private ServerConf conf;

		public StartManagement(ServerConf conf) {
			this.conf = conf;
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			// UDP: not a good option as the message will be dropped

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(conf.getMgmtPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new ManagementInitializer(compressComm));

				// Start the server.

				logger.info("Starting mgmt " + conf.getNodeId()
						+ ", listening on port = " + conf.getMgmtPort());
				ChannelFuture f = b.bind(conf.getMgmtPort())
						.syncUninterruptibly();

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup public handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		}
	}

	/**
	 * this initializes the managers that support the internal communication
	 * network.
	 * 
	 */
	@SuppressWarnings("static-access")
	private void startManagers() {
		if (conf == null)
			return;

		nodeDataManager = NodeDataManager.initManager(conf);
		nodeDataManager.getNodeData().setNodeStatus(RaftStatus.FOLLOWER);

		logList = LogList.initManager();

		logListManager = LogListManager.initManager();
		logger.info("***** Logs after start *****");
		logListManager.printLogs();

		logListManager.getLogsFromDB();
		logger.info("***** Logs after hitting db *****");
		logListManager.printLogs();

		clusterConnectionManager = ClusterConnectionManager.initManager(clusterConfList);
		clusterConnectionManager.start();

		// start the inbound and outbound manager worker threads
		ManagementQueue.startup();

		logger.info("Before discrete queue");

		DiscreteQueue.init();

		logger.info("After discrete queue");

		dataChannelManager = DataChannelManager.initManager(conf);
		appHeartbeatManager = DatabeatManager.initManager(conf);

		// create manager for network changes
		networkMgr = NetworkManager.initManager(conf);

		// create manager for leader election. The number of votes (default 1)
		// is used to break ties where there are an even number of nodes.
		electionMgr = RaftElectionManager.initManager(conf);

		// create manager for accepting jobs
		jobMgr = JobManager.initManager(conf);

		System.out
		.println("---> Server.startManagers() expecting "
				+ conf.getAdjacent().getAdjacentNodes().size()
				+ " connections");
		// establish nearest nodes and start sending heartbeats
		heartbeatMgr = HeartbeatManager.initManager(conf);
		for (NodeDesc nn : conf.getAdjacent().getAdjacentNodes().values()) {
			logger.info("Neighbr:" + nn.getNodeId());
			logger.info("Neighbr Host:" + nn.getHost());
			logger.info("Neighbr Port:" + nn.getPort());
			logger.info("Neighbr Mgmt Port:" + nn.getMgmtPort());
			HeartbeatData node = new HeartbeatData(nn.getNodeId(),
					nn.getHost(), nn.getPort(), nn.getMgmtPort());
			AppData appData = new AppData(nn.getNodeId(), nn.getHost(),
					nn.getPort(), nn.getMgmtPort());
			HeartbeatPusher.getInstance().connectToThisNode(conf.getNodeId(),
					node);
			DatabeatPusher.getInstance().connectToThisNode(
					conf.getNodeId(), appData);
		}
		// manage heartbeatMgr connections
		HeartbeatPusher conn = HeartbeatPusher.getInstance();
		conn.start();

		DatabeatPusher appConn = DatabeatPusher.getInstance();
		appConn.start();

		heartbeatMgr.start();
		appHeartbeatManager.start();

		logger.info("Server " + conf.getNodeId() + ", managers initialized");
	}

	/**
	 * Start the communication for both external (public) and internal
	 * (management)
	 */
	public void run() {
		if (conf == null) {
			logger.error("Missing configuration file");
			return;
		}

		logger.info("Initializing server " + conf.getNodeId());

		startManagers();

		StartManagement mgt = new StartManagement(conf);
		Thread mthread = new Thread(mgt);
		mthread.start();

		logger.info("Server " + conf.getNodeId() + " ready");
		NodeDataManager.getNodeData().setNodeId(
				String.valueOf(conf.getNodeId()));

		StartCommunication comm = new StartCommunication(conf);
		Thread cthread = new Thread(comm);
		cthread.start();

		RaftTimer raftTimer = new RaftTimer();
		Thread timerThread = new Thread(raftTimer);
		timerThread.start();

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: java "
					+ Server.class.getClass().getName() + " conf-file");
			System.exit(1);
		}

		File cfg = new File(args[0]);
		if (!cfg.exists()) {
			Server.logger.error("configuration file does not exist: " + cfg);
			System.exit(2);
		}

		File sCfg = new File(args[1]);
		if (!sCfg.exists()) {
			Server.logger.error("cluster configuration file does not exist: " + sCfg);
			System.exit(2);
		}

		Server svr = new Server(cfg, sCfg);
		svr.run();
	}
}
