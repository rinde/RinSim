package com.github.rinde.rinsim.pdptw.common;

import com.google.common.base.Joiner;

public enum TestObjectiveFunction implements ObjectiveFunction {
  INSTANCE {
    @Override
    public boolean isValidResult(StatisticsDTO stats) {
      return stats.totalParcels == stats.totalDeliveries
          && stats.totalParcels == stats.totalPickups;
    }

    @Override
    public double computeCost(StatisticsDTO stats) {
      return stats.totalDistance;
    }

    @Override
    public String printHumanReadableFormat(StatisticsDTO stats) {
      return Joiner.on("").join("{dist=", stats.totalDistance, ",parcels=",
          stats.totalParcels, "}");
    }
  }
}
