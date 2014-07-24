package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.List;
import java.util.Map;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.Computers;
import rinde.sim.util.io.AbstractMenuOption;
import rinde.sim.util.io.CliException;
import rinde.sim.util.io.MenuOption;
import rinde.sim.util.io.OptionBuilder;
import rinde.sim.util.io.Value;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class ExperimentCli {

  static boolean safeExecute(Experiment.Builder builder, String[] args) {
    final CliMenu menu = new CliMenu(builder);
    return menu.safeExecute(builder, args);
  }

  static boolean execute(Experiment.Builder builder, String[] args)
      throws CliException {
    final CliMenu menu = new CliMenu(builder);
    return menu.execute(builder, args);
  }

  static class CliMenu {
    final Options options;
    final Map<String, MASConfiguration> configMap;
    Map<String, MenuOption<Builder>> optionMap;

    CliMenu(Experiment.Builder builder) {
      configMap = createConfigMap(builder);
      options = new Options();

      // define options and groups
      final List<MenuOption<Builder>> nonConflicting = ImmutableList
          .<MenuOption<Builder>> of(
              MenuOptions.DRY_RUN,
              MenuOptions.HELP,
              MenuOptions.REPETITIONS,
              MenuOptions.SEED,
              MenuOptions.GUI);
      final List<ImmutableList<? extends MenuOption<Builder>>> groups = ImmutableList
          .of(ImmutableList.of(MenuOptions.BATCHES, MenuOptions.THREADS),
              ImmutableList.of(new Include(configMap), new Exclude(configMap)),
              ImmutableList.of(MenuOptions.LOCAL, MenuOptions.JPPF)
          );
      final Map<String, MenuOption<Builder>> optMap = newLinkedHashMap();
      // NOTE it is important that LOCAL and JPPF end up *before* DRY_RUN in the
      // optionMap
      for (final List<? extends MenuOption<Builder>> group : groups) {
        final OptionGroup g = new OptionGroup();
        for (final MenuOption<Builder> mo : group) {
          g.addOption(mo.createOption(builder));
          optMap.put(mo.getShortName(), mo);
        }
        options.addOptionGroup(g);
      }
      for (final MenuOption mo : nonConflicting) {
        options.addOption(mo.createOption(builder));
        optMap.put(mo.getShortName(), mo);
      }
      optionMap = ImmutableMap.copyOf(optMap);
    }

    boolean safeExecute(Experiment.Builder builder, String[] args) {
      try {
        return execute(builder, args);
      } catch (final CliException e) {
        System.err.println(e.getMessage());
        printHelp();
        return false;
      }
    }

    boolean execute(Experiment.Builder builder, String[] args)
        throws CliException {
      final CommandLineParser parser = new BasicParser();
      final CommandLine line;
      try {
        line = parser.parse(options, args);
      } catch (final MissingArgumentException e) {
        throw new CliException(e.getMessage(), e, optionMap.get(e
            .getOption().getOpt()));
      } catch (final AlreadySelectedException e) {
        throw new CliException(e.getMessage(), e, optionMap.get(e
            .getOption().getOpt()));
      } catch (final ParseException e) {
        throw new CliException("Parsing failed. Reason: " + e.getMessage(), e);
      }

      for (final MenuOption option : optionMap.values()) {
        if (line.hasOption(option.getShortName())
            || line.hasOption(option.getLongName())) {
          final Value v = new Value(line, option);
          try {
            if (!option.execute(builder, v)) {
              printHelp();
              return false;
            }
          } catch (final IllegalArgumentException | IllegalStateException e) {
            throw new CliException("Problem with " + v.optionUsed()
                + " option: " + e.getMessage(), e, option);
          }
        }
      }
      return true;
    }

    public void printHelp() {
      final HelpFormatter formatter = new HelpFormatter();
      formatter
          .printHelp(
              "java -jar jarname.jar",
              "RinSim Experiment command line interface.",
              options,
              "For more information see http://github.com/rinde/RinSim");
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
  }

  enum MenuOptions implements MenuOption<Builder> {
    SEED("s", "seed") {
      @Override
      public Option createOption(Experiment.Builder builder) {
        return OptionBuilder.optionBuilder(this)
            .description(
                "Sets the master random seed, default: ", builder.masterSeed,
                ".")
            .numberArg()
            .build();
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
    HELP("h", "help") {
      @Override
      public Option createOption(Experiment.Builder builder) {
        return OptionBuilder.optionBuilder(this)
            .description("Print this message.").build();
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        return false;
      }
    },
    REPETITIONS("r", "repetitions") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder.optionBuilder(this)
            .description(
                "Sets the number of repetitions of each setting, default: "
                , builder.repetitions)
            .numberArg()
            .build();
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
    BATCHES("b", "batches") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder
            .optionBuilder(this)
            .description(
                "Sets the number of batches to use in case of distributed computation, default: ",
                builder.numBatches,
                ". This option can not be used together with --threads.")
            .numberArg()
            .build();
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
    THREADS("t", "threads") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder
            .optionBuilder(this)
            .description(
                "Sets the number of threads to use in case of local computation, default: ",
                builder.numThreads,
                ". This option can not be used together with --batches.")
            .numberArg()
            .build();
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
    DRY_RUN("dr", "dry-run") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder.optionBuilder(this)
            .description(
                "Will perform a 'dry run' of the experiment without doing any"
                    + " actual simulations. A detailed description of the "
                    + "experiment setup will be printed. If an additional "
                    + "argument 'v' or 'verbose' is supplied, more details of"
                    + " the experiment will be printed.")
            .stringArg()
            .optionalArg()
            .build();
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        final boolean verbose = value.hasValue();
        if (verbose) {
          checkArgument(
              value.stringValue().equalsIgnoreCase("v")
                  || value.stringValue().equalsIgnoreCase("verbose"),
              "only accepts 'v', 'verbose' or no argument, not '%s'.",
              value.stringValue());
        }
        builder.dryRun(verbose);
        return true;
      }
    },
    JPPF("j", "jppf") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder
            .optionBuilder(this)
            .description(
                "Compute the experiment using the JPPF framework",
                builder.getComputer() == Computers.DISTRIBUTED ? " (default)"
                    : "",
                ". This option can not be used together with the --local option.")
            .build();
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        builder.computeDistributed();
        return true;
      }
    },
    LOCAL("l", "local") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder
            .optionBuilder(this)
            .description(
                "Compute the experiment locally",
                builder.getComputer() == Computers.LOCAL ? " (default)" : "",
                ". This option can not be used together with the --jppf option.")
            .build();
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        builder.computeLocal();
        return true;
      }

    },
    GUI("g", "show-gui") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder
            .optionBuilder(this)
            .description(
                "Starts the gui for each simulation when 'true' is supplied, hides it when 'false' is supplied. By default the gui is ",
                builder.showGui ? "" : "not",
                " shown. The gui can only be shown if the computation is performed locally and the number of threads is set to 1.")
            .stringArg()
            .build();
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

    private final String shortName;
    private final String longName;

    MenuOptions(String sn, String ln) {
      shortName = sn;
      longName = ln;
    }

    @Override
    public String getShortName() {
      return shortName;
    }

    @Override
    public String getLongName() {
      return longName;
    }
  }

  static class Include extends AbstractMenuOption<Builder> {
    private final Map<String, MASConfiguration> configMap;

    Include(Map<String, MASConfiguration> map) {
      super("i", "include");
      configMap = map;
    }

    @Override
    public Option createOption(Builder builder) {
      final StringBuilder sb = new StringBuilder();
      sb.append("The following configurations can be included in the experiment"
          + " setup. If this option is not used all configurations are automatically "
          + "included. The configurations:\n");
      Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, configMap);
      sb.append("\nThe options should be given as a comma ',' separated list. This option "
          + "can not be used together with --exclude.");
      return OptionBuilder.optionBuilder(this)
          .description(sb.toString())
          .numberArgList()
          .build();
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

  static class Exclude extends AbstractMenuOption<Builder> {
    private final Map<String, MASConfiguration> configMap;

    protected Exclude(Map<String, MASConfiguration> map) {
      super("e", "exclude");
      configMap = map;
    }

    @Override
    public Option createOption(Builder builder) {
      final StringBuilder sb = new StringBuilder();
      sb.append("The following configurations can be excluded from the experiment"
          + " setup. If this option is not used all configurations are automatically "
          + "included. The configurations:\n");
      Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, configMap);
      sb.append("\nThe options should be given as a comma ',' separated list. This option "
          + "can not be used together with --include.");
      return OptionBuilder.optionBuilder(this)
          .description(sb.toString())
          .stringArgList()
          .build();
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
