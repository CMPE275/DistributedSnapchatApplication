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
package snapchatproto.servers.data;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.comm.App.Header;
import snapchatproto.comm.App.Payload;
import snapchatproto.comm.App.Ping;
import snapchatproto.comm.App.Request;
import snapchatproto.constants.Constants;
import snapchatproto.servers.managers.NodeDataManager;

/**
 * The monitor is a client-side component that can exist as as its own client or
 * as an internal component to a server. Its purpose is to process responses
 * from server management messages/responses - heartbeats (HB).
 * 
 * It is conceivable to create a separate application/process that listens to
 * the network. However, one must consider the HB (management port) is more for
 * localized communication through the overlay network and not as a tool for
 * overall health of the network. For an external monitoring device, a UDP-based
 * communication is more appropriate.
 * 
 * @author gash
 * 
 */
public class DataMonitor {
	protected static Logger logger = LoggerFactory.getLogger("mgmt");

	protected ChannelFuture channel; // do not use directly, call connect()!
	private EventLoopGroup group;

	private static int N = 0; // unique identifier
	private int toNodeId;
	private String host;
	private int port;

	private List<DataMonitorListener> listeners = new ArrayList<DataMonitorListener>();

	private DataMonitorHandler handler;

	
	public DataMonitor(int iamNode, String host, int port, int toNodeId) {
		this.toNodeId = toNodeId;
		this.host = host;
		this.port = port;
		this.group = new NioEventLoopGroup();

		logger.info("Creating appdata heartbeat monitor for " + host + "(" + port + ")");
	}

	public DataMonitorHandler getHandler() {
		return handler;
	}

	/**
	 * abstraction of notification in the communication
	 * 
	 * @param listener
	 */
	public void addListener(DataMonitorListener listener) {
		if (handler == null && !listeners.contains(listener)) {
			listeners.add(listener);
			return;
		}

		try {
			handler.addListener(listener);
		} catch (Exception e) {
			logger.error("failed to add DataMonitorListener", e);
		}
	}

	public void release() {
		logger.warn("DataMonitor: releasing resources");

		for (Integer id : handler.listeners.keySet()) {
			DataMonitorListener ml = handler.listeners.get(id);
			ml.connectionClosed();

			listeners.add(ml);
		}

		channel = null;
		handler = null;
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			try {
				handler = new DataMonitorHandler();
				DataMonitorInitializer mi = new DataMonitorInitializer(false, handler);

				Bootstrap b = new Bootstrap();
				b.group(group).channel(NioSocketChannel.class).handler(mi);
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				logger.info("Attempting to connect to " +host +port);

				// Make the connection attempt.
				channel = b.connect(host, port).syncUninterruptibly();
				channel.awaitUninterruptibly(5000l);
				
				logger.info("Sysout 2 Attempting to connect to " +host +port);

				
				channel.channel().closeFuture().addListener(new DataMonitorClosedListener(this));

				if (N == Integer.MAX_VALUE)
					N = 1;
				else
					N++;

				if (listeners.size() > 0) {
					
					for (DataMonitorListener ml : listeners)
					{
						handler.addListener(ml);
						logger.info("The listeners size is " + listeners.size());
						
					}
					listeners.clear();
				}
			} catch (Exception ex) {
				if (logger.isDebugEnabled())
					logger.debug("DataMonitor: failed to initialize the Databeat connection", ex);
			}
		}

		if (channel != null && channel.isDone() && channel.isSuccess())
		{	
			logger.info("Data Channel Created");
			return channel.channel();
		} else {
			return channel.channel();
		}
	}

	public boolean isConnected() {
		
		if (channel == null)
			return false;
		else {
			//System.out.println("is connected : "+ channel.channel().localAddress());
			return channel.channel().isOpen();
		}
	}

	public String getNodeInfo() {
		if (host != null)
			return host + ":" + port;
		else
			return "Unknown";
	}

	public boolean startHeartbeat() {
		boolean rtn = false;
		try {
			Channel ch = connect();
			if (!ch.isWritable()) {
				logger.info("Data Channel to node " + toNodeId + " not writable!");
			}

			logger.info("DataMonitor sending join message to " + toNodeId);

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
			
			
			ch.writeAndFlush(b.build());
			
			rtn = true;
			
			logger.info("msg sent " + toNodeId);
		} catch (Exception e) {
			logger.info("Data Channel to node " + toNodeId + " not writable!");
		}

		return rtn;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	/**
	 * for demo application only - this will enter a loop waiting for
	 * heartbeatMgr messages.
	 * 
	 * Notes:
	 * <ol>
	 * <li>this method is not used by the servers
	 * <li>blocks if connection is created otherwise, it returns if the node is
	 * not available.
	 * </ol>
	 */
	public void waitForever() {
		try {
			boolean connected = startHeartbeat();
			while (connected) {
				Thread.sleep(2000);
			}
		} catch (Exception e) {
			logger.error("Unexpected error", e);
		}
	}

	/**
	 * Called when the channel tied to the monitor closes. Usage:
	 * 
	 */
	public static class DataMonitorClosedListener implements ChannelFutureListener {
		private DataMonitor monitor;

		public DataMonitorClosedListener(DataMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			monitor.release();
		}
	}
}
