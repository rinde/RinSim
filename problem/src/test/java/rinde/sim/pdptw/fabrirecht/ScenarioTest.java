/**
 * 
 */
package rinde.sim.pdptw.fabrirecht;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
    assertEquals(scen.getPossibleEventTypes(), scen2.getPossibleEventTypes());
    final String json2 = FabriRechtParser.toJson(scen2);
    assertEquals(json, json2);
    final FabriRechtScenario scen3 = FabriRechtParser.fromJson(json2);
    assertEquals(scen2, scen3);
    assertEquals(scen2.getPossibleEventTypes(), scen3.getPossibleEventTypes());
  }

}
