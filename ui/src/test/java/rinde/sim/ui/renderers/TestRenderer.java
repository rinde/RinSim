package rinde.sim.ui.renderers;

import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.GC;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.road.RoadModel;

import com.google.common.base.Optional;

public class TestRenderer implements ModelRenderer {

  Optional<RoadModel> roadModel;

  @Override
  public void registerModelProvider(ModelProvider mp) {
    roadModel = Optional.fromNullable(mp.getModel(RoadModel.class));
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    // TODO Auto-generated method stub

  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final List<Point> bounds = roadModel.get().getBounds();

    gc.drawLine(vp.toCoordX(bounds.get(0).x), vp.toCoordY(bounds.get(0).y),
        vp.toCoordX(bounds.get(1).x), vp.toCoordY(bounds.get(1).y));

    gc.drawText("fancy pancy", vp.toCoordX(bounds.get(0).x),
        vp.toCoordY(bounds.get(0).y) + 100, true);

  }

  @Override
  @Nullable
  public ViewRect getViewRect() {
    return null;
  }

}
