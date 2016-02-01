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

import static com.google.common.base.Verify.verifyNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.github.rinde.rinsim.cli.CliException.CauseType;
import com.github.rinde.rinsim.cli.Option.OptionArg;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * An argument parser is responsible for converting strings into the expected
 * type. If the parsing fails, a {@link CliException} is thrown. This class
 * contains a number of {@link ArgumentParser}s for common types.
 * @author Rinde van Lon
 * @param <V> The argument type.
 */
public abstract class ArgumentParser<V> {
  /**
   * Separator of argument lists.
   */
  public static final char ARG_LIST_SEPARATOR = ',';

  /**
   * {@link Splitter} using {@link #ARG_LIST_SEPARATOR}.
   */
  public static final Splitter ARG_LIST_SPLITTER =
    Splitter.on(ARG_LIST_SEPARATOR);

  private static final ArgumentParser<Boolean> BOOLEAN = new BooleanParser();
  private static final ArgumentParser<Long> LONG =
    asParser("long", Longs.stringConverter());
  private static final ArgumentParser<List<Long>> LONG_LIST =
    asListParser("long list", Longs.stringConverter());
  private static final ArgumentParser<Integer> INTEGER =
    asParser("int", Ints.stringConverter());
  private static final ArgumentParser<List<Integer>> INTEGER_LIST =
    asListParser("int list", Ints.stringConverter());
  private static final ArgumentParser<Double> DOUBLE =
    asParser("double", Doubles.stringConverter());
  private static final ArgumentParser<List<Double>> DOUBLE_LIST =
    asListParser("double list", Doubles.stringConverter());
  private static final ArgumentParser<String> STRING =
    asParser("string", Functions.<String>identity());
  private static final ArgumentParser<List<String>> STRING_LIST =
    asListParser("string list", Functions.<String>identity());

  private final String name;

  ArgumentParser(String nm) {
    name = nm;
  }

  abstract V parse(OptionArg<V> option, String arg);

  String name() {
    return name;
  }

  /**
   * @return {@link Integer} parser.
   */
  public static ArgumentParser<Integer> intParser() {
    return INTEGER;
  }

  /**
   * @return List of {@link Integer}s parser.
   */
  public static ArgumentParser<List<Integer>> intList() {
    return INTEGER_LIST;
  }

  /**
   * A prefixed int list allows arguments such as: <code>c0,..,c4</code>, this
   * generates a list containing the following five elements
   * <code>c0,c1,c2,c3,c4</code>.
   * @param prefix The prefix string.
   * @return A new argument parser with the specified prefix.
   */
  public static ArgumentParser<List<String>> prefixedIntList(String prefix) {
    return new PrefixedIntListParser(prefix);
  }

  /**
   * @return {@link Double} parser.
   */
  public static ArgumentParser<Double> doubleParser() {
    return DOUBLE;
  }

  /**
   * @return List of {@link Double}s parser.
   */
  public static ArgumentParser<List<Double>> doubleListParser() {
    return DOUBLE_LIST;
  }

  /**
   * @return {@link String} parser.
   */
  public static ArgumentParser<String> stringParser() {
    return STRING;
  }

  /**
   * @return List of {@link String}s parser.
   */
  public static ArgumentParser<List<String>> stringListParser() {
    return STRING_LIST;
  }

  /**
   * @return {@link Boolean} parser.
   */
  public static ArgumentParser<Boolean> booleanParser() {
    return BOOLEAN;
  }

  /**
   * @return {@link Long} parser.
   */
  public static ArgumentParser<Long> longParser() {
    return LONG;
  }

  /**
   * @return List of {@link Long}s parser.
   */
  public static ArgumentParser<List<Long>> longListParser() {
    return LONG_LIST;
  }

  /**
   * Create a parser for {@link Enum}s.
   * @param name The name for the value of the option (typically the enum name).
   * @param enumClass The class of the enum.
   * @param <T> The class of the enum.
   * @return A new {@link ArgumentParser} for instances of the specified enum.
   */
  public static <T extends Enum<T>> ArgumentParser<T> enumParser(String name,
      Class<T> enumClass) {
    return asParser(name, Enums.stringConverter(enumClass));
  }

  /**
   * Create a parser for lists of {@link Enum}s.
   * @param name The name for the values of the option (typically the enum name
   *          with 'list' appended).
   * @param enumClass The class of the enum.
   * @param <T> The class of the enum.
   * @return A new {@link ArgumentParser} for lists of instances of the
   *         specified enum.
   */
  public static <T extends Enum<T>> ArgumentParser<List<T>> enumListParser(
      String name, Class<T> enumClass) {
    return asListParser(name, Enums.stringConverter(enumClass));
  }

