/**
 * 
 */
package rinde.sim.util;

import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TestUtil {
    public static <T> void testPrivateConstructor(Class<T> clazz) {
        try {
            final Constructor<T> c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            c.newInstance();
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }
}
