/**
 * 
 */
package com.github.rinde.rinsim.ui.renderers;

import com.github.rinde.rinsim.core.graph.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ViewRect {

  public final Point min;
  public final Point max;
  public final double width;
  public final double height;

  public ViewRect(Point pMin, Point pMax) {
    min = pMin;
    max = pMax;
    width = max.x - min.x;
    height = max.y - min.y;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("{ViewRect: ").append(min).append(" ")
        .append(max).append("}").toString();
  }
}
