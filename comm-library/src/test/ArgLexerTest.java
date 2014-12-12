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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


import org.junit.Test;

import utility.ArgLexer;

public class ArgLexerTest {

	/**
	 * A test of the ArgLexer This code should be used to write
	 * Control.processArgs or similar.
	 */
	@Test
	public void simpleLexTest() {
		String argstring = "-1 one -2 two -v -3 three four five -6 -7 seven";
		String expected = "|-1| |one| |-2| |two| |-v| |-3| |three four five| |-6| |-7| |seven|";
		HashMap<String, String> expectedGroups = new HashMap<String, String>();
		expectedGroups.put("-1", "one");
		expectedGroups.put("-2", "two");
		expectedGroups.put("-v", "");
		expectedGroups.put("-3", "three four five");
		expectedGroups.put("-6", "");
		expectedGroups.put("-7", "seven");
		String s = "";
		String[] args = argstring.split(" ");
		// get results
		ArgLexer al = new ArgLexer(args);
		ArrayList<String> tokens = al.getTokens();
		Iterator<String> ti = tokens.iterator();
		while (ti.hasNext()) {
			s += "|" + ti.next() + "| ";
		}
		// remove final " "
		s = s.substring(0, s.length() - 1);
		// compare
		System.out.println(expected);
		System.out.println(s);
		assertTrue(expected.equals(s));
		// check the group maps
		HashMap<String, String> groups = al.getGroups();
		assertTrue(groups.equals(expectedGroups));
	}
}
