package rinde.sim.pdptw.generator;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Ignore;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.ScenarioIO;
import rinde.sim.pdptw.generator.ScenarioGenerator.GeneratedScenario;
import rinde.sim.pdptw.generator.times.PoissonProcess;
import rinde.sim.pdptw.generator.tw.ProportionateUniformTWGenerator;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class GeneratedScenarioIOTest {

  @Ignore
  @Test
  public void testIO() throws IOException {

    final ScenarioGenerator generator = ScenarioGenerator
        .builder()
        .timeUnit(SI.MILLI(SI.SECOND))
        .distanceUnit(SI.KILOMETER)
        .speedUnit(NonSI.KILOMETERS_PER_HOUR)
        .tickSize(1000L)
        .scenarioLength(4 * 60 * 60 * 1000L)

        .arrivalTimes(PoissonProcess.homogenous(4 * 60 * 60 * 1000L, 10))
        .locations(Locations.builder().square(5).uniform())
        .timeWindows(
            new ProportionateUniformTWGenerator(new Point(0, 0),
                4 * 60 * 60 * 1000L, 0L, 0L, 50d))
        // .deliveryDurations(constant(10L))

        .addModel(Models.roadModel(50d, true))
        .addModel(Models.pdpModel(new TardyAllowedPolicy()))

        .build();

    final DynamicPDPTWScenario scenario = generator
        .generate(new MersenneTwister(123));

    final String output = ScenarioIO.write(scenario);

    Files.write(output, new File("files/scen.json"), Charsets.UTF_8);
    final GeneratedScenario converted = ScenarioIO.read(output,
        GeneratedScenario.class);
    assertEquals(scenario, converted);
    System.out.println(scenario.createModels());
    System.out.println(converted.createModels());
  }
}
