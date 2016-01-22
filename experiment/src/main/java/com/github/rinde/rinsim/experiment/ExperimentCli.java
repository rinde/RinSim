/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.cli.ArgHandler;
import com.github.rinde.rinsim.cli.ArgumentParser;
import com.github.rinde.rinsim.cli.Menu;
import com.github.rinde.rinsim.cli.NoArgHandler;
import com.github.rinde.rinsim.cli.Option;
import com.github.rinde.rinsim.cli.Option.OptionArg;
import com.github.rinde.rinsim.cli.Option.OptionNoArg;
import com.github.rinde.rinsim.experiment.Experiment.Builder;
import com.github.rinde.rinsim.experiment.Experiment.Computers;
import com.github.rinde.rinsim.io.FileProviderCli;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Defines a command-line interface for {@link Experiment.Builder}.
 * @author Rinde van Lon
 */
public final class ExperimentCli {

  static final String DEFAULT_LABEL = " (default)";
  static final String S = "s";

  static final ArgHandler<Builder, Integer> BATCHES_HANDLER =
      new ArgHandler<Builder, Integer>() {
        @Override
        public void execute(Builder subject, Optional<Integer> value) {
          subject.numBatches(value.get());
        }
      };

  static final ArgHandler<Builder, String> DRY_RUN_HANDLER =
      new ArgHandler<Experiment.Builder, String>() {
        @Override
        public void execute(Builder builder, Optional<String> value) {
          if (value.isPresent()) {
            checkArgument(
                "v".equalsIgnoreCase(value.get())
                    || "verbose".equalsIgnoreCase(value.get()),
                "only accepts 'v', 'verbose' or no argument, not '%s'.",
                value.get());
          }
          builder.dryRun(value.isPresent(), System.out, System.err);
        }
      };

  static final ArgHandler<Builder, Boolean> GUI_HANDLER =
      new ArgHandler<Experiment.Builder, Boolean>() {
        @Override
        public void execute(Builder builder, Optional<Boolean> value) {
          builder.showGui(value.isPresent() && value.get());
        }
      };

  static final NoArgHandler<Builder> JPPF_HANDLER =
      new NoArgHandler<Experiment.Builder>() {
        @Override
        public void execute(Builder builder) {
          builder.computeDistributed();
        }
      };

  static final NoArgHandler<Builder> LOCAL_HANDLER =
      new NoArgHandler<Builder>() {
        @Override
        public void execute(Builder builder) {
          builder.computeLocal();
        }
      };

  static final ArgHandler<Builder, Integer> REPETITIONS_HANDLER =
      new ArgHandler<Experiment.Builder, Integer>() {
        @Override
        public void execute(Builder builder, Optional<Integer> value) {
          builder.repeat(value.get());
        }
      };

  static final ArgHandler<Builder, Long> SEED_HANDLER =
      new ArgHandler<Builder, Long>() {
        @Override
        public void execute(Builder builder, Optional<Long> value) {
          builder.withRandomSeed(value.get());
        }
      };

  static final ArgHandler<Builder, Integer> THREADS_HANDLER =
      new ArgHandler<Builder, Integer>() {
        @Override
        public void execute(Builder builder, Optional<Integer> value) {
          builder.withThreads(value.get());
        }
      };

  private ExperimentCli() {}

  /**
   * Creates a {@link Menu} for a {@link Experiment.Builder} instance.
   * @param builder The instance to create a {@link Menu} for.
   * @return A newly constructed {@link Menu}.
   */

  public static Menu createMenu(Experiment.Builder builder) {
    return createMenuBuilder(builder).build();
  }

