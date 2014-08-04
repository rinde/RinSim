package rinde.sim.util.cli;

import java.util.List;

import rinde.sim.util.cli.CliException.CauseType;
import rinde.sim.util.cli.Option.OptionArg;

import com.google.common.base.Converter;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public abstract class ArgumentParser<V> {

  public static final ArgumentParser<Boolean> BOOLEAN = new ArgumentParser<Boolean>(
      "boolean") {
    @Override
    Boolean parse(OptionArg<Boolean> option, String arg) {
      if ("T".equalsIgnoreCase(arg) || "true".equalsIgnoreCase(arg)
          || "1".equals(arg)) {
        return true;
      } else if ("F".equalsIgnoreCase(arg) || "false".equalsIgnoreCase(arg)
          || "0".equals(arg)) {
        return false;
      }
      throw new CliException("Expected a boolean but found: '" + arg + "'.",
          CauseType.INVALID_ARG_FORMAT, option);
    }
  };

  public static final ArgumentParser<Long> LONG = new NumberParser<>(
      "long", Longs.stringConverter());

  public static final ArgumentParser<List<Long>> LONG_LIST = new NumberListParser<>(
      "long list", Longs.stringConverter());

  public static final ArgumentParser<Integer> INTEGER = new NumberParser<>(
      "int", Ints.stringConverter());

  public static final ArgumentParser<List<Integer>> INTEGER_LIST = new NumberListParser<>(
      "int list", Ints.stringConverter());

  public static final ArgumentParser<Double> DOUBLE = new NumberParser<>(
      "double", Doubles.stringConverter());

  public static final ArgumentParser<List<Double>> DOUBLE_LIST = new NumberListParser<>(
      "double list", Doubles.stringConverter());

  public static final ArgumentParser<List<String>> STRING_LIST = new ArgumentParser<List<String>>(
      "string list") {
    @Override
    List<String> parse(OptionArg<List<String>> option, String value) {
      return Splitter.on(ARG_LIST_SEPARATOR).splitToList(value);
    }
  };

  public static final ArgumentParser<String> STRING = new ArgumentParser<String>(
      "string") {
    @Override
    String parse(OptionArg<String> option, String value) {
      return value;
    }
  };

  static final char ARG_LIST_SEPARATOR = ',';
  private final String name;

  ArgumentParser(String nm) {
    name = nm;
  }

  abstract V parse(OptionArg<V> option, String arg);

  String name() {
    return name;
  }

  static CliException convertNFE(Option option, NumberFormatException e,
      String value, String argName) {
    return new CliException(String.format(
        "The option %s expects a %s, found '%s'.", option, argName,
        value), e, CauseType.INVALID_ARG_FORMAT, option);
  }

  static class NumberParser<T> extends ArgumentParser<T> {
    private final Converter<String, T> converter;

    NumberParser(String nm, Converter<String, T> conv) {
      super(nm);
      converter = conv;
    }

    @Override
    T parse(OptionArg<T> option, String arg) {
      try {
        return converter.convert(arg);
      } catch (final NumberFormatException e) {
        throw convertNFE(option, e, arg, name());
      }
    }
  }

  static class NumberListParser<T> extends ArgumentParser<List<T>> {
    private final Converter<String, T> converter;

    NumberListParser(String nm, Converter<String, T> conv) {
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
}