  /**
   * Constructs a new {@link ArgumentParser} based on the specified
   * {@link Function}.
   * @param name The name of the value that is expected for the option.
   * @param func A function that converts string to the expected type.
   * @param <T> The type that it is expected.
   * @return A new instance.
   */
  public static <T> ArgumentParser<T> asParser(String name,
      Function<String, T> func) {
    return new FunctionToParserAdapter<>(name, func);
  }

  /**
   * Constructs a new {@link ArgumentParser} for lists based on the specified
   * {@link Function}.
   * @param name The name of the value that is expected for the option.
   * @param func A function that converts string to the expected type.
   * @param <T> The type that it is expected.
   * @return A new instance.
   */
  public static <T> ArgumentParser<List<T>> asListParser(String name,
      Function<String, T> func) {
    return new FunctionToListParserAdapter<>(name, func);
  }

  static CliException convertIAE(Option option, IllegalArgumentException e,
      String value, String argName) {

    return new CliException(
      String.format("The option %s expects a %s, found '%s'.",
        option,
        argName,
        value),
      e,
      CauseType.INVALID_ARG_FORMAT,
      option);
  }

  static class BooleanParser extends ArgumentParser<Boolean> {
    BooleanParser() {
      super("boolean");
    }

    @Override
    Boolean parse(OptionArg<Boolean> option, String arg) {
      if ("T".equalsIgnoreCase(arg)
        || "true".equalsIgnoreCase(arg)
        || "1".equals(arg)) {
        return true;
      } else if ("F".equalsIgnoreCase(arg)
        || "false".equalsIgnoreCase(arg)
        || "0".equals(arg)) {
        return false;
      }
      throw new CliException("Expected a boolean but found: '" + arg
        + "'.",
        CauseType.INVALID_ARG_FORMAT,
        option);
    }
  }

  static class FunctionToParserAdapter<T> extends ArgumentParser<T> {
    private final Function<String, T> converter;

    FunctionToParserAdapter(String nm, Function<String, T> conv) {
      super(nm);
      converter = conv;
    }

    @Override
    T parse(OptionArg<T> option, String arg) {
      try {
        return verifyNotNull(converter.apply(arg),
          "Converter should never return null.");
      } catch (final IllegalArgumentException e) {
        throw convertIAE(option, e, arg, name());
      }
    }
  }

  static class FunctionToListParserAdapter<T, U extends List<T>>
      extends ArgumentParser<List<T>> {
    private final Function<String, T> converter;

    FunctionToListParserAdapter(String nm, Function<String, T> conv) {
      super(nm);
      converter = conv;
    }

    @Override
    List<T> parse(OptionArg<List<T>> option, String arg) {
      final Iterable<String> strings = ARG_LIST_SPLITTER.split(arg);
      try {
        return ImmutableList.copyOf(Iterables.transform(strings, converter));
      } catch (final IllegalArgumentException e) {
        throw convertIAE(option, e, arg, name());
      }
    }

  }

  static class PrefixedIntListParser extends ArgumentParser<List<String>> {
    private final String prefix;
    private final Pattern pattern;

    PrefixedIntListParser(String prefx) {
      super("prefixed int list");
      prefix = prefx;
      pattern = Pattern.compile(prefix + "\\d+");
    }

    @Override
    List<String> parse(OptionArg<List<String>> option, String value) {
      // can not be empty
      final List<String> list = Splitter.on(ARG_LIST_SEPARATOR)
        .splitToList(value);

      final PeekingIterator<String> it = Iterators
        .peekingIterator(list.iterator());

      final List<String> generatedList = new ArrayList<>();
      while (it.hasNext()) {
        final String cur = it.next();
        if ("..".equals(cur)) {
          CliException.checkArgFormat(!generatedList.isEmpty(),
            option,
            "'..' cannot be the first item in the list.");
          CliException.checkArgFormat(it.hasNext(),
            option,
            "After '..' at least one more item is expected.");

          final String prev = generatedList.get(generatedList.size() - 1);
          final int prevNum = Integer.parseInt(prev.substring(prefix.length()));

          final String next = it.peek();
          checkItemFormat(option, next);
          final int nextNum = Integer.parseInt(next.substring(prefix.length()));

          CliException.checkArgFormat(prevNum + 1 < nextNum,
            option,
            "The items adjacent to '..' must be >= 0 and at least one apart. "
              + "Found '%s' and '%s'.",
            prevNum,
            nextNum);

          for (int i = prevNum + 1; i < nextNum; i++) {
            generatedList.add(prefix + Integer.toString(i));
          }
        } else {
          checkItemFormat(option, cur);
          generatedList.add(cur);
        }
      }
      return generatedList;
    }

    void checkItemFormat(Option opt, String str) {
      CliException.checkArgFormat(pattern.matcher(str).matches(),
        opt,
        "'%s' does not match expected pattern: '%s'",
        str,
        pattern.pattern());
    }
  }
}
