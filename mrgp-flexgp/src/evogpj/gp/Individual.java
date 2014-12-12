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
package evogpj.gp;

import evogpj.evaluation.FitnessComparisonStandardizer;
import evogpj.evaluation.FitnessFunction;
import evogpj.genotype.Genotype;
import evogpj.genotype.Tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;


/**
 * This class represents the individuals in a population. Individuals have a
 * {@link Genotype}, a {@link Phenotype} and a fitness. Genotypes are what
 * genetic operations use to create new offspring. Phenotypes are like a preview
 * of how this individual looks, in the problem space (ie the output of an
 * individual in a SR problem for each of the test inputs). Fitness is the
 * standard number representing how fit this individual is. Higher fitness is
 * better.
 * 
 * @author Owen Derby and Ignacio Arnaldo
 * @see Genotype
 * @see Phenotype
 */
public class Individual implements Serializable {
    private static final long serialVersionUID = -1448696047207647168L;
    private final Genotype genotype;
    // store fitness values as key:value pairs to support multiple objectives
    private LinkedHashMap<String, Double> fitnesses;
    // distance from the origin
    private Double euclideanDistance;
    // the "hypervolume" of a particular individual
    private Double crowdingDistance;
    private Integer dominationCount;
    private double threshold;
    private double crossValAreaROC;	
    private double crossValFitness;
    private double scaledMAE;
    private double scaledMSE;
    private double RT_Cost;
    private double minTrainOutput, maxTrainOutput;
    private ArrayList<Double> estimatedDensPos, estimatedDensNeg;
    ArrayList<String> weights;
    String lassoIntercept;
    /**
     * Create an individual with the given genotype. The new individuals
     * phenotype and fitness are left unspecified.
     * 
     * @param genotype an Instance of a {@link Genotype}
     */
    public Individual(Genotype genotype) {
        this.genotype = genotype;
        this.fitnesses = new LinkedHashMap<String, Double>();
        this.euclideanDistance = Double.MAX_VALUE;
        this.crowdingDistance = 0.0;
        this.dominationCount = 0;
        threshold = 0;
        crossValAreaROC = 0;    
        RT_Cost = 0;
    }

    @SuppressWarnings("unchecked")
    private Individual(Individual i) {
        this.crowdingDistance = i.crowdingDistance;
        this.dominationCount = i.dominationCount;
        this.genotype = i.genotype.copy();
        this.fitnesses = (LinkedHashMap<String, Double>) i.fitnesses.clone();
        this.euclideanDistance = i.euclideanDistance;
        this.threshold = i.threshold;
        this .crossValAreaROC = i.crossValAreaROC;     
        RT_Cost = 0;
        this.weights = i.weights;
    }

    /**
     * deep copy of an individual. THe new individual will be identical to the
     * old in every way, but will be completely independent. So any changes made
     * to the old do not affect the new, and vice versa.
     * 
     * @return new individual
     */
    public Individual copy() {
        return new Individual(this);
    }
    
    /**
     * Return Genotype
     * @return 
     */
    public Genotype getGenotype() {
        return this.genotype;
    }

    /**
     * return fitness names
     * @return 
     */
    public Set<String> getFitnessNames() {
        return this.fitnesses.keySet();
    }
	
    /**
     * Return the LinkedHashMap storing fitness values
     * @return
     */
    public LinkedHashMap<String, Double> getFitnesses() {
        return this.fitnesses;
    }

    /**
     * return fitness function name
     * @param key
     * @return 
     */
    public Double getFitness(String key) {
        return this.fitnesses.get(key);
    }

    /**
     * Overload getFitness to support returning simply the first fitness value if prompted
     * @return
     */
    public Double getFitness() {
        String first = getFirstFitnessKey();
        return getFitness(first);
    }

    /**
     * @return the first fitness key from this individual's stored fitness values
     */
    public String getFirstFitnessKey() {
        Set<String> keys = getFitnesses().keySet();
        return keys.iterator().next();
    }

    /**
     * Set fitness functions
     * @param d 
     */
    public void setFitnesses(LinkedHashMap<String, Double> d) {
        this.fitnesses = d;
    }

    /**
     * Update a particular fitness value
     * @param key
     * @param d the new fitness 
     */
    public void setFitness(String key, Double d) {
        fitnesses.put(key, d);
    }

    /**
     * Obtain the memorized euclidean distance of fitness
     * @return
     */
    public Double getEuclideanDistance() {
        return euclideanDistance;
    }

    /**
     * Calculate the euclidean distance of the fitnesses of this individual from
     * the origin.
     * @param fitnessFunctions
     * @param standardizedMins
     * @param standardizedRanges
     */
    public void calculateEuclideanDistance(LinkedHashMap<String, FitnessFunction> fitnessFunctions,LinkedHashMap<String, Double> standardizedMins,
                    LinkedHashMap<String, Double> standardizedRanges) {
        // reset euclidean distance to 0
        euclideanDistance = 0.0;
        for (String fitnessKey : fitnesses.keySet()) {
            // get fitness converted to minimization if necessary
            Double standardizedFitness = FitnessComparisonStandardizer.getFitnessForMinimization(this, fitnessKey,fitnessFunctions);
            // normalize 
            Double normalizedStandardizedFitness = (standardizedFitness - standardizedMins.get(fitnessKey)) / standardizedRanges.get(fitnessKey);
            // add to euclidean distance
            euclideanDistance += Math.pow(normalizedStandardizedFitness,2);
        }
    }
	
