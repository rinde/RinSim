package rinde.sim.util.cli;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

/**
 * 
 * @author rinde
 * 
 * @param <S> The type of the subject
 */
public abstract class CliOption {
  private static final int NUM_ARGS_IN_LIST = 100;
  private static final String ARG_LIST_NAME = "list";

  private static final String NUM_NAME = "num";
  private static final String STRING_NAME = "string";

  public static class CliOptionArg<T> extends CliOption {
    final ArgumentParser<T> argumentType;

    CliOptionArg(ArgBuilder<T> b, boolean help) {
      super(b, help);
      argumentType = b.argumentType;
    }

    @Override
    public Optional<ArgumentParser<?>> getArgument() {
      return Optional.<ArgumentParser<?>> of(argumentType);
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

  public Optional<ArgumentParser<?>> getArgument() {
    return Optional.absent();
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

  // no arg builder
  public static NoArgBuilder builder(String shortName) {
    return new NoArgBuilder(shortName);
  }

  static NoArgBuilder builder(CliOptionNoArg opt) {
    return new NoArgBuilder(opt);
  }

  // arg builder
  public static <T> ArgBuilder<T> builder(String shortName,
      ArgumentParser<T> optionArgType) {
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
    ArgumentParser<V> argumentType;

    ArgBuilder(String sn, ArgumentParser<V> argType) {
      super(sn);
      argumentType = argType;
    }

    ArgBuilder(CliOptionArg<V> opt) {
      super(opt);
      argumentType = opt.argumentType;
    }

    public ArgBuilder<V> argOptional() {
      isArgOptional = true;
      return self();
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

  }

}
