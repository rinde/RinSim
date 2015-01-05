package com.github.rinde.rinsim.experiment.base;

import com.google.common.base.Function;

public interface ExperimentRunnerFactory extends
    Function<SimArgs, ExperimentRunner> {

}
