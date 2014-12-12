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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import network.MessageType;
import utility.IP;

/**
 * An implementation of the nodelist like that described in flex.py
 * 
 * @author Dylan Sherry
 */
public class NodeList implements Serializable {
	// TODO make threadsafe

	private static final long serialVersionUID = -5887543388451092019L;
	// Owner nodeID
	private final String nodeID;
	// used to indicate there is no second tier in this network
	public static final String NO_SECOND_TIER = "NO_SECOND_TIER";
	// used to indicate the subnet id of a node is unknown
	public static final String DEFAULT_SUBNET_ID = "DEFAULT_SUBNET_ID";
	
	// used in contains()
	public enum Status {
		GOOD, BAD, NONE
	};

	// the good and bad nodeTables
	public HashMap<String, NodeListEntry> goodNodeTable;
	public HashMap<String, NodeListEntry> badNodeTable;
	// a list of good subnet neighbors
	public HashSet<String> subnetNeighbors;
	// the string IDing this subnet
	public String subnetID;
	// random number generator from Control thread
	public Random random;
	// the desired number of active neighbors
	// TODO this is currently assumed to be fixed after start up
	public int targetNodeCount;
	public static final int MIN_NEIGHBORS = 25;

	public NodeList(Random _random, int _targetNodeCount, String _nodeID) {
		this(_random, _targetNodeCount, _nodeID, NO_SECOND_TIER);
	}
	
	/**
	 * Construct a NodeList to record neighbors
	 * @param _random
	 * @param _targetNodeCount
	 * @param _nodeID
	 * @param _subnetNeighbors if initialized to null, NodeList will ignore subnetNeighbors entirely
	 */
	public NodeList(Random _random, int _targetNodeCount, String _nodeID, String _subnetID) {
		random = _random;
		nodeID = _nodeID;
		subnetID = _subnetID;
		int sqrt_ncnt = (int) Math.floor(Math.sqrt(_targetNodeCount));
		// we ideally want to have sqrt(total nodes) in our goodNodeTable.
		// However,
		// we realistically must keep this value within [25,total nodes-1], and
		// it
		// must never exceed the total number of nodes-1 (we don't track
		// ourselves!)
		targetNodeCount = sqrt_ncnt > MIN_NEIGHBORS ? sqrt_ncnt : MIN_NEIGHBORS;
		targetNodeCount = _targetNodeCount - 1 > targetNodeCount ? targetNodeCount
				: _targetNodeCount - 1;
		goodNodeTable = new HashMap<String, NodeListEntry>();
		badNodeTable = new HashMap<String, NodeListEntry>();
		subnetNeighbors = new HashSet<String>();
	}

	/**
	 * Is there a second tier active in the network?
	 * @return true if so, else false
	 */
	public boolean hasSecondTier() {
		return (!(subnetID.equals(NO_SECOND_TIER)));
	}

	/**
	 * Check if we have satisfied our desired number of active (good) neighbors
	 * 
	 * @return True iff goodNodeTable.size() >= targetNodeCount
	 */
	public synchronized boolean satisfied() {
		return (hasSecondTier()) ? subnetNeighbors.size() >= targetNodeCount
				: goodNodeTable.size() >= targetNodeCount;
	}

	/**
	 * Make a copy of this NodeList - this copy is just shallow copy of the good
	 * and bad node lists, but it prevents unneeded synchronizations.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized NodeList copy() {
		NodeList dup = new NodeList(random, targetNodeCount + 1, nodeID, subnetID);
		dup.goodNodeTable = (HashMap<String, NodeListEntry>) goodNodeTable
				.clone();
		dup.badNodeTable = (HashMap<String, NodeListEntry>) badNodeTable
				.clone();
		dup.subnetNeighbors = (HashSet<String>) subnetNeighbors.clone();
		return dup;
	}

	/**
	 * Get all nodes whom are second-tier subnet neighbors
	 */
	public synchronized HashSet<String> getSubnetNeighbors() {
		return subnetNeighbors;
	}

