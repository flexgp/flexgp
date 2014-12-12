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
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minidev.json.JSONObject;
import utility.JSONFilter;
import utility.JSONFormatter;
import utility.Signal;

/**
 * A thread for recording JSON logs
 *
 * @author Owen Derby
 */
public class Reporter extends AbstractThread {

	private final BlockingQueue<JSONObject> reportSend;
	private Signal sig;
	private final static int signalFreq = 5; // seconds
	private static Logger localLog = Logger.getLogger("node.Reporter");
	private static Logger sendLog = Logger.getLogger("node.Reporter_send");

	private JSONObject myReport;

	public Reporter(BlockingQueue<JSONObject> reportSend2) {
		Handler fh;
		try {
			localLog.setUseParentHandlers(false);
			fh = new FileHandler("reporter.json");
			fh.setFormatter(new JSONFormatter());
			fh.setFilter(new JSONFilter());
			localLog.addHandler(fh);
			localLog.setLevel(Level.ALL);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			sendLog.setUseParentHandlers(false);
			fh = new FileHandler("reporter_send.json");
			fh.setFormatter(new JSONFormatter());
			fh.setFilter(new JSONFilter());
			sendLog.addHandler(fh);
			sendLog.setLevel(Level.ALL);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		reportSend = reportSend2;
		sig = new Signal(signalFreq);
		myReport = new JSONObject();
	}

	@Override
	public void run() {
		JSONObject report;
		new Thread(sig).start();
		while (true) {
			// See if we have any reports to integrate
			// localLog.info("have " + numReports + " messages to send");
			int numReports = reportSend.size();
			while (numReports > 0) {
				if ((report = reportSend.poll()) == null)
					break;
				processReport(report);
				numReports -= 1;
			}
			if (sig.getSignal()) {
				sig.signalFalse();
				sendReport();
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendReport() {
		if (myReport.size() > 0) {
			myReport.put("timestamp", System.currentTimeMillis());
			sendLog.info(myReport.toJSONString());
			myReport.remove("timestamp");
		}
	}

	/**
	 * TODO: Document expected format of report! 
	 * 
	 * Ensure fields present in a report don't change!
	 * 
	 * @param report
	 */
	@SuppressWarnings("unchecked")
	private void processReport(JSONObject report) {
		localLog.info(report.toJSONString());
		/*
		 * iterate over top-level items (each should correspond to a single
		 * report from a single component) and merge them in.
		 */
		for (String compName : report.keySet()) {
			Object oldCompRep = myReport.get(compName);
			Object newCompRep = report.get(compName);
			/*
			 * If we already have an report for this component, then we need to
			 * copy all new updates over, but keep any non-updated values.
			 */
			if (oldCompRep != null && oldCompRep instanceof JSONObject) {
				((JSONObject) oldCompRep)
						.putAll((Map<? extends String, ? extends Object>) newCompRep);
			} else { // either we haven't seen it before, or it's not a nested
						// object
				myReport.put(compName, newCompRep);
			}
		}
	}
}