    /**
     * return crowding distance
     * @return 
     */
    public Double getCrowdingDistance() {
        return this.crowdingDistance;
    }

    /**
     * Set crowding distance
     * @param newCrowdingDistance 
     */
    public void setCrowdingDistance(Double newCrowdingDistance) {
        this.crowdingDistance = newCrowdingDistance;
    }

    /**
     * Update the crowding distance of this individual with information from a new fitness function
     * @param localCrowdingDistance
     */
    public void updateCrowdingDistance(Double localCrowdingDistance) {
        crowdingDistance *= localCrowdingDistance;
    }

    /**
     * return domination count
     * @return 
     */
    public Integer getDominationCount() {
        return dominationCount;
    }

    /**
     * increment domination count
     */
    public void incrementDominationCount() {
        dominationCount++;
    }

    /**
     * set domination count
     * @param dominationCount 
     */
    public void setDominationCount(Integer dominationCount) {
        this.dominationCount = dominationCount;
    }

    /**
     * get threshold
     * @return 
     */
    public double getThreshold(){
        return threshold;
    }
    
    /**
     * set threshold
     * @param aThreshold 
     */
    public void setThreshold(double aThreshold){
        threshold = aThreshold;
    }	

    /**
     * get area under the ROC curve on validation data
     * @return 
     */
    public double getCrossValAreaROC(){
        return crossValAreaROC;
    }

    /**
     * set auc on validation data
     * @param anArea 
     */
    public void setCrossValAreaROC(double anArea){
        crossValAreaROC = anArea;
    }

    /**
     * Clear any nonessential memorized values
     */
    public void reset() {
        euclideanDistance = Double.MAX_VALUE;
        crowdingDistance = 0.0;
        dominationCount = 0;
        RT_Cost = 0;
    }
	
    /**
     * return whether 2 individuals are equal
     * @param i
     * @return 
     */
    public Boolean equals(Individual i){
        if (!this.getGenotype().equals(i.getGenotype())){
            return false;
        } else{ // g and p are equal, check fitnesses
            return (this.getFitnesses().equals(i.getFitnesses()));
        }
    }

    /**
     * @return unscaled MATLAB infix string
     */
    @Override
    public String toString() {
        return this.genotype.toString();
    }

    /**
     * @param shouldRound
     * @return scaled MATLAB infix string, with appropriate rounding if necessary
     */
    public String toScaledString(boolean shouldRound) {
        String scaledModel = ((Tree) this.genotype).toScaledString();
        return (shouldRound) ? "round(" + scaledModel + ")" : scaledModel ;
    }
    
    /**
     * get final string representation
     * @param shouldRound
     * @return 
     */
    public String toFinalScaledString(boolean shouldRound) {
        String scaledModel = ((Tree) this.genotype).toFinalScaledString();
        return (shouldRound) ? "round(" + scaledModel + ")" : scaledModel ;
    }

    /**
     * get scaled string representation
     * @return 
     */
    public String toScaledString() {
        return toScaledString(false);
    }
    
    /**
     * get final scaled representation
     * @return 
     */
    public String toFinalScaledString() {
        return toFinalScaledString(false);
    }

    /**
     * set fitness on validatio data
     * @param aFitness 
     */
    public void setScaledCrossValFitness(double aFitness){
        this.crossValFitness = aFitness;
    }

    /**
     * get fitness on validation data
     * @return 
     */
    public double getCrossValFitness(){
        return crossValFitness;
    }

    /**
     * set MSE of the scaled model
     * @param aMSE 
     */
    public void setScaledMSE(double aMSE){
        scaledMSE = aMSE;
    }

    /**
     * get MSE of the scaled model
     * @return 
     */
    public double getScaledMSE(){
        return scaledMSE;
    }
    
    /**
     * set MAE of scaled model
     * @param aMAE 
     */
    public void setScaledMAE(double aMAE){
        scaledMAE = aMAE;
    }

    /**
     * get MAE of the scaled value
     * @return 
     */
    public double getScaledMAE(){
        return scaledMAE;
    }


    /**
     * @return the minTrainOutput
     */
    public double getMinTrainOutput() {
        return minTrainOutput;
    }

    /**
     * @param minTrainOutput the minTrainOutput to set
     */
    public void setMinTrainOutput(double minTrainOutput) {
        this.minTrainOutput = minTrainOutput;
    }

    /**
     * @return the maxTrainOutput
     */
    public double getMaxTrainOutput() {
        return maxTrainOutput;
    }

    /**
     * @param maxTrainOutput the maxTrainOutput to set
     */
    public void setMaxTrainOutput(double maxTrainOutput) {
        this.maxTrainOutput = maxTrainOutput;
    }
    

    /**
     * Set LASSO weights
     * @param aWeights 
     */
    public void setWeights(ArrayList<String> aWeights){
        weights = aWeights;
    }

    /**
     * get LASSO weights
     * @return 
     */
    public ArrayList<String> getWeights(){
        return weights;
    }
    
    /**
     * get intercept
     * @return 
     */
    public String getLassoIntercept(){
        return lassoIntercept;
    }
    
    /**
     * set intercept
     * @param aLassoIntercept 
     */
    public void setLassoIntercept(String aLassoIntercept){
        lassoIntercept = aLassoIntercept;
    }
}