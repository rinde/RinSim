package rinde.sim.scenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rinde.sim.event.pdp.StandardType.ADD_PACKAGE;
import static rinde.sim.event.pdp.StandardType.ADD_TRUCK;
import static rinde.sim.event.pdp.StandardType.REMOVE_TRUCK;

import org.apache.commons.math.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;

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
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyController() throws ConfigurationException {
		controller = new TestScenarioController(scenario, 3);
		controller.tick(0, 1);
	}

	/**
	 * check whether the start event was generated. following scenario is
	 * interrupted after 3rd step so there are some events left
	 * @throws ConfigurationException
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

		controller.addListener(new Listener() {

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

		controller.addListener(new Listener() {

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

	@Test(expected = ConfigurationException.class)
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
