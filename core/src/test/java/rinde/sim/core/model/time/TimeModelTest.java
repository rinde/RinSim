/**
 * 
 */
package rinde.sim.core.model.time;

import org.junit.Test;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TimeModelTest {

    @Test
    public void test() {

        final TimeModel tm = new TimeModel(123);

        System.out.println(tm.getModelLinks());

    }

}
