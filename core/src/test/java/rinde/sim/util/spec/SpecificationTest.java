/**
 * 
 */
package rinde.sim.util.spec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static rinde.sim.util.spec.SpecificationTest.Spec.ALWAYS_FALSE;
import static rinde.sim.util.spec.SpecificationTest.Spec.ALWAYS_TRUE;
import static rinde.sim.util.spec.SpecificationTest.Spec.PLAIN;

import org.junit.Test;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class SpecificationTest {

    @Test
    public void test() {

        assertTrue(ALWAYS_TRUE.isSatisfiedBy(true));
        assertTrue(ALWAYS_TRUE.isSatisfiedBy(false));
        assertFalse(ALWAYS_FALSE.isSatisfiedBy(true));
        assertFalse(ALWAYS_FALSE.isSatisfiedBy(false));
        assertTrue(PLAIN.isSatisfiedBy(true));
        assertFalse(PLAIN.isSatisfiedBy(false));

        assertFalse(ALWAYS_TRUE.and(ALWAYS_FALSE).isSatisfiedBy(true));
        assertFalse(ALWAYS_TRUE.and(ALWAYS_FALSE).and(ALWAYS_TRUE)
                .isSatisfiedBy(true));

        assertTrue(ALWAYS_TRUE.and(ALWAYS_FALSE.not()).and(ALWAYS_TRUE)
                .isSatisfiedBy(true));

        assertTrue(ALWAYS_TRUE.and(ALWAYS_FALSE.or(PLAIN.not()))
                .and(ALWAYS_TRUE).isSatisfiedBy(false));
        assertFalse(ALWAYS_TRUE.and(ALWAYS_FALSE.or(PLAIN.not()))
                .and(ALWAYS_TRUE).isSatisfiedBy(true));

        // !(false || true) && true
        assertFalse(ALWAYS_FALSE.or(PLAIN).not().and(ALWAYS_TRUE)
                .isSatisfiedBy(true));
        // !(false || false) && true
        assertTrue(ALWAYS_FALSE.or(PLAIN).not().and(ALWAYS_TRUE)
                .isSatisfiedBy(false));

        // !(true && false) && true
        assertTrue(ALWAYS_TRUE.and(PLAIN).not().and(ALWAYS_TRUE)
                .isSatisfiedBy(false));

        assertTrue(ALWAYS_TRUE.or(ALWAYS_FALSE).isSatisfiedBy(true));

    }

    public static abstract class Spec extends
            CompositeSpecification<Boolean, Spec> {

        public static Spec ALWAYS_TRUE = new Spec() {
            @Override
            public boolean isSatisfiedBy(Boolean context) {
                return true;
            }
        };
        public static Spec ALWAYS_FALSE = new Spec() {
            @Override
            public boolean isSatisfiedBy(Boolean context) {
                return false;
            }
        };
        public static Spec PLAIN = new Spec() {
            @Override
            public boolean isSatisfiedBy(Boolean context) {
                return context;
            }
        };

        @Override
        protected Spec wrap(final ISpecification<Boolean, Spec> spec) {
            return new Spec() {
                @Override
                public boolean isSatisfiedBy(Boolean context) {
                    return spec.isSatisfiedBy(context);
                }
            };
        }
    }

}
