/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.scenario.fabrirecht;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

/**
 * @author Rinde van Lon
 *
 */
public class ScenarioTest {
  @Test
  public void test() throws IOException {
    final FabriRechtScenario scen = FabriRechtParser
        .parse("files/test/fabri-recht/lc101_coord.csv",
          "files/test/fabri-recht/lc101.csv");

    final String json = FabriRechtParser.toJson(scen);
    FabriRechtParser.toJson(scen, new BufferedWriter(new FileWriter(
        "files/test/fabri-recht/lc101.scenario")));

    final FabriRechtScenario scen2 = FabriRechtParser.fromJson(json);
    assertEquals(scen, scen2);
    final String json2 = FabriRechtParser.toJson(scen2);
    assertEquals(json, json2);
    final FabriRechtScenario scen3 = FabriRechtParser.fromJson(json2);
    assertEquals(scen2, scen3);
  }

}
