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
package network;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import node.NodeList;
import node.NodeListEntry;

/**
 * A class to handle sending messages in a separate thread to avoid write
 * blocking
 * 
 * @author Dylan Sherry
 * 
 */
public class MessageSender extends Thread {
	public AbstractMessage m;
	public SendTimer sws;
	public NodeList nodelist;
	public String destID;
	public NodeListEntry nle_dest;
	public static int SEND_TIMEOUT;

	public MessageSender(NodeList _nodelist, String _destID,
			NodeListEntry _nle_dest, AbstractMessage _m, int _SEND_TIMEOUT) {
		m = _m;
		nodelist = _nodelist;
		destID = _destID;
		nle_dest = _nle_dest;
		sws = new SendTimer(nodelist, destID, nle_dest, m);
		SEND_TIMEOUT = _SEND_TIMEOUT;
	}

        @Override
	public void run() {
		sws.run();
		TimerTask notify = new TimerTask() {			
			@Override
			public void run() {
				System.out.println("MessageSender: Interrupting message after "
						+ SEND_TIMEOUT + " seconds: " + m.toString());
				sws.interrupt();
			}
		};
		Timer t = new Timer();
		t.schedule(notify, SEND_TIMEOUT * 1000);
	}

	class SendTimer extends Thread {
		public AbstractMessage m;
		public NodeList nodelist;
		public String destID;
		public NodeListEntry nle_dest;

		public SendTimer(NodeList _nodelist, String _destID,
				NodeListEntry _nle_dest, AbstractMessage _m) {
			m = _m;
			nodelist = _nodelist;
			destID = _destID;
			nle_dest = _nle_dest;
		}

                @Override
		public void run() {
			// TODO handle these failures appropriately in the NodeList
			try {
				m.send(nle_dest);
				nodelist.succeed(destID, nle_dest, NodeList.currTime());
			} catch (SocketTimeoutException e) {
				System.out.println("Caught SocketTimeoutException");
				nodelist.fail(destID, nle_dest, NodeList.currTime());
			} catch (ConnectException e) {
				System.out.println("Caught ConnectException");
			} catch (StreamCorruptedException e) {
				System.out.println("Caught StreamCorruptedException");
			} catch (SocketException e) {
				System.out.println("Caught SocketException");
			} catch (UnknownHostException e) {
				System.out.println("Caught UnknownHostException");
			} catch (InterruptedException e) {
				System.out
						.println("Caught InterruptedException -- thread interrupted");
			} catch (IOException e) {
				System.out.println("Caught IOException");
			}
		}
	}

}
