package rinde.sim.util.cli;

import java.util.List;

import rinde.sim.util.cli.CliException.CauseType;
import rinde.sim.util.cli.CliOption.CliOptionArg;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Longs;

public abstract class ArgumentParser<V> {
  static final char ARG_LIST_SEPARATOR = ',';

  public static final ArgumentParser<List<Long>> LONG_LIST = new ArgumentParser<List<Long>>() {
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

  public static final ArgumentParser<Long> LONG = new ArgumentParser<Long>() {
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

  public static final ArgumentParser<List<String>> STRING_LIST = new ArgumentParser<List<String>>() {
    @Override
    List<String> parseValue(CliOptionArg<List<String>> option, String value) {
      return Splitter.on(ARG_LIST_SEPARATOR).splitToList(value);
    }

    @Override
    String name() {
      return "string list";
    }
  };

  public static final ArgumentParser<String> STRING = new ArgumentParser<String>() {
    @Override
    String parseValue(CliOptionArg<String> option, String value) {
      return value;
    }

    @Override
    String name() {
      return "string";
    }
  };

  abstract V parseValue(CliOptionArg<V> option, String value);

  abstract String name();
}
