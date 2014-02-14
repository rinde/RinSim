package rinde.sim.examples.core.rwalk5;

import java.util.Set;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.ModelReceiver;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.ui.renderers.CanvasRenderer;
import rinde.sim.ui.renderers.UiSchema;
import rinde.sim.ui.renderers.ViewPort;
import rinde.sim.ui.renderers.ViewRect;

public class MessagingLayerRenderer implements CanvasRenderer, ModelReceiver {

  protected RoadModel roadModel;
  protected boolean useEncirclement;
  private final UiSchema uiSchema;

  public MessagingLayerRenderer(UiSchema uiSchema) {
    this.uiSchema = uiSchema;
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final int size = 4;
    uiSchema.initialize(gc.getDevice());

    final Set<RandomWalkAgent> objects = roadModel
        .getObjectsOfType(RandomWalkAgent.class);

    synchronized (objects) {
      for (final RandomWalkAgent a : objects) {
        Point p = a.getPosition();
        if (p == null) {
          continue;
        }
        final int x = (int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale);
        final int y = (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale);

        final int radius = (int) (a.getRadius() * vp.scale);

        Color c = null;
        if (a.getReliability() < 0.15) {
          c = uiSchema.getColor(RandomWalkAgent.C_BLACK);
        } else if (a.getReliability() >= 0.15 && a.getReliability() < 0.3) {
          c = uiSchema.getColor(RandomWalkAgent.C_YELLOW);
        } else {
          c = uiSchema.getColor(RandomWalkAgent.C_GREEN);
        }

        gc.setForeground(c);
        gc.setBackground(c);

        gc.fillOval(x - size, y - size, size * 2, size * 2);

        gc.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.drawText("r:" + a.getNoReceived(), x, y, true);

        final Set<RandomWalkAgent> communicatedWith = a.getCommunicatedWith();
        for (final RandomWalkAgent cw : communicatedWith) {
          p = cw.getPosition();
          if (p == null) {
            continue;
          }
          final int xCW = (int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale);
          final int yCW = (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale);
          gc.drawLine(x, y, xCW, yCW);
        }
      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public ViewRect getViewRect() {
    return null;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    roadModel = mp.getModel(RoadModel.class);
  }

}
