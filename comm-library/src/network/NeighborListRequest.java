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
import node.NodeList;
import node.NodeList.NoSuchNodeIDException;
import node.NodeListEntry;
import utility.MsgID;

/**
 * A request for NodeList sharing
 * @author Dylan
 *
 */
public class NeighborListRequest extends ControlMessage {
	private static final long serialVersionUID = -7747057061117917529L;

	private final NodeList nodes;

	public NeighborListRequest(MsgID msgID, String srcID, String srcIP, int srcPort,
			String srcSubnetID, NodeList _nodes) {
		super(msgID, srcID, srcIP, srcPort, srcSubnetID, MessageType.NODELIST);
		nodes = _nodes;
	}

	/**
	 * Given Control object c, incorporate this list of neighbors into my list
	 * of known neighbors
	 */
	@Override
	public void act(Control c) {
		Control.println("NeighborListRequest: received neighborlist from "
				+ srcID);
		// Control.println("other's nodes: " + nodes.toString());
		try {
			NodeListEntry srcEntry = c.nodeList.get(srcID);
			if (c.nodeList.merge(nodes)) {
				// Control.println("my resulting list: " +
				// c.nodeList.toString());
				// open a socket and send the message
				NeighborListResponse nl = new NeighborListResponse(
						c.makeMessageID(), c.desc.getID(), c.desc.getIP(),
						c.desc.getPort(), c.desc.getSubnetID(),
						c.nodeList.copy(), this.msgID);
				c.sendCtrlMsg(nl, srcID, srcEntry);
			} else {
				PingResponse pr = new PingResponse(c.makeMessageID(),
						c.desc.getID(), c.desc.getIP(), srcPort,
						c.desc.getSubnetID(), this.msgID);
				c.sendCtrlMsg(pr, srcID, srcEntry);
			}
		} catch (NoSuchNodeIDException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
