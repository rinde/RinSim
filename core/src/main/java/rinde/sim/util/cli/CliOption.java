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

/**
 * 
 * @author rinde
 * 
 * @param <S> The type of the subject
 */
public abstract class CliOption {
  private static final int NUM_ARGS_IN_LIST = 100;
  private static final String ARG_LIST_NAME = "list";
  static final char ARG_LIST_SEPARATOR = ',';
  private static final String NUM_NAME = "num";
  private static final String STRING_NAME = "string";

  public static class CliOptionArg<T> extends CliOption {
    final OptionArgType<T> argumentType;

    CliOptionArg(ArgBuilder<T> b, boolean help) {
      super(b, help);
      argumentType = b.argumentType;
    }

    @Override
    Option create() {
      final Option option = super.create();
      argumentType.apply(option);
      return option;
    }
  }

  public static class CliOptionNoArg extends CliOption {
    CliOptionNoArg(NoArgBuilder b, boolean help) {
      super(b, help);
    }
  }

  final String shortName;
  final Optional<String> longName;
  final String description;
  final boolean isArgOptional;
  final boolean isHelpOption;

  CliOption(Builder<?> b, boolean help) {
    shortName = b.shortName;
    longName = b.longName;
    description = b.description;
    isArgOptional = b.isArgOptional;
    isHelpOption = help;
  }

  // CliOption(ArgBuilder<?> b, OptionHandler<S, ?> handler2,
  // Optional<OptionArgType<V>> of) {
  // // TODO Auto-generated constructor stub
  // }

  // <U> void execute(S ref, Optional<U> value) {
  // ((OptionHandler<S, U>) handler).execute(ref, value);
  // }

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
    return isHelpOption;
  }

  Option create() {
    final Option option = new Option(shortName, description);
    if (longName.isPresent()) {
      option.setLongOpt(longName.get());
    }

    option.setOptionalArg(isArgOptional);
    return option;
  }

  // no arg builder
  public static NoArgBuilder builder(String shortName) {
    return new NoArgBuilder(shortName);
  }

  static NoArgBuilder builder(CliOptionNoArg opt) {
    return new NoArgBuilder(opt);
  }

  // arg builder
  public static <T> ArgBuilder<T> builder(String shortName,
      OptionArgType<T> optionArgType) {
    return new ArgBuilder<>(shortName, optionArgType);
  }

  static <T> ArgBuilder<T> builder(CliOptionArg<T> opt) {
    return new ArgBuilder<>(opt);
  }

  // static <T> Builder<?> builder(CliOption<T> opt) {
  // if (opt.argumentType.isPresent()) {
  // return new ArgBuilder<>(opt);
  // }
  // return new NoArgBuilder(opt);
  // }

  public static abstract class OptionArgType<V> {

    // static final OptionArgType<?> NO_ARG = new OptionArgType<Object>() {
    // @Override
    // void apply(Option o) {}
    //
    // @Override
    // Object parseValue(CliOption<?> option, String value) {
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // String name() {
    // return "no arg";
    // }
    // };

    public static final OptionArgType<List<Long>> LONG_LIST = new OptionArgType<List<Long>>() {
      @Override
      void apply(Option o) {
        o.setArgs(NUM_ARGS_IN_LIST);
        o.setArgName(ARG_LIST_NAME);
        o.setType(PatternOptionBuilder.NUMBER_VALUE);
        o.setValueSeparator(ARG_LIST_SEPARATOR);
      }

      @Override
      List<Long> parseValue(CliOptionArg<List<Long>> option, String value) {
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
      Long parseValue(CliOptionArg<Long> option, String value) {
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
      List<String> parseValue(CliOptionArg<List<String>> option, String value) {
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
      String parseValue(CliOptionArg<String> option, String value) {
        return value;
      }

      @Override
      String name() {
        return "string";
      }
    };

    abstract void apply(Option o);

    abstract V parseValue(CliOptionArg<V> option, String value);

    abstract String name();
  }

  public static class NoArgBuilder extends Builder<NoArgBuilder> {
    NoArgBuilder(String sn) {
      super(sn);
    }

    NoArgBuilder(CliOptionNoArg opt) {
      super(opt);
    }

    public CliOptionNoArg build() {
      return new CliOptionNoArg(this, false);
    }

    CliOptionNoArg buildHelpOption() {
      return new CliOptionNoArg(this, true);
    }

    @Override
    protected NoArgBuilder self() {
      return this;
    }

  }

  public static class ArgBuilder<V> extends Builder<ArgBuilder<V>> {
    OptionArgType<V> argumentType;

    ArgBuilder(String sn, OptionArgType<V> argType) {
      super(sn);
      argumentType = argType;
    }

    ArgBuilder(CliOptionArg<V> opt) {
      super(opt);
      argumentType = opt.argumentType;
    }

    public CliOptionArg<V> build() {
      return new CliOptionArg<V>(this, false);
    }

    @Override
    protected ArgBuilder<V> self() {
      return this;
    }
  }

  /**
   * 
   * @author rinde
   * 
   * @param <V> Value type
   */
  abstract static class Builder<T extends Builder<T>> {
    String shortName;
    Optional<String> longName;
    String description;
    boolean isArgOptional;

    Builder(String sn) {
      shortName = sn;
      longName = Optional.absent();
      description = "";
      isArgOptional = false;
    }

    Builder(CliOption opt) {
      shortName = opt.shortName;
      longName = opt.longName;
      description = opt.description;
      isArgOptional = opt.isArgOptional;
    }

    /**
     * Should return 'this', the builder.
     * @return 'this'.
     */
    protected abstract T self();

    public T shortName(String sn) {
      shortName = sn;
      return self();
    }

    public T longName(String ln) {
      longName = Optional.of(ln);
      return self();
    }

    public T description(Object... desc) {
      description = Joiner.on("").join(desc);
      return self();
    }

    public T argOptional() {
      isArgOptional = true;
      return self();
    }

    // public <U> CliOption buildHelpOption() {
    // return new CliOption<V>(this, handler, Optional.of(argumentType));
    // return build((OptionHandler) HELP_HANDLER);
    // }
  }

}
