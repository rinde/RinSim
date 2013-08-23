package rinde.sim.serializers;

import rinde.sim.core.graph.Point;

/**
 * Filters out self-cycles
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class SelfCycleFilter implements SerializerFilter<Double> {

  @Override
  public boolean filterOut(Point from, Point to) {
    return from.equals(to);
  }

  @Override
  public boolean filterOut(Point from, Point to, Double data) {
    return filterOut(from, to);
  }

}
