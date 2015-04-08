/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.scenario.gendreau06;

import static com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser.parse;
import static com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser.parser;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.scenario.TimedEvent;

/**
 * @author Rinde van Lon
 *
 */
public class Gendreau06ParserTest {

  private static final String FILE_NAME = "req_rapide_1_240_24";
  private static final String FILE_DIR = "files/test/gendreau06/";
  private static final String FILE_PATH = FILE_DIR + FILE_NAME;

  /**
   * Test for default behavior for all (convenience) methods.
   * @throws FileNotFoundException When file is not available.
   */
  @Test
  public void parserDefaultsTest() throws FileNotFoundException {
    final List<Gendreau06Scenario> scenarios = newArrayList();
    scenarios.add(parse(new File(FILE_PATH)));
    scenarios.addAll(parser().addFile(new File(FILE_PATH)).parse());
    scenarios.addAll(parser().addFile(FILE_PATH).parse());
    scenarios.addAll(parser().addFile(new FileInputStream(FILE_PATH),
      FILE_NAME).parse());
    scenarios.addAll(parser().addDirectory(new File(FILE_DIR)).parse());
    scenarios.addAll(parser().addDirectory(FILE_DIR).parse());

    assertEquals(6, scenarios.size());
    for (final Gendreau06Scenario scen : scenarios) {
      containsVehicles(scen, 10);
      containsTimeOut(scen, 240);
      assertTrue(isOnline(scen));
      assertFalse(isDiversionAllowed(scen));
      assertEquals(1000L, scen.getTickSize());
    }
  }

  /**
   * Tests whether overriding the number of vehicles works.
   */
  @Test
  public void customNumVehiclesTest() {
    final Gendreau06Scenario scen = Gendreau06Parser.parser()
      .addFile(FILE_PATH)
      .setNumVehicles(5)
      .parse()
      .get(0);
    containsVehicles(scen, 5);
    containsTimeOut(scen, 240);
    assertTrue(isOnline(scen));
    assertFalse(isDiversionAllowed(scen));
    assertEquals(1000L, scen.getTickSize());
  }

  /**
   * Tests whether the offline option actually produces an offline scenario.
   */
  @Test
  public void offlineTest() {
    final Gendreau06Scenario scen = Gendreau06Parser.parser()
      .addFile("files/test/gendreau06/req_rapide_1_240_24")
      .offline()
      .parse()
      .get(0);
    containsVehicles(scen, 10);
    containsTimeOut(scen, 240);
    assertFalse(isOnline(scen));
    assertFalse(isDiversionAllowed(scen));
    assertEquals(1000L, scen.getTickSize());
  }

  /**
   * Tests whether the allowDiversion option actually produces a scenario that
   * allows diversion.
   */
  @Test
  public void allowDiversionTest() {
    final Gendreau06Scenario scen = parser().allowDiversion()
      .addFile(FILE_PATH).parse().get(0);
    containsVehicles(scen, 10);
    containsTimeOut(scen, 240);
    assertTrue(isOnline(scen));
    assertTrue(isDiversionAllowed(scen));
    assertEquals(1000L, scen.getTickSize());
  }

  /**
   * Tests whether the setTickSize option actually produces a scenario with a
   * different tick size.
   */
  @Test
  public void setTickSizeTest() {
    final Gendreau06Scenario scen = parser().setTickSize(101)
      .addFile(FILE_PATH).parse().get(0);
    containsVehicles(scen, 10);
    containsTimeOut(scen, 240);
    assertTrue(isOnline(scen));
    assertFalse(isDiversionAllowed(scen));
    assertEquals(101L, scen.getTickSize());
  }

  /**
   * Tests whether the filter works.
   */
  @Test
  public void filterTest() {
    assertTrue(parser().filter(GendreauProblemClass.LONG_LOW_FREQ)
      .addFile(FILE_PATH).parse().isEmpty());

    assertEquals(1, parser().filter(GendreauProblemClass.SHORT_LOW_FREQ)
      .addFile(FILE_PATH).parse().size());

    assertEquals(1, parser().filter(GendreauProblemClass.LONG_LOW_FREQ,
      GendreauProblemClass.SHORT_LOW_FREQ).addFile(FILE_PATH).parse().size());

    assertEquals(1, parser().filter(GendreauProblemClass.values())
      .addFile(FILE_PATH).parse().size());
  }

  /**
   * Tests for non existing file.
   */
  @Test(expected = IllegalArgumentException.class)
  public void parseWrongFileName() {
    Gendreau06Parser.parse(new File(
      "pointer/to/non-existing/file/req_rapide_1_240_24"));
  }

  /**
   * Invalid file name (intensity).
   */
  @Test(expected = IllegalArgumentException.class)
  public void invalidFileNameTest1() {
    parse(new File("req_rapide_1_240_25"));
  }

  /**
   * Invalid file name (length).
   */
  @Test(expected = IllegalArgumentException.class)
  public void invalidFileNameTest2() {
    parser().addFile("req_rapide_1_241_24");
  }

  /**
   * Invalid file name (no instance number).
   */
  @Test(expected = IllegalArgumentException.class)
  public void invalidFileNameTest3() {
    parser().addFile("req_rapide__241_24");
  }

  /**
   * Tests several file names.
   */
  @Test
  public void validFileNameTest() {
    Gendreau06Parser.checkValidFileName("req_rapide_01_240_24");
    Gendreau06Parser.checkValidFileName("req_rapide_00001_240_24");
    Gendreau06Parser.checkValidFileName("req_rapide_6_240_24");
    Gendreau06Parser.checkValidFileName("req_rapide_6_240_33");
    Gendreau06Parser.checkValidFileName("req_rapide_6_450_24");
  }

  /**
   * Test whether invalid tickSizes are prevented correctly.
   */
  @Test(expected = IllegalArgumentException.class)
  public void invalidTickSize() {
    parser().setTickSize(-1);
  }

  /**
   * Tests whether an illegal number of vehicles is prevented.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testIllegalNumberOfVehicles() {
    Gendreau06Parser.parser().setNumVehicles(0);
  }

  static void containsVehicles(Gendreau06Scenario scen, int num) {
    int vehicles = 0;
    for (final TimedEvent e : scen.asList()) {
      if (e.getEventType() == PDPScenarioEvent.ADD_VEHICLE) {
        vehicles++;
      }
    }
    assertEquals(num, vehicles);
  }

  static void containsTimeOut(Gendreau06Scenario scen, int minutes) {
    final TimedEvent e = scen.asList().get(scen.size() - 1);
    assertEquals(PDPScenarioEvent.TIME_OUT, e.getEventType());
    assertEquals(minutes * 60 * 1000, e.time);
  }

  static boolean isOnline(Gendreau06Scenario scen) {
    for (final TimedEvent e : scen.asList()) {
      if (e.getEventType() == PDPScenarioEvent.ADD_PARCEL && e.time == -1) {
        return false;
      }
    }
    return true;
  }

  static boolean isDiversionAllowed(Gendreau06Scenario scen) {
    return ((PDPRoadModel) scen.getModelBuilders().get(0).build(null))
      .isVehicleDiversionAllowed();
  }
}
