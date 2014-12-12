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
package network;

/**
 * 
 * An "enum" of message types where each type has a small integer value
 * associated with it
 * 
 * @author Dylan Sherry
 */
public class MessageType {
	// Control-related messages
	public final static int PING = 0;
	public static final int PING_RESPONSE = 1;
	public static int STATUS = 2;
	public static int STATUS_RESPONSE = 3;
	public static int CONTROL_FLOW = 4;
	public static int NODELIST = 5;
	public static int NODELIST_RESPONSE = 6;
	// the boundary
	public static int CONTROL = 31;

	// Evolve-related messages
	public static int MIGRANT_MESSAGE = 32;
	public static int BEST_INDIVIDUAL = 33;
	// the boundary
	public static int EVOLVE = 64;

	// Data-related messages
	public static int DATA_DESCRIPTION = 64;
	// the boundary
	public static int DATA = 96;

}
