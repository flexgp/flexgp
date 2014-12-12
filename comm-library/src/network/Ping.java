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

import node.Control;
import node.NodeList.NoSuchNodeIDException;
import node.NodeListEntry;
import utility.MsgID;

/**
 * An implementation of a Ping message
 * 
 * @author Dylan Sherry
 */
public class Ping extends ControlMessage {
	private static final long serialVersionUID = -7263086052036039644L;

	public Ping(MsgID _msgID, String _srcID, String _srcIP, int _srcPort, String _srcSubnetID) {
		super(_msgID, _srcID, _srcIP, _srcPort, _srcSubnetID, MessageType.PING);
	}

	/**
	 * Given access to the Control object, send a response to Ping
	 */
	@Override
	public void act(Control c) {
		// send a PingResponse
		PingResponse pr = new PingResponse(c.makeMessageID(), c.desc.getID(),
				c.desc.getIP(), c.desc.getPort(), c.desc.getSubnetID(), msgID);
		NodeListEntry srcEntry;
		try {
			srcEntry = c.nodeList.get(srcID);
			c.sendCtrlMsg(pr, srcID, srcEntry);
		} catch (NoSuchNodeIDException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
