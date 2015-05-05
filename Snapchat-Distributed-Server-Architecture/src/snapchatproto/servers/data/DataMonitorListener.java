package snapchatproto.servers.data;

import io.netty.channel.ChannelHandlerContext;
import snapchatproto.comm.App.Request;

public interface DataMonitorListener {

	public abstract Integer getListenerID();

	public abstract void onMessage(Request msg, ChannelHandlerContext ctx);

	/**
	 * called when the connection fails or is not readable.
	 */
	public abstract void connectionClosed();

	/**
	 * called when the connection to the node is ready. This may occur on the
	 * initial connection or when re-connection after a failure.
	 */
	public abstract void connectionReady();

}