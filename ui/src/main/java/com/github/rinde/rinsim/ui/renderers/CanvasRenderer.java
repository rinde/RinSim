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

import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.google.common.base.Optional;

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
   *         objects. May be {@link Optional#absent()} to indicate that another
   *         {@link CanvasRenderer} should implement this.
   */
  Optional<ViewRect> getViewRect();

  /**
   * Abstract implementation of {@link CanvasRenderer}.
   * @author Rinde van Lon
   */
  public abstract class AbstractCanvasRenderer extends AbstractModelVoid
    implements CanvasRenderer {

    @Override
    public Optional<ViewRect> getViewRect() {
      return Optional.absent();
    }
  }

  /**
   * Abstract implementation of {@link CanvasRenderer} with support for a
   * specific type.
   * @param <T> The type to support.
   * @author Rinde van Lon
   */
  public abstract class AbstractTypedCanvasRenderer<T> extends AbstractModel<T>
    implements CanvasRenderer {

    @Override
    public Optional<ViewRect> getViewRect() {
      return Optional.absent();
    }
  }
}
