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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.minidev.json.JSONObject;
import network.AbstractMessage;
import network.ControlMessage;
import network.EvolveMessage;
import network.MessageWrapper;
import network.NeighborListRequest;
import network.Ping;
import node.NodeList.NoSuchNodeIDException;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import utility.ArgLexer;
import utility.ArgParser;
import utility.ArgParser.UnhandledArgException;
import utility.IP;
import utility.MsgID;
import utility.MsgIDFactory;
import utility.Signal;
import utility.TextFormatter;

/**
 * Control is the main thread which interprets control messages received from
 * the mailbox
 * 
 * @author Dylan Sherry
 */
public class Control extends AbstractThread implements Publisher {
	// the ID of this node
	public final NodeDescriptor desc;
	// the NodeList
	public NodeList nodeList;
	// the loop control variable
	public boolean go;

	// a Set of outstanding Ping message IDs
	public Map<MsgID, MessageWrapper<AbstractMessage>> pingIDs;
	// the queue for received Control messages
	public BlockingQueue<ControlMessage> ctrlMsgsRecv;
	// the queue for Control messages to send;
	public BlockingQueue<MessageWrapper<AbstractMessage>> ctrlMsgsSend;
	// should we report things via the reporter?
	private final Boolean REPORT;
	// Reporter queue
	private final BlockingQueue<JSONObject> reportSend;
	// the PingCounter
	private final Signal pc;
	// msgID generator
	private MsgIDFactory msgIDFactory;

	// the port to send messages to on other nodes
	public static int PORT = 9000;
	// time (in seconds) to wait between sending random pings
	public final static int randomPingTime = 6;
	// ping timeout (in milliseconds)
	public final static int pingTimeout = 5000;
	// number of neighbors to query for neighbor list updates
	public final static int neighborsToQuery = 1;
	private static Logger log = Logger.getLogger("node.Control");

