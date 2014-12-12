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
package utility;

import node.Control;


/**
 * A simple class used to hold IP information
 * 
 * @author Dylan Sherry
 */
public class IP {
	public String id;
	public String ip;
	public int port;

	public IP(String _id, String _ip, int _port) {
		id = _id;
		ip = _ip;
		port = _port;
	}

	public boolean equals(IP other) {
		return ((this.id == other.id) && (this.ip.equals(other.ip)) && (this.port == other.port));
	}

	@Override
	public String toString() {
		return String.valueOf(id) + ":" + ip + ":" + String.valueOf(port);
	}

	public static final int ipToInt(String addr) {
		String[] addrArray = addr.split("\\.");
		if (addrArray.length != 4) {
			System.err.println("Improper ip address:" + addr);
			return 0;
		}
		int num = 0;
		for (int i = 0; i < addrArray.length; i++) {
			num += ((Integer.parseInt(addrArray[i]) % 256 * Math
					.pow(256, 3 - i)));
		}
		return num;
	}

	public static String makeID(String ip, int port) {
		return ip + ":" + port;
	}
	
	public static String intToIp(int i) {
		return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
				+ ((i >> 8) & 0xFF) + "." + (i & 0xFF);
	}

	/**
	 * Translate an "ip:port" pair into an IP object
	 * @param ip
	 * @return
	 */
	public static IP fromString(String ip) {
		// split into ip and port
		// TODO this should be done in a more stable/predictable way, plus tests
		String[] splitted = ip.split(":");
		if (splitted.length == 2) { 
			String id = IP.makeID(splitted[0], Integer.parseInt(splitted[1]));
			IP result = new IP(id, splitted[0], Integer.parseInt(splitted[1]));
			System.out
					.println(String.format(
							"String: %s    after formatting (2): %s", ip,
							result.toString()));
			return result;
		} else if (splitted.length == 1) {
			String id = IP.makeID(splitted[0], Control.PORT);
			IP result = new IP(id, splitted[0], Control.PORT);
			System.out
					.println(String.format(
							"String: %s    after formatting (1): %s", ip,
							result.toString()));
			return result;
		} else {
			System.err.println(String.format("Invalid IP entered: %s", ip));
			System.exit(-1);
			return null;
		}
	}
}
