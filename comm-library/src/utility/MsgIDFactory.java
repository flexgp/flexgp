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

/**
 * Generate message IDs
 * 
 * @author Owen Derby
 * 
 */
public class MsgIDFactory {

	private long id;
	private final String nodeID;

	public MsgIDFactory(String _nodeID) {
		id = 0;
		nodeID = _nodeID;
	}

	/**
	 * Get a new message id. Synchronized because Control and Evolve threads
	 * access it
	 * @return
	 */
	public synchronized MsgID get() {
		return new MsgID(nodeID, ++id);
	}

}
