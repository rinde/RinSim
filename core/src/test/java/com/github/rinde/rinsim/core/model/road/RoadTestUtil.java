/**
 * 
 */
package com.github.rinde.rinsim.core.model.road;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

/**
 * @author Rinde van Lon 
 * 
 */
public class RoadTestUtil {

  public static Measure<Double, Velocity> kmh(double km) {
    return Measure.valueOf(km, NonSI.KILOMETERS_PER_HOUR);
  }

  public static Measure<Double, Length> km(double km) {
    return Measure.valueOf(km, SI.KILOMETER);
  }

}
