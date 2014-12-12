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

import node.AbstractThread;

/**
 * The signal thread is a counter which resets a flag to true periodically, to
 * enable threads to execute certain events on a real-time schedule.
 * 
 * @author Owen Derby
 *
 */
public class Signal extends AbstractThread {
	// time to wait between signaling
	private final int pauseOffset;
	// the signal boolean
	private boolean signal;
	// a shutdown signal
	private boolean go;

	public Signal(int offset) {
		pauseOffset = offset;
		signal = false;
		go = true;
	}

	public synchronized void shutdown() {
		go = false;
	}

	public synchronized boolean getSignal() {
		return signal;
	}

	public synchronized void signalTrue() {
		signal = true;
	}

	public synchronized void signalFalse() {
		signal = false;
	}

	@Override
	public void run() {
		try {
			// initial sleep to allow for initialization
			Thread.sleep(1000 * pauseOffset);
			while (go) {
				// signal
				signalTrue();
				// sleep
				Thread.sleep(1000 * pauseOffset);
			}
			System.out.println("Signal: finished");
		} catch (InterruptedException e) {
			System.err.println("Signal: failed");
			e.printStackTrace();
		}

	}
}