	public Control(ArgParser p, BlockingQueue<ControlMessage> _ctrlMsgsRecv,
			BlockingQueue<MessageWrapper<AbstractMessage>> msgsSend,
			BlockingQueue<JSONObject> _reportSend) {
		super();
		// for now hard-code REPORT to false
		REPORT = false;
		ctrlMsgsRecv = _ctrlMsgsRecv;
		ctrlMsgsSend = msgsSend;
		reportSend = _reportSend;
		try {
			log.setUseParentHandlers(false);
			Handler fh = new FileHandler("control.log");
			fh.setFormatter(new TextFormatter());
			log.addHandler(fh);
			log.setLevel(Level.ALL);
		} catch (SecurityException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// generate a node description
		// desc = new NodeDescriptor(new IP(makeNodeID(p.my_ip), p.my_ip,
		// PORT));
		PORT = p.my_ip.port;
		desc = new NodeDescriptor(p.my_ip, p.subnetID);

		msgIDFactory = new MsgIDFactory(desc.getID());

		// setup the NodeList
		nodeList = new NodeList(random, p.targetNodeCount, desc.getID(),
				desc.getSubnetID());
		// signal the run() loop to continue once started
		go = true;
		// location to store pingIDs
		pingIDs = Collections
				.synchronizedMap(new HashMap<MsgID, MessageWrapper<AbstractMessage>>());
		// PingCounter init
		pc = new Signal(randomPingTime);
		// add each entry to the NodeList
		for (IP ipEntry : p.ips) {
			nodeList.succeed(ipEntry, NodeList.DEFAULT_SUBNET_ID,
					NodeList.currTime());
		}
		logOneTime("Loaded in " + p.ips.size() + " existing ips, my IP is "
				+ p.my_ip + " expected network size is " + p.targetNodeCount);
		logOneTime("Starting nodelist is " + nodeList.toString());
	}

	public static void println(String arg) {
		println(arg, false);
	}

	public static void println(String arg, boolean error) {
		((error) ? System.err : System.out).println(arg);
		if (error) {
			log.warning(arg);
		} else {
			log.info(arg);
		}
	}

	public static void logOneTime(String message) {
		logOneTime(message, Level.INFO);
	}

	public static void logOneTime(String message, Level level) {
		Logger oneLog = Logger.getLogger("staticLog");
		StackTraceElement st = Thread.currentThread().getStackTrace()[3];
		oneLog.logp(level, st.getClassName(), st.getMethodName(), message);
	}

	public MsgID makeMessageID() {
		return msgIDFactory.get();
	}

	/**
	 * main loop
	 */
	@Override
	public void run() {
		// start PingCounter
		new Thread(pc).start();
		// enter main loop
		while (go) {
			// TODO recursive startup / cleaning
			// make these all helper functions so that the recursive startup bit
			// is cleaner
			// receive and act upon any incoming messages.
			acceptMessages();
			// Check on all old pings and (possibly) send a new one.
			checkAndSendPings();

			// DEBUG /*
			// print out current NodeList
			println(nodeList.toString());
			// print out current outstanding Pings
			println("Control: pingIDs: " + pingIDs.toString());
			// sleep for a second
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			println("");
			// */ END DEBUG
			if (REPORT)
				reportSend.add(report());
		}
		// go has been set to false for some reason
		pc.shutdown();
	}

	private void checkAndSendPings() {
		// check for expired pings
		println("Control: Checking outstanding pings");
		Timestamp t_now = NodeList.currTime();
		Set<MsgID> old = new HashSet<MsgID>();
		synchronized (pingIDs) {
			Set<MsgID> s = pingIDs.keySet();
			Iterator<MsgID> it = s.iterator();
			while (it.hasNext()) {
				MsgID msg = it.next();
				Timestamp t_msg = new Timestamp(
						pingIDs.get(msg).msg.time.getTime() + pingTimeout);
				if (t_now.after(t_msg)) {
					old.add(msg);
				}
			}
		}
		// remove any nodes which timed-out
		for (MsgID msgID : old) {
			String destID = msgID.nodeID;
			MessageWrapper<AbstractMessage> msg = pingIDs.get(msgID);
			println("Control: ping to " + msgID.nodeID + " (" + msg.nle.ip
					+ ":" + msg.nle.port + " timed out after " + pingTimeout
					+ " milliseconds");
			pingIDs.remove(msgID);
			nodeList.fail(destID, msg.nle.ip, msg.nle.port,
					msg.nle.subnetLabel, t_now);
		}

		// check if PingCounter has signaled
		println("Control: Checking PingCounter");
		if (pc.getSignal()) {
			// reset the signal
			pc.signalFalse();
			NodeList nodeList_copy;
			// get a snapshot to work with
			synchronized (nodeList) {
				nodeList_copy = nodeList.copy();
			}
			println("Control: PingCounter has signaled");
			if (nodeList_copy.goodNodeTable.size() > 0) {
				try {
					String destID;
					NodeListEntry nle;
					if (!nodeList_copy.satisfied()) {
						// send our nodeList to others
						Set<String> ids = nodeList_copy.goodNodeTable.keySet();
						if (ids.size() > neighborsToQuery) {
							// grab 6 random nodes to send to
							Set<String> candidates = new HashSet<String>();
							candidates.addAll(ids);
							ids = new HashSet<String>();
							for (int i = 0; i < neighborsToQuery; i++) {
								destID = getRandomKey(candidates);
								candidates.remove(destID);
								ids.add(destID);
							}
						}
						println("Control: need to know about more nodes - sending requests to "
								+ ids.size() + " neighbors!");
						for (String id : ids) {
							nle = nodeList_copy.get(id);
							NeighborListRequest nlrq = new NeighborListRequest(
									makeMessageID(), desc.getID(),
									desc.getIP(), desc.getPort(),
									desc.getSubnetID(), nodeList_copy);
							pingIDs.put(nlrq.msgID, sendCtrlMsg(nlrq, id, nle));
						}
					}

					// send a random ping
					// first select a destination
					// but don't send a ping to someone we're already waiting to
					// hear back from
					// TODO: Should periodically ping dead nodes? In case they
					// come back up?
					for (MsgID id : pingIDs.keySet()) {
						nodeList_copy.goodNodeTable.remove(IP.ipToInt(pingIDs
								.get(id).nle.ip));
					}
					if (nodeList_copy.goodNodeTable.size() > 0) {
						destID = getRandomKey(nodeList_copy.goodNodeTable
								.keySet());
						nle = nodeList_copy.get(destID);
						// Note: passing src IP and port here, not dest
						Ping p = new Ping(makeMessageID(), desc.getID(),
								desc.getIP(), desc.getPort(),
								desc.getSubnetID());
						println(String.format(
								"Control: attempting to send ping to %s:%d",
								nle.ip, nle.port));
						pingIDs.put(p.msgID, sendCtrlMsg(p, destID, nle));
					}
				} catch (NoSuchNodeIDException e) {
					// that would be bad
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Read Control messages from the mailbox
	 */
	private void acceptMessages() {
		int controlMessagesNum;
		ControlMessage currentMsg;
		// watch the mailbox' queue
		println("Control: checking mailbox");
		controlMessagesNum = ctrlMsgsRecv.size();
		println("Control: " + controlMessagesNum + " outstanding messages.");
		while (controlMessagesNum > 0) {
			// allow mailbox to add more messages to the queue
			// pop off queue
			currentMsg = ctrlMsgsRecv.remove();
			println("Control: checking message " + currentMsg.msgID + "-"
					+ currentMsg.msgType + " from node at " + currentMsg.srcIP
					+ ":" + currentMsg.srcPort);
			// update the control queue size
			controlMessagesNum -= 1;
			// record successful response
			nodeList.succeed(currentMsg.srcID, currentMsg.srcIP,
					currentMsg.srcPort, currentMsg.srcSubnetID,
					NodeList.currTime());
			// if this was an outstanding message, remove it from the
			// PingResponse
			// queue
			if (currentMsg.origMsgID != null
					&& pingIDs.remove(currentMsg.origMsgID) != null) {
				Control.println("Response received from node at "
						+ currentMsg.srcIP);
			}
			// take appropriate action
			println("Control: acting");
			currentMsg.act(this);
		}
	}

	// public MessageWrapper<AbstractMessage> sendCtrlMsg(ControlMessage m,
	// String id, String destIP, int destPort) {
	//
	// }

	public MessageWrapper<AbstractMessage> sendCtrlMsg(ControlMessage m,
			String id, NodeListEntry nle_dest) {
		MessageWrapper<AbstractMessage> mw = new MessageWrapper<AbstractMessage>(
				m, id, nle_dest);
		if (ctrlMsgsSend.remainingCapacity() > 0)
			ctrlMsgsSend.add(mw);
		else {
			println("Cannot add message to ctrlMsgsSend: queue full at size "
					+ ctrlMsgsSend.size(), true);
		}
		return mw;
	}

	/**
	 * The main point of entry for all FlexEA code
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Logger oneLog = Logger.getLogger("staticLog");
		try {
			Handler fh = new FileHandler("init.log");
			fh.setFormatter(new SimpleFormatter());
			oneLog.addHandler(fh);
			oneLog.setLevel(Level.ALL);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ArgLexer l = new ArgLexer(args);
		// parse arguments
		// since ArgParser defines the command-line grammar,
		// need to make sure ArgParser can handle parsing parent IP and
		// other recursive startup command-line parameters
		ArgParser p = null;
		try {
			p = new ArgParser(l.groups);
		} catch (UnhandledArgException e) {
			println("Control.main: " + e.getMessage(), true);
			e.printStackTrace();
			System.exit(1);
		}

		// setup queues
		BlockingQueue<ControlMessage> ctrlMsgsRecv = new LinkedBlockingQueue<ControlMessage>(
				p.maxMsgs);
		BlockingQueue<EvolveMessage> evolveMsgsRecv = new LinkedBlockingQueue<EvolveMessage>(
				p.maxMsgs);
		BlockingQueue<MessageWrapper<AbstractMessage>> msgsSend = new LinkedBlockingQueue<MessageWrapper<AbstractMessage>>(
				p.maxMsgs);
		BlockingQueue<JSONObject> reportSend = new LinkedBlockingQueue<JSONObject>();

		// setup Mailbox
		// hardcode REPORT to false for now -- this reporting was added for owen's thesis
		// note: similarly hardcoded to false inside Control/Evolve/Mailbox to ensure
		// the queue doesn't overflow
		Boolean REPORT = false;
		if (REPORT) {
			Reporter r = new Reporter(reportSend);
			new Thread(r).start();
		}
		Control c = new Control(p, ctrlMsgsRecv, msgsSend, reportSend);
		Mailbox m = new Mailbox(p.my_ip.port, c.nodeList, ctrlMsgsRecv,
				evolveMsgsRecv, msgsSend, reportSend);
		if (p.lib_args != null) {
			// setup Evolution
			Evolve e = new Evolve(p, c.nodeList, m, c.msgIDFactory, c.desc,evolveMsgsRecv, msgsSend, reportSend);
			// start Evolution
			new Thread(e).start();
		}
		// start Mailbox
		new Thread(m).start();
		// run
		c.run();
		m.shutdown();
	}

	public static String getExtension(String fileName) {
		String[] tokens = fileName.split("\\.");
		return tokens[tokens.length - 1];
	}

	@Override
	public JSONObject report() {
		JSONObject props = new JSONObject();
		props.put("ip", desc.getIP());
		props.put("id", desc.getID());
		props.put("subnetID", desc.getSubnetID());
		props.put("age", desc.getAge());

		Sigar s = new Sigar();
		JSONObject sys = new JSONObject();
		try {
			Cpu c = s.getCpu();
			Mem m = s.getMem();
			sys.put("cpu_total", c.getTotal());
			sys.put("cpu_user", c.getUser());
			sys.put("mem_total", m.getTotal());
			sys.put("mem_used", m.getActualUsed());
		} catch (SigarException e) {
			e.printStackTrace();
			sys.put("cpu_total", (long) -1);
			sys.put("cpu_user", (long) -1);
			sys.put("mem_total", (long) -1);
			sys.put("mem_used", (long) -1);
		}
		JSONObject control = new JSONObject();
		control.put("props", props);
		control.put("system", sys);
		control.put("timestamp", System.currentTimeMillis());
		JSONObject obj = new JSONObject();
		obj.put("control", control);
		return obj;
	}
}
