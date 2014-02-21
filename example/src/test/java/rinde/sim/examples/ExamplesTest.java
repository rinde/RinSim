package rinde.sim.examples;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.examples.core.SimpleExample;
import rinde.sim.examples.core.comm.AgentCommunicationExample;
import rinde.sim.examples.core.taxi.TaxiExample;
import rinde.sim.examples.pdptw.gradientfield.GradientFieldExample;
import rinde.sim.ui.UITestTools;

public class ExamplesTest {

  @Before
  public void setUp() {
    UITestTools.startAndClose(10000);
  }

  @Test
  public void taxiExample() {
    TaxiExample.main(new String[] {});
  }

  @Test
  public void simpleExample() {
    SimpleExample.main(new String[] {});
  }

  @Test
  public void communicationExample() throws Exception {
    AgentCommunicationExample.main(new String[] {});
  }

  @Test
  public void gradientFieldExample() {
    try {
      GradientFieldExample.main(new String[] {});
    } catch (final RuntimeException e) {
      assertTrue(e.getCause() instanceof IllegalStateException);
      assertTrue(e.getCause().getMessage().contains(
          "The simulation did not result in a valid result"));
    }
  }
}
