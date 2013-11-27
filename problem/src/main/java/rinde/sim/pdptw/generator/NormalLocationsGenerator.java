package rinde.sim.pdptw.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.math.RoundingMode;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.math.DoubleMath;

public class NormalLocationsGenerator implements LocationsGenerator {

  private final double size;
  private final double bin;
  private final double std;
  private final ImmutableSortedSet<Double> probabilities;
  private final ImmutableList<Double> probabilitiesList;

  /**
   * @param envSize The size of the environment (in km), the area is defined as
   *          size^2 km2.
   * @param relativeStd This is the size of the standard deviation relative to
   *          envSize.
   * @param binSize Determines the size (in km) of the bins that will be used to
   *          discretize the Normal distribution.
   */
  public NormalLocationsGenerator(double envSize, double relativeStd,
      double binSize) {
    checkArgument(envSize >= 0.15);
    checkArgument(envSize >= 0);
    checkArgument(relativeStd >= 0);
    checkArgument(binSize >= 0);
    size = envSize;
    std = relativeStd;
    bin = binSize;

    // generate discrete approximation of Normal distribution
    final NormalDistribution nd = new NormalDistribution(size / 2d, size * std);
    final int numBins = DoubleMath
        .roundToInt(envSize / binSize, RoundingMode.UP);
    final ImmutableSortedSet.Builder<Double> b = ImmutableSortedSet
        .naturalOrder();
    b.add(0d);
    for (int i = 1; i < numBins; i++) {
      b.add(nd.cumulativeProbability(i * binSize));
    }
    b.add(1d);
    probabilities = b.build();
    probabilitiesList = probabilities.asList();
  }

  /**
   * @param numOrders The number of orders for which locations need to be
   *          generated. Note that for each order two locations are generated:
   *          the pickup location and the delivery location.
   */
  @Override
  public ImmutableList<Point> generate(int numOrders, RandomGenerator rng) {
    final ImmutableList.Builder<Point> b = ImmutableList.builder();
    for (int i = 0; i < numOrders * 2; i++) {
      final double x = sample(rng);
      final double y = sample(rng);
      checkState(x >= 0 && x < size, "Invalid x value %s, %s", i, x);
      checkState(y >= 0 && y < size, "Invalid y value %s, %s", i, y);
      b.add(new Point(x, y));
    }
    return b.build();
  }

  public double[][] getHistogram() {
    final int numProbs = probabilities.size();
    final double[][] histogram = new double[numProbs][2];

    for (int i = 0; i < numProbs; i++) {
      histogram[i][0] = i * bin;
      if (i == numProbs - 1) {
        histogram[i][1] = (1d - probabilitiesList.get(i)) * (1 / bin);
      } else {
        histogram[i][1] = (probabilitiesList.get(i + 1) - probabilitiesList
            .get(i)) * (1 / bin);
      }
    }
    return histogram;
  }

  public double getEnvSize() {
    return size;
  }

  public double getBinSize() {
    return bin;
  }

  public double getRelativeStd() {
    return std;
  }

  private double sample(RandomGenerator rng) {
    final int index = probabilitiesList.indexOf(probabilities.floor(rng
        .nextDouble()));
    final double lb = index * bin;
    return lb + rng.nextDouble() * bin;
  }
}
