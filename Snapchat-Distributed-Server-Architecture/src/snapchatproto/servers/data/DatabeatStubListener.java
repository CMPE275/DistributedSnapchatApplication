package snapchatproto.servers.data;

import io.netty.channel.ChannelHandlerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.comm.App.Request;
import snapchatproto.servers.queue.DiscreteQueue;

/**
 * This class bridges (glues) the server's processing to the general heart
 * monitor that is shared between external clients and internal use.
 * 
 * @author gash
 * 
 */
public class DatabeatStubListener implements DataMonitorListener {
	protected static Logger logger = LoggerFactory.getLogger("AppHeartbeatStubListener");

	private AppData data;

	public DatabeatStubListener(AppData data) {
		this.data = data;
	}

	public AppData getData() {
		return data;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.monitor.MonitorListener#getListenerID()
	 */
	@Override
	public Integer getListenerID() {
		return data.getNodeId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.monitor.MonitorListener#onMessage(eye.Comm.Management)
	 */
	@Override
	public void onMessage(Request msg, ChannelHandlerContext ctx) {
		if (logger.isDebugEnabled())
			logger.debug("appdata HB from node " + msg.getHeader().getOriginator());
		//System.out.println("onMessage in AppHeartbeatStubListener");
		DiscreteQueue.enqueueIn(msg, ctx.channel());
	}

	@Override
	public void connectionClosed() {
		// note a closed management port is likely to indicate the primary port
		// has failed as well
	}

	@Override
	public void connectionReady() {
		// do nothing at the moment
	}
}
