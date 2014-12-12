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

import utility.IP;

/**
 * The structure responsible for maintaining identifying information about this node
 *
 * @author Owen Derby
 */
public class NodeDescriptor {

	// level 0 means learner, 1 means first-level reporter, 2 means second-level reporter, etc.
	public int level = -1;
	
	public static final int FUNCS = 0x1;
	public static final int TERMS = 0x10;
	public static final int NORMS = 0x100;
	public static final int DATA = 0x1000;
	public int factorizations;
	
	private final long startTime;
	private final IP ip;
	private final String subnetID;
	
	public NodeDescriptor(IP _ip, String _subnetID) {
		startTime = System.currentTimeMillis();
		ip = _ip;
		subnetID = _subnetID;
	}

	public String getID() {
		return ip.id;
	}
	
	public String getIP() {
		return ip.ip;
	}

	public Integer getPort() {
		return ip.port;
	}

	public String getSubnetID() {
		return subnetID;
	}
	
	public long getAge() {
		return System.currentTimeMillis()-startTime;
	}
	
}
