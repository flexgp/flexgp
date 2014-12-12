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
import evogpj.math.Function;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


/**
 * This class selects the top 10 models retrieved from the cloud run
 * before the fusion process
 * @author Ignacio Arnaldo
 */
public class GetFinalModels {
    
    String pathToIps, pathToModels, pathToFusionTraining;
    int timeThreshold;
    private final DataJava data;
    private double minTarget, maxTarget;
    
    
    /**
     * 
     * @param aPathToIps
     * @param aTimeThreshold
     * @param aPathToModels
     * @param aPathToFusion 
     */
    public GetFinalModels(String aPathToIps, int aTimeThreshold,String aPathToModels,String aPathToFusion){
        pathToIps = aPathToIps;
        timeThreshold = aTimeThreshold;
        pathToModels = aPathToModels;
        pathToFusionTraining = aPathToFusion;
        data = new CSVDataJava(pathToFusionTraining);
    }
    
    /**
     * filter models based on their MSE on training data
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void filterFinalModels() throws FileNotFoundException, IOException {
        ArrayList<String> alModels = new ArrayList<String>();
        File modelsFolder = new File(pathToIps);
        File[] IPFolders = modelsFolder.listFiles();
        
        for (File nodeFolder : IPFolders) {
            if (nodeFolder.isDirectory()) {
                File[] filesInFolder = nodeFolder.listFiles();
                ArrayList<String> filesINFolderAL = new ArrayList<String>();
                for(int i=0;i<filesInFolder.length;i++){
                    filesINFolderAL.add(filesInFolder[i].getName());
                }
                if(filesINFolderAL.contains("evolve.log")){
                    Scanner sc = new Scanner(new FileReader(nodeFolder.getAbsolutePath() + "/evolve.log"));
                    int indexLine =0;
                    while(sc.hasNextLine()){
                        String sAux = sc.nextLine();
                        if(sAux.contains("ELAPSED")){
                            String[] tokens = sAux.split(" ");
                            int currentTime = Integer.parseInt(tokens[9]);
                            if(currentTime<timeThreshold) indexLine++;
                        }
                    }
                    if(filesINFolderAL.contains("bestModelGeneration.txt")){
                        Scanner sc2 = new Scanner(new FileReader(nodeFolder.getAbsolutePath() + "/bestModelGeneration.txt"));
                        for(int i=0;i<indexLine-1;i++){
                            String midModel = sc2.nextLine();
                            alModels.add(midModel);
                        }
                        String finalModel = sc2.nextLine();
                        alModels.add(finalModel);
                    }
                }
            }
        }
        HashMap<String,Double> mapMSE = new HashMap<String, Double>();
        
        // Filter final models
        for(int i=0;i<alModels.size();i++){
            String modelS = alModels.get(i);
            String[] tokens = modelS.split(",");
            minTarget = Double.parseDouble(tokens[0]);
            maxTarget = Double.parseDouble(tokens[1]);
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
            
            evalModel(iAux);
            mapMSE.put(modelS,iAux.getScaledMSE());
        }
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(pathToModels));
        PrintWriter printWriter = new PrintWriter(bw);
        HashMap<String,Double> sorted = (HashMap<String,Double>)sortByValue(mapMSE);
        int maxIndex = 10;
        int currentIndex = 0;
        for(Map.Entry<String,Double> entry:sorted.entrySet()){
            if(currentIndex<10){
                double mseAux = entry.getValue();
                if( !Double.isInfinite(mseAux) && !Double.isNaN(mseAux)){
                    printWriter.write(entry.getKey() + "\n");
                    System.out.println("MSE top" + currentIndex + " is: " + mseAux);
                }
                currentIndex++;
            }
            
        }
        
        printWriter.flush();
        printWriter.close();
        
    }
    
    
    
    private double evalModel(Individual ind){
        ArrayList<String> alWeights = ind.getWeights();
        double[] lassoWeights = new double[alWeights.size()];
        for(int i=0;i<alWeights.size();i++){
            lassoWeights[i] = Double.parseDouble(alWeights.get(i));
        }
        double lassoIntercept = Double.parseDouble(ind.getLassoIntercept());
        double sqDiff = 0;
        Tree genotype = (Tree) ind.getGenotype();
        Function func = genotype.generate();
        List<Double> d;
        ArrayList<Double> interVals;
        double[][] inputValuesAux = data.getInputValues();
        double[] targets = data.getTargetValues();
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
            if(prediction<minTarget) prediction = minTarget;
            if(prediction>maxTarget) prediction = maxTarget;
            d.clear();
            interVals.clear();

            sqDiff += Math.pow(targets[i] - prediction, 2);
            d.clear();
        }
        sqDiff = sqDiff / data.getNumberOfFitnessCases();
        ind.setScaledMSE(sqDiff);
        return sqDiff;
    }
    
    static Map sortByValue(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
             @Override
             public int compare(Object o1, Object o2) {
                  return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
             }
        });

       Map result = new LinkedHashMap();
       for (Iterator it = list.iterator(); it.hasNext();) {
           Map.Entry entry = (Map.Entry)it.next();
           result.put(entry.getKey(), entry.getValue());
       }
       return result;
    } 
    
}

