/**
 * 
 */
package rinde.sim.core.graph;

import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Neo4jGraphTest {

	@Test
	public void testSetup() {
		Graph<LengthEdgeData> g = new Neo4jGraph<LengthEdgeData>("testdb", true);
		System.out.println("before: " + g.getNodes());

		g.addConnection(new Point(0, 0), new Point(10, 0));
		g.addConnection(new Point(10, 0), new Point(20, 0));
		g.addConnection(new Point(0, 0), new Point(20, 0));

		System.out.println("after: " + g.getNodes());

	}

}
