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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.github.rinde.rinsim.cli.CliException.CauseType;
import com.github.rinde.rinsim.cli.Option.OptionArg;
import com.google.common.base.Converter;
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
  public static final Splitter ARG_LIST_SPLITTER =
    Splitter.on(ARG_LIST_SEPARATOR);

  /**
   * {@link Boolean} parser.
   */
  public static final ArgumentParser<Boolean> BOOLEAN =
    new ArgumentParser<Boolean>("boolean") {
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
        throw new CliException("Expected a boolean but found: '" + arg + "'.",
            CauseType.INVALID_ARG_FORMAT, option);
      }
    };

  /**
   * {@link Long} parser.
   */
  public static final ArgumentParser<Long> LONG = new NumberParser<>(
      "long", Longs.stringConverter());

  /**
   * List of {@link Long}s parser.
   */
  public static final ArgumentParser<List<Long>> LONG_LIST =
    new NumberListParser<>("long list", Longs.stringConverter());

  /**
   * {@link Integer} parser.
   */
  public static final ArgumentParser<Integer> INTEGER = new NumberParser<>(
      "int", Ints.stringConverter());

  /**
   * List of {@link Integer}s parser.
   */
  public static final ArgumentParser<List<Integer>> INTEGER_LIST =
    new NumberListParser<>("int list", Ints.stringConverter());

  /**
   * {@link Double} parser.
   */
  public static final ArgumentParser<Double> DOUBLE = new NumberParser<>(
      "double", Doubles.stringConverter());

  /**
   * List of {@link Double}s parser.
   */
  public static final ArgumentParser<List<Double>> DOUBLE_LIST =
    new NumberListParser<>("double list", Doubles.stringConverter());

  /**
   * {@link String} parser.
   */
  public static final ArgumentParser<String> STRING =
    new ArgumentParser<String>("string") {
      @Override
      String parse(OptionArg<String> option, String value) {
        return value;
      }
    };

  /**
   * List of {@link String}s parser.
   */
  public static final ArgumentParser<List<String>> STRING_LIST =
    new ArgumentParser<List<String>>("string list") {
      @Override
      List<String> parse(OptionArg<List<String>> option, String value) {
        return Splitter.on(ARG_LIST_SEPARATOR).splitToList(value);
      }
    };

  private final String name;

  ArgumentParser(String nm) {
    name = nm;
  }

  abstract V parse(OptionArg<V> option, String arg);

  String name() {
    return name;
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

  static CliException convertNFE(Option option, NumberFormatException e,
      String value, String argName) {
    return new CliException(String.format(
      "The option %s expects a %s, found '%s'.", option, argName,
      value), e, CauseType.INVALID_ARG_FORMAT, option);
  }

  /**
   * A parser for numbers.
   * @author Rinde van Lon
   * @param <T> The type of number.
   */
  public static class NumberParser<T> extends ArgumentParser<T> {
    private final Converter<String, T> converter;

    /**
     * Construct a new parser.
     * @param nm The name of the argument, shown in the help menu. name.
     * @param conv The {@link Converter} to use to convert strings to type
     *          <code>T</code>.
     */
    public NumberParser(String nm, Converter<String, T> conv) {
      super(nm);
      converter = conv;
    }

    @SuppressWarnings("null")
    @Override
    T parse(OptionArg<T> option, String arg) {
      try {
        return converter.convert(arg);
      } catch (final NumberFormatException e) {
        throw convertNFE(option, e, arg, name());
      }
    }
  }

  /**
   * A parser for lists of numbers.
   * @author Rinde van Lon
   * @param <T> The type of number.
   */
  public static class NumberListParser<T> extends ArgumentParser<List<T>> {
    private final Converter<String, T> converter;

    /**
     * Construct a new list parser.
     * @param nm The name of the argument, shown in the help menu.
     * @param conv The {@link Converter} to use to convert strings to type
     *          <code>T</code>.
     */
    public NumberListParser(String nm, Converter<String, T> conv) {
      super(nm);
      converter = conv;
    }

    @Override
    List<T> parse(OptionArg<List<T>> option, String value) {
      final Iterable<String> strings = Splitter.on(ARG_LIST_SEPARATOR).split(
        value);
      try {
        return ImmutableList.copyOf(Iterables.transform(strings, converter));
      } catch (final NumberFormatException e) {
        throw convertNFE(option, e, value, name());
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
      final List<String> list =
        Splitter.on(ARG_LIST_SEPARATOR).splitToList(value);

      final PeekingIterator<String> it =
        Iterators.peekingIterator(list.iterator());

      final List<String> generatedList = new ArrayList<>();
      while (it.hasNext()) {
        final String cur = it.next();
        if ("..".equals(cur)) {
          CliException.checkArgFormat(!generatedList.isEmpty(), option,
            "'..' cannot be the first item in the list.");
          CliException.checkArgFormat(it.hasNext(), option,
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
            prevNum, nextNum);

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
      CliException.checkArgFormat(pattern.matcher(str).matches(), opt,
        "'%s' does not match expected pattern: '%s'", str, pattern.pattern());
    }
  }
}
