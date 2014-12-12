/**
 * Copyright (c) 2012 Owen Derby
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

import java.sql.Timestamp;
import java.util.Random;

import node.NodeList;

import org.junit.Before;
import org.junit.Test;

import utility.IP;

/**
 * @author Owen Derby
 */
public class NodeListMergeTest {

	private Timestamp t1, t2, t3;
	private IP ip1, ip2, ip3;
	private NodeList n1, n2;
	private Random random;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		random = new Random();
		// generate a couple timestamps
		t1 = NodeList.currTime();
		// advance the clock
		Thread.sleep((long) 250.0);
		t2 = NodeList.currTime();
		Thread.sleep((long) 250.0);
		t3 = NodeList.currTime();

		ip1 = new IP("1", "1", 0);
		ip2 = new IP("2", "2", 0);
		ip3 = new IP("3", "3", 0);

		n1 = new NodeList(random, 100, "1001");
		n1.succeed(ip1, "subnet id 1", t2);
		n1.fail(ip2, "subnet id 1", t2);

		n2 = new NodeList(random, 100, "1002");
	}

	@Test
	public void nullUpdate() {
		// Test merge when other list is empty
		NodeList n1_copy = n1.copy();
		n1.merge(n2.copy());
		assertTrue(n1.equals(n1_copy));
	}

	@Test
	public void noNewInfoUpdate() {
		// Test merge when other has old information
		n2.succeed(ip1, "subnet id 1", t1);
		n2.fail(ip2, "subnet id 1", t1);
		NodeList n1_copy = n1.copy();
		n1.merge(n2.copy());
		assertTrue(n1.equals(n1_copy));

		// If one succeeds recently, make sure it gets merged, and that other isn't affected
		NodeList n2_copy = n2.copy();
		n2_copy.succeed(ip1, "subnet id 1", t3);
		n1_copy.merge(n2_copy.copy());
		assertTrue(n1_copy.goodNodeTable.size() == 1);
		assertTrue(n1_copy.badNodeTable.size() == 1);
		assertTrue(n1_copy.goodNodeTable.get(ip1.id).equals(n2_copy.goodNodeTable.get(ip1.id)));
		assertTrue(n1_copy.badNodeTable.get(ip2.id).equals(n1.badNodeTable.get(ip2.id)));

		// If one fails recently, make sure it gets merged, and that other isn't affected
		n1_copy = n1.copy();
		n2_copy = n2.copy();
		n2_copy.fail(ip2, "subnet id 1", t3);
		n1_copy.merge(n2_copy.copy());
		assertTrue(n1_copy.goodNodeTable.size() == 1);
		assertTrue(n1_copy.badNodeTable.size() == 1);
		assertTrue(n1_copy.goodNodeTable.get(ip1.id).equals(n1.goodNodeTable.get(ip1.id)));
		assertTrue(n1_copy.badNodeTable.get(ip2.id).equals(n2_copy.badNodeTable.get(ip2.id)));

		// Test merge when other has same information, but with more recent time stamps
		n2.succeed(ip1, "subnet id 1", t3);
		n2.fail(ip2, "subnet id 1", t3);
		n1.merge(n2.copy());
		assertTrue(n1.equals(n2));
	}

	@Test
	public void conflictingUpdate() {
		// Test merge with other with conflicting info

		// If old, don't update
		n2.fail(ip1, "subnet id 1", t1);
		n2.succeed(ip2, "subnet id 1", t1);
		NodeList n1_copy = n1.copy();
		n1.merge(n2.copy());
		assertTrue(n1.equals(n1_copy));

		// If one succeeds recently, make sure it gets merged, and that other isn't affected
		NodeList n2_copy = n2.copy();
		n2_copy.succeed(ip2, "subnet id 1", t3);
		n1_copy.merge(n2_copy.copy());
		assertTrue(n1_copy.badNodeTable.isEmpty());
		assertTrue(n1_copy.goodNodeTable.size() == 2);
		assertTrue(n1_copy.goodNodeTable.get(ip2.id).equals(n2_copy.goodNodeTable.get(ip2.id)));
		assertTrue(n1_copy.goodNodeTable.get(ip1.id).equals(n1.goodNodeTable.get(ip1.id)));

		// If one fails recently, make sure it gets merged, and that other isn't affected
		n1_copy = n1.copy();
		n2_copy = n2.copy();
		n2_copy.fail(ip1, "subnet id 1", t3);
		n1_copy.merge(n2_copy.copy());
		assertTrue(n1_copy.goodNodeTable.isEmpty());
		assertTrue(n1_copy.badNodeTable.size() == 2);
		assertTrue(n1_copy.badNodeTable.get(ip1.id).equals(n2_copy.badNodeTable.get(ip1.id)));
		assertTrue(n1_copy.badNodeTable.get(ip2.id).equals(n1.badNodeTable.get(ip2.id)));

		// If both swap, make sure merge is correct
		n2.fail(ip1, "subnet id 1", t3);
		n2.succeed(ip2, "subnet id 1", t3);
		n1.merge(n2.copy());
		assertTrue(n1.equals(n2));
	}

	@Test
	public void newInfoUpdate() {
		n2.succeed(ip3, "subnet id 1", t1);
		n1.merge(n2.copy());
		assertTrue(n1.goodNodeTable.size() == 2);
		assertTrue(n1.badNodeTable.size() == 1);
		assertTrue(n1.goodNodeTable.get(ip3.id).equals(n2.goodNodeTable.get(ip3.id)));

		n2.fail(ip3, "subnet id 1", t2);
		n1.merge(n2.copy());
		assertTrue(n1.goodNodeTable.size() == 1);
		assertTrue(n1.badNodeTable.size() == 2);
		assertTrue(n1.badNodeTable.get(ip3.id).equals(n2.badNodeTable.get(ip3.id)));
	}
}
