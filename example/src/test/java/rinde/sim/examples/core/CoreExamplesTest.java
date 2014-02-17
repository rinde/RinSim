package rinde.sim.examples.core;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.examples.core.comm.AgentCommunicationExample;
import rinde.sim.examples.core.taxi.TaxiExample;
import rinde.sim.ui.UITestTools;

public class CoreExamplesTest {

  @Before
  public void setUp() {
    UITestTools.selectPlayPauseMenuItem();
    UITestTools.closeActiveShell(5000);
  }

  @Test
  public void taxiExample() {
    TaxiExample.main(new String[] {});
  }

  @Test
  public void simpleExample() {
    SimpleExample.main(null);
  }

  @Test
  public void communicationExample() throws Exception {
    AgentCommunicationExample.main(new String[] {});
  }

}
