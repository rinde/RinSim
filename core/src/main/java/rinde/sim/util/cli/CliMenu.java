package rinde.sim.util.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newHashSet;

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
import rinde.sim.util.cli.CliOption.CliOptionArg;
import rinde.sim.util.cli.CliOption.CliOptionNoArg;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Requires a subject which forms the API of the system that you want to create
 * command-line interface for. This API is typically a builder class, but it is
 * not restricted.
 * 
 * 
 * @param <T>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class CliMenu {
  final String header;
  final String footer;
  final String cmdLineSyntax;
  final Options options;
  final Map<String, Executor> optionMap;

  CliMenu(Options op, Builder b) {
    header = b.header;
    footer = b.footer;
    cmdLineSyntax = b.cmdLineSyntax;
    options = op;
    optionMap = ImmutableMap.copyOf(b.optionMap);
  }

  /**
   * Same as {@link #execute(String...)} but catches all thrown
   * {@link CliException}s. If an exception is thrown it's message will be added
   * to the returned error message.
   * @param args The arguments to parse.
   * @return A string containing an error message or {@link Optional#absent()}
   *         if no error occurred.
   */
  public Optional<String> safeExecute(String[] args) {
    try {
      return execute(args);
    } catch (final CliException e) {
      return Optional.of(Joiner.on("\n").join(e.getMessage(), printHelp()));
    }
  }

  /**
   * Parses and executes the provided command-line arguments.
   * @param args The arguments to parse.
   * @return A string containing the help message, or {@link Optional#absent()}
   *         if no help was requested.
   * @throws CliException If anything in the parsing or execution went wrong.
   */
  public Optional<String> execute(String... args) throws CliException {
    final CommandLineParser parser = new BasicParser();
    final CommandLine line;
    try {
      line = parser.parse(options, args);
    } catch (final MissingArgumentException e) {
      final CliOptionArg option = (CliOptionArg) optionMap.get(e.getOption()
          .getOpt()).getOption();
      throw new CliException("The option " + option + " requires a "
          + option.argumentType.name() + " as argument.",
          CauseType.MISSING_ARG,
          option);
    } catch (final AlreadySelectedException e) {
      throw new CliException(e.getMessage(), CauseType.ALREADY_SELECTED,
          optionMap.get(e.getOption().getOpt()).getOption());
    } catch (final ParseException e) {
      throw new CliException("Parsing failed. Reason: " + e.getMessage(),
          CauseType.PARSE_EXCEPTION);
    }

    for (final Option option : line.getOptions()) {
      final Executor ex = optionMap.get(option.getOpt());
      if (ex.getOption().isHelpOption()) {
        return Optional.of(printHelp());
      }
      try {
        ex.execute(line);
      } catch (IllegalArgumentException | IllegalStateException e) {
        throw new CliException(e.getMessage(), e, CauseType.INVALID,
            ex.getOption());
      }
    }
    return Optional.absent();
  }

  // Optional<String> exec(final CliOption option, CommandLine commandLine) {
  // final Optional<?> value;
  // final Optional<String> rawValue = asString(option, commandLine);
  // if (rawValue.isPresent()) {
  // value = Optional
  // .of(option.argumentType.get().parseValue(option, rawValue.get()));
  // } else {
  // value = Optional.absent();
  // }
  //
  // try {
  // if (option.isHelpOption()) {
  // return Optional.of(printHelp());
  // }
  // option.execute(subject, value);
  // return Optional.absent();
  // } catch (IllegalArgumentException | IllegalStateException e) {
  // throw new CliException(e.getMessage(), e, CauseType.INVALID, option);
  // }
  // }

  /**
   * @return The help message as defined by this menu.
   */
  public String printHelp() {
    final HelpFormatter formatter = new HelpFormatter();
    final StringWriter sw = new StringWriter();
    formatter.printHelp(new PrintWriter(sw), formatter.getWidth(),
        cmdLineSyntax, header, options, formatter.getLeftPadding(),
        formatter.getDescPadding(), footer);
    return sw.toString();
  }

  /**
   * Construct a new builder for creating a command-line interface menu.
   * @return A new builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    String header;
    String footer;
    String cmdLineSyntax;
    Map<String, Executor> optionMap;
    List<Set<CliOption>> groups;
    boolean buildingGroup;
    Set<String> optionNames;

    Builder() {
      header = "";
      footer = "";
      cmdLineSyntax = "java -jar jarname <options>";
      optionMap = newLinkedHashMap();
      groups = newArrayList();
      buildingGroup = false;
      optionNames = newHashSet();
    }

    public <V, S> Builder add(CliOptionArg<V> option, S subject,
        ArgHandler<S, V> handler) {
      add(new ArgExecutor<>(option, subject, handler));
      return this;
    }

    public <S> Builder add(CliOptionNoArg option, S subject,
        NoArgHandler<S> handler) {
      add(new NoArgExecutor<>(option, subject, handler));
      return this;
    }

    void checkDuplicateOption(String name) {
      checkArgument(!optionNames.contains(name),
          "Duplicate options are not allowed, found duplicate: '%s'.", name);
    }

    void add(Executor e) {
      final CliOption option = e.getOption();
      final String sn = option.getShortName();

      checkDuplicateOption(sn);
      optionNames.add(sn);
      optionMap.put(sn, e);
      if (option.getLongName().isPresent()) {
        final String ln = option.getLongName().get();
        checkDuplicateOption(ln);
        optionNames.add(ln);
        optionMap.put(ln, e);
      }

      if (buildingGroup) {
        groups.get(groups.size() - 1).add(option);
      }
    }

    public Builder addHelpOption(String sn, String ln, String desc) {
      final CliOptionNoArg option = CliOption.builder(sn)
          .longName(ln)
          .description(desc)
          .buildHelpOption();
      add(new HelpExecutor(option));
      return this;
    }

    public Builder openGroup() {
      if (buildingGroup) {
        closeGroup();
      }
      buildingGroup = true;
      groups.add(Sets.<CliOption> newLinkedHashSet());
      return this;
    }

    public Builder closeGroup() {
      buildingGroup = false;
      if (groups.get(groups.size() - 1).isEmpty()) {
        throw new IllegalArgumentException(
            "No options were added to the group.");
      }
      return this;
    }

    public Builder header(String string) {
      header = string;
      return this;
    }

    public Builder footer(String string) {
      footer = string;
      return this;
    }

    public Builder commandLineSyntax(String string) {
      cmdLineSyntax = string;
      return this;
    }

    public Builder addSubMenu(String shortPrefix, String longPrefix,
        Builder subMenuBuilder) {
      checkArgument(!shortPrefix.trim().isEmpty());
      checkArgument(!longPrefix.trim().isEmpty());
      checkState(!buildingGroup);
      for (final Set<CliOption> group : subMenuBuilder.groups) {
        openGroup();
        for (final CliOption option : group) {
          final Executor exec = subMenuBuilder.optionMap.get(option
              .getShortName());
          if (!exec.getOption().isHelpOption()) {
            add(adapt(exec, shortPrefix, longPrefix));
          }
        }
        closeGroup();
      }
      for (final Executor exec : subMenuBuilder.optionMap.values()) {
        if (!optionMap.containsKey(exec.getOption().getShortName())
            && !exec.getOption().isHelpOption()) {
          add(adapt(exec,
              shortPrefix,
              longPrefix));
        }
      }
      return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Executor adapt(Executor exec, String subMenuShortPrefix,
        String subMenuLongPrefix) {
      final CliOption opt = exec.getOption();
      if (opt instanceof CliOptionArg<?>) {
        final CliOptionArg<?> adapted = CliOption
            .builder((CliOptionArg<?>) opt)
            .shortName(subMenuShortPrefix + opt.getShortName())
            .longName(subMenuLongPrefix + opt.getLongName())
            .build();
        return ((ArgExecutor) exec).newInstance(adapted);
      } else if (exec.getOption() instanceof CliOptionNoArg) {
        final CliOptionNoArg adapted = CliOption
            .builder((CliOptionNoArg) opt)
            .shortName(subMenuShortPrefix + opt.getShortName())
            .longName(subMenuLongPrefix + opt.getLongName())
            .build();
        return ((NoArgExecutor<?>) exec).newInstance(adapted);
      }
      throw new IllegalStateException();
    }

    public CliMenu build() {
      final Options options = new Options();
      for (final Set<CliOption> g : groups) {
        final OptionGroup og = new OptionGroup();
        for (final CliOption option : g) {
          og.addOption(option.create());
        }
        options.addOptionGroup(og);
      }
      for (final Executor exec : optionMap.values()) {
        if (!options.hasOption(exec.getOption().getShortName())) {
          options.addOption(exec.getOption().create());
        }
      }
      return new CliMenu(options, this);
    }

    // <U, X> CliOption<T> adapt1(String shortPrefix,
    // String longPrefix,
    // CliOption<U> option, U subj) {
    // // FIXME clean this mess
    // return ((ArgBuilder<T>) CliOption
    // .<U> builder(option)
    // .shortName(shortPrefix + option.getShortName())
    // .longName(longPrefix + option.getLongName())).build(
    // new OptionHandlerAdapter<T, U, X>(
    // (OptionHandler<U, X>) option.handler, subj));
    // }
  }

  interface Executor {

    CliOption getOption();

    void execute(CommandLine commandLine);
  }

  static class HelpExecutor implements Executor {
    final CliOption option;

    HelpExecutor(CliOption opt) {
      option = opt;
    }

    @Override
    public CliOption getOption() {
      return option;
    }

    @Override
    public void execute(CommandLine commandLine) {}
  }

  static class NoArgExecutor<S> implements Executor {
    private final CliOptionNoArg option;
    private final S subject;
    private final NoArgHandler<S> handler;

    NoArgExecutor(CliOptionNoArg o, S s, NoArgHandler<S> h) {
      option = o;
      subject = s;
      handler = h;
    }

    @Override
    public void execute(CommandLine commandLine) {
      handler.execute(subject);
    }

    @Override
    public CliOption getOption() {
      return option;
    }

    Executor newInstance(CliOptionNoArg o) {
      return new NoArgExecutor<S>(o, subject, handler);
    }

  }

  static class ArgExecutor<S, V> implements Executor {
    private final CliOptionArg<V> option;
    private final S subject;
    private final ArgHandler<S, V> handler;

    ArgExecutor(CliOptionArg<V> o, S s, ArgHandler<S, V> h) {
      option = o;
      subject = s;
      handler = h;
    }

    @Override
    public void execute(CommandLine commandLine) {
      final Optional<String> rawValue = asString(option, commandLine);
      Optional<V> value;
      if (rawValue.isPresent()) {
        value = Optional
            .of(option.argumentType.parseValue(option, rawValue.get()));
      } else {
        value = Optional.absent();
      }
      handler.execute(subject, value);
    }

    static Optional<String> asString(CliOptionArg<?> option,
        CommandLine commandLine) {
      final String[] vals = commandLine.getOptionValues(option.getShortName());

      if (vals == null) {
        if (option.isArgOptional) {
          return Optional.absent();
        }
        throw new CliException("The option " + option + " requires a "
            + option.argumentType.name() + " argument.",
            CauseType.MISSING_ARG,
            option);
      }

      return Optional.of(Joiner.on(CliOption.ARG_LIST_SEPARATOR).join(vals));
    }

    @Override
    public CliOption getOption() {
      return option;
    }

    Executor newInstance(CliOptionArg<V> o) {
      return new ArgExecutor<S, V>(o, subject, handler);
    }

  }

  static class OptionHandlerAdapter<T, U, X> implements ArgHandler<T, X> {
    private final ArgHandler<U, X> delegate;
    private final U subject;

    OptionHandlerAdapter(ArgHandler<U, X> deleg, U subj) {
      delegate = deleg;
      subject = subj;
    }

    @Override
    public void execute(T ref, Optional<X> value) {
      delegate.execute(subject, value);
    }
  }
}
