/**
 * Copyright (c) 2012 Evolutionary Design and Optimization Group
 * 
 * Licensed under the MIT License.
 * 
 * See the "LICENSE" file for a copy of the license.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 *
 */
package node;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minidev.json.JSONObject;
import network.AbstractMessage;
import network.ControlMessage;
import network.EvolveMessage;
import network.MessageType;
import network.MessageWrapper;

import org.apache.commons.lang3.exception.ExceptionUtils;

import utility.TextFormatter;

/**
 * The Mailbox thread which accepts and stores incoming messages
 * 
 * @author Dylan Sherry
 */
public class Mailbox implements Runnable, Publisher {
	// the queue for received Control messages
	private final BlockingQueue<ControlMessage> ctrlMsgsRecv;
	// the received Evolve message queue
	private final BlockingQueue<EvolveMessage> evolveMsgsRecv;
	// the queue for messages to send;
	private final BlockingQueue<MessageWrapper<AbstractMessage>> msgsSend;
	// should we report things via the reporter?
	private final Boolean REPORT;
	// the queue to send out reports on
	private final BlockingQueue<JSONObject> reportSend;

	// the mailbox socket for incoming messages
	ServerSocket serverSocket;

	// a signaling variable shared with Control for shutdown
	boolean shutdown;

	// the port number and default serverSocket backlog size
	final int port;
	final NodeList nodeList;
	int backlog;

	private static Logger log = Logger.getLogger("node.Mailbox");

	/**
	 * set up the mailbox
	 * 
	 * @param _msgsSend
	 * @param _evolveMsgsRecv
	 * @param _ctrlMsgsRecv
	 * @param reportSend2
	 */
	public Mailbox(int _port, NodeList _nodeList, BlockingQueue<ControlMessage> _ctrlMsgsRecv,
			BlockingQueue<EvolveMessage> _evolveMsgsRecv,
			BlockingQueue<MessageWrapper<AbstractMessage>> _msgsSend,
			BlockingQueue<JSONObject> _reportSend) {
		// for now, hard-code REPORT to false
		REPORT = false;
		ctrlMsgsRecv = _ctrlMsgsRecv;
		evolveMsgsRecv = _evolveMsgsRecv;
		msgsSend = _msgsSend;
		reportSend = _reportSend;
		port = _port;
		nodeList = _nodeList;
		shutdown = false;
		try {
			log.setUseParentHandlers(false);
			Handler fh = new FileHandler("mailbox.log");
			fh.setFormatter(new TextFormatter());
			log.addHandler(fh);
			log.setLevel(Level.ALL);
		} catch (SecurityException e) {
			e.printStackTrace();
			log.severe(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			e.printStackTrace();
			log.severe(ExceptionUtils.getStackTrace(e));
		}
		Control.logOneTime("Mailbox started up");
	}

	public Mailbox(int _port, int _backlog, NodeList _nodeList,
			BlockingQueue<ControlMessage> _ctrlMsgsRecv,
			BlockingQueue<EvolveMessage> _evolveMsgsRecv,
			BlockingQueue<MessageWrapper<AbstractMessage>> _msgsSend,
			BlockingQueue<JSONObject> _reportSend) {
		this(_port, _nodeList, _ctrlMsgsRecv, _evolveMsgsRecv, _msgsSend, _reportSend);
		backlog = _backlog;
	}

	public static void println(String arg) {
		println(arg, false);
	}

	// TODO make these methods part of AbstractThread, and make
	// Mailbox extend AbstractThread
	public static void println(String arg, boolean error) {
		((error) ? System.err : System.out).println("Mailbox: " + arg);
		if (error) {
			log.warning(arg);
		} else {
			log.info(arg);
		}
	}
	
	/**
	 * main loop
	 */
	@Override
	public void run() {
		Socket socket = null;
		// set up server socket
		try {
			if (backlog == 0) {
				serverSocket = new ServerSocket(port);
			} else {
				serverSocket = new ServerSocket(port, backlog);
			}
			serverSocket.setSoTimeout(100);
			println("Opened up server socket on port " + port);
		} catch (SocketException e) {
			Control.logOneTime(e.getLocalizedMessage(), Level.SEVERE);
			println(e.getLocalizedMessage());
			System.exit(-1);
		} catch (IOException e) {
			Control.logOneTime("Mailbox error: could not listen on port "
					+ port + ". stacktrace: " + e.getLocalizedMessage(),
					Level.SEVERE);
			e.printStackTrace();
			log.severe(ExceptionUtils.getStackTrace(e));
			System.exit(-1);
		}
		// enter main loop
		while (!shutdown) {
			if (REPORT)
				reportSend.add(report());
			println(report().toJSONString());

			// block until a connection is accepted
			println("Waiting for a connection");
			try {
				socket = serverSocket.accept();
				InetAddress inetaddr = socket.getInetAddress();

				println("Received connection from " + inetaddr);
				// get the message id, then call the appropriate message handler
				acceptMessage(socket);
				// close the socket
				socket.close();
			} catch (SocketTimeoutException e) {
				println("No connection: socket timed out", true);
			} catch (ConnectException e) {
				println("Caught ConnectException", true);
			} catch (SocketException e) {
				println("Caught SocketException", true);
			} catch (IOException e) {
				e.printStackTrace();
				log.severe(ExceptionUtils.getStackTrace(e));
			}

			// See if we have any messages to send
			if (msgsSend.size() > 0)
				println("Have " + msgsSend.size() + " messages to send");
			while (msgsSend.size() > 0) {
				MessageWrapper<AbstractMessage> currentMsg = msgsSend.remove();
				println("Current message: " + currentMsg.toString());
				NodeListEntry nle_dest = currentMsg.nle;
				String destID = currentMsg.destID;
				printMessageSize(currentMsg);
				currentMsg.msg.scheduleSend(nodeList, destID, nle_dest);
				println("Mailbox: Message of type " + currentMsg.msg.msgType
						+ " with id# " + currentMsg.msg.msgID + " scheduled for sending to "
						+ nle_dest.toString());
			}
			// check if it's time to shut down (signaled from Control)
			if (shutdown) {
				// write a log message, then exit
				// serverSocket.close()
				println("Mailbox exited");
				return;
			}
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.severe(ExceptionUtils.getStackTrace(e));
			}
		}
	}

