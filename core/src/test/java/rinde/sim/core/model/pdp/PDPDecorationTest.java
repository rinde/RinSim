package rinde.sim.core.model.pdp;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import rinde.sim.core.model.road.RoadModel;

public class PDPDecorationTest {

  @Test
  public void decoration() {
    final PDPModel defaultPDPModel = new DefaultPDPModel();
    assertSame(defaultPDPModel, defaultPDPModel.self);

    final PDPModel forwardA = new ForwardingA(defaultPDPModel);
    assertSame(forwardA, defaultPDPModel.self);

    final PDPModel forwardB = new ForwardingB(forwardA);
    assertSame(forwardB, defaultPDPModel.self);

    final PDPModel forwardC = new ForwardingC(forwardB);
    assertSame(forwardC, forwardA.self);
    assertSame(forwardC, forwardB.self);
    assertSame(forwardC, forwardC.self);
    assertSame(forwardC, defaultPDPModel.self);
  }

  /**
   * Tests whether decorating an already initialized PDPModel fails correctly.
   */
  @SuppressWarnings("unused")
  @Test
  public void decorationFail() {
    final PDPModel defaultPDPModel = new DefaultPDPModel();
    assertSame(defaultPDPModel, defaultPDPModel.self);

    final PDPModel forwardA = new ForwardingA(defaultPDPModel);
    assertSame(forwardA, defaultPDPModel.self);

    forwardA.register(new Depot() {
      @Override
      public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
    });

    boolean fail = false;
    try {
      new ForwardingA(forwardA);
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);

    boolean fail2 = false;
    try {
      new ForwardingA(defaultPDPModel);
    } catch (final IllegalStateException e) {
      fail2 = true;
    }
    assertTrue(fail2);
  }

  static class ForwardingA extends ForwardingPDPModel {
    ForwardingA(PDPModel deleg) {
      super(deleg);
    }
  }

  static class ForwardingB extends ForwardingPDPModel {
    ForwardingB(PDPModel deleg) {
      super(deleg);
    }
  }

  static class ForwardingC extends ForwardingPDPModel {
    ForwardingC(PDPModel deleg) {
      super(deleg);
    }
  }

}
