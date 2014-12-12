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
import evogpj.evaluation.java.CSVDataJava;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class implements the Adaptive Regression by Mixing algorithm as 
 * described in Yuhong Yang, Adaptive Regression by Mixing, Journal of American
 * Statistical Association, 96:454, 574-588, 2001.
 * @author Ignacio Arnaldo
 */
public class RGPModelFuserARM {
    
    String fusionTrainingSet;
    DataJava ad;
    int numIters;
    // DataJava ad;
    int numberOfLines;
    Population models; // referred to as procedures in the article
    int numModels;
    int indexSplit;
    int numberOfFeatures;
    boolean round;
    double[][] predictions, predictionsShuffled;
    double[] target,targetShuffled;
    
    double[] expEjs;
    double[][] lnWjs;
    double[] wjs;
    
    /**
     * 
     * @param aTrainingSet
     * @param aNumberOfFeatures
     * @param aPop
     * @param aNumIters
     * @param aRound 
     */
    public RGPModelFuserARM(String aTrainingSet, int aNumberOfFeatures, Population aPop,int aNumIters,boolean aRound){
        fusionTrainingSet = aTrainingSet;
        ad = new CSVDataJava(fusionTrainingSet);
        numberOfLines = ad.getNumberOfFitnessCases();
        numberOfFeatures = aNumberOfFeatures;
        target = ad.getTargetValues();
        numIters = aNumIters;
        models = aPop;
        numModels = models.size();
        round = aRound;
        target = new double[numberOfLines];
        targetShuffled = new double[numberOfLines];
        predictions = new double[numberOfLines][numModels];
        predictionsShuffled = new double[numberOfLines][numModels];
        expEjs = new double[numModels];
        
        lnWjs = new double[numIters][numModels];
        wjs = new double[numModels];
        //ad = new DataJava(aTrainingSet);
    }
    
    /*
     Computes std of the subset of the input vector [0;indexSplit-1]
     */
    private double std(double[] input,int indexSplit){ 
        double sum = 0;
        for (int i = 0; i < indexSplit; i++){
            sum += input[i];
        }
        //double mean = sum/input.length;
        double mean = sum/indexSplit;
        double sd = 0;
        for (int i = 0; i < indexSplit; i++){
            //sd += Math.pow(input[i] - mean,2) / input.length ;
            sd += Math.pow(input[i] - mean,2) / indexSplit ;
        }
        double standardDeviation = Math.sqrt(sd);
        return standardDeviation;
    }

    private void steps0TO3(){ 
        
        // STEP 0: randomly permute the order of the observations
        // we don't need to compute the predictions again
        // we just permute the order of the target values and the order of the predictions of every model
        ArrayList<Integer> indices = new ArrayList();
        for(int i=0;i<numberOfLines;i++){
            indices.add(i,i); // add(index,Integer)
        }
        Collections.shuffle(indices);
        //predictionsShuffled = new double[numberOfLines][numModels];
        for(int i=0;i<numberOfLines;i++){
            int shuffledIndex = indices.get(i);
            targetShuffled[i] = target[shuffledIndex];
            //for(int j=0;j<numModels;j++) predictionsShuffled[i][j] = predictions[i][j];
            // CHECK IF THIS WORKS
            System.arraycopy(predictions[shuffledIndex], 0, predictionsShuffled[i], 0, numModels);
        }
        
        // STEP 1: split the data in two parts Z1=(X1,Y1) and Z2=(X2,Y2)
        indexSplit = (int)Math.round(numberOfLines / (double)2);
        
        // STEP 2: for each model j do 
        //         a) obtain estimates of f based on Z1 == predictions of f in Z1 -> done in STEP 0
        //         b) estimate variance of predictions in Z1 vs. Y1
        for(int j=0;j<numModels;j++){
            double[] rawError = new double[numberOfLines];
            double[] squaredError = new double[numberOfLines];
            for(int i=0;i<numberOfLines;i++){
                rawError[i] = targetShuffled[i] - predictionsShuffled[i][j];
                squaredError[i] = Math.pow(rawError[i],2);
            }
            // compute std of subset [0;indexSplit-1]
            double sigmaEst = std(rawError,indexSplit);
            //compute sum of squared errors in predictions[indexSplit;N-1]
            double sumSquaredError = 0;
            for(int i=indexSplit;i<numberOfLines;i++){
                sumSquaredError += squaredError[i]; 
            }
            // compute Ej as in paper, "if the variance estimator is a constant function..."
            // instead, compute only the exponent of Ej, we have Ej = exp(exponentEj)
            // replace first sigma with exp(log(sigma))
            // operating in the exp space to avoid numerical underflow
            double firstTermExp = (-numberOfLines/(double)2) * Math.log(sigmaEst);
            double secondTermExp = ((-sumSquaredError * Math.pow(sigmaEst,-2))/2);
            expEjs[j] = firstTermExp + secondTermExp;
            //expEjs[j] = ((-numberOfLines/2) * Math.log(sigmaEst)) + ((-sumSquaredError * Math.pow(sigmaEst,-2))/2);
            
            rawError = null;
            squaredError = null;
        }
    }
    
    // compute current weights wj = Ej / sum_1^J (Ej)
    // instead, compute ln of the weights Wjs
    // lnWj = expEj - logsumExp(expEl), for l in [1;J]
    private void step04(int iter){
        //compute logsumexp(expEl), for l in [1;J]
        double lnSumExp = logSumExpExpEjs();
        
        for(int j=0;j<numModels;j++){
            lnWjs[iter][j] = expEjs[j] - lnSumExp;
        }
        
        
    }
   
    /*function s = logsumexp(a, dim, method)

        % subtract the largest in each column
        y = max(a,[],dim);
        a = bsxfun(@minus, a, y);
        s = y + log(sum(exp(a),dim));
        i = find(~isfinite(y));
        if ~isempty(i)
          s(i) = y(i);
    end*/
    private double logSumExpExpEjs(){
        //if(expEjs.length == 1) return expEjs[0];
        double maxExpEj = - Double.MAX_VALUE;
        for(int i=0;i<expEjs.length;i++){
            if(expEjs[i]>maxExpEj) maxExpEj = expEjs[i];
        } 
        double sumExps = 0.0;
        for(int i=0;i<expEjs.length;i++){
            //sumExps += Math.exp(expEjs[j]);
            if(expEjs[i] != Double.NEGATIVE_INFINITY){
                sumExps += Math.exp(expEjs[i] - maxExpEj);
            }
        }
        //double lnSumExp = Math.log(sumExps);
        double lnSumExp = maxExpEj + Math.log(sumExps);
        if(lnSumExp == Double.MAX_VALUE) lnSumExp = maxExpEj;
        return lnSumExp;
    }
    
    private void step5(){ // STEP 5: average weights over m iterations
        for(int j=0;j<numModels;j++){
            double sumExpLnWjs = 0;    
            for(int m=0;m<numIters;m++){
                sumExpLnWjs += Math.exp(lnWjs[m][j]);
            }
            wjs[j] = sumExpLnWjs / numIters;
        }
        
    }
    
    /**
     * This method computes the weights of the procedures/models 
     * according to the five steps described in the paper by Yang.
     * @return weights associated to the different predictors
     */
    public double[] arm_weights(){
        EvalRGPModels em = new EvalRGPModels(ad);
        for(int indexInd=0;indexInd<models.size();indexInd++){
            Individual ind = models.get(indexInd);
            em.eval(ind,indexInd,predictions,round);
        }
        
        for(int iter=0;iter<numIters;iter++){
            steps0TO3();
            step04(iter);
        }
        
        step5();
        return wjs;
    }
    

    
}
