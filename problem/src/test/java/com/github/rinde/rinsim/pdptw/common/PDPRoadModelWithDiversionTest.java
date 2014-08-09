package com.github.rinde.rinsim.pdptw.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PDPRoadModelWithDiversionTest extends PDPRoadModelCommonTest {

  public PDPRoadModelWithDiversionTest() {
    super(true);
  }

  /**
   * Check whether mixing diversion allowed/disallowed methods produce the
   * expected results.
   */
  @Test
  public void test() {
    // first move by using the location
    rm.moveTo(dv1, dp1.getPickupLocation(), time(10));
    assertEquals(rm.getPosition(dv1), dp1.getPickupLocation());
    pm.pickup(dv1, dp1, time(1));
    assertTrue(pm.containerContains(dv1, dp1));

    // now move by using the reference to the parcel
    rm.moveTo(dv1, dp1, time(300));
    assertEquals(rm.getPosition(dv1), dp1.getDestination());
  }
}
