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

import evogpj.evaluation.java.SRLARSJava;
import evogpj.gp.Population;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import evogpj.algorithm.Parameters;

import net.minidev.json.JSONObject;
import network.AbstractMessage;
import network.EvolveMessage;
import network.MessageWrapper;
import node.Algorithm.AlgorithmException;
import utility.ArgParser;
import utility.MsgIDFactory;
import utility.TextFormatter;

/**
 * The Evolve thread maintains evolutionary computation while providing access
 * from Control
 * 
 * @author Owen Derby
 */
public class Evolve extends AbstractThread {

	private Algorithm alg;
	private static Logger log = Logger.getLogger("node.Evolve");
	// should we report things via the reporter?
	private final Boolean REPORT;
	private final BlockingQueue<JSONObject> reportSend;

	// the queue for received Evolve messages
	public BlockingQueue<EvolveMessage> evolveMsgsRecv;
	// the queue for Evolve messages to send;
	public BlockingQueue<MessageWrapper<AbstractMessage>> evolveMsgsSend;
	// for storing incoming migrants
	public Population migrants;
	// reference to Control thread's nodelist
	public NodeList nodeList;
	// the mailbox
	public Mailbox mailbox;
	private MsgIDFactory msgIDFactory;
	private final NodeDescriptor desc;
	private final ArgParser p;

	
	public Evolve(ArgParser _p, NodeList _nodeList, Mailbox _mailbox,
			MsgIDFactory _msgIDFactory, NodeDescriptor _desc,
			BlockingQueue<EvolveMessage> _evolveMsgsRecv,
			BlockingQueue<MessageWrapper<AbstractMessage>> _msgsSend,
			BlockingQueue<JSONObject> _reportSend) {
		super();
		// for now hard-code REPORT to false
		REPORT = false;
		p = _p;
		nodeList = _nodeList;
		mailbox = _mailbox;
		msgIDFactory = _msgIDFactory;
		desc = _desc;
		evolveMsgsRecv = _evolveMsgsRecv;
		evolveMsgsSend = _msgsSend;
		reportSend = _reportSend;
		try {
			log.setUseParentHandlers(false);
			Handler fh = new FileHandler("evolve.log");
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
		migrants = new Population();

		// set up EvoGPJ in run() method, so that the data gets loaded in a separate thread.
		alg = null;
		// Don't log evolve messages to reportSend, since they're saved in evogpj-log.json
		// reportSend.add(rep);
	}

	public static Logger getLog() {
		return log;
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

	/**
	 * Read Evolve messages from the mailbox, and repopulate migrants
	 */
	private void acceptMessages() {
		// TODO copy this from Evolve.acceptMessages
		int evolveMessagesNum;
		EvolveMessage currentMsg;
		// watch the mailbox' queue
		println("Evolve: checking mailbox");
		evolveMessagesNum = evolveMsgsRecv.size();
		println("Evolve: " + evolveMessagesNum + " outstanding messages.");
		while (evolveMessagesNum > 0) {
			// allow mailbox to add more messages to the queue
			// pop off queue
			currentMsg = evolveMsgsRecv.remove();
			println("Evolve: checking message " + currentMsg.msgID + "-"
					+ currentMsg.msgType + " from node at " + currentMsg.srcIP);
			// update the Evolve queue size
			evolveMessagesNum -= 1;
			// record successful response
			nodeList.succeed(currentMsg.srcID, currentMsg.srcIP,
					currentMsg.srcPort, currentMsg.srcSubnetID, NodeList.currTime());
			// take appropriate action
			println("Evolve: acting");
			currentMsg.act(this);
		}
	}

	public MessageWrapper<AbstractMessage> sendEvolveMsg(EvolveMessage m, String id,
			NodeListEntry nle_dest) {
		MessageWrapper<AbstractMessage> mw = new MessageWrapper<AbstractMessage>(
				m, id, nle_dest);
		if (evolveMsgsSend.remainingCapacity() > 0)
			evolveMsgsSend.add(mw);
		else {
			println("Cannot add message to evolveMsgsSend: queue full at size "
					+ evolveMsgsSend.size(), true);
		}
		return mw;
	}
	
	/**
	 * Add new migrants to the incoming migrant pool
	 * @param newMigrants
	 */
	public void addMigrants(Population newMigrants) {
		migrants.addAll(newMigrants);
	}

	/**
	 * main loop
	 */
	@Override
	public void run() {
            try {
                // import data and set up algo
                alg = new EvoGPJ(this, p, nodeList, mailbox, msgIDFactory, desc, migrants);
            } catch (IOException ex) {
                Logger.getLogger(Evolve.class.getName()).log(Level.SEVERE, null, ex);
            }
            //JSONObject rep = alg.report();
            //log.info(rep.toJSONString());
            while (alg.running()) {
                // check for migrants
                acceptMessages();
                // advance evolution by another generation.
                // import migrants if necessary inside proceed
                try {
                    println("Evolve: algorithm proceeding");
                    alg.proceed();
                } catch (AlgorithmException e) {
                    LogRecord lr = new LogRecord(Level.WARNING,"Algorithm threw an error");
                    lr.setThrown(e);
                    log.log(lr);
                    e.printStackTrace();
                }
                //rep = alg.report();
                //log.info(rep.toJSONString());
                //if (REPORT)
                  //  reportSend.add(rep);
            }
            // finally, deallocate dataset from shared memory
            alg.cleanup();
            log.info("Finished");
	}
}
