package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.BATCHES;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.DRY_RUN;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.GUI;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.HELP;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.JPPF;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.LOCAL;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.REPETITIONS;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.SEED;
import static rinde.sim.pdptw.experiment.ExperimentCli.Handlers.THREADS;

import java.util.List;
import java.util.Map;

import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.Computers;
import rinde.sim.util.cli.CliException;
import rinde.sim.util.cli.CliMenu;
import rinde.sim.util.cli.CliOption;
import rinde.sim.util.cli.ICliOption;
import rinde.sim.util.cli.OptionHandler;
import rinde.sim.util.cli.Value;
import rinde.sim.util.io.FileProviderCli;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class ExperimentCli {

  private ExperimentCli() {}

  static boolean safeExecute(Experiment.Builder builder, String[] args) {
    return createMenu(builder).safeExecute(args);
  }

  static boolean execute(Experiment.Builder builder, String[] args)
      throws CliException {
    return createMenu(builder).execute(args);
  }

  static CliMenu<Experiment.Builder> createMenu(Experiment.Builder expBuilder) {
    final Map<String, MASConfiguration> configMap = createConfigMap(expBuilder);

    final CliMenu.Builder<Experiment.Builder> menuBuilder = CliMenu
        .builder(expBuilder);
    menuBuilder
        .commandLineSyntax("java -jar jarname.jar")
        .header("RinSim Experiment command line interface.")
        .footer("For more information see http://github.com/rinde/RinSim")
        .addGroup(
            BATCHES.createOption(expBuilder),
            THREADS.createOption(expBuilder))
        .addGroup(
            createIncludeOption(configMap),
            createExcludeOption(configMap))
        .addGroup(
            LOCAL.createOption(expBuilder),
            JPPF.createOption(expBuilder))
        .add(
            DRY_RUN.createOption(expBuilder),
            HELP.createOption(expBuilder),
            REPETITIONS.createOption(expBuilder),
            SEED.createOption(expBuilder),
            GUI.createOption(expBuilder));

    if (expBuilder.scenarioProviderBuilder.isPresent()) {
      menuBuilder.addSubMenu("s", "scenarios.",
          FileProviderCli
              .createDefaultMenuBuilder(expBuilder.scenarioProviderBuilder
                  .get()));
    }

    return menuBuilder.build();
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

  enum Handlers implements OptionHandler<Builder> {
    SEED {
      @Override
      public ICliOption<Builder> createOption(Experiment.Builder builder) {
        return CliOption.builder("s")
            .setLongName("seed")
            .description(
                "Sets the master random seed, default: ", builder.masterSeed,
                ".")
            .argNumber()
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        final Optional<Long> num = value.longValue();
        checkArgument(num.isPresent(),
            "Found: '%s' but expected an integer value.",
            value.stringValue());
        builder.withRandomSeed(num.get());
        return true;
      }

    },
    HELP {
      @Override
      public ICliOption<Builder> createOption(Experiment.Builder builder) {
        return CliOption.builder("h")
            .setLongName("help")
            .description("Print this message.")
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        return false;
      }
    },
    REPETITIONS {
      @Override
      public ICliOption<Builder> createOption(Builder builder) {
        return CliOption.builder("r")
            .setLongName("repetitions")
            .description(
                "Sets the number of repetitions of each setting, default: "
                , builder.repetitions)
            .argNumber()
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        final Optional<Long> num = value.longValue();
        checkArgument(num.isPresent(),
            "Found: '%s' but expected a strictly positive integer value.",
            value.stringValue());
        builder.repeat(num.get().intValue());
        return true;
      }

    },
    BATCHES {
      @Override
      public ICliOption<Builder> createOption(Builder builder) {
        return CliOption
            .builder("b")
            .setLongName("batches")
            .description(
                "Sets the number of batches to use in case of distributed computation, default: ",
                builder.numBatches,
                ". This option can not be used together with --threads.")
            .argNumber()
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        final Optional<Long> num = value.longValue();
        checkArgument(num.isPresent(),
            "Found: '%s' but expected a strictly positive integer value.",
            value.stringValue());
        builder.numBatches(num.get().intValue());
        return true;
      }

    },
    THREADS {
      @Override
      public ICliOption<Builder> createOption(Builder builder) {
        return CliOption
            .builder("t")
            .setLongName("threads")
            .description(
                "Sets the number of threads to use in case of local computation, default: ",
                builder.numThreads,
                ". This option can not be used together with --batches.")
            .argNumber()
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        final Optional<Long> num = value.longValue();
        checkArgument(num.isPresent(),
            "Found: '%s' but expected a strictly positive integer value.",
            value.stringValue());
        builder.withThreads(num.get().intValue());
        return true;
      }
    },
    DRY_RUN {
      @Override
      public ICliOption<Builder> createOption(Builder builder) {
        return CliOption.builder("dr")
            .setLongName("dry-run")
            .description(
                "Will perform a 'dry run' of the experiment without doing any"
                    + " actual simulations. A detailed description of the "
                    + "experiment setup will be printed. If an additional "
                    + "argument 'v' or 'verbose' is supplied, more details of"
                    + " the experiment will be printed.")
            .argString()
            .argOptional()
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        final boolean verbose = value.hasValue();
        if (verbose) {
          checkArgument(
              "v".equalsIgnoreCase(value.stringValue())
                  || "verbose".equalsIgnoreCase(value.stringValue()),
              "only accepts 'v', 'verbose' or no argument, not '%s'.",
              value.stringValue());
        }
        builder.dryRun(verbose);
        return true;
      }
    },
    JPPF {
      @Override
      public ICliOption<Builder> createOption(Builder builder) {
        return CliOption
            .builder("j")
            .setLongName("jppf")
            .description(
                "Compute the experiment using the JPPF framework",
                builder.getComputer() == Computers.DISTRIBUTED ? " (default)"
                    : "",
                ". This option can not be used together with the --local option.")
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        builder.computeDistributed();
        return true;
      }
    },
    LOCAL {
      @Override
      public ICliOption<Builder> createOption(Builder builder) {
        return CliOption
            .builder("l")
            .setLongName("local")
            .description(
                "Compute the experiment locally",
                builder.getComputer() == Computers.LOCAL ? " (default)" : "",
                ". This option can not be used together with the --jppf option.")
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        builder.computeLocal();
        return true;
      }

    },
    GUI {
      @Override
      public ICliOption<Builder> createOption(Builder builder) {
        return CliOption
            .builder("g")
            .setLongName("show-gui")
            .description(
                "Starts the gui for each simulation when 'true' is supplied, hides it when 'false' is supplied. By default the gui is ",
                builder.showGui ? "" : "not",
                " shown. The gui can only be shown if the computation is performed locally and the number of threads is set to 1.")
            .argString()
            .build(this);
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        final boolean b = Boolean.parseBoolean(value.stringValue());
        if (b) {
          builder.showGui();
        } else {
          builder.showGui = false;
        }
        return true;
      }

    };

    public abstract ICliOption<Builder> createOption(Experiment.Builder builder);

  }

  static ICliOption<Builder> createIncludeOption(
      Map<String, MASConfiguration> configMap) {
    final StringBuilder sb = new StringBuilder();
    sb.append("The following configurations can be included in the experiment"
        + " setup. If this option is not used all configurations are automatically "
        + "included. The configurations:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, configMap);
    sb.append("\nThe options should be given as a comma ',' separated list. This option "
        + "can not be used together with --exclude.");
    return CliOption.builder("i")
        .setLongName("include")
        .description(sb.toString())
        .argNumberList()
        .build(new IncludeHandler(configMap));
  }

  static class IncludeHandler implements OptionHandler<Builder> {
    private final Map<String, MASConfiguration> configMap;

    IncludeHandler(Map<String, MASConfiguration> map) {
      configMap = map;
    }

    @Override
    public boolean execute(Builder builder, Value value) {
      final List<String> keys = value.asList();
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
      return true;
    }
  }

  static ICliOption<Builder> createExcludeOption(
      Map<String, MASConfiguration> configMap) {
    final StringBuilder sb = new StringBuilder();
    sb.append("The following configurations can be excluded from the experiment"
        + " setup. If this option is not used all configurations are automatically "
        + "included. The configurations:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, configMap);
    sb.append("\nThe options should be given as a comma ',' separated list. This option "
        + "can not be used together with --include.");
    return CliOption.builder("e")
        .setLongName("exclude")
        .description(sb.toString())
        .argStringList()
        .build(new ExcludeHandler(configMap));
  }

  static class ExcludeHandler implements OptionHandler<Builder> {
    private final Map<String, MASConfiguration> configMap;

    protected ExcludeHandler(Map<String, MASConfiguration> map) {
      configMap = map;
    }

    @Override
    public boolean execute(Builder builder, Value value) {
      final List<String> keys = value.asList();
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
      return true;
    }
  }
}
