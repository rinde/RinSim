/**
 * 
 */
package rinde.sim.central;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import rinde.sim.central.GlobalStateObject.VehicleState;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.problem.common.DefaultParcel;
import rinde.sim.problem.common.DefaultVehicle;

import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Converter {

    private Converter() {}

    public static GlobalStateObject convert(RoadModel rm, PDPModel pm,
            DefaultVehicle vehicle, Collection<DefaultParcel> availableParcels,
            long time) {
        final ImmutableSet<VehicleState> vehicles = ImmutableSet
                .of(new VehicleState(rm.getPosition(vehicle), toDefaultParcelSet(pm
                        .getContents(vehicle))));
        return new GlobalStateObject(toDefaultParcelSet(availableParcels), vehicles);
    }

    public static GlobalStateObject convert(RoadModel rm, PDPModel pm, long time) {

        final ImmutableSet<DefaultParcel> parcels = ImmutableSet.copyOf(rm
                .getObjectsOfType(DefaultParcel.class));
        final Set<DefaultVehicle> vehicles = rm
                .getObjectsOfType(DefaultVehicle.class);
        final ImmutableSet.Builder<VehicleState> vbuilder = ImmutableSet
                .builder();
        for (final DefaultVehicle v : vehicles) {
            vbuilder.add(new VehicleState(rm.getPosition(v), toDefaultParcelSet(pm
                    .getContents(v))));
        }

        return new GlobalStateObject(parcels, vbuilder.build());
    }

    // TODO convert GlobalStateObject to matrices

    static ImmutableSet<DefaultParcel> toDefaultParcelSet(
            Collection<? extends Parcel> parcels) {
        final ImmutableSet.Builder<DefaultParcel> builder = ImmutableSet
                .builder();
        for (final Parcel p : parcels) {
            checkArgument(p instanceof DefaultParcel, "this method expects DefaultParcels");
            builder.add((DefaultParcel) p);
        }
        return builder.build();
    }

    /**
     * Converts the list of points on a plane into a travel time matrix. For
     * distance between two points the euclidean distance is used, i.e. no
     * obstacles or graph structure. This method is unit agnostic, i.e. it can
     * be used with any unit of time and space.
     * @param points The set of points which will be converted to a travel time
     *            matrix.
     * @param distToTimeFactor The factor which with every distance will be
     *            multiplied to obtain the travel time.
     * @param rm The result of the multiplication needs to be rounded. The
     *            rounding mode indicates how numbers are rounded, see
     *            {@link RoundingMode} for the available options.
     * @return A <code>n x n</code> travel time matrix, where <code>n</code> is
     *         the size of the <code>points</code> list.
     */
    public static int[][] toTravelTimeMatrix(List<Point> points,
            double distToTimeFactor, RoundingMode rm) {
        checkArgument(points.size() >= 2);
        final int[][] matrix = new int[points.size()][points.size()];
        for (int i = 0; i < points.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (i != j) {
                    final int tt = DoubleMath.roundToInt(//
                    Point.distance(points.get(i), points.get(j))
                            * distToTimeFactor, rm);
                    matrix[i][j] = tt;
                    matrix[j][i] = tt;
                }
            }
        }
        return matrix;
    }
}
