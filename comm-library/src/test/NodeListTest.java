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
package test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Random;

import node.NodeList;
import node.NodeListEntry;
import node.NodeList.NoSuchNodeIDException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Dylan Sherry
 */
public class NodeListTest {

	private NodeList nodeList;
	private HashMap<String, NodeListEntry> h1;
	private HashMap<String, NodeListEntry> h1copy;

	private Timestamp t1;
	private Timestamp t2;
	private Random random;

	String id1;
	String id2;
	String id3;
	String id4;
	String id5;
	
	@Before
	public void setup() throws InterruptedException {
		random = new Random();
		// generate a couple timestamps
		t1 = NodeList.currTime();
		// advance the clock
		Thread.sleep((long) 250.0);
		t2 = NodeList.currTime();

		id1 = "18.19.20.21:9000";
		id2 = "18.19.20.22:9000";
		id3 = "18.19.20.23:9000";
		id4 = "18.19.20.24:9000";
		id5 = "18.19.20.25:9000";
		
		// An exact copy of h1
		h1copy = new HashMap<String, NodeListEntry>();
		// this entry is the same in both
		h1copy.put(id1, new NodeListEntry("18.19.20.21", 10, "subnet id 1", t1));
		// this entry is only in h1
		h1copy.put(id2, new NodeListEntry("18.19.20.22", 11, "subnet id 1", t1));
		// this entry is in both but more recent in h1
		h1copy.put(id3, new NodeListEntry("18.19.20.23", 12, "subnet id 1", t2));
		// this entry is in both but more recent in h2
		h1copy.put(id4, new NodeListEntry("18.19.20.24", 13, "subnet id 1", t1));

		h1 = new HashMap<String, NodeListEntry>();
		// this entry is "the same" in both
		h1.put(id1, new NodeListEntry("18.19.20.21", 10, "subnet id 1", t1));
		// this entry is only in h1
		h1.put(id2, new NodeListEntry("18.19.20.22", 11, "subnet id 1", t1));
		// this entry is in both but more recent in h1
		h1.put(id3, new NodeListEntry("18.19.20.23", 12, "subnet id 1", t2));
		// this entry is in both but more recent in h2
		h1.put(id4, new NodeListEntry("18.19.20.24", 13, "subnet id 1", t1));

		// make a nodelist
		nodeList = new NodeList(random, 100, id1);
		nodeList.succeed(id1, "18.19.20.21", 9000, "subnet id 1", t1);
		nodeList.succeed(id2, "18.19.20.22", 9000, "subnet id 1", t1);
		nodeList.succeed(id3, "18.19.20.23", 9001, "subnet id 1", t1);
		nodeList.fail(id4, "18.19.20.24", 9000, "subnet id 1", t1);
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////
	// TESTS
	// /////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * A test of NodeList.currTime()
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void currTimeTest() throws InterruptedException {
		Timestamp t1 = NodeList.currTime();
		Thread.sleep((long) 250.0);
		Timestamp t2 = NodeList.currTime();
		System.out.println("t1: " + t1);
		System.out.println("t2: " + t2);
		assertTrue(t1.compareTo(t2) != 0);
		assertTrue(!t1.equals(t2));
	}

	/**
	 * Check that NodeListEntry.equals works properly
	 */
	@Test
	public void nodeListEntryEqualsTest() {
		Timestamp t = NodeList.currTime();
		NodeListEntry n1 = new NodeListEntry("a", 1, "subnet id 1", t);
		NodeListEntry n2 = new NodeListEntry("a", 1, "subnet id 1", t);
		assertTrue(n1.equals(n2));
	}

	/**
	 * Another check that NodeListEntry.equals works properly
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void nodeListEntryNotEqualsTest() throws InterruptedException {
		Timestamp t = NodeList.currTime();
		NodeListEntry n1 = new NodeListEntry("a", 1, "subnet id 1", t);
		Thread.sleep((long) 250.0);
		NodeListEntry n2 = new NodeListEntry("a", 1, "subnet id 1", NodeList.currTime());
		assertTrue(!n1.equals(n2));
	}

	/**
	 * Check the constructor
	 */
	@Test
	public void constructorTest() {
		nodeList = new NodeList(random, 100, id1);
		assertTrue(nodeList.goodNodeTable.isEmpty());
		assertTrue(nodeList.goodNodeTable.isEmpty());
		assertTrue(nodeList.targetNodeCount == 25);
	}

	/**
	 * Simple successful connection test
	 */
	@Test
	public void simpleSuccessTest() {
		nodeList = new NodeList(random, 100, "1234");
		nodeList.succeed(id1, "18.19.20.21", 9000, "subnet id 1", t1);

		assertTrue(nodeList.goodNodeTable.containsKey(id1));
		// a little cheating to get the timestamp to work
		NodeListEntry retreivedEntry = nodeList.goodNodeTable.get(id1);
		Timestamp t = retreivedEntry.timestamp;
		NodeListEntry origEntry = new NodeListEntry("18.19.20.21", 9000, "subnet id 1", t);
		assertTrue(retreivedEntry.equals(origEntry));
	}

	/**
	 * A test of NodeList.equals
	 * 
	 * @throws InterruptedException
	 * @throws NoSuchNodeIDException
	 */
	@Test
	public void equivSuccessTest() throws InterruptedException,
			NoSuchNodeIDException {
		// now try an equivalence test
		NodeList n1 = new NodeList(random, 100, "1234");
		n1.succeed(id1, "18.19.20.21", 9000, "subnet id 1", t1);
		assertTrue(!n1.goodNodeTable.isEmpty());
		assertTrue(n1.badNodeTable.isEmpty());
		// create another nodeList
		NodeList n2 = new NodeList(random, 100, "1");
		// Thread.sleep((long) 250.0);
		n2.succeed(id1, "18.19.20.21", 9000, "subnet id 1", t1);
		// a hack to make the timestamps equal
		n2.goodNodeTable.put(id1,
				new NodeListEntry("18.19.20.21", 9000, "subnet id 1", n1.get(id1).timestamp));
		// onwards
		assertTrue(!n2.goodNodeTable.isEmpty());
		assertTrue(n2.badNodeTable.isEmpty());
		// test equivalence
		assertTrue(n1.goodNodeTable.keySet().equals(n2.goodNodeTable.keySet()));
		System.out.println(n1.goodNodeTable.values());
		System.out.println(n2.goodNodeTable.values());
		// assertTrue(n1.goodNodeTable.values().equals(n2.goodNodeTable.values()));
		// assertTrue(n1.goodNodeTable.entrySet().equals(n2.goodNodeTable.entrySet()));
		assertTrue(NodeList.compare(n1.goodNodeTable, n2.goodNodeTable));
		// assertTrue(n1.equals(n2));
	}

	@Test
	public void mergeTest() {
		NodeList n1 = new NodeList(random, 100, "1004", "subnet id 1");
		NodeList n2 = new NodeList(random, 100, "1003", "subnet id 1");
		// Test simple merge
		n1.succeed(id1, "0.0.0.1", 0, "subnet id 1", t2);
		n2.succeed(id2, "0.0.0.2", 0, "subnet id 1", t1);
		n1.merge(n2);
		assertTrue(n1.badNodeTable.size()==0);
		assertTrue(n1.goodNodeTable.size()==2);
		assertTrue(n1.goodNodeTable.containsKey(id1));
		assertTrue(n1.goodNodeTable.containsKey(id2));
		assertTrue(n1.subnetNeighbors.contains(id1));
		assertTrue(n1.subnetNeighbors.contains(id2));

		// test merge with bad nodes
		n1.fail(id2, "0.0.0.2", 0, "subnet id 1", t2);
		n2.merge(n1);
		assertTrue(n2.goodNodeTable.size()==1);
		assertTrue(n2.goodNodeTable.containsKey(id1));
		assertTrue(n2.badNodeTable.size()==1);
		assertTrue(n2.badNodeTable.containsKey(id2));
		assertTrue(n1.subnetNeighbors.contains(id1));
		assertTrue(!n1.subnetNeighbors.contains(id2));

	}
	
	/**
	 * Successful connection test when the node was previously good
	 */
	@Test
	public void successGoodTest() {

	}

	/**
	 * Successful connection test when the node was previously bad
	 */
	@Test
	public void successBadTest() {

	}

	/**
	 * Simple failed connection test
	 */
	@Test
	public void simplefailTest() {

	}

	/**
	 * failed connection test when the node was previously good
	 */
	@Test
	public void failGoodTest() {

	}

	/**
	 * failed connection test when the node was previously bad
	 */
	@Test
	public void failBadTest() {

	}

	/**
	 * verify NodeList is in fact serializable
	 * 
	 * @throws IOException
	 */
	public void testIsSerializable() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(nodeList);
		oos.close();
		Assert.assertTrue(out.toByteArray().length > 0);
	}

