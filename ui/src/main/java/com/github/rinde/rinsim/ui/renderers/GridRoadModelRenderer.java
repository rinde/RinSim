package com.github.rinde.rinsim.ui.renderers;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.GridRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class GridRoadModelRenderer implements ModelRenderer {

  Optional<GridRoadModel> model;

  GridRoadModelRenderer() {
    model = Optional.absent();
  }

  @Override
  public void registerModelProvider(ModelProvider mp) {
    model = Optional.of(requireNonNull(mp.getModel(GridRoadModel.class),
        "GridRoadModel could not be found."));
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {

    gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));

    final int xMin = vp.toCoordX(model.get().getCellSize() / -2);
    final int xMax = vp.toCoordX(model.get().getNumXCells()
        * model.get().getCellSize() - model.get().getCellSize() / 2);
    final int yMin = vp.toCoordY(model.get().getCellSize() / -2);
    final int yMax = vp.toCoordY(model.get().getNumYCells()
        * model.get().getCellSize() - model.get().getCellSize() / 2);

    for (int j = 0; j < model.get().getNumYCells() + 1; j++) {
      final int y = vp.toCoordY(j * model.get().getCellSize()
          - model.get().getCellSize() / 2);
      gc.drawLine(xMin, y, xMax, y);
    }

    for (int i = 0; i < model.get().getNumXCells() + 1; i++) {
      final int x = vp.toCoordY(i * model.get().getCellSize()
          - model.get().getCellSize() / 2);
      gc.drawLine(x, yMin, x, yMax);
    }

    // gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
    // for (int i = 0; i < model.get().getNumXCells(); i++) {
    // for (int j = 0; j < model.get().getNumYCells(); j++) {
    // final int x = vp.toCoordY(i * model.get().getCellSize());
    // final int y = vp.toCoordY(j * model.get().getCellSize());
    // gc.drawOval(x - 2, y - 2, 4, 4);
    // }
    // }

  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    // TODO Auto-generated method stub

    final Map<RoadUser, Point> objPos = model.get().getObjectsAndPositions();

    for (final Entry<RoadUser, Point> entry : objPos.entrySet()) {

    }

  }

  private static final double MARGIN_CELL_SIZE_PERC = .25;

  @Nullable
  @Override
  public ViewRect getViewRect() {
    final List<Point> bounds = model.get().getBounds();
    final double margin = MARGIN_CELL_SIZE_PERC * model.get().getCellSize();
    final Point min = new Point(bounds.get(0).x - margin, bounds.get(0).y
        - margin);
    final Point max = new Point(bounds.get(1).x + margin, bounds.get(1).y
        + margin);
    return new ViewRect(min, max);
  }
}
