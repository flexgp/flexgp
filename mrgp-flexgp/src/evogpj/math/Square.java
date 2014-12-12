/**
 * Copyright (c) 2011-2013 Evolutionary Design and Optimization Group
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
package evogpj.math;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Owen Derby and Ignacio Arnaldo
 */
public class Square extends OneArgFunction {
        /**
         * 
         * @param a1 
         */
	public Square(Function a1) {
		super(a1);
		// TODO Auto-generated constructor stub
	}

    @Override
    public Double eval(List<Double> t) {
        return Math.pow(arg.eval(t), 2);
    }

    /**
     * Evaluate expression with corresponding weight
     * @param t
     * @param interVals
     * @return 
     */
    @Override
    public Double evalIntermediate(List<Double> t, ArrayList<Double> interVals) {
        double result = Math.pow(arg.evalIntermediate(t,interVals), 2);
        interVals.add(result);
        return result;
    }
    
    @Override
    public String getInfixFormatString() {
        //return "(%s.^2)";
        return "(square %s)";
    }
}
