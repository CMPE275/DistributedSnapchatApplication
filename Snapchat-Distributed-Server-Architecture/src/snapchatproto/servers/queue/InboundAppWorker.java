/*
 * copyright 2015, gash
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
package snapchatproto.servers.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.comm.App.Request;
import snapchatproto.constants.Constants;
import snapchatproto.servers.cluster.ClusterConnectionManager;
import snapchatproto.servers.data.DataChannelManager;
import snapchatproto.servers.managers.JobManager;
import snapchatproto.servers.queue.DiscreteQueue.OneQueueEntry;

public class InboundAppWorker extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("InboundAppWorker");

	int workerId;
	boolean forever = true;


	public InboundAppWorker(ThreadGroup tgrp, int workerId) {
		super(tgrp, "inbound-" + workerId);
		this.workerId = workerId;

		if (DiscreteQueue.inbound == null)
			throw new RuntimeException("connection worker detected null inbound queue");
	}

	@Override
	public void run() {

		while (true) {
			if (!forever && DiscreteQueue.inbound.size() == 0)
				break;

			try {
				// block until a message is enqueued
				OneQueueEntry msg = DiscreteQueue.inbound.take();

				// process request and enqueue response
				if (msg.req instanceof Request) {

					if(msg.req.getBody().hasClusterMessage()) {
						ClusterConnectionManager.getInstance().processRequest(msg.req, msg.channel);
					} else if(msg.req.getBody().hasClientMessage()) {
						if(msg.req.getBody().getPing().getTag().equalsIgnoreCase(Constants.SERVER_JOIN_TAG)) {
							DataChannelManager.getInstance().processRequest(msg.req, msg.channel);
						} else if ((msg.req.getBody().getPing().getTag().equalsIgnoreCase(Constants.CLIENT_TAG)) || (msg.req.getBody().getPing().getTag().equalsIgnoreCase(Constants.SERVER_FORWARD_TAG))) {
							JobManager jobManager = JobManager.getInstance();
							jobManager.processRequest(msg);
						}

					}
				}
			} catch (InterruptedException ie) {
				ie.printStackTrace();
				break;
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Unexpected processing failure", e);
				break;
			}
		}

		if (!forever) {
			logger.info("connection queue closing");
		}
	}
}