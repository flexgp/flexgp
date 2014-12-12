package evogpj.operator;

import java.util.Properties;
import evogpj.gp.Individual;
import evogpj.gp.MersenneTwisterFast;
import evogpj.gp.Population;

/**
 * Crowd tournament selection
 * @author nacho
 */
public class CrowdedTournamentSelection extends TournamentSelection {

    /**
     * Constructor
     * @param rand
     * @param props 
     */
    public CrowdedTournamentSelection(MersenneTwisterFast rand,Properties props) {
        super(rand, props);
    }

    /**
     * Perform crowded tournament selection
     * 
     * Note: this depends on the NonDominationRank, which is memoized from the
     * last call to NonDominatedSort. It also depends on the crowding distance,
     * which is memoized from the call to CrowdedSort.computeCrowdingDistances
     * @param pop
     * @return 
     */
    @Override
    public Individual select(Population pop) {
        int n = pop.size();
        Individual best, challenger;
        best = pop.get(rand.nextInt(n));
        for (int j = 0; j < TOURNEY_SIZE - 1; j++) {
            challenger = pop.get(rand.nextInt(n));
            // challenger wins if it dominates best
            if (challenger.getDominationCount() < best.getDominationCount()) {
                best = challenger;
                // or if neither dominates the other (same nondom rank) and
                // challenger has higher crowding distance
            } else if ((challenger.getDominationCount().equals(best.getDominationCount()))&& (challenger.getCrowdingDistance() > best.getCrowdingDistance())) {
                best = challenger;
            }
        }
        return best;
    }
}
