/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