  /**
   * Creates a {@link com.github.rinde.rinsim.cli.Menu.Builder} for a
   * {@link Experiment.Builder} instance. Via this instance the command-line
   * interface menu can be extended.
   * @param builder The experiment builder to create a menu builder for.
   * @return A newly constructed menu builder.
   */
  public static Menu.Builder createMenuBuilder(Experiment.Builder builder) {
    final Map<String, MASConfiguration> cfgMap = createConfigMap(builder);

    final Menu.Builder menuBuilder = Menu.builder();
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
      menuBuilder.addSubMenu(S, "scenarios.",
          FileProviderCli
              .createDefaultMenu(builder.scenarioProviderBuilder
                  .get()));
    }
    return menuBuilder;
  }

  static OptionArg<Integer> createBatchesOpt(Builder expBuilder) {
    return Option
        .builder("b", ArgumentParser.INTEGER)
        .longName("batches")
        .description(
            "Sets the number of batches to use in case of distributed "
                + "computation, default: ",
            expBuilder.numBatches,
            ". This option can not be used together with --threads.")
        .build();
  }

  static Map<String, MASConfiguration> createConfigMap(
      Experiment.Builder builder) {
    final List<MASConfiguration> configs = ImmutableList
        .copyOf(builder.configurationsSet);
    final ImmutableMap.Builder<String, MASConfiguration> mapBuilder =
        ImmutableMap
            .builder();
    for (int i = 0; i < configs.size(); i++) {
      mapBuilder.put("c" + i, configs.get(i));
    }
    return mapBuilder.build();
  }

  static OptionArg<String> createDryRunOpt(Builder builder) {
    return Option.builder("dr", ArgumentParser.STRING)
        .longName("dry-run")
        .description(
            "Will perform a 'dry run' of the experiment without doing any"
                + " actual simulations. A detailed description of the "
                + "experiment setup will be printed. If an additional "
                + "argument 'v' or 'verbose' is supplied, more details of"
                + " the experiment will be printed.")
        .setOptionalArgument()
        .build();
  }

  static OptionArg<List<String>> createExcludeOpt(
      Map<String, MASConfiguration> configMap) {
    return Option
        .builder("e", ArgumentParser.STRING_LIST)
        .longName("exclude")
        .description(
            "The following configurations can be excluded from the experiment"
                + " setup:",
            createConfigString(configMap),
            "This option can not be used together with --include.")
        .build();
  }

  static OptionArg<Boolean> createGuiOpt(Builder builder) {
    return Option
        .builder("g", ArgumentParser.BOOLEAN)
        .longName("show-gui")
        .description(
            "Starts the gui for each simulation when 'true' is supplied, hides "
                + "it when 'false' is supplied. By default the gui is ",
            builder.showGui ? "" : "not",
            " shown. The gui can only be shown if the computation is performed "
                + "locally and the number of threads is set to 1.")
        .build();
  }

  static OptionArg<List<String>> createIncludeOpt(
      Map<String, MASConfiguration> configMap) {
    return Option
        .builder("i", ArgumentParser.STRING_LIST)
        .longName("include")
        .description(
            "The following configurations can be included in the experiment "
                + "setup:",
            createConfigString(configMap),
            "This option can not be used together with --exclude.")
        .build();
  }

  static String createConfigString(Map<String, MASConfiguration> configMap) {
    final StringBuilder sb = new StringBuilder(System.lineSeparator());
    Joiner
        .on(System.lineSeparator())
        .withKeyValueSeparator(" = ")
        .appendTo(sb, toStringMap(configMap))
        .append("\nThe options should be given as a comma ',' separated list. ")
        .append(
            "If this option is not used all configurations are automatically "
                + "included. ");
    return sb.toString();
  }

  static Map<String, String> toStringMap(
      Map<String, MASConfiguration> configMap) {
    return Maps.transformValues(configMap, ConfigToName.INSTANCE);
  }

  static OptionNoArg createJppfOpt(Builder builder) {
    return Option
        .builder("j")
        .longName("jppf")
        .description(
            "Compute the experiment using the JPPF framework",
            builder.getComputer() == Computers.DISTRIBUTED ? DEFAULT_LABEL
                : "",
            ". This option can not be used together with the --local option.")
        .build();
  }

  static OptionNoArg createLocalOpt(Builder builder) {
    return Option.builder("l")
        .longName("local")
        .description(
            "Compute the experiment locally",
            builder.getComputer() == Computers.LOCAL ? DEFAULT_LABEL : "",
            ". This option can not be used together with the --jppf option.")
        .build();
  }

  static OptionArg<Integer> createRepetitionsOpt(Builder builder) {
    return Option
        .builder("r", ArgumentParser.INTEGER)
        .longName("repetitions")
        .description(
            "Sets the number of repetitions of each setting, default: ",
            builder.repetitions)
        .build();
  }

  static OptionArg<Long> createSeedOption(Experiment.Builder builder) {
    return Option.builder(S, ArgumentParser.LONG)
        .longName("seed")
        .description(
            "Sets the master random seed, default: ", builder.masterSeed, ".")
        .build();
  }

  static OptionArg<Integer> createThreadsOpt(Experiment.Builder builder) {
    return Option
        .builder("t", ArgumentParser.INTEGER)
        .longName("threads")
        .description(
            "Sets the number of threads to use in case of local computation, "
                + "default: ",
            builder.numThreads,
            ". This option can not be used together with --batches.")
        .build();
  }

  static Optional<String> execute(Experiment.Builder builder, String[] args) {
    return createMenu(builder).execute(args);
  }

  static Optional<String> safeExecute(Experiment.Builder builder,
      String[] args) {
    return createMenu(builder).safeExecute(args);
  }

  enum ConfigToName implements Function<MASConfiguration, String> {
    INSTANCE {
      @Override
      @Nullable
      public String apply(@Nullable MASConfiguration input) {
        return verifyNotNull(input).getName();
      }
    }
  }

  static class ExcludeHandler extends ConfigHandler {
    protected ExcludeHandler(Map<String, MASConfiguration> map) {
      super(map);
    }

    @Override
    void checkNumArgs(List<String> args) {
      checkArgument(
          args.size() < configMap.size(),
          "Too many configurations, at most %s configurations can be excluded.",
          configMap.size() - 1);

    }

    @Override
    void doExecute(Builder builder, List<MASConfiguration> selectedConfigs) {
      builder.configurationsSet.removeAll(selectedConfigs);
    }
  }

  static class IncludeHandler extends ConfigHandler {
    IncludeHandler(Map<String, MASConfiguration> map) {
      super(map);
    }

    @Override
    void checkNumArgs(List<String> args) {
      checkArgument(
          args.size() <= configMap.size(),
          "Too many configurations, at most %s configurations can be included.",
          configMap.size());
    }

    @Override
    void doExecute(Builder builder, List<MASConfiguration> selectedConfigs) {
      builder.configurationsSet.retainAll(selectedConfigs);
    }
  }

  abstract static class ConfigHandler implements
      ArgHandler<Builder, List<String>> {
    final Map<String, MASConfiguration> configMap;

    ConfigHandler(Map<String, MASConfiguration> map) {
      configMap = map;
    }

    @Override
    public final void execute(Builder builder,
        Optional<List<String>> argument) {
      final List<String> args = argument.get();
      final List<MASConfiguration> selectedConfigs = newArrayList();
      checkNumArgs(args);
      for (final String k : args) {
        checkArgument(configMap.containsKey(k),
            "The key '%s' is not valid. Valid keys: %s.", k,
            configMap.keySet());
        selectedConfigs.add(configMap.get(k));
      }
      doExecute(builder, selectedConfigs);
    }

    abstract void checkNumArgs(List<String> args);

    abstract void doExecute(Builder builder,
        List<MASConfiguration> selectedConfigs);
  }
}