	public void printMessageSize(MessageWrapper<AbstractMessage> message) {
		try {
			ByteArrayOutputStream baostemp = new ByteArrayOutputStream();
			ObjectOutputStream oostemp = new ObjectOutputStream(baostemp);
			oostemp.writeObject(message.msg);
			oostemp.flush();
			println("Message is " + baostemp.size() + " bytes");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	@Override
	public JSONObject report() {
		JSONObject props = new JSONObject();
		props.put("ctrlLen", ctrlMsgsRecv.size());
		props.put("evoLen", evolveMsgsRecv.size());
		props.put("outLen", msgsSend.size());
		props.put("timestamp", System.currentTimeMillis());
		JSONObject obj = new JSONObject();
		obj.put("mailbox", props);
		return obj;
	}

	/**
	 * Accept a message from a socket. Adds the message to the appropriate queue
	 */
	private void acceptMessage(Socket socket) {
		// get the source address
		String srcIP = socket.getInetAddress().getHostAddress();
		ObjectInputStream ois;
		try {
			InputStream is = socket.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			ois = new ObjectInputStream(bis);
			// now the Mailbox can block until it receives all message bytes
			// accept the message object
			AbstractMessage a = (AbstractMessage) ois.readObject();
			println("Received msg " + a.toString());
			// TODO MARK MESSAGE WITH OLD TIMESTAMP
			// save the srcIP for later. srcPort is already part of the message
			a.srcIP = srcIP;

			// cast to the appropriate message type
			if (a.msgType < MessageType.CONTROL) {
				// call the appropriate verifier, place the message in the queue
				// and call the appropriate message handler to reconstruct the
				// message over the network
				ControlMessage m = (ControlMessage) a;
				ctrlMsgsRecv.add(m);
			} else if (a.msgType < MessageType.EVOLVE) {
				EvolveMessage m = (EvolveMessage) a;
				evolveMsgsRecv.add(m);
			} else if (a.msgType < MessageType.DATA) {
				// TODO handle Data-specific messages (dataset/indexing
				// information, db webserver address, etc)
				throw new NoSuchMethodError("Handling DATA messages is not yet implemented");
			}
		} catch (StreamCorruptedException e) {
			println("Caught StreamCorruptedException: client must have terminated transmission", true);
		} catch (EOFException e) {
			println("Caught EOFException: client must have terminated transmission", true);
		} catch (IOException e) {
			log.log(Level.SEVERE,
					"Mailbox error: failed to accept message due to IOException",
					e);
			e.printStackTrace();
			log.severe(ExceptionUtils.getStackTrace(e));
		} catch (ClassNotFoundException e) {
			log.log(Level.SEVERE,
					"Mailbox error: failed to accept message due to ClassNotFoundException",
					e);
			e.printStackTrace();
			log.severe(ExceptionUtils.getStackTrace(e));
		}
	}

	public synchronized void shutdown() {
		shutdown = true;
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.severe(ExceptionUtils.getStackTrace(e));
		}
	}
	
	/**
	* Obtain the number of bytes a serialized object occupies
	 * 
	 * @throws IOException
	 */
	public static <N> int getSerializedSize(N n) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(n);
		oos.close();
		return out.toByteArray().length;
	}	
}
