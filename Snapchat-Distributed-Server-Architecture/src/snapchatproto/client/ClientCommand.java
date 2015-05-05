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
package snapchatproto.client;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import snapchatproto.client.comm.CommConnection;
import snapchatproto.client.comm.CommListener;
import snapchatproto.comm.App.ClientMessage;
import snapchatproto.comm.App.ClientMessage.MessageType;
import snapchatproto.comm.App.Header;
import snapchatproto.comm.App.Payload;
import snapchatproto.comm.App.Ping;
import snapchatproto.comm.App.Request;
import snapchatproto.constants.Constants;
import snapchatproto.util.FileUtil;

/**
 * The command class is the concrete implementation of the functionality of our
 * network. One can view this as a interface or facade that has a one-to-one
 * implementation of the application to the underlining communication.
 * IN OTHER WORDS (pay attention): One method per functional behavior!
 * @author gash
 * 
 */
public class ClientCommand {
	protected static Logger logger = LoggerFactory.getLogger("ClientCommand");

	private String host;
	private int port;
	private static CommConnection comm;

	public ClientCommand(String host, int port) {
		this.host = host;
		this.port = port;
		init();
	}

	private void init() {
		comm = new CommConnection(host, port);
	}

	/**
	 * add an application-level listener to receive messages from the server (as
	 * in replies to requests).
	 * @param listener
	 */
	public void addListener(CommListener listener) {
		comm.addListener(listener);
	}

	/**
	 * Our network's equivalent to ping
	 * @param tag
	 * @param num
	 */
	public void sendFile(String fname, String fpath) {
		try {
			// data to send
			Ping.Builder f = Ping.newBuilder();
			f.setTag(Constants.CLIENT_TAG);
			f.setNumber(Constants.CLIENT_ID);

			// pay-load containing data
			Request.Builder r = Request.newBuilder();
			Payload.Builder p = Payload.newBuilder();
			p.setPing(f.build());

			ClientMessage.Builder cMsgBuilder = ClientMessage.newBuilder(); 
			cMsgBuilder.setMsgImageName(fname);

			ByteString bs = FileUtil.readFile(fname, fpath);

			if(bs != null) {
				cMsgBuilder.setMsgImageBits(bs);
				cMsgBuilder.setMessageType(MessageType.REQUEST);

				p.setClientMessage(cMsgBuilder.build());


				r.setBody(p.build());

				// header with routing info
				Header.Builder h = Header.newBuilder();
				h.setOriginator(Constants.CLIENT_ID);
				h.setTag(Constants.CLIENT_TAG);
				h.setTime(System.currentTimeMillis());
				h.setRoutingId(Header.Routing.PING);
				r.setHeader(h.build());

				Request req = r.build();

				comm.sendMessage(req);

			} 
		} catch (Exception e) {
			logger.error("Unable to deliver message, queuing");
		}

	}

	public void readFile(String fname) {
		try {
			// data to send
			Ping.Builder f = Ping.newBuilder();
			f.setTag(Constants.CLIENT_TAG);
			f.setNumber(Constants.CLIENT_ID);

			// pay-load containing data
			Request.Builder r = Request.newBuilder();
			Payload.Builder p = Payload.newBuilder();
			p.setPing(f.build());

			ClientMessage.Builder cMsgBuilder = ClientMessage.newBuilder(); 
			cMsgBuilder.setMsgImageName(fname);

			cMsgBuilder.setMessageType(MessageType.REQUEST);
			p.setClientMessage(cMsgBuilder.build());

			r.setBody(p.build());

			// header with routing info
			Header.Builder h = Header.newBuilder();
			h.setOriginator(Constants.CLIENT_ID);
			h.setTag(Constants.CLIENT_TAG);
			h.setTime(System.currentTimeMillis());
			h.setRoutingId(Header.Routing.PING);
			r.setHeader(h.build());

			Request req = r.build();

			comm.sendMessage(req); 
		} catch (Exception e) {
			logger.error("Unable to deliver message, queuing");
		}


	}

	public static void main(String args[]) {
		Scanner sc = new Scanner(System.in);
		String fname = Constants.EMPTY_STRING;
		String fpath = Constants.EMPTY_STRING;

		ClientCommand comm = new ClientCommand(Constants.LOCALHOST, Constants.CLIENT_PORT);
		while(true) {
			System.out.println("Enter Operation : \n 1. Read File\n 2.Send File\n 3.Exit");
			int op = sc.nextInt();

			switch(op) {
			case 1: System.out.println("Enter File Path : ");
			fpath = sc.next().trim();
			System.out.println("Enter File Name : ");
			fpath = sc.next().trim();
			comm.sendFile(fname, fpath);
			break;
			case 2: System.out.println("Enter File Name : ");
			fpath = sc.next().trim();
			comm.readFile(fname);
			break;
			case 3: System.out.println("Bye");
			sc.close();
			System.exit(0);
			default:System.out.println("Enter Again");
			break;
			}
		}

	}
}
