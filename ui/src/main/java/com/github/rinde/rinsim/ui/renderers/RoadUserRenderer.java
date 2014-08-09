/**
 * 
 */
package com.github.rinde.rinsim.ui.renderers;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> changes in
 *         handling colors
 * 
 */
public final class RoadUserRenderer implements ModelRenderer {

  @Nullable
  private RoadModel rs;
  private final boolean useEncirclement;
  private final UiSchema uiSchema;
  @Nullable
  private final ViewRect viewRect;

  public RoadUserRenderer() {
    this(null, null, false);
  }

  public RoadUserRenderer(UiSchema schema, boolean useEncirclement) {
    this(null, schema, useEncirclement);
  }

  public RoadUserRenderer(@Nullable ViewRect rect, @Nullable UiSchema schema,
      boolean useEncirclement) {
    viewRect = rect;
    this.useEncirclement = useEncirclement;
    uiSchema = schema == null ? new UiSchema() : schema;
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final int radius = 4;
    final int outerRadius = 10;
    uiSchema.initialize(gc.getDevice());
    gc.setBackground(uiSchema.getDefaultColor());

    final Map<RoadUser, Point> objects = rs.getObjectsAndPositions();
    synchronized (objects) {
      for (final Entry<RoadUser, Point> entry : objects.entrySet()) {
        final Point p = entry.getValue();
        final Class<?> type = entry.getKey().getClass();
        final Image image = uiSchema.getImage(type);
        final int x = vp.toCoordX(p.x) - radius;
        final int y = vp.toCoordY(p.y) - radius;

        if (image != null) {
          final int offsetX = x - image.getBounds().width / 2;
          final int offsetY = y - image.getBounds().height / 2;
          gc.drawImage(image, offsetX, offsetY);
        } else {
          final Color color = uiSchema.getColor(type);
          if (color == null) {
            continue;
          }
          gc.setBackground(color);
          if (useEncirclement) {
            gc.setForeground(gc.getBackground());
            gc.drawOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale)
                - outerRadius, (int) (vp.origin.y + (p.y - vp.rect.min.y)
                * vp.scale)
                - outerRadius, 2 * outerRadius, 2 * outerRadius);
          }
          gc.fillOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale)
              - radius, (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale)
              - radius, 2 * radius, 2 * radius);
        }

      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    return viewRect;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    rs = mp.getModel(RoadModel.class);
  }
}
