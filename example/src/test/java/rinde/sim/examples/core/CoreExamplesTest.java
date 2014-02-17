package rinde.sim.examples.core;

import org.junit.Test;

import rinde.sim.examples.core.comm.AgentCommunicationExample;
import rinde.sim.examples.core.taxi.TaxiExample;
import rinde.sim.ui.UITestTools;

public class CoreExamplesTest {

  @Test
  public void taxiExample() {
    System.out.println("taxi example");
    UITestTools.startAndClose(5000);
    TaxiExample.main(new String[] {});
  }

  @Test
  public void simpleExample() {
    System.out.println("simple example");
    UITestTools.startAndClose(5000);
    SimpleExample.main(null);
  }

  @Test
  public void communicationExample() throws Exception {
    System.out.println("communication example");
    UITestTools.startAndClose(5000);
    AgentCommunicationExample.main(new String[] {});
  }

}
