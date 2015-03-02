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

import com.github.rinde.rinsim.geom.Point;

/**
 * Value object containing information about the region of the screen which is
 * used for rendering.
 *
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 *
 */
public class ViewPort {

  public final Point origin;
  public final ViewRect rect;
  public final double scale;

  public ViewPort(Point pOrigin, ViewRect pViewRect, double pZoom) {
    origin = pOrigin;
    rect = pViewRect;
    scale = pZoom;
  }

  public int toCoordX(double x) {
    return (int) (origin.x + (x - rect.min.x) * scale);
  }

  public int toCoordY(double y) {
    return (int) (origin.y + (y - rect.min.y) * scale);
  }

  public int scale(double i) {
    return (int) (scale * i);
  }

  double invScale(int val) {
    return 1d / scale * val;
  }
}
