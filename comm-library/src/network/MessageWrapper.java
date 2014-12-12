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

import node.NodeListEntry;

/**
 * Simple class to provide metadata about messages.
 * 
 * @author Owen Derby
 * 
 * @param <T>
 *            type of message to wrap
 */
public class MessageWrapper<T extends AbstractMessage> {

	public final T msg;
	public final String destID;
	public final NodeListEntry nle;
	
	public MessageWrapper(T m, String _destID, NodeListEntry _nle) {
		msg = m;
		destID = _destID;
		nle = _nle;
	}

	@Override
	public String toString() {
		return destID + ":" + nle.ip + ":" + nle.port + "-" + msg.msgType;
	}

}
