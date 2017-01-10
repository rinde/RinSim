/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.time;

import javax.measure.quantity.Duration;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public final class TimeLapseFactory {

  private TimeLapseFactory() {}

  // this should only be used in tests!

  public static TimeLapse create(Unit<Duration> unit, long start, long end) {
    return new TimeLapse(unit, start, end);
  }

  public static TimeLapse create(long start, long end) {
    return create(SI.MILLI(SI.SECOND), start, end);
  }

  public static TimeLapse time(long start, long end) {
    return create(start, end);
  }

  public static TimeLapse time(long end) {
    return create(0, end);
  }

  public static TimeLapse hour(long start, long end) {
    return create(NonSI.HOUR, start, end);
  }

  public static TimeLapse hour(long end) {
    return create(NonSI.HOUR, 0, end);
  }

  public static TimeLapse ms(long start, long end) {
    return create(SI.MILLI(SI.SECOND), start, end);
  }

  public static TimeLapse ms(long end) {
    return ms(0, end);
  }

}
