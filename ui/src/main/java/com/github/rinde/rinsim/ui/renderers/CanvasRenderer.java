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

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.GC;

/**
 * A {@link Renderer} that allows rendering on the main canvas of the
 * visualization.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public interface CanvasRenderer extends Renderer {
  /**
   * Should render static objects (such as a graph).
   * @param gc The graphic context of the canvas.
   * @param vp The {@link ViewPort}.
   */
  void renderStatic(GC gc, ViewPort vp);

  /**
   * Should render dynamic objects (such as agents).
   * @param gc The graphic context of the canvas.
   * @param vp The {@link ViewPort}.
   * @param time The current time of the simulator.
   */
  void renderDynamic(GC gc, ViewPort vp, long time);

  /**
   * @return A {@link ViewRect} indicating the dimensions of the rendered
   *         objects. May be <code>null</code> to indicate that another
   *         {@link CanvasRenderer} should implement this.
   */
  @Nullable
  ViewRect getViewRect();
}
