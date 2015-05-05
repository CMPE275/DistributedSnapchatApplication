package snapchatproto.servers.data;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

public class DataMonitorInitializer extends ChannelInitializer<SocketChannel> {
	boolean compress = false;
	DataMonitorHandler appMonitorHandler;

	public DataMonitorInitializer(boolean enableCompression, DataMonitorHandler appMonitorHandler) {
		this.compress = enableCompression;
		this.appMonitorHandler = appMonitorHandler;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		// Enable stream compression (you can remove these two if unnecessary)
		
		/**
		 * length (4 bytes).
		 * 
		 * Note: max message size is 64 Mb = 67108864 bytes this defines a
		 * framer with a max of 64 Mb message, 4 bytes are the length, and strip
		 * 4 bytes
		 */
		pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(990517518, 0, 4, 0, 4));

		// decoder must be first
		pipeline.addLast("protobufDecoder", new ProtobufDecoder(snapchatproto.comm.App.Request.getDefaultInstance()));
		pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		pipeline.addLast("protobufEncoder", new ProtobufEncoder());

		// our server processor (new instance for each connection)
		pipeline.addLast("handler", appMonitorHandler);
	}
}