	/**
	 * Retrieve an entry from the {@link NodeList}, or throw an
	 * {@link NoSuchNodeIDException}
	 * 
	 * @param nodeID
	 * @return
	 */
	public synchronized NodeListEntry get(String nID) throws NoSuchNodeIDException {
		if (goodNodeTable.containsKey(nID))
			return goodNodeTable.get(nID);
		else if (badNodeTable.containsKey(nID))
			return badNodeTable.get(nID);
		else
			throw new NoSuchNodeIDException();
	}
	
	/**
	 * Return values indicating if the good/bad node tables contain an entry for
	 * nID
	 * 
	 * @param nID
	 */
	public synchronized Status contains(String nID) {
		if (goodNodeTable.containsKey(nID))
			return Status.GOOD;
		else if (badNodeTable.containsKey(nID))
			return Status.BAD;
		return Status.NONE;
	}

	public class NoSuchNodeIDException extends Exception {
		private static final long serialVersionUID = 5780011381489528722L;
	}

	public class NoSuchIPException extends Exception {
		private static final long serialVersionUID = 812997894613089723L;
	}

	/**
	 * Get a current timestamp
	 * 
	 * @return
	 */
	public static Timestamp currTime() {
		return new Timestamp(new Date().getTime());
	}

	public void succeed(IP ip, String s, Timestamp t) {
		succeed(ip.id, ip.ip, ip.port, s, t);
	}

	public void succeed(String nodeID, NodeListEntry nle, Timestamp t) {
		succeed(nodeID, nle.ip, nle.port, nle.subnetLabel, t);
	}
	
	public synchronized void succeed(String nodeID, String ip, int port,
			String s, Timestamp t) {
		if (nodeID.equals(this.nodeID)) {
			// Don't add ourselves!
			return;
		}
		// add an entry to the goodNodeTable
		// TODO CHECK TO MAKE SURE THIS ENTRY IS MORE UP-TO-DATE
		Control.println(String.format("NodeList: recording success for node %s:%d",ip,port));
		if (!goodNodeTable.containsKey(nodeID)) {
			if (badNodeTable.containsKey(nodeID)) {
				badNodeTable.remove(nodeID);
				reportDeltas(1, goodNodeTable.size(), 1, 0, -1, 0,
						badNodeTable.size(), 0, 1, 0, MessageType.PING);
			} else {
				reportDeltas(1, goodNodeTable.size(), 0, 1, 0, 0,
						badNodeTable.size(), 0, 1, 0, MessageType.PING);
			}
		}
		goodNodeTable.put(nodeID, new NodeListEntry(ip, port, s, t));
		if (hasSecondTier() && (s.equals(this.subnetID))) {
			subnetNeighbors.add(nodeID);
		}
	}

	public void fail(IP ip, String s, Timestamp t) {
		fail(ip.id, ip.ip, ip.port, s, t);
	}

	public void fail(String nodeID, NodeListEntry nle, Timestamp t) {
		fail(nodeID, nle.ip, nle.port, nle.subnetLabel, t);
	}

	/**
	 * Observe an unsuccessful connection with a node
	 */
	public synchronized void fail(String nodeID, String ip, int port, String s, Timestamp t) {
		if (nodeID.equals(this.nodeID)) {
			// Don't add ourselves!
			return;
		}
		Control.println("NodeList: recording failure for node " + ip);
		if (!badNodeTable.containsKey(nodeID)) {
			if (goodNodeTable.containsKey(nodeID)) {
				// Move entry to bad node table
				goodNodeTable.remove(nodeID);
				reportDeltas(-1, goodNodeTable.size(), 0, 0, 1, 1,
						badNodeTable.size(), 0, 0, 1, MessageType.PING);
			} else {
				reportDeltas(0, 0, 0, 0, 1, 0, badNodeTable.size(), 1, 0, 1, MessageType.PING);
			}
		}
		badNodeTable.put(nodeID, new NodeListEntry(ip, port, s, t));
		if (hasSecondTier() && (s.equals(this.subnetID))) {
			subnetNeighbors.remove(nodeID);
		}
	}

