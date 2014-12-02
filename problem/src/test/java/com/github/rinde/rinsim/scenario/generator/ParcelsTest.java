/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.scenario.generator;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.Parcels.ParcelGenerator;

/**
 * Test for {@link Parcels}.
 * @author Rinde van Lon
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
        TravelTimesUtil.distance(), scenarioLength);

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
    pg2.generate(123, TravelTimesUtil.distance(), scenarioLength);
  }

}
