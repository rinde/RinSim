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
package com.github.rinde.rinsim.cli;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

/**
 * Represents an option in a command-line interface.
 * @author Rinde van Lon
 */
public abstract class Option {

  /**
   * This is the regular expression to which all option names must conform to:
   * <code>[a-zA-Z][a-zA-Z\\-\\.]*</code>.
   */
  public static final String NAME_REGEX = "[a-zA-Z][a-zA-Z\\-\\.]*";

  /**
   * The short option prefix: '-'.
   */
  public static final String SHORT_PREFIX = "-";

  /**
   * The long option prefix: '--'.
   */
  public static final String LONG_PREFIX = "--";

  final String shortName;
  final Optional<String> longName;
  final String description;
  final boolean isArgOptional;
  final boolean isHelpOption;

  Option(Builder<?> b, boolean help) {
    shortName = b.shortName;
    longName = b.longName;
    description = b.description;
    isArgOptional = b.isArgOptional;
    isHelpOption = help;
  }

  /**
   * @return The short name of this option.
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * @return An {@link Optional} containing the long name of this option or
   *         {@link Optional#absent()} if this option has no long name.
   */
  public Optional<String> getLongName() {
    return longName;
  }

  /**
   * @return An {@link Optional} containing the {@link ArgumentParser} of this
   *         option or {@link Optional#absent()} if this option doesn't require
   *         an argument.
   */
  public Optional<ArgumentParser<?>> getArgument() {
    return Optional.absent();
  }

  /**
   * @return The description of this option, displayed in the help menu.
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return <code>true</code> if this option has an argument <b>and</b> it is
   *         optional, <code>false</code> otherwise.
   */
  public boolean isArgOptional() {
    return isArgOptional;
  }

  @Override
  public String toString() {
    if (longName.isPresent()) {
      return Joiner.on("").join(SHORT_PREFIX, shortName, ",", LONG_PREFIX,
        longName.get());
    }
    return SHORT_PREFIX + shortName;
  }

  boolean isHelpOption() {
    return isHelpOption;
  }

  /**
   * Create a builder for building {@link OptionNoArg} instances.
   * @param shortName he short name to use, must conform to this regular
   *          expression: {@link #NAME_REGEX}.
   * @return A new builder instance.
   */
  public static NoArgBuilder builder(String shortName) {
    return new NoArgBuilder(shortName);
  }

  static NoArgBuilder builder(OptionNoArg opt) {
    return new NoArgBuilder(opt);
  }

  /**
   * Create a builder for building {@link OptionArg} instances.
   * @param shortName The short name to use, must conform to this regular
   *          expression: {@link #NAME_REGEX}.
   * @param argumentParser The {@link ArgumentParser} defines the type of
   *          argument that the resulting option expects.
   * @param <T> The type of argument that the resulting {@link OptionArg} will
   *          expect.
   * @return A new builder instance.
   */
  public static <T> ArgBuilder<T> builder(String shortName,
      ArgumentParser<T> argumentParser) {
    return new ArgBuilder<>(shortName, argumentParser);
  }

  static <T> ArgBuilder<T> builder(OptionArg<T> opt) {
    return new ArgBuilder<>(opt);
  }

  /**
   * An {@link Option} that optionally requires an argument of type
   * <code>T</code>.
   * @author Rinde van Lon
   * @param <T> The type of the argument.
   */
  public static class OptionArg<T> extends Option {
    final ArgumentParser<T> argumentType;

    OptionArg(ArgBuilder<T> b, boolean help) {
      super(b, help);
      argumentType = b.argumentType;
    }

    @Override
    public Optional<ArgumentParser<?>> getArgument() {
      return Optional.<ArgumentParser<?>>of(argumentType);
    }
  }

  /**
   * An {@link Option} that does not support any arguments.
   * @author Rinde van Lon
   */
  public static class OptionNoArg extends Option {
    OptionNoArg(NoArgBuilder b, boolean help) {
      super(b, help);
    }
  }

  /**
   * A builder for creating {@link OptionNoArg} instances.
   * @author Rinde van Lon
   */
  public static class NoArgBuilder extends Builder<NoArgBuilder> {
    NoArgBuilder(String sn) {
      super(sn);
    }

    NoArgBuilder(OptionNoArg opt) {
      super(opt);
    }

    /**
     * @return A new {@link OptionNoArg} instance.
     */
    public OptionNoArg build() {
      return new OptionNoArg(this, false);
    }

    OptionNoArg buildHelpOption() {
      return new OptionNoArg(this, true);
    }

    @Override
    protected NoArgBuilder self() {
      return this;
    }
  }

  /**
   * A builder for creating {@link OptionArg} instances.
   * @author Rinde van Lon
   * @param <V> The type of the argument that options that are created by this
   *          builder require.
   */
  public static class ArgBuilder<V> extends Builder<ArgBuilder<V>> {
    ArgumentParser<V> argumentType;

    ArgBuilder(String sn, ArgumentParser<V> argType) {
      super(sn);
      argumentType = argType;
    }

    ArgBuilder(OptionArg<V> opt) {
      super(opt);
      argumentType = opt.argumentType;
    }

    /**
     * Calling this method will make the argument of the option optional.
     * @return This, as per the builder pattern.
     */
    public ArgBuilder<V> setOptionalArgument() {
      isArgOptional = true;
      return self();
    }

    /**
     * @return A new {@link OptionArg} instance.
     */
    public OptionArg<V> build() {
      return new OptionArg<>(this, false);
    }

    @Override
    protected ArgBuilder<V> self() {
      return this;
    }
  }

  /**
   * Builder of {@link Option} instances.
   * @author Rinde van Lon
   * @param <T> Value type
   */
  abstract static class Builder<T extends Builder<T>> {
    String shortName;
    Optional<String> longName;
    String description;
    boolean isArgOptional;

    Builder(String sn) {
      checkOptionName(sn);
      shortName = sn;
      longName = Optional.absent();
      description = "";
      isArgOptional = false;
    }

    Builder(Option opt) {
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

    /**
     * Set the short name of the option. The name must conform to this regular
     * expression: {@link #NAME_REGEX}. Any previous value is overridden.
     * @param sn The short name to use.
     * @return This, as per the builder pattern.
     */
    public T shortName(String sn) {
      checkOptionName(sn);
      shortName = sn;
      return self();
    }

    /**
     * Set the long name of the option. The name must conform to this regular
     * expression: {@link #NAME_REGEX}. Any previous value is overridden.
     * @param ln The long name to use.
     * @return This, as per the builder pattern.
     */
    public T longName(String ln) {
      checkOptionName(ln);
      longName = Optional.of(ln);
      return self();
    }

    /**
     * Sets the description of the option, to be displayed in the help menu.
     * @param desc The description, if multiple strings are supplied they are
     *          concatenated.
     * @return This, as per the builder pattern.
     */
    public T description(Object... desc) {
      description = Joiner.on("").join(desc);
      return self();
    }

    static void checkOptionName(String name) {
      checkArgument(name.matches(NAME_REGEX),
        "%s is not a valid option name, it must conform to %s.", name,
        NAME_REGEX);
    }
  }
}
