/**
 * 
 */
package rinde.sim.scenario;

import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ScenarioBuilderTest {

	@Test(expected = IllegalArgumentException.class)
	public void constructorFail() {
		new ScenarioBuilder(null);
	}

}
