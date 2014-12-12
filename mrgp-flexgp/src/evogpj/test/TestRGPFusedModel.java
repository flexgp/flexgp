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
package evogpj.test;

import evogpj.evaluation.java.CSVDataJava;
import evogpj.evaluation.java.DataJava;
import evogpj.genotype.Tree;
import evogpj.genotype.TreeGenerator;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import java.util.ArrayList;
import java.util.List;

import evogpj.math.Function;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * Implements fitness evaluation for symbolic regression.
 * 
 * @author Ignacio Arnaldo
 */
public class TestRGPFusedModel{

    private String pathToData;
    private final DataJava data;

    private String pathToPop;
    private Population models;

    private boolean round;

    private double[] weightsARM;
    
    double mse;
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aPathToData
     * @param aPathToPop
     * @param aRound
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public TestRGPFusedModel(String aPathToData, String aPathToPop,boolean aRound) throws IOException, ClassNotFoundException {
        pathToData = aPathToData;
        pathToPop = aPathToPop;
        round = aRound;
        this.data = new CSVDataJava(pathToData);
        readFusedModel(pathToPop);
    }

    private void readFusedModel(String filePath) throws IOException, ClassNotFoundException{
        models = new Population();
        ArrayList<String> alModels = new ArrayList<String>();
        Scanner sc = new Scanner(new FileReader(filePath));
        int indexModel =0;
        while(sc.hasNextLine()){
            String sAux = sc.nextLine();
            alModels.add(indexModel, sAux);
            indexModel++;
        }
        int popSize = alModels.size();
        weightsARM = new double[popSize];
        for(int i=0;i<popSize;i++){
            String scaledModel = alModels.get(i);
            String[] tokens = scaledModel.split(",");
            weightsARM[i] = Double.parseDouble(tokens[0]);
            double minTargetAux = Double.parseDouble(tokens[1]);
            double maxTargetAux = Double.parseDouble(tokens[2]);
            String[] weightsArrayS = tokens[3].split(" ");
            ArrayList<String> alWeights = new ArrayList<String>();
            for(int j=0;j<weightsArrayS.length;j++){
                alWeights.add(weightsArrayS[j]);
            }
            String interceptS = tokens[4];
            String model = tokens[5];
            Tree g = TreeGenerator.generateTree(model);
            Individual iAux = new Individual(g);
            iAux.setWeights(alWeights);
            iAux.setLassoIntercept(interceptS);
            iAux.setMinTrainOutput(minTargetAux);
            iAux.setMaxTrainOutput(maxTargetAux);
            models.add(i, iAux);
        }

    }
    
    /**
     * @return 
     * @see Function
     */
    public ArrayList<Double> eval() {
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        double MSE = 0;
        double MAE = 0;
        double[] target = data.getTargetValues();

        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesAux[i][j]);
            }
            double predictedValue = 0;
            for(int j=0;j<models.size();j++){
                if(weightsARM[j] >= 0.00001){
                    Individual ind = models.get(j);
                    ArrayList<String> alWeights = ind.getWeights();
                    double[] lassoWeights = new double[alWeights.size()];
                    for(int w=0;w<alWeights.size();w++){
                        lassoWeights[w] = Double.parseDouble(alWeights.get(w));
                    }
                    double lassoIntercept = Double.parseDouble(ind.getLassoIntercept());
                    Tree genotype = (Tree) ind.getGenotype();
                    Function func = genotype.generate();
                    float[] intermediateValues = new float[genotype.getSize()];
                    ArrayList<Double> interVals = new ArrayList<Double>();
                    func.evalIntermediate(d,interVals);
                    for(int t=0;t<interVals.size();t++){
                        intermediateValues[t] = interVals.get(t).floatValue();
                    }
                    double prediction = 0;
                    for(int w=0;w<lassoWeights.length;w++){
                        prediction += intermediateValues[w]*lassoWeights[w];
                    }
                    prediction += lassoIntercept;
                    if (round) prediction = Math.round(prediction); 
                    if(prediction<ind.getMinTrainOutput()) prediction = ind.getMinTrainOutput();
                    if(prediction>ind.getMaxTrainOutput()) prediction = ind.getMaxTrainOutput();
                    interVals.clear();
                    
                    predictedValue += weightsARM[j] * prediction;
                    func = null;
                }
            }
            if(round) predictedValue = Math.round(predictedValue);
            double difference = target[i] - predictedValue;
            MSE += Math.pow(difference, 2);
            MAE += Math.abs(target[i] - predictedValue);
            d.clear();
        }
        int numFitnessCases = data.getNumberOfFitnessCases();
        MSE = MSE / numFitnessCases;
        MAE = MAE / numFitnessCases;
        ArrayList<Double> errors = new ArrayList<Double>();
        errors.add(MSE);
        errors.add(MAE);
        return errors;
    }
}