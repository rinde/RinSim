/**
 * 
 */
package rinde.sim.core.model.rng;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface RandomReceiver {

    void receiveRandom(RandomGenerator rng);

}
