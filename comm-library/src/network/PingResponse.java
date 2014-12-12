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
 * The response to a Ping message
 * 
 * @author Dylan Sherry
 */
public class PingResponse extends ControlMessage {
	private static final long serialVersionUID = -8289111686719900409L;

	public PingResponse(MsgID _msgID, String _srcID, String _srcIP, int _srcPort, String _srcSubnetID, MsgID _origMsgID) {
		super(_msgID, _srcID, _srcIP, _srcPort, _srcSubnetID, MessageType.PING_RESPONSE, _origMsgID);
	}

	@Override
	public void act(Control c) {
		// do nothing
		return;
	}
}
