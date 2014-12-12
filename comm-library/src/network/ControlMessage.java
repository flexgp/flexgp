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
import utility.MsgID;

/**
 * A class describing the basic requirements of a Control message
 * 
 * @author Dylan Sherry
 */
public abstract class ControlMessage extends AbstractMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1748116204491689452L;
	// if this is responding to another message, track the message ID of the
	// originally received message
	// null, otherwise.
	public MsgID origMsgID;

	public ControlMessage(MsgID _msgID, String _srcID, String _srcIP, int _srcPort, String _srcSubnetID, int _msgType) {
		super(_msgID, _srcID, _srcIP, _srcPort, _srcSubnetID, _msgType);
		origMsgID = null;
	}

	public ControlMessage(MsgID _msgID, String _srcID, String _srcIP, int _srcPort, String _srcSubnetID, int _msgType,
			MsgID _origMsgID) {
		super(_msgID, _srcID, _srcIP, _srcPort, _srcSubnetID, _msgType);
		origMsgID = _origMsgID;
	}

	/**
	 * Given an instance of this message, and access to the Control thread's
	 * resources (NodeList) and methods, take the appropriate action.
         * @param c: control thread
	 */
	public abstract void act(Control c);
}
