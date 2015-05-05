///*
// * copyright 2014, gash
// * 
// * Gash licenses this file to you under the Apache License,
// * version 2.0 (the "License"); you may not use this file except in compliance
// * with the License. You may obtain a copy of the License at:
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations
// * under the License.
// */
//package snapchatproto.client;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.protobuf.ByteString;
//import com.google.protobuf.GeneratedMessage;
//
//import snapchatproto.client.comm.CommConnection;
//import snapchatproto.client.comm.CommListener;
//import snapchatproto.comm.App.ClusterMessage;
//import snapchatproto.comm.App.ClusterMessage.MessageType;
//import snapchatproto.comm.App.Header;
//import snapchatproto.comm.App.Payload;
//import snapchatproto.comm.App.Ping;
//import snapchatproto.comm.App.Request;
//
///**
// * The command class is the concrete implementation of the functionality of our
// * network. One can view this as a interface or facade that has a one-to-one
// * implementation of the application to the underlining communication.
// * 
// * IN OTHER WORDS (pay attention): One method per functional behavior!
// * 
// * @author gash
// * 
// */
//public class ClientCommandNew {
//	protected static Logger logger = LoggerFactory.getLogger("client");
//
//	private int numberOfNodes = 4;
//	private String[] host = new String[numberOfNodes];
//	private int[] port = new int[numberOfNodes];
//	private static ArrayList<CommConnection> con;
//
//	public ClientCommandNew() {
//		host[2] = "192.168.0.5";
//		port[2] = 5570;
//
//		host[0] = "192.168.0.5";
//		port[0] = 5570;
//		
//		host[1] = "192.168.0.5";
//		port[1] = 5570;
//		
//		host[3] = "192.168.0.5";
//		port[3] = 5570;
//		
//		init();
//	}
//
//	private void init() {
//		con = new ArrayList<CommConnection>();
//		for(int i=0;i<numberOfNodes;i++) {
//			con.add(new CommConnection(host[i], port[i]));
//		}
//	}
//
//	/**
//	 * add an application-level listener to receive messages from the server (as
//	 * in replies to requests).
//	 * 
//	 * @param listener
//	 */
//	public void addListener(CommListener listener) {
//		for(int i=0; i<numberOfNodes; i++)
//			con.get(i).addListener(listener);
//	}
//
//	/**
//	 * Our network's equivalent to ping
//	 * 
//	 * @param tag
//	 * @param num
//	 */
//	public void poke(String tag, int num) {
//		// data to send
//		Ping.Builder f = Ping.newBuilder();
//		f.setTag(tag);
//		f.setNumber(num);
//
//		// payload containing data
//		Request.Builder r = Request.newBuilder();
//		Payload.Builder p = Payload.newBuilder();
//		ClusterMessage.Builder cBuilder = ClusterMessage.newBuilder();
//
//		cBuilder.setMsgImageName("1.jpg");
//
//		String fpath = "/home/vishwarulz/Desktop/ClientFolder/temp.jpg" ;
//
//
//
//		File file = new File(fpath);
//
//		System.out.println("req.getFileName() : " + fpath);
//
//		if(file.exists() && file.isFile()) {
//			System.out.println("in read jobmamanger flie exists");
//
//			FileInputStream fis = null;
//			try {
//				fis = new FileInputStream(file);
//			} catch (FileNotFoundException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//
//
//			ByteString bs = null;
//			try {
//				bs = ByteString.readFrom(fis);
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//			System.out.println("Size of the file is"+bs.size());
//
//			cBuilder.setMsgImageBits(bs);
//
//			cBuilder.setMessageType(MessageType.REQUEST);
//
//			p.setPing(f.build());
//
//			p.setClusterMessage(cBuilder.build());
//
//
//			r.setBody(p.build());
//
//			// header with routing info
//			Header.Builder h = Header.newBuilder();
//			h.setOriginator(1000);
//			h.setTag("test finger");
//			h.setTime(System.currentTimeMillis());
//			h.setRoutingId(Header.Routing.PING);
//			r.setHeader(h.build());
//
//				Request req = r.build();
//
//			try {
//				
//				for(int i=0;i <numberOfNodes; i++)
//					con.get(i).sendMessage(req);
//			} catch (Exception e) {
//				logger.warn("Unable to deliver message, queuing");
//			}
//		}
//		
//	}
//
//
//	public static void main(String args[]) {
//		ClientCommandNew comm = new ClientCommandNew();
//		comm.poke("client", 1000);
//	}
//}
