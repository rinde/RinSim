package com.github.rinde.rinsim.scenario.generator;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.TimeSeries;
import com.github.rinde.rinsim.scenario.generator.Parcels.ParcelGenerator;
import com.github.rinde.rinsim.scenario.generator.TimeWindowsTest.FakeTravelTimes;

/**
 * Test for {@link Parcels}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ParcelsTest {

  /**
   * Tests whether all generated times are in the interval [0,length).
   */
  @Test
  public void timesTest() {
    final int scenarioLength = 10;
    final ParcelGenerator pg = Parcels.builder()
        .announceTimes(TimeSeries.homogenousPoisson(scenarioLength, 100))
        .build();

    final List<AddParcelEvent> events = pg.generate(123,
        FakeTravelTimes.DISTANCE, scenarioLength);

    for (final AddParcelEvent ape : events) {
      assertTrue(ape.time < scenarioLength);
    }
  }

  /**
   * Tests whether times which are outside the interval [0,length) are correctly
   * rejected.
   */
  @Test(expected = IllegalArgumentException.class)
  public void timesFail() {
    final int scenarioLength = 10;
    final ParcelGenerator pg2 = Parcels
        .builder()
        .announceTimes(
            TimeSeries.homogenousPoisson(scenarioLength + 0.1, 100))
        .build();
    pg2.generate(123, FakeTravelTimes.DISTANCE, scenarioLength);
  }

}
