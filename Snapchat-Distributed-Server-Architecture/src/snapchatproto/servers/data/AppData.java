package snapchatproto.servers.data;

import io.netty.channel.Channel;

import java.net.SocketAddress;

import snapchatproto.servers.managers.ConnectionManager;

public class AppData {

	public static final int sWeakThresholdDefault = 2;
	public static final int sFailureThresholdDefault = 4;
	public static final int sFailureToSendThresholdDefault = 10;
	public static final int sBeatIntervalDefault = 10000;

	private int nodeId;
	private String host;
	private Integer port;
	private Integer mgmtport;
	private BeatStatus status = BeatStatus.Unknown;
	private int beatInterval = sBeatIntervalDefault;
	private int weakTheshold = sWeakThresholdDefault;
	private int failureThreshold = sFailureThresholdDefault;
	private int failures;
	private int failuresOnSend;
	private long initTime;
	private long lastBeat;
	private long lastBeatSent;
	private long lastFailed;

	public SocketAddress sa;

	public AppData(int nodeId, String host, Integer port, Integer mgmtport) {
		this.nodeId = nodeId;
		this.host = host;
		this.port = port;
		this.mgmtport = mgmtport;
	}

	/**
	 * 
	 * @return
	 */
	public Channel getChannel() {
		return ConnectionManager.getConnection(nodeId, false);
	}

	/**
	 * @param channel
	 */
	public void setAppChannel(Channel channel) {
		 ConnectionManager.addConnection(nodeId, channel, false);
	}

	public Integer getMgmtport() {
		return mgmtport;
	}

	public void setMgmtport(Integer mgmtport) {
		this.mgmtport = mgmtport;
	}

	/**
	 * a DataChannelManager may not be active (established) on initialization so, we
	 * need to provide a way to initialize the meta-data from the connection
	 * data.
	 * 
	 * This is the primary way the connection manager in populated with
	 * channels/connections
	 * 
	 * @param channel The newly established data channel
	 * @param sa
	 * @param nodeId the node ID of the channel
	 */
	public void setConnection(Channel channel, SocketAddress sa, Integer nodeId) {
		ConnectionManager.addConnection(nodeId, channel, false);
		this.sa = sa;
	}

	/**
	 * clear/reset internal tracking information and connection. The
	 * host/port/ID are retained.
	 */
	public void clearAll() {
		clearHeartData();
		initTime = 0;
		lastBeat = 0;
		lastBeatSent = 0;
		lastFailed = 0;
		failures = 0;
		failuresOnSend = 0;
		status = BeatStatus.Unknown;
	}

	public void incrementFailures() {
		failures++;
	}

	public void incrementFailuresOnSend() {
		failuresOnSend++;
	}

	public int getFailuresOnSend() {
		return failuresOnSend;
	}

	public void setFailuresOnSend(int failuresOnSend) {
		this.failuresOnSend = failuresOnSend;
	}

	public long getLastBeatSent() {
		return lastBeatSent;
	}

	public void setLastBeatSent(long lastBeatSent) {
		this.lastBeatSent = lastBeatSent;
	}

	public int getFailures() {
		return failures;
	}

	public void setFailures(int failures) {
		this.failures = failures;
	}

	public void clearHeartData() {
		if (ConnectionManager.getConnection(nodeId, false) != null)
			ConnectionManager.getConnection(nodeId, false).close();

		ConnectionManager.removeConnection(nodeId, false);
		sa = null;
	}

	/**
	 * verify the connection is good and if not determine if it should be
	 * disabled.
	 * @return
	 */
	public boolean isGood() {
		if (status == BeatStatus.Active || status == BeatStatus.Weak) {
			Channel ch = ConnectionManager.getConnection(nodeId, true);
			boolean rtn = ch.isOpen() && ch.isWritable();
			if (!rtn) {
				lastFailed = System.currentTimeMillis();
				failures++;
				if (failures >= failureThreshold)
					status = BeatStatus.Failed;
				else if (failures >= weakTheshold)
					status = BeatStatus.Weak;
			} else {
				failures = 0;
				lastFailed = 0;
			}

			return rtn;
		} else
			return false;
	}

	/**
	 * An assigned unique key (node ID) to the remote connection
	 * @return
	 */
	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * The host to connect to
	 * 
	 * @return
	 */
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * The port the remote connection is listening to
	 * @return
	 */
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public BeatStatus getStatus() {
		return status;
	}

	public void setStatus(BeatStatus status) {
		this.status = status;
	}

	public int getWeakTheshold() {
		return weakTheshold;
	}

	public void setWeakTheshold(int weakTheshold) {
		this.weakTheshold = weakTheshold;
	}

	public int getFailureThreshold() {
		return failureThreshold;
	}

	public void setFailureThreshold(int failureThreshold) {
		this.failureThreshold = failureThreshold;
	}

	public long getInitTime() {
		return initTime;
	}

	public void setInitTime(long initTime) {
		this.initTime = initTime;
	}

	public long getLastBeat() {
		return lastBeat;
	}

	public void setLastBeat(long lastBeat) {
		this.lastBeat = lastBeat;
	}

	public long getLastFailed() {
		return lastFailed;
	}

	public void setLastFailed(long lastFailed) {
		this.lastFailed = lastFailed;
	}

	public int getBeatInterval() {
		return beatInterval;
	}

	public void setBeatInterval(int beatInterval) {
		this.beatInterval = beatInterval;
	}

	public enum BeatStatus {
		Unknown, Init, Active, Weak, Failed
	}
}
