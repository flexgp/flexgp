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

import java.util.ArrayList;
import java.util.HashMap;

import node.NodeList;

/**
 * A class to parse command-line args lexed by ArgLexer
 * 
 * @author dylan
 */
public class ArgParser {
	// the lexed arguments
	public final HashMap<String, String> groups;

	// the command-line flags
	// verbosity
	public final static String VERBOSE = "-v";
	public final int verbosity;
	// local ip address - easiest to just have system tell us
	public final static String MY_IP = "-i";
	public final String my_ip_string;
	public final IP my_ip;
	// ip list
	public final static String IPS = "-n";
	public final ArrayList<IP> ips;
	// target node count
	public final static String TARGET_NODE_COUNT = "-t";
	public final int targetNodeCount;
	// is migration enabled?
	public final static String MIGRATION_ENABLED = "--migration";
	public final boolean migrationEnabled;
	// generations per emigration
	public final static String MIGRATION_RATE = "--migration-rate";
	public final int migrationRate;
	// generation # in which to begin migration
	public final static String MIGRATION_START = "--migration-start";
	public final int migrationStart;
	// # of migrants per migration
	public final static String MIGRATION_SIZE = "--migration-size";
	public final int migrationSize;

	// Library Args - any string after this are passed through to the underlying
	// library
	public final static String LIB_ARGS = "--args";
	public final String lib_args;
	// Data file - file encoding all the information needed to access the
	// training data
	// The proper database interface is selected via the file extension
	public final static String DATA_FILE = "-d";
	public final String data_file;

	// max messages allowed per queue
	public final static String MSG_COUNT = "-m";
	public final int maxMsgs;
	private final static int maxMsgsDefault = 100;

	// subnet ID
	public final static String SUBNET_ID = "-s";
	public final String subnetID;
	private final static String subnetIDDefault = NodeList.DEFAULT_SUBNET_ID;

	public ArgParser(HashMap<String, String> _groups)
			throws UnhandledArgException {
		groups = _groups;

		// REQUIRED args
		// require this ip address
		if (!(groups.containsKey(MY_IP)))
			throw new UnhandledArgException("no argument for my IP");
		my_ip_string = groups.get(MY_IP);
		// parse port number too
		my_ip = IP.fromString(my_ip_string);
		// target node count
		if (groups.containsKey(TARGET_NODE_COUNT))
			targetNodeCount = Integer.valueOf(groups.get(TARGET_NODE_COUNT));
		else
			throw new UnhandledArgException("no argument for target node count");
		if (groups.containsKey(LIB_ARGS))
			lib_args = groups.get(LIB_ARGS);
		else {
			System.out.println("no arguments provided for underlying library");
			lib_args = null;
		}
		if (groups.containsKey(DATA_FILE))
			data_file = groups.get(DATA_FILE);
		else {
			System.out.println("no database file provided");
			data_file = null;
		}

		// parse list of known IPs
		ips = (groups.containsKey(IPS)) ? parseIPs(groups.get(IPS)) : new ArrayList<IP>();

		// OPTIONAL args
		// verbosity -- ignored for now
		verbosity = (groups.containsKey(VERBOSE)) ? Integer.valueOf(groups.get(VERBOSE)) : 0;
		maxMsgs = (groups.containsKey(MSG_COUNT)) ? Integer.valueOf(groups.get(MSG_COUNT)) : maxMsgsDefault;
		migrationEnabled = (groups.containsKey(MIGRATION_ENABLED)) ? Boolean.valueOf(groups.get(MIGRATION_ENABLED)) : false;
		migrationRate = (groups.containsKey(MIGRATION_RATE)) ? Integer.valueOf(groups.get(MIGRATION_RATE)) : 1;
		migrationStart = (groups.containsKey(MIGRATION_START)) ? Integer.valueOf(groups.get(MIGRATION_START)) : 0;
		migrationSize = (groups.containsKey(MIGRATION_SIZE)) ? Integer.valueOf(groups.get(MIGRATION_SIZE)) : 25;
		subnetID = (groups.containsKey(SUBNET_ID)) ? groups.get(SUBNET_ID) : subnetIDDefault;
	}

	/**
	 * Parse IPs of the form "id1:ip1:port1 id2:ip2:port2 id3:ip3:port3" etc
	 * 
	 * @param ips
	 * @return
	 * @throws UnhandledArgException
	 */
	public ArrayList<IP> parseIPs(String rawIPs) throws UnhandledArgException {
		// TODO think of the best way for this to interface with
		// Control/NodeList
		// remember, there are no node IDs yet
		// one idea: have a special stack of unidentified ppl
		ArrayList<IP> ips = new ArrayList<IP>();
		if (rawIPs.isEmpty()) {
			System.out.println("ArgParser: no ips specified");
		}
		String[] splitted = rawIPs.split(" ");
		// process each potential ip
		for (String ip : splitted) {
			ips.add(IP.fromString(ip));
			// ips.add(new IP(IP.ipToInt(ip), ip, Control.PORT));
		}
		return ips;
	}

	public class UnhandledArgException extends Exception {
		private static final long serialVersionUID = 1256884202673890421L;

		String msg;

		public UnhandledArgException(String _msg) {
			msg = _msg;
		}

		@Override
		public String toString() {
			return "UnhandledArgException: " + msg;
		}
	}
}
