package rinde.sim.util.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import rinde.sim.util.cli.CliException.CauseType;
import rinde.sim.util.cli.CliOption.OptionArgType;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Requires a subject which forms the API of the system that you want to create
 * command-line interface for. This API is typically a builder class, but it is
 * not restricted.
 * 
 * 
 * @param <T>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class CliMenu<T> {
  final String header;
  final String footer;
  final String cmdLineSyntax;
  final Options options;
  final Map<String, CliOption> optionMap;
  final T subject;

  CliMenu(Options op, Builder<T> b) {
    header = b.header;
    footer = b.footer;
    cmdLineSyntax = b.cmdLineSyntax;
    options = op;
    optionMap = ImmutableMap.copyOf(b.optionMap);
    subject = b.subject;
  }

  public Optional<String> safeExecute(String[] args) {
    try {
      return execute(args);
    } catch (final CliException e) {
      System.err.println(e.getMessage());
      printHelp();
      return Optional.of("");
    }
  }

  public Optional<String> execute(String... args) throws CliException {
    final CommandLineParser parser = new BasicParser();
    final CommandLine line;
    try {
      line = parser.parse(options, args);
    } catch (final MissingArgumentException e) {
      final CliOption option = optionMap.get(e.getOption().getOpt());
      throw new CliException("The option " + option + " requires a "
          + option.argumentType.name() + " as argument.",
          CauseType.MISSING_ARG,
          option);
    } catch (final AlreadySelectedException e) {
      throw new CliException(e.getMessage(), CauseType.ALREADY_SELECTED,
          optionMap.get(e.getOption().getOpt()));
    } catch (final ParseException e) {
      throw new CliException("Parsing failed. Reason: " + e.getMessage(),
          CauseType.PARSE_EXCEPTION);
    }

    for (final Option option : line.getOptions()) {
      final Optional<String> error = exec(optionMap.get(option.getOpt()), line);
      if (error.isPresent()) {
        return error;
      }
    }
    return Optional.absent();
  }

  Optional<String> exec(final CliOption option, CommandLine commandLine) {
    final Optional<?> value;
    final Optional<String> rawValue = asString(option, commandLine);
    if (rawValue.isPresent()) {
      value = Optional
          .of(option.argumentType.parseValue(option, rawValue.get()));
    } else {
      value = Optional.absent();
    }

    try {
      if (option.isHelpOption()) {
        return Optional.of(printHelp());
      }
      option.execute(subject, value);
      return Optional.absent();
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new CliException(e.getMessage(), e, CauseType.INVALID, option);
    }
  }

  static Optional<String> asString(CliOption option, CommandLine commandLine) {
    final String[] vals = commandLine.getOptionValues(option.getShortName());

    if (vals == null) {
      if (option.isArgOptional || option.argumentType == OptionArgType.NO_ARG) {
        return Optional.absent();
      }
      throw new CliException("The option " + option + " requires a "
          + option.argumentType.name() + " argument.", CauseType.MISSING_ARG,
          option);
    }

    return Optional.of(Joiner.on(CliOption.ARG_LIST_SEPARATOR).join(vals));
  }

  public String printHelp() {
    final HelpFormatter formatter = new HelpFormatter();
    final StringWriter sw = new StringWriter();
    formatter.printHelp(new PrintWriter(sw), formatter.getWidth(),
        cmdLineSyntax, header, options, formatter.getLeftPadding(),
        formatter.getDescPadding(), footer);
    return sw.toString();
  }

  public static <T> Builder<T> builder(T subject) {
    return new Builder<>(subject);
  }

  public static final class Builder<T> {
    String header;
    String footer;
    String cmdLineSyntax;
    Map<String, CliOption> optionMap;
    List<Set<CliOption>> groups;
    final T subject;

    Builder(T t) {
      subject = t;
      header = "";
      footer = "";
      cmdLineSyntax = "java -jar jarname <options>";
      optionMap = newLinkedHashMap();
      groups = newArrayList();
    }

    public Builder<T> add(Iterable<CliOption> options) {
      for (final CliOption mo : options) {
        checkArgument(!optionMap.containsKey(mo.getShortName()),
            "An option with %s already exists.", mo.getShortName());
        optionMap.put(mo.getShortName(), mo);
      }
      return this;
    }

    @SafeVarargs
    public final Builder<T> add(CliOption... options) {
      return add(asList(options));
    }

    public Builder<T> addGroup(Iterable<CliOption> options) {
      groups.add(ImmutableSet.copyOf(options));
      add(options);
      return this;
    }

    @SafeVarargs
    public final Builder<T> addGroup(CliOption... options) {
      return addGroup(asList(options));
    }

    public Builder<T> header(String string) {
      header = string;
      return this;
    }

    public Builder<T> footer(String string) {
      footer = string;
      return this;
    }

    public Builder<T> commandLineSyntax(String string) {
      cmdLineSyntax = string;
      return this;
    }

    public <U> Builder<T> addSubMenu(
        String subMenuShortPrefix,
        String subMenuLongPrefix,
        CliMenu.Builder<U> subMenuBuilder) {
      checkArgument(!subMenuShortPrefix.trim().isEmpty());
      checkArgument(!subMenuLongPrefix.trim().isEmpty());
      for (final Set<CliOption> group : subMenuBuilder.groups) {
        final List<CliOption> adaptedOptions = newArrayList();
        for (final CliOption option : group) {
          adaptedOptions.add(
              CliMenu.<T, U> adapt1(
                  subMenuShortPrefix,
                  subMenuLongPrefix,
                  option,
                  subMenuBuilder.subject));
        }
        addGroup(adaptedOptions);
      }
      for (final CliOption option : subMenuBuilder.optionMap.values()) {
        if (!optionMap.containsKey(option.getShortName())) {
          add(CliMenu.<T, U> adapt1(
              subMenuShortPrefix,
              subMenuLongPrefix,
              option,
              subMenuBuilder.subject));
        }
      }
      return this;
    }

    public CliMenu<T> build() {
      final Options options = new Options();
      for (final Set<CliOption> g : groups) {
        final OptionGroup og = new OptionGroup();
        for (final CliOption option : g) {
          og.addOption(option.create());
        }
        options.addOptionGroup(og);
      }
      for (final CliOption option : optionMap.values()) {
        if (!options.hasOption(option.getShortName())) {
          options.addOption(option.create());
        }
      }
      return new CliMenu<T>(options, this);
    }
  }

  static <T, U> CliOption adapt1(String shortPrefix,
      String longPrefix,
      CliOption option, U subj) {

    return adapt(shortPrefix, longPrefix, option, subj);
  }

  static <T, U, X> CliOption adapt(
      String shortPrefix,
      String longPrefix,
      CliOption option, U subj) {
    return CliOption
        .builder(option)
        .shortName(shortPrefix + option.getShortName())
        .longName(longPrefix + option.getLongName())
        .build(
            new OptionHandlerAdapter<T, U, X>(
                (OptionHandler<U, X>) option.handler, subj));
  }

  static class OptionHandlerAdapter<T, U, X> implements OptionHandler<T, X> {
    private final OptionHandler<U, X> delegate;
    private final U subject;

    OptionHandlerAdapter(OptionHandler<U, X> deleg, U subj) {
      delegate = deleg;
      subject = subj;
    }

    @Override
    public void execute(T ref, Optional<X> value) {
      delegate.execute(subject, value);
    }
  }
}
