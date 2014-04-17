package rinde.sim.pdptw.scenario;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.SupplierRngs;

import com.google.common.collect.ImmutableList;

public class Depots {
  private static final DepotGenerator SINGLE_CENTERED_DEPOT_GENERATOR = new DepotGenerator() {
    @Override
    public Iterable<? extends AddDepotEvent> generate(long seed, Point center) {
      return ImmutableList.of(new AddDepotEvent(-1, center));
    }
  };

  public static DepotGenerator singleCenteredDepot() {
    return SINGLE_CENTERED_DEPOT_GENERATOR;
  }

  public static Builder builder() {
    return new Builder();
  }

  public interface DepotGenerator {
    Iterable<? extends AddDepotEvent> generate(long seed, Point center);
  }

  public static class Builder {
    SupplierRng<Point> positions;
    SupplierRng<Integer> numberOfDepots;
    SupplierRng<Long> times;

    Builder() {
      positions = SupplierRngs.constant(new Point(0d, 0d));
      numberOfDepots = SupplierRngs.constant(1);
      times = SupplierRngs.constant(-1L);
    }

    public Builder positions(SupplierRng<Point> ps) {
      positions = ps;
      return this;
    }

    public Builder numerOfDepots(SupplierRng<Integer> nd) {
      numberOfDepots = nd;
      return this;
    }

    public Builder times(SupplierRng<Long> ts) {
      times = ts;
      return this;
    }

    public DepotGenerator build() {
      return new MultiDepotGenerator(this);
    }
  }

  private static class MultiDepotGenerator implements DepotGenerator {
    private final SupplierRng<Point> positions;
    private final SupplierRng<Integer> numberOfDepots;
    private final SupplierRng<Long> times;
    private final RandomGenerator rng;

    MultiDepotGenerator(Builder b) {
      positions = b.positions;
      numberOfDepots = b.numberOfDepots;
      times = b.times;
      rng = new MersenneTwister();
    }

    @Override
    public Iterable<? extends AddDepotEvent> generate(long seed, Point center) {
      rng.setSeed(seed);
      final int num = numberOfDepots.get(rng.nextLong());
      final ImmutableList.Builder<AddDepotEvent> builder = ImmutableList
          .builder();
      for (int i = 0; i < num; i++) {
        final long time = times.get(rng.nextLong());
        final Point position = positions.get(rng.nextLong());
        builder.add(new AddDepotEvent(time, position));
      }
      return builder.build();
    }
  }
}
