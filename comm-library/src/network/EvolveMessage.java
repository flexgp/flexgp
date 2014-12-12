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

import node.Evolve;
import utility.MsgID;

/**
 * 
 * A class describing the basic requirements of an Evolve message
 * 
 * @author Dylan Sherry
 */
public abstract class EvolveMessage extends AbstractMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4481401951762535848L;

	public EvolveMessage(MsgID _msgID, String _srcID, String _srcIP, int _srcPort, String _srcSubnetID, int _msgType) {
		super(_msgID, _srcID, _srcIP, _srcPort, _srcSubnetID, _msgType);
	}

	/**
	 * Given an instance of this message, and access to the Evolve thread's
	 * resources and methods, take the appropriate action.
         * @param e: evolve (MRGP) thread
	 */
	public abstract void act(Evolve e);

}
