package snapchatproto.servers.data;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.comm.App.Request;
import snapchatproto.servers.conf.ServerConf;

public class DataChannelManager {
	protected static Logger logger = LoggerFactory.getLogger("DataChannelManager");
	protected static AtomicReference<DataChannelManager> instance = new AtomicReference<DataChannelManager>();

	private static ServerConf conf;

	public static DataChannelManager initManager(ServerConf conf) {
		DataChannelManager.conf = conf;
		instance.compareAndSet(null, new DataChannelManager());
		return instance.get();
	}

	public static DataChannelManager getInstance() {
		return instance.get();
	}

	/**
	 * initialize the manager for this server
	 * 
	 */
	protected DataChannelManager() {

	}

	/**
	 * @param args
	 */
	public void processRequest(Request req, Channel channel) {
		if (channel.isOpen()) {
			SocketAddress socka = channel.localAddress();
			if (socka != null) {
				// this node will send messages to the requesting client

				InetSocketAddress isa = (InetSocketAddress) socka;
				//logger.info("appdata APP REQUEST TO JOIN : " + isa.getHostName() + ", " + isa.getPort());
				DatabeatManager.getInstance().addOutgoingChannel(req.getHeader().getOriginator(), isa.getHostName(),
						isa.getPort(), channel, socka);
			}
		} else
			logger.warn(req.getHeader().getOriginator() + " not writable");
	}
}
