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
import org.eclipse.swt.graphics.Path;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

class RenderHelper {
  private Optional<GC> gc;
  private Optional<ViewPort> vp;

  RenderHelper() {
    gc = Optional.absent();
    vp = Optional.absent();
  }

  void adapt(GC g, ViewPort v) {
    gc = Optional.of(g);
    vp = Optional.of(v);
  }

  void drawLine(Point p1, Point p2) {
    gc.get().drawLine(
        vp.get().toCoordX(p1.x), vp.get().toCoordY(p1.y),
        vp.get().toCoordX(p2.x), vp.get().toCoordY(p2.y));
  }

  void setForegroundSysCol(int next) {
    gc.get().setForeground(gc.get().getDevice().getSystemColor(next));
  }

  void setBackgroundSysCol(int next) {
    gc.get().setBackground(gc.get().getDevice().getSystemColor(next));
  }

  void drawCurve(Point p1, Point p2, Point control) {
    final Path path = new Path(gc.get().getDevice());
    path.moveTo(vp.get().toCoordX(p1.x), vp.get().toCoordY(p1.y));
    path.quadTo(
        vp.get().toCoordX(control.x), vp.get().toCoordY(control.y),
        vp.get().toCoordX(p2.x), vp.get().toCoordY(p2.y));
    gc.get().drawPath(path);
    path.dispose();
  }

  void drawCircle(Point p, double radius) {
    gc.get().drawOval(
        vp.get().toCoordX(p.x - radius),
        vp.get().toCoordY(p.y - radius),
        vp.get().scale(radius * 2),
        vp.get().scale(radius * 2));
  }

  void fillCircle(Point p, double radius) {
    gc.get().fillOval(
        vp.get().toCoordX(p.x - radius),
        vp.get().toCoordY(p.y - radius),
        vp.get().scale(radius * 2),
        vp.get().scale(radius * 2));
  }

  void fillCircle(Point p, int radiusInPixels) {
    gc.get().fillOval(
        vp.get().toCoordX(p.x) - radiusInPixels,
        vp.get().toCoordY(p.y) - radiusInPixels,
        radiusInPixels * 2,
        radiusInPixels * 2);
  }

  void drawCircle(Point p, int radius) {
    gc.get().drawOval(
        vp.get().toCoordX(p.x) - radius,
        vp.get().toCoordY(p.y) - radius,
        radius * 2,
        radius * 2);
  }

  void drawRect(Point corner1, Point corner2) {
    final int x1 = vp.get().toCoordX(corner1.x);
    final int y1 = vp.get().toCoordY(corner1.y);
    final int x2 = vp.get().toCoordX(corner2.x);
    final int y2 = vp.get().toCoordY(corner2.y);
    gc.get().drawPolygon(new int[] {x1, y1, x2, y1, x2, y2, x1, y2});
  }

  void fillRect(Point corner1, Point corner2) {
    final int x1 = vp.get().toCoordX(corner1.x);
    final int y1 = vp.get().toCoordY(corner1.y);
    final int x2 = vp.get().toCoordX(corner2.x);
    final int y2 = vp.get().toCoordY(corner2.y);
    gc.get().fillPolygon(new int[] {x1, y1, x2, y1, x2, y2, x1, y2});
  }

  int[] toCoordinates(Point... points) {
    final int[] coordinates = new int[points.length * 2];
    for (int i = 0; i < points.length; i++) {
      coordinates[i * 2] = vp.get().toCoordX(points[i].x);
      coordinates[i * 2 + 1] = vp.get().toCoordY(points[i].y);
    }
    return coordinates;
  }

  void fillPolygon(Point... points) {
    gc.get().fillPolygon(toCoordinates(points));
  }

  void drawPolygon(Point... points) {
    gc.get().drawPolygon(toCoordinates(points));
  }

  void drawPolyline(Point... points) {
    gc.get().drawPolyline(toCoordinates(points));
  }

  void drawArrow(Point from, Point to, double width, double height) {
    final Point left = PointUtil.perp(to, from, height, width / 2d);
    final Point right = PointUtil.perp(to, from, height, -width / 2d);
    drawLine(from, PointUtil.on(from, to, height));
    fillPolygon(left, right, to);
  }

  void drawArrow(Point from, Point to, int width, int height) {
    final double w = vp.get().invScale(width);
    final double h = vp.get().invScale(height);

    final Point left = PointUtil.perp(to, from, h, w / 2d);
    final Point right = PointUtil.perp(to, from, h, -w / 2d);
    drawLine(from, PointUtil.on(from, to, h));
    fillPolygon(left, right, to);
  }

  void drawString(String string, Point pos, boolean isTransparent) {
    drawString(string, pos, isTransparent, 0, 0);
  }

  void drawString(String string, Point pos, boolean isTransparent, int xOffset,
      int yOffset) {
    gc.get().drawString(string,
        vp.get().toCoordX(pos.x) + xOffset,
        vp.get().toCoordY(pos.y) + yOffset,
        isTransparent);
  }
}
