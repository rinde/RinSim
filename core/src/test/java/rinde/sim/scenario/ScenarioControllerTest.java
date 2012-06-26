package rinde.sim.scenario;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rinde.sim.event.pdp.StandardType.ADD_PACKAGE;
import static rinde.sim.event.pdp.StandardType.ADD_TRUCK;
import static rinde.sim.event.pdp.StandardType.REMOVE_PACKAGE;
import static rinde.sim.event.pdp.StandardType.REMOVE_TRUCK;
import static rinde.sim.scenario.ScenarioController.Type.SCENARIO_FINISHED;
import static rinde.sim.scenario.ScenarioController.Type.SCENARIO_STARTED;

import org.apache.commons.math.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.TimeLapseFactory;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.event.ListenerEventHistory;

public class ScenarioControllerTest {

	protected ScenarioController controller;
	protected Scenario scenario;

	@Before
	public void setUp() throws Exception {
		ScenarioBuilder sb = new ScenarioBuilder(ADD_PACKAGE, ADD_TRUCK, REMOVE_TRUCK);
		sb.addEvent(new TimedEvent(ADD_PACKAGE, 0)).addEvent(new TimedEvent(ADD_TRUCK, 0))
				.addEvent(new TimedEvent(ADD_TRUCK, 0)).addEvent(new TimedEvent(ADD_PACKAGE, 1))
				.addEvent(new TimedEvent(REMOVE_TRUCK, 5)).addEvent(new TimedEvent(REMOVE_TRUCK, 100));
		scenario = sb.build();
		assertNotNull(scenario);
		ScenarioController.Type.valueOf("SCENARIO_STARTED");

	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyController() throws ConfigurationException {
		controller = new TestScenarioController(scenario, 3);
		controller.tick(TimeLapseFactory.create(0, 1));
	}

	@Test(expected = ConfigurationException.class)
	public void initializeFail() throws ConfigurationException {
		ScenarioController sc = new ScenarioController(scenario, 1) {
			@Override
			protected Simulator createSimulator() throws Exception {
				throw new RuntimeException("this is what we want");
			}
		};
		sc.initialize();
	}

	@Test
	public void handleStandard() {
		ScenarioController sc = new ScenarioController(scenario, 1) {
			@Override
			protected Simulator createSimulator() throws Exception {
				return null;
			}
		};
		assertFalse(sc.handleStandard(new Event(ADD_PACKAGE, this)));
		assertFalse(sc.handleStandard(new Event(REMOVE_PACKAGE, this)));
		assertFalse(sc.handleStandard(new Event(ADD_TRUCK, this)));
		assertFalse(sc.handleStandard(new Event(REMOVE_TRUCK, this)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void eventNotHandled() {
		ScenarioController sc = new ScenarioController(scenario, 1) {
			@Override
			protected Simulator createSimulator() throws Exception {
				return null;
			}
		};
		sc.handleEvent(new Event(SCENARIO_FINISHED, null));
	}

	@Test
	public void finiteSimulation() throws ConfigurationException, InterruptedException {
		ScenarioController sc = new TestScenarioController(scenario, 101) {
			@Override
			protected boolean handleAddPackage(Event event) {
				return true;
			}

			@Override
			protected boolean handleRemovePackage(Event event) {
				return true;
			}

			@Override
			protected boolean handleAddTruck(Event event) {
				return true;
			}

			@Override
			protected boolean handleRemoveTruck(Event e) {
				return true;
			}
		};

		ListenerEventHistory leh = new ListenerEventHistory();
		sc.eventAPI.addListener(leh);
		assertFalse(sc.isScenarioFinished());
		sc.start();
		while (!sc.isScenarioFinished()) {
			Thread.sleep(100);
		}
		assertEquals(asList(SCENARIO_STARTED, ADD_PACKAGE, ADD_TRUCK, ADD_TRUCK, ADD_PACKAGE, REMOVE_TRUCK, REMOVE_TRUCK, SCENARIO_FINISHED), leh
				.getEventTypeHistory());

		assertTrue(sc.isScenarioFinished());
		sc.stop();
		synchronized (sc.getSimulator()) {
			long before = sc.getSimulator().getCurrentTime();
			sc.start();// should have no effect

			// FIXME sometimes produces errors...

			// Failed tests:
			// finiteSimulation(rinde.sim.scenario.ScenarioControllerTest):
			// expected:<1322> but was:<1323>

			assertEquals(before, sc.getSimulator().getCurrentTime());
		}
		TimeLapse emptyTime = TimeLapseFactory.create(0, 1);
		emptyTime.consumeAll();
		sc.tick(emptyTime);
	}

	@Test
	public void fakeUImode() throws ConfigurationException {
		ScenarioController sc = new TestScenarioController(scenario, 3) {

			@Override
			protected boolean createUserInterface() {
				return true;
			}

			@Override
			protected boolean handleCustomEvent(Event e) {
				return true;
			}

		};

		sc.start();
		sc.stop();
		TimeLapse emptyTime = TimeLapseFactory.create(0, 1);
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

		controller = new TestScenarioController(scenario, 3) {

			@Override
			protected boolean handleAddPackage(Event e) {
				return true;
			}

			@Override
			protected boolean handleAddTruck(Event e) {
				return true;
			}

		};

		final boolean[] r = new boolean[1];
		final int[] i = new int[1];

		controller.eventAPI.addListener(new Listener() {

			@Override
			public void handleEvent(Event e) {
				if (e.getEventType() == ScenarioController.Type.SCENARIO_STARTED) {
					r[0] = true;
				} else if (!r[0]) {
					fail();
				} else {
					i[0] += 1;
				}
			}
		});

		controller.getSimulator().tick();
		assertTrue("event generated", r[0]);
		assertEquals(3, i[0]);
	}

	@Test
	public void runningWholeScenario() throws ConfigurationException, InterruptedException {
		controller = new TestScenarioController(scenario, -1) {

			@Override
			protected boolean handleAddPackage(Event e) {
				return true;
			}

			@Override
			protected boolean handleAddTruck(Event e) {
				return true;
			}

			@Override
			protected boolean handleRemoveTruck(Event e) {
				return true;
			}
		};

		final boolean[] r = new boolean[1];
		final int[] i = new int[1];

		controller.eventAPI.addListener(new Listener() {

			@Override
			public void handleEvent(Event e) {
				if (e.getEventType() == ScenarioController.Type.SCENARIO_FINISHED) {
					synchronized (controller) {
						r[0] = true;
						controller.notifyAll();
					}
				} else {
					i[0] += 1;
				}
			}
		});

		controller.start();

		synchronized (controller) {
			while (!r[0]) {
				controller.wait();
			}
			assertTrue(r[0]);
			assertEquals(scenario.asList().size() + 1, i[0]);
			assertTrue(controller.isScenarioFinished());
		}

		controller.stop();
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testNullScenario() throws ConfigurationException {
		new TestScenarioController(null, -1);
	}

	@Test(expected = ConfigurationException.class)
	public void testIncorrectUseOfScenarioController() throws ConfigurationException {
		ScenarioController c = new ScenarioController(scenario, 1) {
			@Override
			protected Simulator createSimulator() {
				// designed behavior for this test
				return null;
			}
		};
		c.start();
	}

}

class TestScenarioController extends ScenarioController {

	public TestScenarioController(Scenario scen, int numberOfTicks) throws ConfigurationException {
		super(scen, numberOfTicks);
		initialize();
	}

	@Override
	protected Simulator createSimulator() {
		MersenneTwister rand = new MersenneTwister(123);
		return new Simulator(rand, 1);
	}

}
