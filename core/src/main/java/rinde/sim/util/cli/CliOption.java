package rinde.sim.util.cli;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.PatternOptionBuilder;

import rinde.sim.util.cli.CliException.CauseType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

public final class CliOption {
  private static final int NUM_ARGS_IN_LIST = 100;
  private static final String ARG_LIST_NAME = "list";
  private static final char ARG_LIST_SEPARATOR = ',';
  private static final String NUM_NAME = "num";
  private static final String STRING_NAME = "string";

  final String shortName;
  final String longName;
  final String description;
  final OptionArgType<?> argumentType;
  final boolean isArgOptional;
  final OptionHandler<?, ?> handler;

  CliOption(Builder b, OptionHandler<?, ?> h) {
    shortName = b.shortName;
    longName = b.longName;
    description = b.description;
    argumentType = b.argumentType;
    isArgOptional = b.isArgOptional;
    handler = h;
  }

  public <T, U> boolean execute(T ref, Value<U> value) {
    return ((OptionHandler<T, U>) handler).execute(ref, value);
  }

  public String getShortName() {
    return shortName;
  }

  public String getLongName() {
    return longName;
  }

  public String getDescription() {
    return description;
  }

  public boolean isArgOptional() {
    return isArgOptional;
  }

  Option create() {
    final Option option = new Option(shortName, description);
    option.setLongOpt(longName);
    argumentType.apply(option);
    option.setOptionalArg(isArgOptional);
    return option;
  }

  // no arg builder
  public static Builder<String> builder(String shortName) {
    return new Builder<>(shortName);
  }

  // arg builder
  public static <T> Builder<T> builder(String shortName,
      OptionArgType<T> optionArgType) {
    return new Builder<>(shortName, optionArgType);
  }

  static Builder builder(CliOption opt) {
    return new Builder(opt);
  }

  public static abstract class OptionArgType<T> {

    static final OptionArgType<String> NO_ARG = new OptionArgType<String>() {
      @Override
      void apply(Option o) {}

      @Override
      Value<String> parseValue(CliOption option,
          CommandLine commandLine) {
        return new Value<>("", optionUsed(option, commandLine), "");
      }
    };

    public static final OptionArgType<List<Long>> LONG_LIST = new OptionArgType<List<Long>>() {
      @Override
      void apply(Option o) {
        o.setArgs(NUM_ARGS_IN_LIST);
        o.setArgName(ARG_LIST_NAME);
        o.setType(PatternOptionBuilder.NUMBER_VALUE);
        o.setValueSeparator(ARG_LIST_SEPARATOR);
      }

      @Override
      Value<List<Long>> parseValue(CliOption option, CommandLine commandLine) {
        final String opt = optionUsed(option, commandLine);
        final String str = asString(option, commandLine);
        final List<String> strings = asList(commandLine.getOptionValues(option
            .getShortName()));
        final List<Long> longs = ImmutableList.copyOf(Lists.transform(
            strings, Longs.stringConverter()));
        return new Value<>(str, opt, longs);
      }
    };
    public static final OptionArgType<Long> LONG = new OptionArgType<Long>() {
      @Override
      void apply(Option o) {
        o.setArgs(1);
        o.setArgName(NUM_NAME);
        o.setType(PatternOptionBuilder.NUMBER_VALUE);
      }

      @Override
      Value<Long> parseValue(CliOption option, CommandLine commandLine) {
        final String optionUsed = optionUsed(option, commandLine);
        final String str = asString(option, commandLine);
        try {
          final Long i = Long.parseLong(str);
          return new Value<>(str, optionUsed, i);
        } catch (final NumberFormatException e) {
          throw new CliException("The option " + optionUsed
              + " expects a long, found " + str,
              CauseType.NOT_A_LONG, option);
        }
      }
    };

    public static final OptionArgType<List<String>> STRING_LIST = new OptionArgType<List<String>>() {
      @Override
      void apply(Option o) {
        o.setArgs(NUM_ARGS_IN_LIST);
        o.setArgName(ARG_LIST_NAME);
        o.setType(PatternOptionBuilder.STRING_VALUE);
        o.setValueSeparator(ARG_LIST_SEPARATOR);
      }

      @Override
      Value<List<String>> parseValue(CliOption option, CommandLine cmd) {
        return new Value<List<String>>(
            asString(option, cmd),
            optionUsed(option, cmd),
            ImmutableList.copyOf(cmd.getOptionValues(option
                .getShortName())));
      }
    };

    public static final OptionArgType<String> STRING = new OptionArgType<String>() {
      @Override
      void apply(Option o) {
        o.setArgs(1);
        o.setArgName(STRING_NAME);
        o.setType(PatternOptionBuilder.NUMBER_VALUE);
      }

      @Override
      Value<String> parseValue(CliOption option,
          CommandLine commandLine) {
        final String str = asString(option, commandLine);
        return new Value<>(str, optionUsed(option, commandLine), str);
      }
    };

    static String optionUsed(CliOption option, CommandLine commandLine) {
      if (commandLine.hasOption(option.getShortName())) {
        return "-" + option.getShortName();
      } else if (commandLine.hasOption(option.getLongName())) {
        return "--" + option.getLongName();
      } else {
        throw new IllegalArgumentException();
      }
    }

    static String asString(CliOption option, CommandLine commandLine) {
      return Joiner.on(",").join(
          commandLine.getOptionValues(option.getShortName()));
    }

    abstract void apply(Option o);

    abstract Value<T> parseValue(CliOption option, CommandLine commandLine);
  }

  public static class Builder<T> {
    String shortName;
    String longName;
    String description;
    OptionArgType<?> argumentType;
    boolean isArgOptional;

    Builder(String sn) {
      this(sn, OptionArgType.NO_ARG);
    }

    Builder(String sn, OptionArgType<?> argType) {
      shortName = sn;
      longName = "";
      description = "";
      argumentType = argType;
      isArgOptional = false;
    }

    Builder(CliOption opt) {
      shortName = opt.shortName;
      longName = opt.longName;
      description = opt.description;
      argumentType = opt.argumentType;
      isArgOptional = opt.isArgOptional;
    }

    public Builder<T> shortName(String sn) {
      shortName = sn;
      return this;
    }

    public Builder<T> longName(String ln) {
      longName = ln;
      return this;
    }

    public Builder<T> description(Object... desc) {
      description = Joiner.on("").join(desc);
      return this;
    }

    public Builder<T> argOptional() {
      isArgOptional = true;
      return this;
    }

    public <U> CliOption build(OptionHandler<U, T> handler) {
      return new CliOption(this, handler);
    }

    public <U> CliOption buildHelpOption() {
      return build(new HelpHandler<U, T>());
    }
  }

  static class HelpHandler<T, U> implements OptionHandler<T, U> {
    @Override
    public boolean execute(T ref, Value<U> value) {
      return false;
    }
  }
}
