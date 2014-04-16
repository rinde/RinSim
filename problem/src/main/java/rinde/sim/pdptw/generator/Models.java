package rinde.sim.pdptw.generator;

import javax.measure.Measure;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.DefaultPDPModel;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.twpolicy.TimeWindowPolicy;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.PDPRoadModel;

import com.google.common.base.Supplier;

public final class Models {

  private Models() {}

  public static ModelSupplierSupplier<RoadModel> roadModel(double maxSpeed,
      boolean allowDiversion) {
    return new DefaultRoadModelSupplier(maxSpeed, allowDiversion);
  }

  public static Supplier<PDPModel> pdpModel(TimeWindowPolicy twp) {
    return new PDPModelSupplier(twp);
  }

  public static <T extends Model<?>> ModelSupplierSupplier<T> adapt(
      Supplier<T> sup) {
    return new Adapter<T>(sup);
  }

  public interface ModelSupplierSupplier<T extends Model<?>> {
    Supplier<T> get(ScenarioGenerator sg);
  }

  private static class Adapter<T extends Model<?>> implements
      ModelSupplierSupplier<T> {
    private final Supplier<T> supplier;

    Adapter(Supplier<T> sup) {
      supplier = sup;
    }

    @Override
    public Supplier<T> get(ScenarioGenerator sg) {
      return supplier;
    }
  }

  private static class PDPModelSupplier implements Supplier<PDPModel> {
    private final TimeWindowPolicy timeWindowPolicy;

    PDPModelSupplier(TimeWindowPolicy twp) {
      timeWindowPolicy = twp;
    }

    @Override
    public PDPModel get() {
      return new DefaultPDPModel(timeWindowPolicy);
    }
  }

  private static class DefaultRoadModelSupplier implements
      ModelSupplierSupplier<RoadModel> {
    final double speed;
    final boolean diversion;

    DefaultRoadModelSupplier(double maxSpeed, boolean allowDiversion) {
      speed = maxSpeed;
      diversion = allowDiversion;
    }

    @Override
    public Supplier<RoadModel> get(final ScenarioGenerator sg) {
      final Point min = sg.getMin();
      final Point max = sg.getMax();

      return new Supplier<RoadModel>() {
        @Override
        public RoadModel get() {
          return new PDPRoadModel(new PlaneRoadModel(
              min,
              max,
              sg.getDistanceUnit(),
              Measure.valueOf(speed, sg.getSpeedUnit())),
              diversion);
        }
      };
    }
  }
}
