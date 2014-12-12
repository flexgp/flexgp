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
package main;

import evogpj.algorithm.Parameters;
import evogpj.algorithm.SymbRegMOO;
import evogpj.gp.Individual;
import evogpj.test.FuseRGPModels;
import evogpj.test.GetFinalModels;
import evogpj.test.TestRGPFusedModel;
import evogpj.test.TestRGPModels;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Manager class to run the desktop version of MRGP
 * and to fuse the models retrieved from the FlexGP cloud runs
 * 
 * @author Ignacio Arnaldo
 */
public class SRLearnerMenuManager {
   
    /**
     * Empty constructor
     */
    public SRLearnerMenuManager(){
        
    }
    
    /**
     * Print usage of the command line interface of the executable
     */
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("TRAIN:");
        System.err.println("java -jar mrgp-flexgp.jar -train path_to_data -minutes min [-properties path_to_properties]");
        System.err.println();
        System.err.println("OBTAIN PREDICTIONS:");
        System.err.println("java -jar mrgp-flexgp.jar -predict path_to_data -o path_to_predictions -integer true -scaled path_to_scaled_models");
        System.err.println("or");
        System.err.println("java -jar mrgp-flexgp.jar -predict path_to_data -o path_to_predictions -integer true -fused path_to_fused_model");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar mrgp-flexgp.jar -test path_to_data");
        System.err.println("or");
        System.err.println("java -jar mrgp-flexgp.jar -test path_to_data -integer true -scaled path_to_scaled_models");
        System.err.println("or");
        System.err.println("java -jar mrgp-flexgp.jar -test path_to_data -integer true -fused path_to_fused_model");
        System.err.println();
        System.err.println("GET FILTERED MODELS:");
        System.err.println("java -jar mrgp-flexgp.jar -getFinalModels path_to_ips secondsThreshold path_to_fusion_data");
        System.err.println();
        System.err.println("FUSE AND STATS:");
        System.err.println("java -jar mrgp-flexgp.jar -fusedStats path_to_pop path_to_fusion_data path_to_testing_data");
        System.err.println();
    }
    
    /**
     * parse the train command
     * @param args
     * @throws IOException 
     */
    public void parseSymbolicRegressionTrain(String args[]) throws IOException{
        String dataPath;
        int numMinutes=0;
        String propsFile = "";
        SymbRegMOO srEvoGPj;
        if(args.length==4 || args.length==5 || args.length==6 || args.length==7 || args.length==8){
            dataPath = args[1];
            // run evogpj with standard properties
            Properties props = new Properties();
            props.put(Parameters.Names.PROBLEM, dataPath);
            if (args[2].equals("-minutes")) {
                numMinutes = Integer.valueOf(args[3]);
                if(args.length==4){// JAVA NO PROPERTIES
                    // run evogpj with standard properties
                    srEvoGPj = new SymbRegMOO(props,numMinutes*60);
                    Individual bestIndi = srEvoGPj.run_population();
                }
                if(args.length==6 || args.length==8){
                    if(args[4].equals("-properties")){ // JAVA WITH PROPERTIES
                        propsFile = args[5];
                        // run evogpj with properties file and modified properties
                        srEvoGPj = new SymbRegMOO(props,propsFile,numMinutes*60);
                        Individual bestIndi = srEvoGPj.run_population();
                    }else{
                        System.err.println("Error: wrong argument. Expected -cpp flag");
                        printUsage();
                    }
                }
            }else{
                System.err.println("Error: must specify the optimization time in minutes");
                printUsage();
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
        }
    }
    
    
    //java -jar evogpj.jar -predictions path_to_data -o filename -integer true -scaled path_to_scaled_models
    /**
     * parse the predictions command
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public void parseSymbolicRegressionPredictions(String args[]) throws IOException, ClassNotFoundException{
        String dataPath;
        String popPath;
        String predPath;
        boolean integerTarget;
        if(args.length==8){
            dataPath = args[1];
            if(args[2].equals("-o")){
                predPath = args[3];
                if(args[4].equals("-integer")){
                    integerTarget = Boolean.valueOf(args[5]);
                    popPath = args[7];
                    if(args[6].equals("-scaled")){
                        TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                        tsm.predictionsPop(predPath);
                    }else{
                        System.err.println("Error: wrong argument. Expected -scaled flag");
                        printUsage();
                    }
                }else{
                    System.err.println("Error: wrong argument. Expected -integer flag");
                    printUsage();
                }
            }else{
                System.err.println("Error: wrong argument. Expected -o flag");
                printUsage();
            }
        }else {
            System.err.println("Error: wrong number of arguments");
            printUsage();
        }
    }
    
    //java -jar evogpj.jar -test path_to_data -integer true -scaled path_to_scaled_models
    /**
     * parse the test command
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public void parseSymbolicRegressionTest(String args[]) throws IOException, ClassNotFoundException{
        String dataPath;
        String popPath;
        boolean integerTarget;
        if (args.length==2){
            // by default integer targets = false
            integerTarget = false;
            dataPath = args[1];
            // check if knee model exists
            popPath = "knee.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING KNEE MODEL:");
                TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "mostAccurate.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING MOST ACCURATE MODEL: ");
                TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "leastComplex.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING SIMPLEST MODEL: ");
                TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "pareto.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING PARETO MODELS: ");
                TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            
        }else if(args.length==6){
            dataPath = args[1];
            if(args[2].equals("-integer")){
                integerTarget = Boolean.valueOf(args[3]);
                popPath = args[5];
                if(args[4].equals("-scaled")){
                    TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                    tsm.evalPop();
                    tsm.saveModelsToFile("test"+popPath);
                }else{
                    System.err.println("Error: wrong argument. Expected -scaled or -fused flag");
                    printUsage();
                }
            }else{
                System.err.println("Error: wrong argument. Expected -integer flag");
                printUsage();
            }
        }else {
            System.err.println("Error: wrong number of arguments");
            printUsage();
        }
        
    }
    
    
    //java -jar evogpj.jar -getFinalModel path_to_ips secondsThreshold path_to_fusion_data
    /**
     * parse filter command
     * @param args
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void parseSymbolicRegressionGetFinalModels(String args[]) throws FileNotFoundException, IOException {
        String pathToIps, pathToFusionTraining;
        int secondsThreshold;
        if (args.length==4){
            pathToIps = args[1];
            secondsThreshold = Integer.parseInt(args[2]);
            pathToFusionTraining = args[3];
            String modelsPath = "finalModels.txt";
            GetFinalModels gfm = new GetFinalModels(pathToIps,secondsThreshold,modelsPath,pathToFusionTraining);
            gfm.filterFinalModels();
        }else{
            printUsage();
        }
            
    }
    
    
    //java -jar evogpj.jar -fusedStats path_to_pop path_to_fusion_data path_to_testing_data
    /**
     * parse fuse command
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public void parseSymbolicRegressionFusedStats(String args[]) throws IOException, ClassNotFoundException{
        String fusionTrainingPath;
        String testingPath;
        String popPath;
        boolean integerTarget;
        if (args.length==4){
            // by default integer targets = false
            integerTarget = false;
            popPath = args[1];
            fusionTrainingPath = args[2];
            testingPath = args[3];
            // check if knee model exists
            String modelPath = "fusedModel.txt";
            if(new File(popPath).isFile()){
                System.out.println("FUSING MODELS:");
                FuseRGPModels fm = new FuseRGPModels(fusionTrainingPath, popPath,integerTarget);
                fm.fuseModels(modelPath);
            }
            if(new File(modelPath).isFile()){
                System.out.println("TESTING FUSED MODEL:");
                TestRGPFusedModel tfm = new TestRGPFusedModel(testingPath, modelPath, integerTarget);
                ArrayList<Double> errors = tfm.eval();
                System.out.println("MSE fused Model: " + errors.get(0));
                System.out.println("MAE fused Model: " + errors.get(1));
                System.out.println();
            }
        }else{
            printUsage();
        }
    }
        

    /**
     * Entry point of the code,
     * Detects subcommand to parse
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     */
    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException{
        SRLearnerMenuManager m = new SRLearnerMenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
        }else{
            if (args[0].equals("-train")) {
                m.parseSymbolicRegressionTrain(args);
            }else if(args[0].equals("-predict")){
                m.parseSymbolicRegressionPredictions(args);
            }else if(args[0].equals("-test")){
                m.parseSymbolicRegressionTest(args);
            }else if(args[0].equals("-getFinalModels")){
                m.parseSymbolicRegressionGetFinalModels(args);
            }else if(args[0].equals("-fusedStats")){
                m.parseSymbolicRegressionFusedStats(args);
            }else{
                System.err.println("Error: unknown argument");
                m.printUsage();
            }
        }
    }
}



