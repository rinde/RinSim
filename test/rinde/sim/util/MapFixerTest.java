/**
 * 
 */
package rinde.sim.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.Point;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class MapFixerTest {

	@Before
	public void setUp() {

	}

	@Test
	public void testFindNotFullyConnectedNodes1() {
		Point a, b, c, d, e;
		Multimap<Point, Point> graph = HashMultimap.create();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.put(a, b);
		graph.put(a, c);
		graph.put(b, c);
		graph.put(c, a);
		graph.put(c, d);
		graph.put(b, e);
		graph.put(d, e);
		graph.put(e, d);
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
		Multimap<Point, Point> graph = HashMultimap.create();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.put(a, b);
		graph.put(a, c);
		graph.put(b, c);
		graph.put(c, a);
		graph.put(c, d);
		graph.put(b, e);
		graph.put(d, e);

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
		Multimap<Point, Point> graph = HashMultimap.create();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.put(a, b);
		graph.put(a, c);
		graph.put(b, c);
		graph.put(c, a);
		graph.put(c, d);
		graph.put(b, e);
		graph.put(d, e);
		graph.put(e, d);

		Multimap<Point, Point> result = MapFixer.connect(graph);
		DotUtils.saveToDot(graph, "files/test/input");
		DotUtils.saveToDot(result, "files/test/fixed");
	}

	@Test
	public void testConnect2() {
		Point a, b, c, d, e;
		Multimap<Point, Point> graph = HashMultimap.create();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(0, 10);
		d = new Point(10, 10);
		e = new Point(15, 5);

		graph.put(a, b);
		graph.put(a, c);
		graph.put(b, c);
		graph.put(c, a);
		graph.put(c, d);
		graph.put(b, e);
		graph.put(d, e);
		graph.put(e, d);

		Multimap<Point, Point> result = MapFixer.connect2(graph);
		DotUtils.saveToDot(graph, "files/test/input2");
		DotUtils.saveToDot(result, "files/test/fixed2");
	}
}
