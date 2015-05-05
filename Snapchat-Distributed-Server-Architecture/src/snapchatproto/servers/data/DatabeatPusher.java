package snapchatproto.servers.data;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabeatPusher extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("DatabeatPusher");
	protected static AtomicReference<DatabeatPusher> instance = new AtomicReference<DatabeatPusher>();

	private ConcurrentLinkedQueue<DataMonitor> monitors = new ConcurrentLinkedQueue<DataMonitor>();
	private int sConnectRate = 2000; // msec
	private boolean forever = true;

	public static DatabeatPusher getInstance() {
		instance.compareAndSet(null, new DatabeatPusher());
		return instance.get();
	}

	/**
	 * The connector will only add nodes for connections that this node wants to
	 * establish. Outbound requests do not come through
	 * this class.
	 * 
	 * @param node
	 */
	public void connectToThisNode(int iamNode, AppData node) {
		if (node == null || node.getNodeId() < 0)
			throw new RuntimeException("Null nodes or negative IDs are not allowed");

		DatabeatManager.getInstance().addAdjacentNode(node);

		DataMonitor hm = new DataMonitor(iamNode, node.getHost(), node.getPort(), node.getNodeId());
		monitors.add(hm);

		// artifact of the client-side listener - processing is done in the
		// inbound app worker
		DatabeatStubListener notused = new DatabeatStubListener(node);
		hm.addListener(notused);
	}

	@Override
	public void run() {
		if (monitors.size() == 0) {
			logger.info("App Data connection monitor not started, no connections to establish");
			return;
		} else
			logger.info("App Data connection monitor starting, node has " + monitors.size() + " connections");

		while (forever) {
			try {
				Thread.sleep(sConnectRate);
				
				// try to establish connections to our nearest nodes
				for (DataMonitor hb : monitors) {
					if (!hb.isConnected()) {
						try {
							if (logger.isDebugEnabled())
								logger.debug("appdata attempting to connect to node: " + hb.getNodeInfo());
							hb.startHeartbeat();
						} catch (Exception ie) {
							logger.error("Unexpected error ", ie);
						}
					}
				}
			} catch (InterruptedException e) {
				logger.error("Unexpected Databeat connector failure", e);
				break;
			}
		}
		logger.info("DatabeatPusher: ending DatabeatManager connection monitoring thread");
	}
}
