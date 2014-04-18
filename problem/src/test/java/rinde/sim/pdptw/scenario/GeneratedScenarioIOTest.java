package rinde.sim.pdptw.scenario;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.pdptw.scenario.PDPScenario.DefaultScenario;
import rinde.sim.pdptw.scenario.times.PoissonProcess;
import rinde.sim.pdptw.scenario.tw.ProportionateUniformTWGenerator;

import com.google.common.base.Charsets;
import com.google.common.base.Predicates;
import com.google.common.io.Files;

public class GeneratedScenarioIOTest {

  @Test
  public void testIO() throws IOException {

    final ScenarioGenerator generator = ScenarioGenerator
        .builder()
        .timeUnit(SI.MILLI(SI.SECOND))
        .distanceUnit(SI.KILOMETER)
        .speedUnit(NonSI.KILOMETERS_PER_HOUR)
        .tickSize(1000L)
        .scenarioLength(4 * 60 * 60 * 1000L)
        .stopCondition(
            Predicates.and(StopCondition.ANY_TARDINESS,
                StopCondition.TIME_OUT_EVENT))

        .arrivalTimes(PoissonProcess.homogenous(4 * 60 * 60 * 1000L, 10))
        .locations(Locations.builder().square(5).uniform())
        .timeWindows(
            new ProportionateUniformTWGenerator(new Point(0, 0),
                4 * 60 * 60 * 1000L, 0L, 0L, 50d))
        // .deliveryDurations(constant(10L))
        .addModel(Models.roadModel(50d, true))
        .addModel(Models.pdpModel(new TardyAllowedPolicy()))
        .build();

    final PDPScenario scenario = generator
        .generate(new MersenneTwister(123));

    final String output = ScenarioIO.write(scenario);

    Files.write(output, new File("files/scen.json"), Charsets.UTF_8);
    final DefaultScenario converted = ScenarioIO.read(output,
        DefaultScenario.class);
    assertEquals(scenario, converted);
    System.out.println(scenario.createModels());
    System.out.println(converted.createModels());
  }
}
