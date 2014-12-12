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

/**
 * Interface to outline the basic requirements of any algorithm to be used with
 * the FlexGP distributed computing system.
 * 
 * @author Owen Derby
 */
public interface Algorithm extends Publisher {

	/**
	 * Test method to determine if the algorithm has more work to do.
	 * 
	 * @return True as long as the implementing algorithm has work.
	 */
	public boolean running();

	/**
	 * Step the algorithm forward one computing chunk (e.g. generation for GP)
	 * 
	 * @throws AlgorithmException on any errors thrown by underlying algorithm
	 */
	public void proceed() throws AlgorithmException;

	/**
	 * Take any final action which needs to be taken before exiting
	 */
	public void cleanup();
	
	public class AlgorithmException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1221203738037833999L;

		public AlgorithmException(String string) {
			super(string);
		}
	}
}
