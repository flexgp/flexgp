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

import evogpj.gp.Population;
import node.Evolve;
import utility.MsgID;


/**
 * MigrantMessage is used to transport individuals from one node to another
 *
 * @author Dylan Sherry
 */
public class MigrantMessage extends EvolveMessage {

	public MigrantMessage(MsgID _msgID, String _srcID, String _srcIP, int _srcPort, String _srcSubnetID, Population _migrants) {
		super(_msgID, _srcID, _srcIP, _srcPort, _srcSubnetID, MessageType.MIGRANT_MESSAGE);
		migrants = _migrants;
	}

	private static final long serialVersionUID = -616540213050345443L;

	// this is just a general wrapper for containing migrants
	public Population migrants;

	@Override
	public void act(Evolve e) {
            e.addMigrants(migrants);
	}

	
}