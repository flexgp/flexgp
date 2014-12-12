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

import java.io.Serializable;
import java.sql.Timestamp;

public class NodeListEntry extends Object implements Serializable {
	private static final long serialVersionUID = -1638892096321023484L;
	
	// the IP
	public String ip;
	// the port
	public int port;
	// sub-network label for second-tier nodes
	public String subnetLabel;
	// timestamp indicating last valid connection
	public Timestamp timestamp;

	public NodeListEntry(String _ip, int _port, String _subnetLabel, Timestamp _timestamp) {
		ip = _ip;
		port = _port;
		subnetLabel = _subnetLabel;
		timestamp = _timestamp;
	}

	// can ignore socket for equals method
	public boolean equals(NodeListEntry other) {
		return ((ip.equals(other.ip)) && (port == other.port) && (timestamp
				.compareTo(other.timestamp) == 0) && (subnetLabel.equals(other.subnetLabel)));
	}

	@Override
	public String toString() {
		return ip + ":" + Integer.toString(port) + "--" + subnetLabel + "--" + timestamp.toString();
	}
}
