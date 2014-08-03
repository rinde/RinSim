package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;

import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.Computers;
import rinde.sim.util.cli.ArgHandler;
import rinde.sim.util.cli.ArgumentParser;
import rinde.sim.util.cli.CliException;
import rinde.sim.util.cli.CliMenu;
import rinde.sim.util.cli.CliOption;
import rinde.sim.util.cli.CliOption.CliOptionArg;
import rinde.sim.util.cli.CliOption.CliOptionNoArg;
import rinde.sim.util.cli.NoArgHandler;
import rinde.sim.util.io.FileProviderCli;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class ExperimentCli {

  private ExperimentCli() {}

  static Optional<String> safeExecute(Experiment.Builder builder, String[] args) {
    return createMenu(builder).safeExecute(args);
  }

  static Optional<String> execute(Experiment.Builder builder, String[] args)
      throws CliException {
    return createMenu(builder).execute(args);
  }

  static CliMenu createMenu(Experiment.Builder builder) {
    final Map<String, MASConfiguration> cfgMap = createConfigMap(builder);

    final CliMenu.Builder menuBuilder = CliMenu.builder();
    menuBuilder
        .commandLineSyntax("java -jar jarname.jar <options>")
        .header("RinSim Experiment command line interface.")
        .footer("For more information see http://github.com/rinde/RinSim")
        .openGroup()
        .add(createBatchesOpt(builder), builder, BATCHES_HANDLER)
        .add(createThreadsOpt(builder), builder, THREADS_HANDLER)
        .openGroup()
        .add(createIncludeOpt(cfgMap), builder, new IncludeHandler(cfgMap))
        .add(createExcludeOpt(cfgMap), builder, new ExcludeHandler(cfgMap))
        .openGroup()
        .add(createLocalOpt(builder), builder, LOCAL_HANDLER)
        .add(createJppfOpt(builder), builder, JPPF_HANDLER)
        .closeGroup()
        .add(createDryRunOpt(builder), builder, DRY_RUN_HANDLER)
        .add(createRepetitionsOpt(builder), builder, REPETITIONS_HANDLER)
        .add(createSeedOption(builder), builder, SEED_HANDLER)
        .add(createGuiOpt(builder), builder, GUI_HANDLER)
        .addHelpOption("h", "help", "Print this message.");

    if (builder.scenarioProviderBuilder.isPresent()) {
      menuBuilder.addSubMenu("s", "scenarios.",
          FileProviderCli
              .createDefaultMenu(builder.scenarioProviderBuilder
                  .get()));
    }

    return menuBuilder.build();
  }

  static CliOptionArg<Integer> createBatchesOpt(Builder expBuilder) {
    return CliOption
        .builder("b", ArgumentParser.INTEGER)
        .longName("batches")
        .description(
            "Sets the number of batches to use in case of distributed computation, default: ",
            expBuilder.numBatches,
            ". This option can not be used together with --threads.")
        .build();
  }

  static Map<String, MASConfiguration> createConfigMap(
      Experiment.Builder builder) {
    final List<MASConfiguration> configs = ImmutableList
        .copyOf(builder.configurationsSet);
    final ImmutableMap.Builder<String, MASConfiguration> mapBuilder = ImmutableMap
        .builder();
    for (int i = 0; i < configs.size(); i++) {
      mapBuilder.put("c" + i, configs.get(i));
    }
    return mapBuilder.build();
  }

  static CliOptionArg<Integer> createThreadsOpt(Experiment.Builder builder) {
    return CliOption
        .builder("t", ArgumentParser.INTEGER)
        .longName("threads")
        .description(
            "Sets the number of threads to use in case of local computation, default: ",
            builder.numThreads,
            ". This option can not be used together with --batches.")
        .build();
  }

  static CliOptionArg<Long> createSeedOption(Experiment.Builder builder) {
    return CliOption.builder("s", ArgumentParser.LONG)
        .longName("seed")
        .description(
            "Sets the master random seed, default: ", builder.masterSeed, ".")
        .build();
  }

  static ArgHandler<Builder, Integer> BATCHES_HANDLER = new ArgHandler<Builder, Integer>() {
    @Override
    public void execute(Builder subject, Optional<Integer> value) {
      subject.numBatches(value.get());
    }
  };

  static ArgHandler<Builder, Long> SEED_HANDLER = new ArgHandler<Builder, Long>() {
    @Override
    public void execute(Builder builder, Optional<Long> value) {
      builder.withRandomSeed(value.get());
    }
  };

  static ArgHandler<Builder, Integer> THREADS_HANDLER = new ArgHandler<Builder, Integer>() {
    @Override
    public void execute(Builder builder, Optional<Integer> value) {
      builder.withThreads(value.get());
    }
  };

  static CliOptionArg<Integer> createRepetitionsOpt(Builder builder) {
    return CliOption
        .builder("r", ArgumentParser.INTEGER)
        .longName("repetitions")
        .description(
            "Sets the number of repetitions of each setting, default: ",
            builder.repetitions)
        .build();
  }

  static ArgHandler<Builder, Integer> REPETITIONS_HANDLER = new ArgHandler<Experiment.Builder, Integer>() {
    @Override
    public void execute(Builder builder, Optional<Integer> value) {
      builder.repeat(value.get());
    }
  };

  static CliOptionArg<String> createDryRunOpt(Builder builder) {
    return CliOption.builder("dr", ArgumentParser.STRING)
        .longName("dry-run")
        .description(
            "Will perform a 'dry run' of the experiment without doing any"
                + " actual simulations. A detailed description of the "
                + "experiment setup will be printed. If an additional "
                + "argument 'v' or 'verbose' is supplied, more details of"
                + " the experiment will be printed.")
        .argOptional()
        .build();
  }

  static ArgHandler<Builder, String> DRY_RUN_HANDLER = new ArgHandler<Experiment.Builder, String>() {
    @Override
    public void execute(Builder builder, Optional<String> value) {
      if (value.isPresent()) {
        checkArgument(
            "v".equalsIgnoreCase(value.get())
                || "verbose".equalsIgnoreCase(value.get()),
            "only accepts 'v', 'verbose' or no argument, not '%s'.",
            value.get());
      }
      builder.dryRun(value.isPresent());
    }
  };

  static CliOptionNoArg createJppfOpt(Builder builder) {
    return CliOption
        .builder("j")
        .longName("jppf")
        .description(
            "Compute the experiment using the JPPF framework",
            builder.getComputer() == Computers.DISTRIBUTED ? " (default)"
                : "",
            ". This option can not be used together with the --local option.")
        .build();
  }

  static NoArgHandler<Builder> JPPF_HANDLER = new NoArgHandler<Experiment.Builder>() {
    @Override
    public void execute(Builder builder) {
      builder.computeDistributed();
    }
  };

  static CliOptionNoArg createLocalOpt(Builder builder) {
    return CliOption.builder("l")
        .longName("local")
        .description(
            "Compute the experiment locally",
            builder.getComputer() == Computers.LOCAL ? " (default)" : "",
            ". This option can not be used together with the --jppf option.")
        .build();
  }

  static NoArgHandler<Builder> LOCAL_HANDLER = new NoArgHandler<Builder>() {
    @Override
    public void execute(Builder builder) {
      builder.computeLocal();
    }
  };

  static CliOptionArg<Boolean> createGuiOpt(Builder builder) {
    return CliOption
        .builder("g", ArgumentParser.BOOLEAN)
        .longName("show-gui")
        .description(
            "Starts the gui for each simulation when 'true' is supplied, hides it when 'false' is supplied. By default the gui is ",
            builder.showGui ? "" : "not",
            " shown. The gui can only be shown if the computation is performed locally and the number of threads is set to 1.")
        .build();
  }

  static ArgHandler<Builder, Boolean> GUI_HANDLER = new ArgHandler<Experiment.Builder, Boolean>() {
    @Override
    public void execute(Builder builder, Optional<Boolean> value) {
      if (value.isPresent() && value.get()) {
        builder.showGui();
      } else {
        builder.showGui = false;
      }
    }
  };

  static CliOptionArg<List<String>> createIncludeOpt(
      Map<String, MASConfiguration> configMap) {
    return CliOption
        .builder("i", ArgumentParser.STRING_LIST)
        .longName("include")
        .description(
            "The following configurations can be included in the experiment",
            " setup. If this option is not used all configurations are automatically ",
            "included. The configurations:\n",
            Joiner.on("\n").withKeyValueSeparator(" = ").join(configMap),
            "\nThe options should be given as a comma ',' separated list. This option ",
            "can not be used together with --exclude.")
        .build();
  }

  static class IncludeHandler implements ArgHandler<Builder, List<String>> {
    private final Map<String, MASConfiguration> configMap;

    IncludeHandler(Map<String, MASConfiguration> map) {
      configMap = map;
    }

    @Override
    public void execute(Builder builder, Optional<List<String>> value) {
      final List<String> keys = value.get();
      final List<MASConfiguration> configs = newArrayList();
      checkArgument(
          keys.size() <= configMap.size(),
          "Too many configurations, at most %s configurations can be included.",
          configMap.size());
      for (final String k : keys) {
        checkArgument(configMap.containsKey(k),
            "The key '%s' is not valid. Valid keys: %s.", k, configMap.keySet());
        configs.add(configMap.get(k));
      }
      builder.configurationsSet.retainAll(configs);
    }
  }

  static CliOptionArg<List<String>> createExcludeOpt(
      Map<String, MASConfiguration> configMap) {
    return CliOption
        .builder("e", ArgumentParser.STRING_LIST)
        .longName("exclude")
        .description(
            "The following configurations can be excluded from the experiment",
            " setup. If this option is not used all configurations are automatically ",
            "included. The configurations:\n",
            Joiner.on("\n").withKeyValueSeparator(" = ").join(configMap),
            "\nThe options should be given as a comma ',' separated list. This option ",
            "can not be used together with --include.")
        .build();
  }

  static class ExcludeHandler implements ArgHandler<Builder, List<String>> {
    private final Map<String, MASConfiguration> configMap;

    protected ExcludeHandler(Map<String, MASConfiguration> map) {
      configMap = map;
    }

    @Override
    public void execute(Builder builder, Optional<List<String>> value) {
      final List<String> keys = value.get();
      final List<MASConfiguration> configs = newArrayList();
      checkArgument(
          keys.size() < configMap.size(),
          "Too many configurations, at most %s configurations can be excluded.",
          configMap.size() - 1);
      for (final String k : keys) {
        checkArgument(configMap.containsKey(k),
            "The key '%s' is not valid. Valid keys: %s.", k, configMap.keySet());
        configs.add(configMap.get(k));
      }
      builder.configurationsSet.removeAll(configs);
    }
  }
}
