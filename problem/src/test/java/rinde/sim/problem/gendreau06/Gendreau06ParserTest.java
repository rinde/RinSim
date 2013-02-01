/**
 * 
 */
package rinde.sim.problem.gendreau06;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static rinde.sim.problem.gendreau06.Gendreau06Parser.parse;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Gendreau06ParserTest {

	@Test
	public void parseTest() throws IOException {

		final Gendreau06Scenario scen = parse("data/test/gendreau06/req_rapide_1_240_24", 5);

		final List<TimedEvent> events = scen.asList();

		int vehicles = 0;
		boolean containsTimeOut = false;
		for (final TimedEvent e : events) {
			if (e.getEventType() == PDPScenarioEvent.ADD_VEHICLE) {
				vehicles++;
			} else if (e.getEventType() == PDPScenarioEvent.TIME_OUT) {
				containsTimeOut = true;
				assertEquals(240 * 60 * 1000, e.time);
			}
		}
		assertTrue(containsTimeOut);
		assertEquals(5, vehicles);

	}

	@Test(expected = IllegalArgumentException.class)
	public void parseWrongFileName() throws IOException {
		parse(null, "pointer/to/non-existing/file/req_rapide_1_241_24", 5);
	}

}
