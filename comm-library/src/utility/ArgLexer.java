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

/**
 * Parse command-line arguments from Control.main(String[] args)
 * 
 * @author Dylan Sherry
 * 
 */
public class ArgLexer {
	// the raw arguments
	public String[] args;
	// the lexed tokens
	public ArrayList<String> tokens;
	// the grouped tokens, of the form '-i' --> 'commandLine text'
	public HashMap<String, String> groups;

	public ArgLexer(String[] _args) {
		args = _args;
		makeTokens();
	}

	public ArgLexer(String _args) {
		this(_args.split(" "));
	}

	public ArrayList<String> getTokens() {
		return tokens;
	}

	public HashMap<String, String> getGroups() {
		return groups;
	}

	/**
	 * Iteratively process raw args into tokens
	 * 
	 * @return
	 */
	public void makeTokens() {
		groups = new HashMap<String, String>();
		tokens = new ArrayList<String>();
		String next = "";
		String lastFlag = "";
		boolean flag = true;
		for (int i = 0; i < args.length; i++) {
			// get next token
			next = args[i];
			if (next.substring(0, 1).equals("-")) {
				// the next raw chunk starts with "-", make a new token
				tokens.add(next);
				// add to groups
				groups.put(next, "");
				lastFlag = next;
				// toggle flag
				flag = true;
			} else if (flag) {
				// the previous chunk was a flag, and this chunk is not -- new
				tokens.add(next);
				// new entry in groups map
				groups.put(lastFlag, next);
				// toggle flag
				flag = false;
			} else {
				// the previous chunk was not a flag, and this is a continuation
				tokens.set(tokens.size() - 1, tokens.get(tokens.size() - 1)
						+ " " + next);
				// update groups map
				String p = groups.get(lastFlag)
						+ ((groups.get(lastFlag).equals("")) ? "" : " ") + next;
				groups.put(lastFlag, p);
			}
		}
	}
}
