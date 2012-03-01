/**
 * 
 */
package rinde.sim.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class MapFixerTest {

	Class<? extends Graph> graphType;

	public Graph createGraph() {
		try {
			return graphType.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public MapFixerTest(Class<? extends Graph> c) {
		graphType = c;
	}

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { MultimapGraph.class }, { TableGraph.class } });
	}

	@Test
	public void testFindNotFullyConnectedNodes1() {
		Point a, b, c, d, e;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.addConnection(a, b);
		graph.addConnection(a, c);
		graph.addConnection(b, c);
		graph.addConnection(c, a);
		graph.addConnection(c, d);
		graph.addConnection(b, e);
		graph.addConnection(d, e);
		graph.addConnection(e, d);
		for (Point root : asList(a, b, c)) {
			Set<Point> unconnectedSet = MapFixer.findNotFullyConnectedNodes(graph, root).get(0);
			assertTrue(unconnectedSet.size() == 2);
			assertTrue(unconnectedSet.containsAll(asList(e, d)));
		}

		for (Point root : asList(d, e)) {
			Set<Point> unconnectedSet = MapFixer.findNotFullyConnectedNodes(graph, root).get(0);
			assertTrue(unconnectedSet.size() == 3);
			assertTrue(unconnectedSet.containsAll(asList(a, b, c)));
		}

	}

	@Test
	public void testFindNotFullyConnectedNodes2() {
		Point a, b, c, d, e;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.addConnection(a, b);
		graph.addConnection(a, c);
		graph.addConnection(b, c);
		graph.addConnection(c, a);
		graph.addConnection(c, d);
		graph.addConnection(b, e);
		graph.addConnection(d, e);

		for (Point root : asList(a, b, c)) {
			Set<Point> unconnectedSet = MapFixer.findNotFullyConnectedNodes(graph, root).get(0);
			assertTrue(unconnectedSet.size() == 2);
			assertTrue(unconnectedSet.containsAll(asList(e, d)));
		}

		Set<Point> unconnectedSet = MapFixer.findNotFullyConnectedNodes(graph, d).get(0);
		assertTrue(unconnectedSet.size() == 4);
		assertTrue(unconnectedSet.containsAll(asList(a, b, c, e)));

		unconnectedSet = MapFixer.findNotFullyConnectedNodes(graph, e).get(0);
		assertTrue(unconnectedSet.size() == 4);
		assertTrue(unconnectedSet.containsAll(asList(a, b, c, d)));
	}

	@Test
	public void testConnect() {
		Point a, b, c, d, e;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.addConnection(a, b);
		graph.addConnection(a, c);
		graph.addConnection(b, c);
		graph.addConnection(c, a);
		graph.addConnection(c, d);
		graph.addConnection(b, e);
		graph.addConnection(d, e);
		graph.addConnection(e, d);

		Graph result = MapFixer.connect(graph);
		DotUtils.saveToDot(graph, "files/test/input", true);
		DotUtils.saveToDot(result, "files/test/fixed", true);
	}

	@Test
	public void testConnect2() {
		Point a, b, c, d, e;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.addConnection(a, b);
		graph.addConnection(a, c);
		graph.addConnection(b, c);
		graph.addConnection(c, a);
		graph.addConnection(c, d);
		graph.addConnection(b, e);
		graph.addConnection(d, e);
		graph.addConnection(e, d);

		Graph result = MapFixer.connect2(graph);
		DotUtils.saveToDot(graph, "files/test/input2", true);
		DotUtils.saveToDot(result, "files/test/fixed2", true);
	}

	@Test
	public void testSimplify1() {
		Point a, b, c;
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);
		Graph graph = createGraph();

		Graphs.addPath(graph, a, b, c, a);
		DotUtils.saveToDot(graph, "files/test/mapfixer/simplify-1-in", true);
		Graph out = MapFixer.simplify(graph);
		DotUtils.saveToDot(out, "files/test/mapfixer/simplify-1-out", true);

		graph.addConnection(b, a);
		DotUtils.saveToDot(graph, "files/test/mapfixer/simplify-2-in", true);
		out = MapFixer.simplify(graph);
		DotUtils.saveToDot(out, "files/test/mapfixer/simplify-2-out", true);
		assertEquals(graph, out);

		graph = createGraph();
		Graphs.addBiPath(graph, a, b, c, a);
		assertEquals(graph, MapFixer.simplify(graph));
	}

	@Test
	public void testSimplify3() {
		Graph graph = createGraph();
		Point a, b, c, d, e, f;
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);
		d = new Point(6, 0);
		e = new Point(6, 2);
		f = new Point(2, 2);
		Graph out;
		//		Graphs.addPath(graph, a, b, c, d, e, f);
		//		DotUtils.saveToDot(graph, "files/test/mapfixer/simplify-3-in");
		//		Graph out = MapFixer.simplify(graph);
		//		DotUtils.saveToDot(out, "files/test/mapfixer/simplify-3-out");
		//
		//		assertEquals(1, out.getNumberOfConnections());
		//		assertEquals(12.0, out.connectionLength(a, f), 0.0002);

		graph = createGraph();
		Graphs.addBiPath(graph, a, b, c, d, e, f);
		DotUtils.saveToDot(graph, "files/test/mapfixer/simplify-4-in", true);

		assertEquals(MapFixer.ContractType.RIGHT, MapFixer.isContractable(graph, a, b));

		out = MapFixer.simplify(graph);
		DotUtils.saveToDot(out, "files/test/mapfixer/simplify-4-out", true);
		assertEquals(2, out.getNumberOfNodes());
		assertEquals(2, out.getNumberOfConnections());
		assertEquals(12.0, out.connectionLength(a, f), 0.0002);
	}

	@Test
	public void testIsContractableZero() {
		Point a, b, c;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);

		Graphs.addPath(graph, a, b, c);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-1", true);
		assertEquals(MapFixer.ContractType.RIGHT, MapFixer.isContractable(graph, a, b));
		assertEquals(MapFixer.ContractType.LEFT, MapFixer.isContractable(graph, b, a));
		assertEquals(MapFixer.ContractType.LEFT, MapFixer.isContractable(graph, b, c));
		assertEquals(MapFixer.ContractType.RIGHT, MapFixer.isContractable(graph, c, b));

		Graphs.addPath(graph, c, b, a);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-2", true);
		assertEquals(MapFixer.ContractType.RIGHT, MapFixer.isContractable(graph, a, b));
		assertEquals(MapFixer.ContractType.LEFT, MapFixer.isContractable(graph, b, a));
		assertEquals(MapFixer.ContractType.LEFT, MapFixer.isContractable(graph, b, c));
		assertEquals(MapFixer.ContractType.RIGHT, MapFixer.isContractable(graph, c, b));

		Point d = new Point(3, 0);
		graph.addConnection(b, d);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-3", true);
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, a, b));
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, b, a));

		graph = createGraph();
		Graphs.addPath(graph, a, b);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-4", true);
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, a, b));
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, b, a));

		Graphs.addPath(graph, b, a);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-5", true);
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, a, b));
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, b, a));

		graph = createGraph();
		graph.addConnection(a, b);
		graph.addConnection(b, a);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-zero-6", true);
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, a, b));
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, b, a));

	}

	@Test(expected = IllegalArgumentException.class)
	public void testIsContractableIllegal() {
		Point a, b, c;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);

		Graphs.addPath(graph, a, b, c);
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, a, c));
	}

	@Test
	public void testIsContractableOneAndOne() {
		Point a, b, c, d;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);
		d = new Point(3, 0);

		Graphs.addPath(graph, a, b, c, d);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-1", true);
		assertEquals(MapFixer.ContractType.BOTH, MapFixer.isContractable(graph, b, c));
		assertEquals(MapFixer.ContractType.BOTH, MapFixer.isContractable(graph, c, b));

		graph.addConnection(b, a);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-2", true);
		assertEquals(MapFixer.ContractType.RIGHT, MapFixer.isContractable(graph, b, c));
		assertEquals(MapFixer.ContractType.LEFT, MapFixer.isContractable(graph, c, b));

		graph.addConnection(c, b);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-3", true);
		assertEquals(MapFixer.ContractType.LEFT, MapFixer.isContractable(graph, b, c));
		assertEquals(MapFixer.ContractType.RIGHT, MapFixer.isContractable(graph, c, b));

		graph.addConnection(d, c);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-4", true);
		assertEquals(MapFixer.ContractType.BOTH, MapFixer.isContractable(graph, b, c));
		assertEquals(MapFixer.ContractType.BOTH, MapFixer.isContractable(graph, c, b));

		graph.removeConnection(b, c);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-5", true);
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, b, c));
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, c, b));

		graph = createGraph();
		Graphs.addPath(graph, a, b, c, d);
		graph.addConnection(c, b);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-one-and-one-6", true);
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, b, c));
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, c, b));

	}

	@Test
	public void testIsContractableMoreThanOne() {
		Point a, b, c, d, e, f;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);
		d = new Point(3, 0);
		e = new Point(4, 0);
		f = new Point(5, 0);

		Graphs.addPath(graph, a, c, d, f);
		Graphs.addPath(graph, a, b, c);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-more-than-one-1", true);
		assertEquals(MapFixer.ContractType.RIGHT, MapFixer.isContractable(graph, c, d));
		assertEquals(MapFixer.ContractType.LEFT, MapFixer.isContractable(graph, d, c));

		Graphs.addPath(graph, f, e, d);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-more-than-one-2", true);
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, c, d));
		assertEquals(MapFixer.ContractType.NO, MapFixer.isContractable(graph, d, c));
	}

	@Test
	public void testIsContractableSameNeigh() {
		Point a, b, c;
		Graph graph = createGraph();
		a = new Point(0, 0);
		b = new Point(1, 0);
		c = new Point(2, 0);

		Graphs.addPath(graph, a, b, c, a);
		DotUtils.saveToDot(graph, "files/test/mapfixer/contractable-same-neigh-1", true);
		assertEquals(MapFixer.ContractType.BOTH, MapFixer.isContractable(graph, a, b));
		assertEquals(MapFixer.ContractType.BOTH, MapFixer.isContractable(graph, b, a));
	}
}
