/**
 * 
 */
package rinde.sim.problem.gendreau06;

import rinde.sim.problem.common.ObjectiveFunction;
import rinde.sim.problem.common.StatsTracker.StatisticsDTO;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Gendreau06ObjectiveFunction implements ObjectiveFunction {

	protected final double alpha;
	protected final double beta;

	public Gendreau06ObjectiveFunction() {
		alpha = 1d;
		beta = 1d;
	}

	/**
	 * All parcels need to be delivered, all vehicles need to be back at the
	 * depot.
	 */
	@Override
	public boolean isValidResult(StatisticsDTO stats) {
		return stats.totalParcels == stats.acceptedParcels && stats.totalParcels == stats.totalPickups
				&& stats.totalParcels == stats.totalDeliveries && stats.simFinish
				&& stats.totalVehicles == stats.vehiclesAtDepot;
	}

	/**
	 * Computes the cost according to the definition of the paper: <i>the cost
	 * function used throughout this work is to minimize a weighted sum of three
	 * different criteria: total travel time, sum of lateness over all pick-up
	 * and delivery locations and sum of overtime over all vehicles</i>. The
	 * function is defined as:
	 * <code>sum(Tk) + alpha sum(max(0,tv-lv)) + beta sum(max(0,tk-l0))</code>
	 * Where: Tk is the total travel time on route Rk, alpha and beta are
	 * weighting parameters which were set to 1 in the paper. The definition of
	 * lateness: <code>max(0,lateness)</code> is commonly referred to as
	 * <i>tardiness</i>.
	 * 
	 */
	@Override
	public double computeCost(StatisticsDTO stats) {
		// FIXME distance / speed?
		final double totalTravelTime = (stats.totalDistance / 30.0) * 3600000.0;
		final long sumTardiness = stats.pickupTardiness + stats.deliveryTardiness;
		final long overTime = stats.overTime;
		return totalTravelTime + (alpha * sumTardiness) + (beta * overTime);
	}

}
