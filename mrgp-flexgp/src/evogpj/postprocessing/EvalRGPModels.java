/**
 * Copyright (c) 2011-2014 Evolutionary Design and Optimization Group
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
package evogpj.postprocessing;

import evogpj.evaluation.java.DataJava;
import evogpj.genotype.Tree;
import evogpj.gp.Individual;

import java.util.ArrayList;
import java.util.List;

import evogpj.math.Function;

/**
 * Implements fitness evaluation for MRGP models
 * 
 * @author Ignacio Arnaldo
 */
public class EvalRGPModels {
    
    /**
     * Data used to evaluate models
     */
    private final DataJava data;
    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aData
     */
    public EvalRGPModels(DataJava aData) {
        this.data = aData;
    }

    /**
     * @param ind
     * @param indIndex
     * @param predictions
     * @param round
     * @see Function
     */
    public void eval(Individual ind,int indIndex,double[][] predictions,boolean round) {
        ArrayList<String> alWeights = ind.getWeights();
        double[] lassoWeights = new double[alWeights.size()];
        for(int i=0;i<alWeights.size();i++){
            lassoWeights[i] = Double.parseDouble(alWeights.get(i));
        }
        double lassoIntercept = Double.parseDouble(ind.getLassoIntercept());
        Tree genotype = (Tree) ind.getGenotype();
        Function func = genotype.generate();
        List<Double> d;
        ArrayList<Double> interVals;
        double[][] inputValuesAux = data.getInputValues();
        float[][] intermediateValues = new float[data.getNumberOfFitnessCases()][genotype.getSize()];
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesAux[i][j]);
            }
            interVals = new ArrayList<Double>();
            func.evalIntermediate(d,interVals);
            for(int t=0;t<interVals.size();t++){
                intermediateValues[i][t] = interVals.get(t).floatValue();
            }
            double prediction = 0;
            for(int j=0;j<lassoWeights.length;j++){
                prediction += intermediateValues[i][j]*lassoWeights[j];
            }
            prediction += lassoIntercept;
            if(prediction<ind.getMinTrainOutput()) prediction = ind.getMinTrainOutput();
            if(prediction>ind.getMaxTrainOutput()) prediction = ind.getMaxTrainOutput();
            
            //phenotype_tmp.addNewDataValue(prediction);
            if (round) prediction = Math.round(prediction);

            predictions[i][indIndex] = prediction;
	                
            d.clear();
            interVals.clear();
        }
    }

}