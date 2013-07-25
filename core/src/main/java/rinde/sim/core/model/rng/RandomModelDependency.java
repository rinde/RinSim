/**
 * 
 */
package rinde.sim.core.model.rng;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface RandomModelDependency {

    void injectRandomModel(RandomModel rm);

}
