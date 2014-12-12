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
package node;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import evogpj.gp.GPException;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import evogpj.algorithm.SymbRegMOO;
import evogpj.algorithm.Parameters;

import net.minidev.json.JSONObject;
import network.MigrantMessage;
import node.NodeList.EmptyNodeListException;
import node.NodeList.NoSuchNodeIDException;
import utility.ArgParser;
import utility.MsgIDFactory;


/**
 * This class is the bridge between FlexGP library and the actual GP
 * implementation in use (evogpj).
 * 
 * @author Owen Derby
 */
public class EvoGPJ extends SymbRegMOO implements Algorithm {

	private NodeList nodeList;
	private Mailbox mailbox;
	private MsgIDFactory msgIDFactory;
	private NodeDescriptor desc;
	private Evolve evolve;
	private static Logger log;

	// migration parameters
	private Population migrants;
	Boolean migrationEnabled;
	Integer migrationRate;
	Integer migrationSize;
	Integer migrationStart;
	
    /**
     * @param _evolve
     * @param p
     * @param _nodeList
     * @param _mailbox
     * @param _msgIDFactory
     * @param _desc
     * @param _migrants
     * @throws java.io.IOException
     */
    public EvoGPJ(Evolve _evolve, ArgParser p, NodeList _nodeList,Mailbox _mailbox, MsgIDFactory _msgIDFactory, NodeDescriptor _desc,Population _migrants) throws IOException {
        nodeList = _nodeList;
        migrants = _migrants;
        mailbox = _mailbox;
        msgIDFactory = _msgIDFactory;
        desc = _desc;
        evolve = _evolve;

        generation = 0;
        startTime = System.currentTimeMillis();

        // logging
        log = Evolve.getLog();

        // handle migration params
        migrationEnabled = p.migrationEnabled;
        migrationSize = p.migrationSize;
        migrationRate = p.migrationRate;
        migrationStart = p.migrationStart;
        Control.logOneTime(String.format("Migration enabled: %b, size: %d, rate: %d, start: %d",
                        migrationEnabled, migrationSize, migrationRate, migrationStart));

        String lib_args_raw = p.lib_args;
        String[] lib_args = lib_args_raw.split(" ");
        String propsFileName = lib_args[0];
        Properties props = SymbRegMOO.loadProps(propsFileName);
        if (props == null) {
            Control.logOneTime("Can't load properties file from " + propsFileName + "- exiting!", Level.SEVERE);
            System.exit(1);
        }

        // Attempt to load data from specified file
        if (lib_args.length > 1) {
            String probFile = lib_args[1];
            String ext = Control.getExtension(probFile);
            props.put("problem", lib_args[1]);
        } // otherwise, assume data is initiliazed or intentionally left out
        Control.logOneTime("Loaded properties: " + props.toString());
        long seed = System.currentTimeMillis();
        Control.logOneTime("Running evogpj with seed: " + seed);

        // Read parameters
        loadParams(props);
        // Instantiate operators
        create_operators(props, seed);
        // log.info(String.format("Loaded %d training cases.",((SRDataFitness)this.fitness).getFitnessCases().size()));


        java.util.Date date = new java.util.Date();
        Timestamp generationTimestamp = new java.sql.Timestamp(date.getTime());

        // print information about this generation
        


        best = pop.get(0);
        // best is the individual with the best first fitness
        for (int index = 0; index < pop.size(); index++) {
            Individual individual = pop.get(index);
            if(individual.getFitness() > best.getFitness()){
                best = individual;
            }
        }
        //this.saveText(MODELS_PATH, best.toScaledString(COERCE_TO_INT) + "\n", true);
        saveText(MODELS_PATH, minTarget + "," + maxTarget + ",", true);
        for(int j = 0;j<best.getWeights().size()-1;j++){
            this.saveText(MODELS_PATH, best.getWeights().get(j) + " ", true);
        }
        this.saveText(MODELS_PATH, best.getWeights().get(best.getWeights().size()-1) + ",", true);
        this.saveText(MODELS_PATH, best.getLassoIntercept() + ",", true);
        this.saveText(MODELS_PATH, best.toString() + "\n", true);
        

        System.out.format("Best individual for generation %d:%n", generation);
        System.out.flush();
        System.out.println(best.getFitnesses() + ", " + best.toString());
        generation++;
    }
	
