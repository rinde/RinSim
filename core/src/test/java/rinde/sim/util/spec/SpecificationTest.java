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

import rinde.sim.util.TestUtil;
import rinde.sim.util.spec.Specification.ISpecification;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class SpecificationTest {

  @Test
  public void test() {
    TestUtil.testPrivateConstructor(Specification.class);

    assertTrue(ALWAYS_TRUE.isSatisfiedBy(true));
    assertTrue(ALWAYS_TRUE.isSatisfiedBy(false));
    assertFalse(ALWAYS_FALSE.isSatisfiedBy(true));
    assertFalse(ALWAYS_FALSE.isSatisfiedBy(false));
    assertTrue(PLAIN.isSatisfiedBy(true));
    assertFalse(PLAIN.isSatisfiedBy(false));

    assertFalse(Specification.of(ALWAYS_TRUE).and(ALWAYS_FALSE).build()
        .isSatisfiedBy(true));
    assertFalse(Specification.of(ALWAYS_TRUE).and(ALWAYS_FALSE)
        .and(ALWAYS_TRUE).build().isSatisfiedBy(true));

    assertTrue(Specification.of(ALWAYS_TRUE).andNot(ALWAYS_FALSE)
        .and(ALWAYS_TRUE).build().isSatisfiedBy(true));

    assertTrue(Specification.of(ALWAYS_TRUE).and(ALWAYS_FALSE).orNot(PLAIN)
        .and(ALWAYS_TRUE).build().isSatisfiedBy(false));
    assertFalse(Specification.of(ALWAYS_TRUE).and(ALWAYS_FALSE).orNot(PLAIN)
        .and(ALWAYS_TRUE).build().isSatisfiedBy(true));

    // !(false || true) && true
    assertFalse(Specification.of(ALWAYS_FALSE).or(PLAIN).not().and(ALWAYS_TRUE)
        .build().isSatisfiedBy(true));

    // !(false || false) && true
    assertTrue(Specification.of(ALWAYS_FALSE).or(PLAIN).not().and(ALWAYS_TRUE)
        .build().isSatisfiedBy(false));

    // !(true && false) && true
    assertTrue(Specification.of(ALWAYS_TRUE).and(PLAIN).not().and(ALWAYS_TRUE)
        .build().isSatisfiedBy(false));

    assertTrue(Specification.of(ALWAYS_TRUE).or(ALWAYS_FALSE).build()
        .isSatisfiedBy(true));

    assertTrue(Specification.of(ALWAYS_TRUE).xor(ALWAYS_FALSE).build()
        .isSatisfiedBy(true));
  }

  public static abstract class Spec implements ISpecification<Boolean> {

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

  }

}
