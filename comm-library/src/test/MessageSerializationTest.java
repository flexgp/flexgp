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
import evogpj.genotype.TreeGenerator;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import network.MigrantMessage;
import network.NeighborListRequest;
import network.NeighborListResponse;
import network.Ping;
import network.PingResponse;
import node.NodeList;

import org.junit.Test;

import utility.MsgID;

/**
 * These tests are message-class-specific checks that serialization works
 * properly
 * 
 * @author Dylan
 * 
 */
public class MessageSerializationTest {

	// /////////////////////////////////////////////////////////////////////////////////////////////////
	// HELPER FUNCTIONS
	// /////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * To verify an object is in fact serializable
	 * 
	 * @throws IOException
	 */
	public <N> boolean isSerializable(N n) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(n);
		oos.close();
		return out.toByteArray().length > 0;
	}

	/**
	 * A simple serialization test--spits out a serialized-deserialized copy of
	 * the original object
	 * 
	 * @param n
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public <N> N simpleSerialization(N n) throws IOException,
			ClassNotFoundException {
		// serialize
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(n);
		oos.close();

		// deserialize
		byte[] pickled = out.toByteArray();
		InputStream in = new ByteArrayInputStream(pickled);
		ObjectInputStream ois = new ObjectInputStream(in);
		Object o = ois.readObject();
		N copy = (N) o;

		return copy;
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////
	// TESTS
	// /////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Check that Ping can be serialized
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void simplePing() throws IOException, ClassNotFoundException {
		Ping p = new Ping(new MsgID("1234", 0), "5678", "127.0.0.1", 9000, NodeList.DEFAULT_SUBNET_ID);
		assertTrue(isSerializable(p));
		// check that it works
		Ping pnew = simpleSerialization(p);
		assertTrue(p.msgID.id == pnew.msgID.id);
		assertTrue(p.msgID.nodeID.equals(pnew.msgID.nodeID));
		assertTrue(p.srcID.equals(pnew.srcID));
		assertTrue(p.srcPort == pnew.srcPort);
		assertTrue(p.srcSubnetID.equals(pnew.srcSubnetID));
		//assertTrue(p.msgType.equals(pnew.msgType));
		assertTrue(p.equals(pnew));
		assertTrue(pnew.equals(p));
		assertTrue(pnew.hashCode() == p.hashCode());
	}

	/**
	 * Check that PingResponse can be serialized
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void simplePingResponse() throws IOException, ClassNotFoundException {
		PingResponse p = new PingResponse(new MsgID("1234", 0), "5678", "127.0.0.1", 9000,
				NodeList.DEFAULT_SUBNET_ID, new MsgID("9876", 0));
		assertTrue(isSerializable(p));
		// check that it works
		PingResponse pnew = simpleSerialization(p);
		assertTrue(p.msgID.id == pnew.msgID.id);
		assertTrue(p.msgID.nodeID.equals(pnew.msgID.nodeID));
		assertTrue(p.srcID.equals(pnew.srcID));
		assertTrue(p.srcPort == pnew.srcPort);
		assertTrue(p.srcSubnetID.equals(pnew.srcSubnetID));
		assertTrue(p.msgType == pnew.msgType);
		assertTrue(p.equals(pnew));
	}

	/**
	 * Check that NeighborListRequest can be serialized
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void simpleNeighborListRequest() throws IOException, ClassNotFoundException {
		NodeList nl = new NodeList(null, 1, "yo");
		NeighborListRequest n = new NeighborListRequest(new MsgID("1234", 0),
				"5678", "1.2.3.4", 9000, NodeList.DEFAULT_SUBNET_ID, nl);
		assertTrue(isSerializable(n));
		// check that it works
		NeighborListRequest pnew = simpleSerialization(n);
		assertTrue(n.msgID.id == pnew.msgID.id);
		assertTrue(n.msgID.nodeID.equals(pnew.msgID.nodeID));
		assertTrue(n.srcID.equals(pnew.srcID));
		assertTrue(n.srcPort == pnew.srcPort);
		assertTrue(n.srcSubnetID.equals(pnew.srcSubnetID));
		assertTrue(n.msgType == pnew.msgType);
		assertTrue(n.equals(pnew));
	}

	/**
	 * Check that NeighborListResponse can be serialized
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void simpleNeighborListResponse() throws IOException, ClassNotFoundException {
		NodeList nl = new NodeList(null, 1, "yo");
		NeighborListResponse n = new NeighborListResponse(new MsgID("1234", 0),
				"5678", "1.2.3.4", 9000, NodeList.DEFAULT_SUBNET_ID, nl,
				new MsgID("010101", 12345));
		assertTrue(isSerializable(n));
		// check that it works
		NeighborListResponse pnew = simpleSerialization(n);
		assertTrue(n.msgID.id == pnew.msgID.id);
		assertTrue(n.msgID.nodeID.equals(pnew.msgID.nodeID));
		assertTrue(n.srcID.equals(pnew.srcID));
		assertTrue(n.srcPort == pnew.srcPort);
		assertTrue(n.srcSubnetID.equals(pnew.srcSubnetID));
		assertTrue(n.msgType == pnew.msgType);
		assertTrue(n.equals(pnew));
	}

	@Test
	public void largeMigrantMessageSerialization() throws IOException, ClassNotFoundException {
		MsgID msgID = new MsgID("nodenodenode", 1234567);
		String srcID = "src id";
		String srcIP = "100.200.300.400";
		int srcPort = 9000;
		String srcSubnetID = "src subnet id 1";
		// start with simple message
		Population migrants = new Population();
		MigrantMessage m = new MigrantMessage(msgID, srcID , srcIP, srcPort, srcSubnetID, migrants);
		assertTrue(isSerializable(m));
		MigrantMessage mnew = simpleSerialization(m);
		assertTrue(m.msgID.id == mnew.msgID.id);
		assertTrue(m.msgID.nodeID.equals(mnew.msgID.nodeID));
		assertTrue(m.srcID.equals(mnew.srcID));
		assertTrue(m.srcPort == mnew.srcPort);
		assertTrue(m.srcSubnetID.equals(mnew.srcSubnetID));
		assertTrue(m.msgType == mnew.msgType);
		assertTrue(m.migrants.equals(mnew.migrants));
		assertTrue(m.equals(mnew));
		
		// add 1 individual to migrants to create a realistically large number of individuals
		Individual individual = new Individual(TreeGenerator.generateTree("(+ (+ (- (+ (+ (cube (cube (cube X4))) (+ (+ (- (+ (- (+ X11 (- (+ (cube (cube X4)) (- X14 (sqrt (+ (cube (cube X7)) (exp X18))))) (square X15))) (cube X4)) (- (+ (cube X7) (- X6 (sqrt (+ X9 (exp X18))))) (cube (square X15)))) (sqrt (+ (cube X18) (- X13 X15)))) (+ (cube X13) (- (- (+ (+ (cube (cube (cube X4))) (- X6 (sqrt (+ (- (cube X4) (cube X18)) (exp X18))))) (* (sqrt X5) X13)) (cube X4)) (cube X13)))) (+ X4 X7))) (- (+ (cube (cube X4)) (- X6 (sqrt (+ X9 (sqrt (+ X9 (exp X18))))))) (cube (- (cube X4) (cube X18))))) (cube (quart X11))) (+ (- (- (cube (+ X13 X5)) (cube X15)) X12) (- (+ (+ (cube (cube (cube (cube X4)))) X14) (* (sqrt X7) X13)) X2))) (+ X6 X7))"));
		migrants.add(individual);
		m = new MigrantMessage(msgID, srcID , srcIP, srcPort, srcSubnetID, migrants);
		assertTrue(isSerializable(m));
		mnew = simpleSerialization(m);
		assertTrue(m.msgID.id == mnew.msgID.id);
		assertTrue(m.msgID.nodeID.equals(mnew.msgID.nodeID));
		assertTrue(m.srcID.equals(mnew.srcID));
		assertTrue(m.srcPort == mnew.srcPort);
		assertTrue(m.srcSubnetID.equals(mnew.srcSubnetID));
		assertTrue(m.msgType == mnew.msgType);
		assertTrue(m.migrants.get(0).equals(mnew.migrants.get(0)));
		assertTrue(m.migrants.equals(mnew.migrants));
		assertTrue(m.equals(mnew));

		// now add 100 more individuals and repeat
		for (int i = 0; i < 100; i++) {
			Individual individual2 = individual.copy();
			migrants.add(individual2);
		}
		m = new MigrantMessage(msgID, srcID , srcIP, srcPort, srcSubnetID, migrants);
		assertTrue(isSerializable(m));
		mnew = simpleSerialization(m);
		assertTrue(m.msgID.id == mnew.msgID.id);
		assertTrue(m.msgID.nodeID.equals(mnew.msgID.nodeID));
		assertTrue(m.srcID.equals(mnew.srcID));
		assertTrue(m.srcPort == mnew.srcPort);
		assertTrue(m.srcSubnetID.equals(mnew.srcSubnetID));
		assertTrue(m.msgType == mnew.msgType);
		assertTrue(m.migrants.get(0).equals(mnew.migrants.get(0)));
		assertTrue(m.migrants.equals(mnew.migrants));
		assertTrue(m.equals(mnew));
	}
}
