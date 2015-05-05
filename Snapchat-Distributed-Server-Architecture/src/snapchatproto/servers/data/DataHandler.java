/*
 * copyright 2012, gash
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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.comm.App.Request;
import snapchatproto.servers.managers.NodeDataManager;
import snapchatproto.servers.queue.DiscreteQueue;


/**
 * @author gash
 * 
 */
public class DataHandler extends SimpleChannelInboundHandler<Request> {
	protected static Logger logger = LoggerFactory.getLogger("DataHandler");
	NodeDataManager nodeDataManager;

	public DataHandler() {
		// logger.info("** HeartbeatHandler created **");
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Request req) throws Exception {
		logger.info("---> datahandler got a message from " + req.getHeader().getOriginator());
		DiscreteQueue.enqueueIn(req, ctx.channel());

	}
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object req) throws Exception {
		logger.info("---> datahandler got a message from " + ((Request)req).getHeader().getOriginator());
		DiscreteQueue.enqueueIn((Request)req, ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.error("data channel inactive");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}

	/**
	 * usage:
	 * 
	 * <pre>
	 * channel.getCloseFuture().addListener(new ManagementClosedListener(queue));
	 * </pre>
	 * 
	 * @author gash
	 * 
	 */
	public static class DataClosedListener implements ChannelFutureListener {
		public DataClosedListener(DiscreteQueue sq) {
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			
		}

	}
}
