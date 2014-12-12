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
package utility;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import net.minidev.json.JSONValue;

/**
 * @author Owen Derby
 *
 */
public class JSONFilter implements Filter {

	/* (non-Javadoc)
	 * @see java.util.logging.Filter#isLoggable(java.util.logging.LogRecord)
	 */
	@Override
	public boolean isLoggable(LogRecord record) {
		return JSONValue.isValidJsonStrict(record.getMessage());
	}

}
