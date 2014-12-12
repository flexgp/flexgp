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

import java.util.Random;
import java.util.Set;

/**
 * A class used to define makeMessageID for both the Control and Evolve threads
 * 
 * @author Dylan Sherry
 * 
 */
public abstract class AbstractThread implements Runnable {

	protected Random random;

	public AbstractThread() {
		random = new Random();
	}

	/**
	 * Synchronized wrapper to get random number
	 */
	public synchronized int nextInt(int max) {
		return random.nextInt(max);
	}
	
	/**
	 * Grab a random key from a HashSet<String>. O(n)
	 * 
	 * @param hs
	 * @return
	 */
	protected String getRandomKey(Set<String> hs) {
		int size = hs.size();
		int item = nextInt(size);
		int i = 0;
		for (String s : hs) {
			if (i == item)
				return s;
			i += 1;
		}
		return "";
	}

}
