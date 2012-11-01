package rinde.sim.scenario;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rinde.sim.scenario.ScenarioController.EventType.SCENARIO_FINISHED;
import static rinde.sim.scenario.ScenarioController.EventType.SCENARIO_STARTED;
import static rinde.sim.scenario.ScenarioControllerTest.TestEvents.EVENT_A;
import static rinde.sim.scenario.ScenarioControllerTest.TestEvents.EVENT_B;
import static rinde.sim.scenario.ScenarioControllerTest.TestEvents.EVENT_C;
import static rinde.sim.scenario.ScenarioControllerTest.TestEvents.EVENT_D;

import java.util.Set;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.TimeLapseFactory;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.event.ListenerEventHistory;
import rinde.sim.scenario.ScenarioController.UICreator;

public class ScenarioControllerTest {

    protected ScenarioController controller;
    protected Scenario scenario;
    protected Simulator simulator;

    public enum TestEvents {
        EVENT_A, EVENT_B, EVENT_C, EVENT_D;
    }

    @Before
    public void setUp() throws Exception {
        final ScenarioBuilder sb = new ScenarioBuilder(EVENT_A, EVENT_B,
                EVENT_C, EVENT_D);
        sb.addEvent(new TimedEvent(EVENT_A, 0))
                .addEvent(new TimedEvent(EVENT_B, 0))
                .addEvent(new TimedEvent(EVENT_B, 0))
                .addEvent(new TimedEvent(EVENT_A, 1))
                .addEvent(new TimedEvent(EVENT_C, 5))
                .addEvent(new TimedEvent(EVENT_C, 100));
        scenario = sb.build();
        assertNotNull(scenario);
        ScenarioController.EventType.valueOf("SCENARIO_STARTED");
        simulator = new Simulator(new MersenneTwister(123), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyController() throws ConfigurationException {
        controller = new ScenarioController(scenario, simulator,
                new TestHandler(), 3);
        controller.tick(TimeLapseFactory.create(0, 1));
    }

    // @Test(expected = ConfigurationException.class)
    // public void initializeFail() throws ConfigurationException {
    // final ScenarioController sc = new ScenarioController(scenario, 1) {
    // @Override
    // protected Simulator createSimulator() throws Exception {
    // throw new RuntimeException("this is what we want");
    // }
    //
    // @Override
    // protected boolean handleTimedEvent(TimedEvent event) {
    // return true;
    // }
    // };
    // sc.initialize();
    // }

    @Test
    public void handleTimedEvent() {
        final ScenarioController sc = new ScenarioController(scenario,
                simulator, new TestHandler(), 1);

        assertFalse(sc.timedEventHandler.handleTimedEvent(new TimedEvent(
                EVENT_A, 0)));
        assertFalse(sc.timedEventHandler.handleTimedEvent(new TimedEvent(
                EVENT_B, 0)));
        assertFalse(sc.timedEventHandler.handleTimedEvent(new TimedEvent(
                EVENT_C, 0)));
        assertFalse(sc.timedEventHandler.handleTimedEvent(new TimedEvent(
                EVENT_D, 0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void eventNotHandled() {
        final ScenarioController sc = new ScenarioController(scenario,
                simulator, new TestHandler(), 1);
        sc.disp.dispatchEvent(new TimedEvent(EVENT_A, 0));
    }

    class TestHandler implements TimedEventHandler {
        Set<Enum<?>> types;

        public TestHandler(Enum<?>... handledTypes) {
            types = newHashSet(handledTypes);
        }

        @Override
        public boolean handleTimedEvent(TimedEvent event) {
            return types.contains(event.getEventType());
        }
    }

    @Test
    public void finiteSimulation() throws ConfigurationException,
            InterruptedException {
        final ScenarioController sc = new ScenarioController(scenario,
                simulator, new TestHandler(TestEvents.values()), 101);

        final ListenerEventHistory leh = new ListenerEventHistory();
        sc.eventAPI.addListener(leh);
        assertFalse(sc.isScenarioFinished());
        sc.start();
        assertEquals(asList(SCENARIO_STARTED, EVENT_A, EVENT_B, EVENT_B, EVENT_A, EVENT_C, EVENT_C, SCENARIO_FINISHED), leh
                .getEventTypeHistory());

        assertTrue(sc.isScenarioFinished());
        sc.stop();
        final long before = sc.simulator.getCurrentTime();
        sc.start();// should have no effect

        assertEquals(before, sc.simulator.getCurrentTime());
        final TimeLapse emptyTime = TimeLapseFactory.create(0, 1);
        emptyTime.consumeAll();
        sc.tick(emptyTime);
    }

    @Test
    public void fakeUImode() throws ConfigurationException {
        final ScenarioController sc = new ScenarioController(scenario,
                simulator, new TestHandler(TestEvents.values()), 3);
        sc.enableUI(new UICreator() {

            @Override
            public void createUI(Simulator sim) {
                // TODO Auto-generated method stub

            }
        });

        sc.start();
        sc.stop();
        final TimeLapse emptyTime = TimeLapseFactory.create(0, 1);
        emptyTime.consumeAll();
        sc.tick(emptyTime);
    }

    /**
     * check whether the start event was generated. following scenario is
     * interrupted after 3rd step so there are some events left
     * @throws ConfigurationException yes
     */
    @Test
    public void testStartEventGenerated() throws ConfigurationException {
        controller = new ScenarioController(scenario, simulator,
                new TestHandler(EVENT_A, EVENT_B), 3);

        // {
        //
        // @Override
        // protected boolean handleTimedEvent(TimedEvent event) {
        // if (event.getEventType() == EVENT_A
        // || event.getEventType() == EVENT_B) {
        // return true;
        // }
        //
        // return super.handleTimedEvent(event);
        // }
        //
        // };

        final boolean[] r = new boolean[1];
        final int[] i = new int[1];

        controller.eventAPI.addListener(new Listener() {

            @Override
            public void handleEvent(Event e) {
                if (e.getEventType() == ScenarioController.EventType.SCENARIO_STARTED) {
                    r[0] = true;
                } else if (!r[0]) {
                    fail();
                } else {
                    i[0] += 1;
                }
            }
        });

        controller.simulator.tick();
        assertTrue("event generated", r[0]);
        assertEquals(3, i[0]);
    }

    @Test
    public void runningWholeScenario() throws ConfigurationException,
            InterruptedException {
        controller = new ScenarioController(scenario, simulator,
                new TestHandler(EVENT_A, EVENT_B, EVENT_C), -1);

        final boolean[] r = new boolean[1];
        final int[] i = new int[1];

        controller.eventAPI.addListener(new Listener() {

            @Override
            public void handleEvent(Event e) {
                if (e.getEventType() == ScenarioController.EventType.SCENARIO_FINISHED) {
                    synchronized (controller) {
                        r[0] = true;
                        controller.stop();
                    }
                } else {
                    i[0] += 1;
                }
            }
        });

        controller.start();

        assertTrue(r[0]);
        assertEquals(scenario.asList().size() + 1, i[0]);
        assertTrue(controller.isScenarioFinished());

        controller.stop();
    }

    // @SuppressWarnings("unused")
    // @Test(expected = IllegalArgumentException.class)
    // public void testNullScenario() throws ConfigurationException {
    // new TestScenarioController(null, -1);
    // }

    // @Test(expected = ConfigurationException.class)
    // public void testIncorrectUseOfScenarioController()
    // throws ConfigurationException {
    // final ScenarioController c = new ScenarioController(scenario, 1) {
    // @Override
    // protected Simulator createSimulator() {
    // // designed behavior for this test
    // return null;
    // }
    //
    // @Override
    // protected boolean handleTimedEvent(TimedEvent event) {
    // return false;
    // }
    // };
    // c.start();
    // }

}

// class TestScenarioController extends ScenarioController {
//
// public TestScenarioController(Scenario scen, int numberOfTicks)
// throws ConfigurationException {
// super(scen, numberOfTicks);
// initialize();
// }
//
// @Override
// protected Simulator createSimulator() {
// final MersenneTwister rand = new MersenneTwister(123);
// return new Simulator(rand, 1);
// }
//
// @Override
// protected boolean handleTimedEvent(TimedEvent event) {
// return false;
// }
//
// }