	/**
	 * A simple test of NodeListEntry serialization
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void simpleEntrySerializationTest() throws IOException,
			ClassNotFoundException {
		// serialize
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		NodeListEntry nle = new NodeListEntry("18.19.20.21", 9000, "subnet id 1", t1);
		oos.writeObject(nle);
		oos.close();

		// deserialize
		byte[] pickled = out.toByteArray();
		InputStream in = new ByteArrayInputStream(pickled);
		ObjectInputStream ois = new ObjectInputStream(in);
		Object o = ois.readObject();
		NodeListEntry copy = (NodeListEntry) o;

		// test the result
		assertTrue(copy.ip, copy.ip.equals("18.19.20.21"));
		assertTrue(!copy.ip.equals("18.19.20afafasdf.21"));
		assertTrue(copy.port == 9000);
		assertTrue(copy.port != 900213);
		assertTrue(copy.timestamp.equals(t1));
		assertTrue(!copy.timestamp.equals(t2));
		assertTrue(nle.equals(copy));
		// assertTrue(nodeList.equals(copy));
	}

	/**
	 * A simple test of NodeList serialization
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void simpleSerializationTest() throws IOException,
			ClassNotFoundException {
		// serialize
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		assertTrue(!nodeList.goodNodeTable.isEmpty());
		assertTrue(!nodeList.badNodeTable.isEmpty());
		oos.writeObject(nodeList);
		assertTrue(!nodeList.goodNodeTable.isEmpty());
		assertTrue(!nodeList.badNodeTable.isEmpty());
		oos.close();

		// deserialize
		byte[] pickled = out.toByteArray();
		InputStream in = new ByteArrayInputStream(pickled);
		ObjectInputStream ois = new ObjectInputStream(in);
		Object o = ois.readObject();
		NodeList copy = (NodeList) o;

		// test the result
		assertTrue(nodeList.targetNodeCount == copy.targetNodeCount);
		assertTrue(NodeList.compare(nodeList.badNodeTable, copy.badNodeTable));
		assertTrue(NodeList.compare(nodeList.goodNodeTable, copy.goodNodeTable));
		// assertTrue(nodeList.equals(copy));
	}

	/**
	 * Test HashMap.equals for equivalent hashMaps
	 */
	@Test
	public void hashMapEqualsTest() {
		assertTrue(NodeList.compare(h1, h1copy));
		// assertTrue(h1.equals(h1copy));
	}
}
