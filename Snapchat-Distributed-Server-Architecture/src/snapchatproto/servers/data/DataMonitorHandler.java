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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.comm.App.Request;
import snapchatproto.servers.queue.ChannelQueue;

import com.google.protobuf.GeneratedMessage;

/**
 * Receive a heartbeat (HB). This class is used internally by servers to receive
 * HB notices for nodes in its neighborhood list (DAG).
 * 
 * @author gash
 * 
 */
public class DataMonitorHandler extends SimpleChannelInboundHandler<Request> {
	protected static Logger logger = LoggerFactory.getLogger("appdata");

	protected ConcurrentMap<Integer, DataMonitorListener> listeners = new ConcurrentHashMap<Integer, DataMonitorListener>();
	private volatile Channel channel;
	
	private ChannelQueue queue;

	public DataMonitorHandler() {
	}

	public String getNodeName() {
		if (channel != null)
			return channel.localAddress().toString();
		else
			return String.valueOf(this.hashCode());
	}

	public Integer getNodeId() {
		if (listeners.size() > 0)
			return listeners.values().iterator().next().getListenerID();
		else
			return -9999;
	}

	public void addListener(DataMonitorListener listener) {
		if (listener == null)
			return;

		logger.info(listener.getListenerID() +" added");
		listeners.putIfAbsent(listener.getListenerID(), listener);
	}

	public boolean send(GeneratedMessage msg) {
		// TODO a queue is needed to prevent overloading of the socket
		// connection. For the demonstration, we don't need it
		ChannelFuture cf = channel.writeAndFlush(msg);
		if (cf.isDone() && !cf.isSuccess()) {
			logger.error("failed to send appdata message!");
			return false;
		}

		return true;
	}

	/**
	 * a message was received from the server. Here we dispatch the message to
	 * the listeners to process - note if the listeners are not threaded, this
	 * read will block until all listeners have processed the message.
	 * 
	 * @param msg
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Request msg) throws Exception {
		//System.out.println("channelRead0 in AppMonitorHandler");
		for (Integer id : listeners.keySet()) {
			DataMonitorListener ml = listeners.get(id);
			// TODO this may need to be delegated to a thread pool to allow
			// async processing of replies
			ml.onMessage(msg, ctx);
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		//super.channelReadComplete(ctx);
		//System.out.println("In channel read complete");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		//System.out.println("channelRead in AppMonitorHandler");
		for (Integer id : listeners.keySet()) {
			DataMonitorListener ml = listeners.get(id);
			// TODO this may need to be delegated to a thread pool to allow
			// async processing of replies
			ml.onMessage((Request)msg, ctx);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.error("appdata monitor channel inactive");

		// TODO try to reconnect
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from appdata downstream.", cause);
		ctx.close();
	}

}