	/**
	 * Merge this {@link NodeList} with other
	 * 
	 * @param other the other NodeList to merge into this one
	 * 	NOTE: this method mutates other, so don't pass in anything important!
	 * @return true if this {@link NodeList} has node entries which the other
	 *         doesn't
	 */
	public synchronized boolean merge(NodeList other) {
		boolean new_nodes = false;
		int otherGoodSize = other.goodNodeTable.size();
		int otherBadSize = other.badNodeTable.size();
		int newGood = 0;
		int good2good = 0;
		int bad2good = 0;
		int newBad = 0;
		int good2bad = 0;
		int bad2bad = 0;
		HashMap<String, NodeListEntry> new_good_list = new HashMap<String, NodeListEntry>();
		HashMap<String, NodeListEntry> new_bad_list = new HashMap<String, NodeListEntry>();
		// Iterate over our good list, copying all which aren't covered by a
		// newer entry in other bad list
		for (String i : goodNodeTable.keySet()) {
			NodeListEntry nle = goodNodeTable.get(i);
			if (other.goodNodeTable.containsKey(i)) {
				// if both have entry, add latest
				if (nle.timestamp
						.compareTo(other.goodNodeTable.get(i).timestamp) < 0) {
					// m1 entry after m2 entry
					new_good_list.put(i, other.goodNodeTable.get(i));
				} else {
					new_good_list.put(i, nle);
				}
				other.goodNodeTable.remove(i);
				good2good++;
			} else if (other.badNodeTable.containsKey(i)) {
				if (other.badNodeTable.get(i).timestamp
						.compareTo(nle.timestamp) < 0) {
					// if our good is newer than their bad, keep it
					new_good_list.put(i, nle);
					good2good++;
				} else {
					new_bad_list.put(i, other.badNodeTable.get(i));
					good2bad++;
				}
				other.badNodeTable.remove(i);
			} else {
				// if other doesn't know about this node, then be sure to keep
				// it!
				new_good_list.put(i, nle);
				new_nodes = true;
				good2good++;
			}
		}
		// Now iterate over our bad list, add to new bad list all which are
		// aren't covered by a newer entry in other good list.
		for (String i : badNodeTable.keySet()) {
			NodeListEntry nle = badNodeTable.get(i);
			if (other.badNodeTable.containsKey(i)) {
				// if both have entry, add latest
				if (nle.timestamp
						.compareTo(other.badNodeTable.get(i).timestamp) < 0) {
					// m1 entry after m2 entry
					new_bad_list.put(i, other.badNodeTable.get(i));
				} else {
					new_bad_list.put(i, nle);
				}
				other.badNodeTable.remove(i);
				bad2bad++;
			} else if (other.goodNodeTable.containsKey(i)) {
				if (other.goodNodeTable.get(i).timestamp
						.compareTo(nle.timestamp) < 0) {
					// if node has good entry newer than our old bad entry,
					// we'll take it as good
					new_bad_list.put(i, nle);
					bad2bad++;
				} else {
					new_good_list.put(i, other.goodNodeTable.get(i));
					bad2good++;
				}
				other.goodNodeTable.remove(i);
			} else {
				new_bad_list.put(i, nle);
				new_nodes = true;
				bad2bad++;
			}
		}
		// Iterate over what remains in other good list, add to new good list
		for (String i : other.goodNodeTable.keySet()) {
			if (i.equals(nodeID))
				continue;
			new_good_list.put(i, other.goodNodeTable.get(i));
			newGood++;
		}
		// add whatever is left in other bad list to ours
		for (String i : other.badNodeTable.keySet()) {
			if (i.equals(nodeID))
				continue;
			new_bad_list.put(i, other.badNodeTable.get(i));
			newBad++;
		}
		int goodDelta = new_good_list.size() - goodNodeTable.size();
		int badDelta = new_bad_list.size() - badNodeTable.size();
		reportDeltas(goodDelta, good2good, bad2good, newGood, badDelta,
				good2bad, bad2bad, newBad, otherGoodSize, otherBadSize, MessageType.NODELIST);

		goodNodeTable = new_good_list;
		badNodeTable = new_bad_list;
		this.targetNodeCount = (this.targetNodeCount > other.targetNodeCount) ? this.targetNodeCount
				: other.targetNodeCount;
		// now recalculate subnetNeighbors if necessary
		if (this.hasSecondTier()) {
			this.subnetNeighbors = new HashSet<String>();
			for (String nodeID : goodNodeTable.keySet()) {
				NodeListEntry nle = goodNodeTable.get(nodeID);
				if (nle.subnetLabel.equals(this.subnetID))
					this.subnetNeighbors.add(nodeID);
			}
		}
		return new_nodes;
	}

