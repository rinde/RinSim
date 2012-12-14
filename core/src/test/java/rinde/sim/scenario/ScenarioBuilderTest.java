/**
 * 
 */
package rinde.sim.scenario;

import static java.util.Arrays.asList;
import static java.util.Collections.frequency;
import static org.junit.Assert.assertEquals;
import static rinde.sim.scenario.ScenarioBuilderTest.TestEvents.EVENT_A;
import static rinde.sim.scenario.ScenarioBuilderTest.TestEvents.EVENT_B;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.scenario.ScenarioBuilder.EventCreator;
import rinde.sim.scenario.ScenarioBuilder.EventTypeFunction;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ScenarioBuilderTest {

    protected ScenarioBuilder scenarioBuilder;

    enum TestEvents {
        EVENT_A, EVENT_B;
    }

    @Before
    public void setUp() {
        scenarioBuilder = new ScenarioBuilder(EVENT_A, EVENT_B);
    }

    @Test
    public void addMultipleEvents() {
        scenarioBuilder
                .addEventGenerator(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
                        100, 3, new ScenarioBuilder.EventTypeFunction(EVENT_A)));
        scenarioBuilder.addMultipleEvents(0, 2, EVENT_A);
        final Scenario s = scenarioBuilder.build();

        assertEquals(2, frequency(s.asList(), new TimedEvent(EVENT_A, 0)));
        assertEquals(asList(new TimedEvent(EVENT_A, 0), new TimedEvent(EVENT_A,
                0), new TimedEvent(EVENT_A, 100), new TimedEvent(EVENT_A, 100), new TimedEvent(
                EVENT_A, 100)), s.asList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addMultipleEventsFail1() {
        scenarioBuilder.addMultipleEvents(-1, 1, (EventCreator<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addMultipleEventsFail2() {
        scenarioBuilder.addMultipleEvents(0, 0, (EventCreator<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addMultipleEventsFail3() {
        scenarioBuilder.addMultipleEvents(0, 1, (EventCreator<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNull() {
        scenarioBuilder.addEventGenerator(null);
    }

    @Test
    public void addTimeSeriesOfEvents() {
        scenarioBuilder.addTimeSeriesOfEvents(0, 3, 1, EVENT_A);
        assertEquals(asList(new TimedEvent(EVENT_A, 0), new TimedEvent(EVENT_A,
                1), new TimedEvent(EVENT_A, 2), new TimedEvent(EVENT_A, 3)), scenarioBuilder
                .build().asList());

        scenarioBuilder.addTimeSeriesOfEvents(5, 10, 3, EVENT_A);
        assertEquals(asList(new TimedEvent(EVENT_A, 0), new TimedEvent(EVENT_A,
                1), new TimedEvent(EVENT_A, 2), new TimedEvent(EVENT_A, 3), new TimedEvent(
                EVENT_A, 5), new TimedEvent(EVENT_A, 8)), scenarioBuilder
                .build().asList());

        scenarioBuilder.addEvent(new TimedEvent(EVENT_B, 0));
        assertEquals(asList(new TimedEvent(EVENT_B, 0), new TimedEvent(EVENT_A,
                0), new TimedEvent(EVENT_A, 1), new TimedEvent(EVENT_A, 2), new TimedEvent(
                EVENT_A, 3), new TimedEvent(EVENT_A, 5), new TimedEvent(
                EVENT_A, 8)), scenarioBuilder.build().asList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTimeSeriesOfEventsFail1() {
        scenarioBuilder.addTimeSeriesOfEvents(0, 0, 0, (EventCreator<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTimeSeriesOfEventsFail2() {
        scenarioBuilder
                .addTimeSeriesOfEvents(-2, -1, 0, (EventCreator<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTimeSeriesOfEventsFail3() {
        scenarioBuilder.addTimeSeriesOfEvents(0, 1, 0, (EventCreator<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTimeSeriesOfEventsFail4() {
        scenarioBuilder.addTimeSeriesOfEvents(0, 1, 1, (EventCreator<?>) null);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void eventTypeFunctionConstructor() {
        new EventTypeFunction(null);
    }

}
