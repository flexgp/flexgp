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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import node.Control;
import node.NodeList.NoSuchNodeIDException;
import node.NodeList.Status;

import org.junit.Before;
import org.junit.Test;

import utility.ArgLexer;
import utility.ArgParser;
import utility.ArgParser.UnhandledArgException;

/**
 * @author Dylan Sherry
 */
public class ArgParserTest {
	String ips;
	String args;
	ArgLexer l;
	ArgParser p;

	String ip1 = "0.0.0.1";
	String ip2 = "0.0.0.2";
	String ip3 = "0.0.0.3";

	@Before
	public void before() {
		ips = ip1 + " " + ip2 + " " + ip3;
		args = "-i localhost -t 100 -p 9000 -n " + ips;
		l = new ArgLexer(args);
		try {
			p = new ArgParser(l.groups);
		} catch (UnhandledArgException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testParseArgs() {
		try {
			Control c = new Control(p, null, null, null);
			// was the target node count?
			assertEquals(c.nodeList.targetNodeCount, 25);
			// were the new IPs?
			assertTrue(c.nodeList.goodNodeTable.size() == 3);
			assertTrue(c.nodeList.badNodeTable.size() == 0);
			assertTrue(c.nodeList.contains("0.0.0.1:9000").equals(Status.GOOD));
			assertTrue(c.nodeList.contains("0.0.0.2:9000").equals(Status.GOOD));
			assertTrue(c.nodeList.contains("0.0.0.3:9000").equals(Status.GOOD));
			assertTrue(c.nodeList.get("0.0.0.1:9000").ip.equals(ip1));
			// assertTrue(c.nodeList.get(1).port == 10);
			assertTrue(c.nodeList.get("0.0.0.2:9000").ip.equals(ip2));
			// assertTrue(c.nodeList.get(2).port == 20);
			assertTrue(c.nodeList.get("0.0.0.3:9000").ip.equals(ip3));
			// assertTrue(c.nodeList.get(3).port == 30);
		} catch (NoSuchNodeIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Invaild node specified in testParseArgs");
		}
	}
}