	private static void reportDeltas(int goodDelta, int good2good,
			int bad2good, int newGood, int badDelta, int good2bad, int bad2bad,
			int newBad, int inGood, int inBad, int msgType) {
		Control.println(String.format("Deltas:%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",
				goodDelta, good2good, bad2good, newGood, badDelta, good2bad,
				bad2bad, newBad, inGood, inBad, msgType));
	}

	public synchronized boolean equals(NodeList other) {
		return ((this.goodNodeTable.equals(other.goodNodeTable))
				&& (this.badNodeTable.equals(other.badNodeTable))
				&& (this.targetNodeCount == other.targetNodeCount)
				&& (this.subnetID.equals(other.subnetID))
				&& (this.subnetNeighbors.equals(other.subnetNeighbors)));
	}

	/**
	 * Given two HashMap<String, NodeListEntry> instances, determine if they
	 * are equal
	 * 
	 * @param h1
	 * @param h2
	 * @return
	 */
	public static boolean compare(HashMap<String, NodeListEntry> h1,
			HashMap<String, NodeListEntry> h2) {
		// first compare the size
		if (h1.size() != h2.size()) {
			return false;
		}
		// check all keys in h1 (same size, so presumable all keys in h2 too)
		for (String i : h1.keySet()) {
			if (!h2.containsKey(i)) {
				return false;
			}
		}
		// check all values in h1
		for (String i : h1.keySet()) {
			if (!h1.get(i).equals(h2.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public synchronized String toString() {
		String s = "NodeList[";
		s += goodNodeTable.size() + "/" + targetNodeCount + ", ";
		if (hasSecondTier()) {
			s += "subnetID=" + subnetID + ", subnetNeighbors(";
			for (String i : subnetNeighbors) {
				s += " " + i + ",";
			}
			s += "), ";
		}
		s += "goodNodeTable(";
		for (String i : goodNodeTable.keySet()) {
			s += i + ": " + goodNodeTable.get(i).toString() + ", ";
		}
		// take off the last characters
		if (!(goodNodeTable.isEmpty())) {
			s = s.substring(0, s.length() - 2);
		}
		s += "), badNodeTable(";
		for (String i : badNodeTable.keySet()) {
			s += i + ":" + badNodeTable.get(i).toString() + ", ";
		}
		// take off the last characters
		if (!(badNodeTable.isEmpty())) {
			s = s.substring(0, s.length() - 2);
		}
		return s + ")]";
	}

	public class EmptyNodeListException extends Exception {
		private static final long serialVersionUID = 29889123706L;
	}
	
	/**
	 * Select a random neighbor's ID from list.
	 * @return
	 * @throws EmptyNodeListException if good list is empty
	 */
	public String getRandomNeighbor() throws EmptyNodeListException {
		// if we support a second tier of nodes, use only the maintained subnetNeighbors set
		Set<String> nodeIDs = (hasSecondTier()) ? subnetNeighbors : goodNodeTable.keySet();
		if (nodeIDs.isEmpty())
			throw new EmptyNodeListException();
		return getRandomElement(nodeIDs);
	}

	/**
	 * Given a set of elements, randomly select one.
	 * @param elements
	 * @return
	 */
	private <Element> Element getRandomElement(Set<Element> elements) {
		int size = elements.size();
		int targetIndex = new Random().nextInt(size);
		int i = 0;
		for (Element element : elements) {
			if (i == targetIndex)
				return element;
			i++;
		}
		return null;		
	}
	
}