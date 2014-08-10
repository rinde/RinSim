/**
 * 
 */
package com.github.rinde.rinsim.pdptw.common;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A renderer that draws the route for any {@link RouteFollowingVehicle}s that
 * exist in the {@link RoadModel}.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class RouteRenderer implements CanvasRenderer, ModelReceiver {

  Optional<RoadModel> rm;
  Optional<PDPModel> pm;

  /**
   * Create a new route renderer.
   */
  public RouteRenderer() {
    rm = Optional.absent();
    pm = Optional.absent();
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    final Set<RouteFollowingVehicle> vehicles = rm.get().getObjectsOfType(
        RouteFollowingVehicle.class);
    for (final RouteFollowingVehicle v : vehicles) {
      final Set<DefaultParcel> seen = newHashSet();
      final Point from = rm.get().getPosition(v);
      int prevX = vp.toCoordX(from.x);
      int prevY = vp.toCoordY(from.y);

      for (final DefaultParcel parcel : ImmutableList.copyOf(v.getRoute())) {
        Point to;
        if (pm.get().getParcelState(parcel).isPickedUp()
            || seen.contains(parcel)) {
          to = parcel.dto.deliveryLocation;
        } else {
          to = parcel.dto.pickupLocation;
        }
        seen.add(parcel);
        final int x = vp.toCoordX(to.x);
        final int y = vp.toCoordY(to.y);
        gc.drawLine(prevX, prevY, x, y);

        prevX = x;
        prevY = y;
      }
    }
  }

  @Override
  @Nullable
  public ViewRect getViewRect() {
    return null;
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    rm = Optional.fromNullable(mp.getModel(RoadModel.class));
    pm = Optional.fromNullable(mp.getModel(PDPModel.class));
  }
}
