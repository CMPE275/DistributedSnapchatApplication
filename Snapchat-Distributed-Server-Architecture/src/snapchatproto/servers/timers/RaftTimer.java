package snapchatproto.servers.timers;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snapchatproto.servers.election.RaftElection;
import snapchatproto.servers.managers.NodeDataManager;
import snapchatproto.servers.managers.RaftElectionManager;
import snapchatproto.servers.managers.NodeData.RaftStatus;


public class RaftTimer implements Runnable {

	protected static Logger logger = LoggerFactory.getLogger("RaftTimer");
	
	protected static AtomicReference<RaftTimer> instance = new AtomicReference<RaftTimer>();

	private Integer syncPt = 1;
	public static RaftTimer initManager() {
		instance.compareAndSet(null, new RaftTimer());
		return instance.get();
	}

	public static RaftTimer getInstance() {
		// TODO throw exception if not initialized!
		return instance.get();
	}

	private static long lastBeatTime = System.currentTimeMillis();

	public long getLastBeatTime() {
		return lastBeatTime;
	}

	public static void setLastBeatTime(long lastBeatTime1) {
		lastBeatTime = lastBeatTime1;
	}

	@Override
	public void run() {

		//generate a random timeout 
		long randomTimeOut = getRandomTimeOut();

		while(true)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
			
			// If the follower does not receive heartbeat from leader node within the timeout interval, then start election
			if((System.currentTimeMillis()-lastBeatTime) > getRandomTimeOut())
			{
					
				lastBeatTime = System.currentTimeMillis();
				RaftElectionManager raftManager = RaftElectionManager.getInstance();

				if(NodeDataManager.getInstance().getNodeData().getNodeStatus() != RaftStatus.LEADER)
				{	
					logger.info("Timer has elapsed. This Follower node did not receive heartbeat from leader node. Starting election now..");
					raftManager.startElection();
				}

			}

		}		

	}

    //Generate random timeout
	public int getRandomTimeOut() {
		
		Random rgen = new Random();
		
		int rtimeout = rgen.nextInt(50000);
		rtimeout = rtimeout > 20000 ? rtimeout : (rtimeout + 30000) ;

		return rtimeout;
	}

}