/**
 * 
 */
package com.github.rinde.rinsim.ui.renderers;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public final class RandomObjectRenderer implements ModelRenderer {

  @Nullable
  private RoadModel rm;
  @Nullable
  private Color defaultColor;

  public RandomObjectRenderer() {}

  @Override
  public void renderDynamic(GC gc, ViewPort viewPort, long time) {
    final int radius = 4;
    final int outerRadius = 10;
    if (defaultColor == null) {
      defaultColor = gc.getDevice().getSystemColor(SWT.COLOR_RED);
    }

    gc.setBackground(defaultColor);

    checkState(rm != null);
    final Map<RoadUser, Point> objects = rm.getObjectsAndPositions();
    synchronized (objects) {
      for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
        final Point p = entry.getValue();

        gc.setBackground(defaultColor);

        final int x = (int) (viewPort.origin.x + (p.x - viewPort.rect.min.x)
            * viewPort.scale)
            - radius;
        final int y = (int) (viewPort.origin.y + (p.y - viewPort.rect.min.y)
            * viewPort.scale)
            - radius;

        gc.fillOval(x, y, 2 * radius, 2 * radius);
        gc.drawText(entry.getKey() + "", x + 5, y - 15);
      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    return null;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    rm = mp.getModel(PlaneRoadModel.class);
  }
}
