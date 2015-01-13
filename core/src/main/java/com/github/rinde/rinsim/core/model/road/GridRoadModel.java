package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.GridRoadModel.Position;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

public class GridRoadModel extends AbstractRoadModel<Position> {
  private final double cellSize;
  private final int numXCells;
  private final int numYCells;

  private final Map<Position, RoadUser> locationMap;

  GridRoadModel(Builder b) {
    super(b.distanceUnit, b.speedUnit);
    cellSize = b.cellSize;
    numXCells = b.numXCells;
    numYCells = b.numYCells;
    locationMap = new LinkedHashMap<>();
  }

  enum Obstacle implements RoadUser {
    INSTANCE {
      @Override
      public void initRoadUser(RoadModel model) {}
    }
  }

  @AutoValue
  static abstract class Position {

    abstract int x();

    abstract int y();

    static Position create(int x, int y) {
      return new AutoValue_GridRoadModel_Position(x, y);
    }

    static boolean areNeighbors(Position p1, Position p2) {
      return Math.abs(p1.x() - p2.x()) + Math.abs(p1.y() - p2.y()) == 1;
    }
  }

  public void addObstacle(Point pos) {

  }

  public void addObstacleRect(Point corner1, Point corner2) {

  }

  public void clear(Point pos) {
    locationMap.remove(point2LocObj(pos));
  }

  public void clearRect(Point corner1, Point corner2) {

  }

  public boolean isOccupied(Point pos) {
    return locationMap.containsKey(point2LocObj(pos));
  }

  public boolean areNeighbors(Point p1, Point p2) {
    return Position.areNeighbors(point2LocObj(p1), point2LocObj(p2));
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    checkArgument(!isOccupied(pos));
    super.addObjectAt(newObj, pos);
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser obj, Queue<Point> path,
      TimeLapse time) {

    final Point origin = getPosition(obj);
    final Point dest = path.peek();
    checkArgument(time.getTimeConsumed() == 0);
    checkArgument(!isOccupied(dest), "The destination %s is occupied.", dest);
    checkArgument(
        areNeighbors(origin, dest),
        "The destination %s is not a neighbor of the current position %s of %s.",
        dest, origin, obj);

    path.remove();
    objLocs.put(obj, point2LocObj(dest));

    final Measure<Double, Length> distTraveled = Measure.valueOf(
        toExternalDistConv.convert(cellSize), externalDistanceUnit);
    final Measure<Long, Duration> timeConsumed = Measure.valueOf(
        time.getTimeConsumed(), time.getTimeUnit());

    return new MoveProgress(distTraveled, timeConsumed, asList(origin));
  }

  @Override
  public ImmutableList<Point> getBounds() {
    final Point min = new Point(cellSize / -2, cellSize / -2);
    final Point max = new Point(numXCells * cellSize - cellSize / 2, numYCells
        * cellSize - cellSize / 2);
    return ImmutableList.of(min, max);
  }

  @Override
  protected Position point2LocObj(Point point) {
    final int x = DoubleMath.roundToInt(point.x / cellSize,
        RoundingMode.HALF_UP);
    final int y = DoubleMath.roundToInt(point.y / cellSize,
        RoundingMode.HALF_UP);
    return Position.create(x, y);
  }

  void checkBounds(Position pos) {
    checkElementIndex(pos.x(), numXCells);
    checkElementIndex(pos.y(), numYCells);
  }

  @Override
  protected Point locObj2point(Position locObj) {
    return new Point(locObj.x() * cellSize, locObj.y() * cellSize);
  }

  @Override
  public Point getRandomPosition(RandomGenerator rnd) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Point> getShortestPathTo(Point from, Point to) {
    // TODO Auto-generated method stub
    return null;
  }

  public double getCellSize() {
    return cellSize;
  }

  public int getNumXCells() {
    return numXCells;
  }

  public int getNumYCells() {
    return numYCells;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    Unit<Length> distanceUnit;
    Unit<Velocity> speedUnit;
    double cellSize;
    int numXCells;
    int numYCells;

    Builder() {
      distanceUnit = SI.METER;
      speedUnit = SI.METERS_PER_SECOND;
      cellSize = 1d;
      numXCells = 20;
      numYCells = 20;
    }

    public Builder setDistanceUnit(Unit<Length> distanceUnit) {
      this.distanceUnit = distanceUnit;
      return this;
    }

    public Builder setSpeedUnit(Unit<Velocity> speedUnit) {
      this.speedUnit = speedUnit;
      return this;
    }

    public Builder setCellSize(double cellSize) {
      this.cellSize = cellSize;
      return this;
    }

    public Builder setNumXCells(int numXCells) {
      this.numXCells = numXCells;
      return this;
    }

    public Builder setNumYCells(int numYCells) {
      this.numYCells = numYCells;
      return this;
    }

    public GridRoadModel build() {
      return new GridRoadModel(this);
    }
  }

  private static final class Factory implements
      Supplier<Map<Integer, RoadUser>> {
    Factory() {}

    @Override
    public Map<Integer, RoadUser> get() {
      return newLinkedHashMap();
    }
  }
}
