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

import evogpj.postprocessing.RGPModelFuserARM;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Class that reads MRGP models and fuses them via ARM
 * 
 * @author Ignacio Arnaldo
 */
public class FuseRGPModels {
    
    private String pathToData;
    private final DataJava data;
        
    private String pathToPop;
    private Population models;
    private int numFeatures;
    private boolean round;
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
    public FuseRGPModels(String aPathToData, String aPathToPop,boolean aRound) throws IOException, ClassNotFoundException {
        pathToData = aPathToData;
        pathToPop = aPathToPop;
        round = aRound;
        this.data = new CSVDataJava(pathToData);
        numFeatures = data.getNumberOfFeatures();
        readRGPModels(pathToPop);
    }

    /**
     * read MRGP models
     * @param filePath
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private void readRGPModels(String filePath) throws IOException, ClassNotFoundException{
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
        for(int i=0;i<popSize;i++){
            String scaledModel = alModels.get(i);
            String[] tokens = scaledModel.split(",");
            
            double minTargetAux = Double.parseDouble(tokens[0]);
            double maxTargetAux = Double.parseDouble(tokens[1]);
            
            String[] weightsArrayS = tokens[2].split(" ");
            ArrayList<String> alWeights = new ArrayList<String>();
            for(int j=0;j<weightsArrayS.length;j++){
                alWeights.add(weightsArrayS[j]);
            }
            String interceptS = tokens[3];
            String model = tokens[4];
            
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
     * fuse MRGP models
     * @param modelPath
     * @throws IOException 
     */
    public void fuseModels(String modelPath) throws IOException{
        int iters = 100;
        RGPModelFuserARM mfa = new RGPModelFuserARM(pathToData,numFeatures,models,iters,round);
        double[] armWeights = mfa.arm_weights();
        BufferedWriter bw = new BufferedWriter(new FileWriter(modelPath));
        PrintWriter printWriter = new PrintWriter(bw);
        
        // print fused model
        for(int i=0;i<models.size();i++){
            Individual ind = models.get(i);
            printWriter.write(armWeights[i] + ",");
            printWriter.write(ind.getMinTrainOutput() + ",");
            printWriter.write(ind.getMaxTrainOutput() + ",");
            for(int j = 0;j<ind.getWeights().size()-1;j++){
                printWriter.write(ind.getWeights().get(j) + " ");
            }
            printWriter.write(ind.getWeights().get(ind.getWeights().size()-1) + ",");
            printWriter.write(ind.getLassoIntercept() + ",");
            printWriter.write(ind.toString() + "\n");
        }
        printWriter.flush();
        printWriter.close();
        
        System.out.println();
    }
    
}