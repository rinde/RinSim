/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.ui.renderers;

import org.eclipse.swt.graphics.RGB;

/**
 * @author Rinde van Lon
 *
 */
class ColorUtil {

  static RGB interpolate(RGB c1, RGB c2, double i) {
    return new RGB(
        interpolate(c1.red, c2.red, i),
        interpolate(c1.green, c2.green, i),
        interpolate(c1.blue, c2.blue, i));
  }

  static int interpolate(int c1, int c2, double i) {
    final double diff = c2 - c1;
    return c1 + (int) (i * diff);
  }
}
