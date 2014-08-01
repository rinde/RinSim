package rinde.sim.util.cli;

import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.PatternOptionBuilder;

import rinde.sim.util.cli.CliException.CauseType;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Longs;

public final class CliOption {
  private static final int NUM_ARGS_IN_LIST = 100;
  private static final String ARG_LIST_NAME = "list";
  static final char ARG_LIST_SEPARATOR = ',';
  private static final String NUM_NAME = "num";
  private static final String STRING_NAME = "string";

  private static final OptionHandler<Object, Object> HELP_HANDLER = new OptionHandler<Object, Object>() {
    @Override
    public void execute(Object ref, Optional<Object> value) {}
  };

  final String shortName;
  final Optional<String> longName;
  final String description;
  final OptionArgType<?> argumentType;
  final boolean isArgOptional;
  final OptionHandler<?, ?> handler;

  CliOption(Builder<?> b, OptionHandler<?, ?> h) {
    shortName = b.shortName;
    longName = b.longName;
    description = b.description;
    argumentType = b.argumentType;
    isArgOptional = b.isArgOptional;
    handler = h;
  }

  <T, U> void execute(T ref, Optional<U> value) {
    ((OptionHandler<T, U>) handler).execute(ref, value);
  }

  public String getShortName() {
    return shortName;
  }

  public Optional<String> getLongName() {
    return longName;
  }

  public String getDescription() {
    return description;
  }

  public boolean isArgOptional() {
    return isArgOptional;
  }

  @Override
  public String toString() {
    if (longName.isPresent()) {
      return Joiner.on("").join("-", shortName, "(", longName.get(), ")");
    }
    return "-" + shortName;
  }

  boolean isHelpOption() {
    return handler == HELP_HANDLER;
  }

  Option create() {
    final Option option = new Option(shortName, description);
    if (longName.isPresent()) {
      option.setLongOpt(longName.get());
    }
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
      String parseValue(CliOption option, String value) {
        throw new UnsupportedOperationException();
      }

      @Override
      String name() {
        return "no arg";
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
      List<Long> parseValue(CliOption option, String value) {
        final Iterable<String> strings = Splitter.on(ARG_LIST_SEPARATOR).split(
            value);
        try {
          final List<Long> longs = ImmutableList.copyOf(Iterables.transform(
              strings, Longs.stringConverter()));
          return longs;
        } catch (final NumberFormatException e) {
          throw convertNFE(option, e, value, name());
        }
      }

      @Override
      String name() {
        return "long list";
      }
    };

    static CliException convertNFE(CliOption option, NumberFormatException e,
        String value, String argName) {
      return new CliException(String.format(
          "The option %s expects a %s, found '%s'.", option, argName,
          value), e, CauseType.INVALID_NUMBER_FORMAT, option);
    }

    public static final OptionArgType<Long> LONG = new OptionArgType<Long>() {
      @Override
      void apply(Option o) {
        o.setArgs(1);
        o.setArgName(NUM_NAME);
        o.setType(PatternOptionBuilder.NUMBER_VALUE);
      }

      @Override
      Long parseValue(CliOption option, String value) {
        try {
          return Long.parseLong(value);
        } catch (final NumberFormatException e) {
          throw convertNFE(option, e, value, name());
        }
      }

      @Override
      String name() {
        return "long";
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
      List<String> parseValue(CliOption option, String value) {
        return Splitter.on(ARG_LIST_SEPARATOR).splitToList(value);
      }

      @Override
      String name() {
        return "string list";
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
      String parseValue(CliOption option, String value) {
        return value;
      }

      @Override
      String name() {
        return "string";
      }
    };

    abstract void apply(Option o);

    abstract T parseValue(CliOption option, String value);

    abstract String name();
  }

  public static class Builder<T> {
    String shortName;
    Optional<String> longName;
    String description;
    OptionArgType<?> argumentType;
    boolean isArgOptional;

    Builder(String sn) {
      this(sn, OptionArgType.NO_ARG);
    }

    Builder(String sn, OptionArgType<?> argType) {
      shortName = sn;
      longName = Optional.absent();
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
      longName = Optional.of(ln);
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
      return build((OptionHandler<U, T>) HELP_HANDLER);
    }
  }

}
