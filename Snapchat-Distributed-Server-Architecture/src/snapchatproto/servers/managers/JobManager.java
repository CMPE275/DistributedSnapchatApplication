package snapchatproto.servers.managers;


import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import snapchatproto.comm.App.ClientMessage;
import snapchatproto.comm.App.ClientMessage.MessageType;
import snapchatproto.comm.App.Header;
import snapchatproto.comm.App.JobBid;
import snapchatproto.comm.App.Payload;
import snapchatproto.comm.App.Ping;
import snapchatproto.comm.App.Request;
import snapchatproto.constants.Constants;
import snapchatproto.servers.conf.ServerConf;
import snapchatproto.servers.queue.DiscreteQueue;
import snapchatproto.servers.queue.DiscreteQueue.OneQueueEntry;
import snapchatproto.util.FileUtil;

/**
 * The job manager class is used by the system to hold (enqueue) jobs and can be
 * used in conjunction to the voting manager for cooperative, de-centralized job
 * scheduling. This is used to ensure leveling of the servers take into account
 * the diversity of the network.
 * 
 * @author gash
 * 
 */
public class JobManager {
	protected static Logger logger = LoggerFactory.getLogger("job");
	protected static AtomicReference<JobManager> instance = new AtomicReference<JobManager>();

	public static JobManager initManager(ServerConf conf) {
		instance.compareAndSet(null, new JobManager());
		return instance.get();
	}

	public static JobManager getInstance() {
		return instance.get();
	}

	public JobManager() {
	}

	/**
	 * a new job proposal has been sent out that I need to evaluate if I can run
	 * it
	 * 
	 * @param req
	 *            The proposal
	 */
	public void processRequest(OneQueueEntry oneQueueEntry) {
		if (oneQueueEntry == null)
			return;
		
		if(oneQueueEntry.req.getBody().hasClusterMessage()) {
			//request to join from another cluster node
			
		} else if (oneQueueEntry.req.getBody().hasClientMessage()) {
			saveFileToDisk(oneQueueEntry);
			broadcastImageToAdjacentNodes(oneQueueEntry);
			LogListManager.getInstance().addNewLogEntry();
			sendResponseToClient(oneQueueEntry);
		}

	}

	private void saveFileToDisk(OneQueueEntry oneQueueEntry) {
		Request req = oneQueueEntry.req;
		ByteString fileBits = req.getBody().getClientMessage().getMsgImageBits();
		String fname = req.getBody().getClientMessage().getMsgImageName();
		logger.info("The size of received file is" + fileBits.size());
		FileUtil.writeFile(fname, Constants.SERVER_FILE_SAVE_PATH , fileBits.toByteArray());


	}

	private void broadcastImageToAdjacentNodes(OneQueueEntry oneQueueEntry) {
		try {
			Request req = oneQueueEntry.req;
		
			if(req != null && !req.getBody().getPing().getTag().equalsIgnoreCase("server-forward")) {
				return;
			}

			ClientMessage.Builder cm = ClientMessage.newBuilder();
			cm.setMessageType(MessageType.REQUEST);
			cm.setMsgImageBits(req.getBody().getClientMessage().getMsgImageBits());

			Ping.Builder pb = Ping.newBuilder();
			pb.setTag(Constants.SERVER_JOIN_TAG);
			pb.setNumber(Constants.SERVER_JOIN_NUMBER);

			Payload.Builder payB = Payload.newBuilder();
			payB.setClientMessage(cm.build());

			Request.Builder resp = Request.newBuilder();
			resp.setBody(payB.build());

			Header.Builder h = Header.newBuilder();
			h.setOriginator(123);
			h.setTag("test finger");
			h.setTime(System.currentTimeMillis());
			h.setRoutingId(Header.Routing.PING);
			resp.setHeader(h.build());

			System.out.println("File sent by client recieved by server..");

			ConnectionManager.broadcast2(resp.build());
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	private void sendResponseToClient(OneQueueEntry oneQueueEntry) {
		try {
			int myNodeId = Integer.parseInt(NodeDataManager.getInstance().getNodeData().getNodeId());

			Request req = oneQueueEntry.req;
			if(req != null && !req.getBody().getPing().getTag().equalsIgnoreCase("client"))
				return;

			ClientMessage.Builder cm = ClientMessage.newBuilder();
			cm.setMessageType(MessageType.SUCCESS);

			Payload.Builder pb = Payload.newBuilder();
			pb.setClientMessage(cm.build());


			Request.Builder resp = Request.newBuilder();
			resp.setBody(pb.build());

			Header.Builder h = Header.newBuilder();
			h.setOriginator(myNodeId);
			h.setTag(Constants.SUCCESS_TAG);
			h.setTime(System.currentTimeMillis());
			h.setRoutingId(Header.Routing.PING);
			resp.setHeader(h.build());


			DiscreteQueue.enqueueResp(resp.build(), oneQueueEntry.channel);

			System.out.println("File recieved");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * a job bid for my job
	 * 
	 * @param req
	 *            The bid
	 */
	public void processRequest(JobBid req) {

	}
}
