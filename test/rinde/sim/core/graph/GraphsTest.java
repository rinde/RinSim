package rinde.sim.core.graph;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class GraphsTest {
	Graph graph;
	Class<? extends Graph> graphType;

	public GraphsTest(Class<? extends Graph> c) {
		graphType = c;
	}

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { MultimapGraph.class }, { TableGraph.class } });
	}

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException {
		graph = graphType.newInstance();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addConnection2() {
		graph.addConnection(new Point(0, 0), new Point(0, 0));
	}

}
