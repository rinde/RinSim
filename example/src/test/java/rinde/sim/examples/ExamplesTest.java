package rinde.sim.examples;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rinde.sim.examples.core.SimpleExample;
import rinde.sim.examples.core.comm.AgentCommunicationExample;
import rinde.sim.examples.core.taxi.TaxiExample;
import rinde.sim.examples.pdptw.gradientfield.GradientFieldExample;

public class ExamplesTest {

  @Test
  public void taxiExample() {
    TaxiExample.run(true);
  }

  @Test
  public void simpleExample() {
    SimpleExample.run(true);
  }

  @Test
  public void communicationExample() throws Exception {
    AgentCommunicationExample.run(true);
  }

  /**
   * Run the gradient field example class.
   */
  @Test
  public void gradientFieldExample() {
    try {
      GradientFieldExample.run(true);
    } catch (final RuntimeException e) {
      // find the root cause of the exception
      Throwable cur = e;
      while (cur.getCause() != null) {
        cur = cur.getCause();
      }
      // verify that the exception was caused by the early termination of the
      // simulation
      assertTrue(cur instanceof IllegalStateException);
      assertTrue(cur.getMessage().contains(
          "The simulation did not result in a valid result"));
    }
  }
}