    /**
     * should we send emigrants somewhere, according to the migration parameters?
     * @param gen_num
     * @return 
     */
    public Boolean shouldEmigrate(Integer gen_num) {
        return ((gen_num - migrationStart) % migrationRate) == 0;
    }
	
	/**
	 * Choose a random neighbor to send migrants to, and send the best N individuals there.
     * @param bestIndividuals
	 * @throws EmptyNodeListException if there are no neighbors to send to
	 */
	public void emigrate(Population bestIndividuals) throws EmptyNodeListException {
		try {
			// select random neighbor from nodelist
			String randomNeighborID = nodeList.getRandomNeighbor();
			NodeListEntry nle = nodeList.get(randomNeighborID);
			Evolve.println(String.format("EvoGPJ: sending migrants to node " + randomNeighborID));
			// construct message and send
			MigrantMessage message = new MigrantMessage(msgIDFactory.get(),
					desc.getID(), desc.getIP(), desc.getPort(),
					desc.getSubnetID(), bestIndividuals);
			evolve.sendEvolveMsg(message, randomNeighborID, nle);
		} catch (NoSuchNodeIDException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
    /**
     * main loop
     */
    @Override
    public void proceed() throws AlgorithmException {
        // TODO dylan this method should be more integrated with run_population
        // in AlgorithBase
        if (running()) {
            try {
                if (migrationEnabled) {
                    if (migrants.size() > 0) {
                        // accept new migrants which have accumulated
                        Evolve.println(String.format("EvoGPJ: accepting %d migrants",migrants.size()));
                        acceptMigrants(migrants);
                        // reset migrants for the next generation
                        migrants.clear();
                    }
                }

                step();
                
                if (migrationEnabled) {
                    if (shouldEmigrate(generation)){
                        
                        Evolve.println(String.format("EvoGPJ: sending migrants"));
                        Population bestIndividuals = new Population();
                        for (int index = 0; index < migrationSize; index++) bestIndividuals.add(pop.get(index));
                        try {
                            emigrate(bestIndividuals);
                        } catch (EmptyNodeListException e) {
                            // simply continue without emigration
                            Evolve.println(String.format("EvoGPJ: no neighbors available to accept migrants"));
                        }
                    }
                }


                Evolve.println(String.format("EvoGPJ: generation %d",generation));
                long timeStamp = (System.currentTimeMillis() - startTime) / 1000;
                Evolve.println("ELAPSED TIME: " + timeStamp);
                // print information about this generation
                System.out.format("Best individual for generation %d:%n", generation);
                System.out.flush();
                System.out.println(best.getFitnesses() + ", " + best.toString());

                // record the best individual in models.txt
                //this.saveText(MODELS_PATH, best.toScaledString(COERCE_TO_INT) + "\n", true);
                this.saveText(MODELS_PATH, minTarget + "," + maxTarget + ",", true);
                for(int j = 0;j<best.getWeights().size()-1;j++){
                    this.saveText(MODELS_PATH, best.getWeights().get(j) + " ", true);
                }
                this.saveText(MODELS_PATH, best.getWeights().get(best.getWeights().size()-1) + ",", true);
                this.saveText(MODELS_PATH, best.getLassoIntercept() + ",", true);
                this.saveText(MODELS_PATH, best.toString() + "\n", true);
                
                
                generation++;

            } catch (GPException e) {
                    throw new AlgorithmException(e.getMessage());
            }
        }
    }

    @Override
    public boolean running() {
            return generation < NUM_GENS;
    }

    @Override
    public void cleanup() {
        
    }

    @Override
    public JSONObject report() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
        
}
