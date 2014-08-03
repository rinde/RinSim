package rinde.sim.util.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;

import rinde.sim.util.cli.CliException.CauseType;
import rinde.sim.util.cli.CliOption.CliOptionArg;
import rinde.sim.util.cli.CliOption.CliOptionNoArg;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

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
  final ImmutableMap<String, OptionParser> optionMap;
  final ImmutableList<ImmutableSet<CliOption>> groups;
  final ImmutableMultimap<CliOption, CliOption> groupMap;

  CliMenu(Builder b) {
    header = b.header;
    footer = b.footer;
    cmdLineSyntax = b.cmdLineSyntax;
    optionMap = ImmutableMap.copyOf(b.optionMap);

    final ImmutableList.Builder<ImmutableSet<CliOption>> groupsBuilder = ImmutableList
        .builder();
    final ImmutableMultimap.Builder<CliOption, CliOption> groups2Builder = ImmutableMultimap
        .builder();
    for (final Set<CliOption> group : b.groups) {
      groupsBuilder.add(ImmutableSet.copyOf(group));
      for (final CliOption opt : group) {
        final Set<CliOption> groupWithoutMe = newLinkedHashSet(group);
        groupWithoutMe.remove(opt);
        groups2Builder.putAll(opt, groupWithoutMe);
      }
    }
    groups = groupsBuilder.build();
    groupMap = groups2Builder.build();
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
    final PeekingIterator<String> it = Iterators.peekingIterator(Iterators
        .forArray(args));
    final Set<CliOption> selectedOptions = newLinkedHashSet();
    while (it.hasNext()) {
      final String arg = it.next();
      final Optional<OptionParser> exec = parseOption(arg);
      if (exec.isPresent()) {
        if (selectedOptions.contains(exec.get().getOption())) {
          throw new CliException("Option is already selected: "
              + exec.get().getOption() + ".", CauseType.ALREADY_SELECTED, exec
              .get().getOption());
        }
        if (groupMap.containsKey(exec.get().getOption())) {
          // this option is part of a option group
          final SetView<CliOption> intersect = Sets.intersection(
              selectedOptions,
              newLinkedHashSet(groupMap.get(exec.get().getOption())));
          if (!intersect.isEmpty()) {
            final String msg = new StringBuilder()
                .append("An option from the same group as '")
                .append(exec.get().getOption())
                .append("'has already been selected: '")
                .append(intersect.iterator().next())
                .append("'.")
                .toString();
            throw new CliException(msg, CauseType.ALREADY_SELECTED, exec.get()
                .getOption());
          }
        }

        selectedOptions.add(exec.get().getOption());
        if (exec.get().getOption().isHelpOption()) {
          return Optional.of(printHelp());
        }
        final List<String> arguments = newArrayList();
        // if a non-option string is following the current option, it must be
        // the argument of the current option.
        while (it.hasNext() && !parseOption(it.peek()).isPresent()) {
          arguments.add(it.next());
        }
        try {
          exec.get().parse(arguments);
        } catch (IllegalArgumentException | IllegalStateException e) {
          throw new CliException(e.getMessage(), e, CauseType.INVALID,
              exec.get().getOption());
        }
      } else {
        throwUnexpectedArtifact(arg);
      }
    }

    // try {
    // line = parser.parse(options, args);
    // } catch (final MissingArgumentException e) {
    // final CliOptionArg option = (CliOptionArg) optionMap.get(e.getOption()
    // .getOpt()).getOption();
    // throw new CliException("The option " + option + " requires a "
    // + option.argumentType.name() + " as argument.",
    // CauseType.MISSING_ARG,
    // option);
    // } catch (final AlreadySelectedException e) {
    // e.printStackTrace();
    // throw new CliException(e.getMessage(), CauseType.ALREADY_SELECTED,
    // optionMap.get(e.getOption().getOpt()).getOption());
    // } catch (final ParseException e) {
    // throw new CliException("Parsing failed. Reason: " + e.getMessage(),
    // CauseType.PARSE_EXCEPTION);
    // }
    //
    // for (final Option option : line.getOptions()) {
    // final Executor ex = optionMap.get(option.getOpt());
    // if (ex.getOption().isHelpOption()) {
    // return Optional.of(printHelp());
    // }
    // try {
    // ex.execute(line);
    // } catch (IllegalArgumentException | IllegalStateException e) {
    // throw new CliException(e.getMessage(), e, CauseType.INVALID,
    // ex.getOption());
    // }
    // }
    return Optional.absent();
  }

  Optional<OptionParser> parseOption(String arg) {
    if (arg.startsWith("-")) {
      final String optName;
      if (arg.startsWith("--")) {
        optName = arg.substring(2);
      } else {
        optName = arg.substring(1);
      }
      if (optionMap.containsKey(optName)) {
        return Optional.of(optionMap.get(optName));
      }
    }
    return Optional.absent();
  }

  static void throwUnexpectedArtifact(String artifact) {
    final String msg = new StringBuilder()
        .append("Found unexpected artifact: '")
        .append(artifact)
        .append("'.")
        .toString();
    throw new CliException(msg, CauseType.PARSE_EXCEPTION);
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
    final List<CliOption> options = newArrayList();
    for (final OptionParser exec : newLinkedHashSet(optionMap.values())) {
      options.add(exec.getOption());
    }
    Collections.sort(options, new Comparator<CliOption>() {
      @Override
      public int compare(CliOption o1, CliOption o2) {
        return o1.getShortName().compareTo(o2.getShortName());
      }
    });

    final List<String> optionNames = newArrayList();
    int maxLength = 0;
    for (final CliOption opt : options) {
      final StringBuilder sb = new StringBuilder();
      sb.append(" -").append(opt.getShortName());
      if (opt.getLongName().isPresent()) {
        sb.append(",--").append(opt.getLongName().get());
      }

      if (opt.getArgument().isPresent()) {
        sb.append(" <").append(opt.getArgument().get().name()).append(">");
      }
      optionNames.add(sb.toString());
      maxLength = Math.max(sb.length(), maxLength);
    }
    final int total = 80;
    final int nameLength = maxLength + 3;
    final int descLength = total - nameLength;

    final StringBuilder sb = new StringBuilder();
    if (!cmdLineSyntax.trim().isEmpty()) {
      sb.append("usage: ").append(cmdLineSyntax).append(System.lineSeparator());
    }
    if (!header.trim().isEmpty()) {
      sb.append(header).append(System.lineSeparator());
    }

    for (int i = 0; i < options.size(); i++) {
      sb.append(Strings.padEnd(optionNames.get(i), nameLength, ' '));
      final String[] descParts = options.get(i).getDescription()
          .split(System.lineSeparator());
      for (int j = 0; j < descParts.length; j++) {
        if (j > 0) {
          sb.append(Strings.padEnd("", nameLength, ' '));
        }
        sb.append(
            WordUtils.wrap(
                descParts[j],
                descLength,
                Strings.padEnd(System.lineSeparator(), nameLength + 1, ' '),
                false))
            .append(System.lineSeparator());
      }
    }
    if (!footer.trim().isEmpty()) {
      sb.append(footer).append(System.lineSeparator());
    }
    return sb.toString();
  }

  /**
   * Checks whether the specified option name is an option in this menu.
   * @param optionName The option name to check.
   * @return <code>true</code> if this menu has an option with the specified
   *         option name, <code>false</code> otherwise.
   */
  public boolean containsOption(String optionName) {
    return optionMap.containsKey(optionName);
  }

  /**
   * @return The set of option names this menu supports.
   */
  public ImmutableSet<String> getOptionNames() {
    return optionMap.keySet();
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
    Map<String, OptionParser> optionMap;
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
      optionNames = newLinkedHashSet();
    }

    public <V, S> Builder add(CliOptionArg<V> option, S subject,
        ArgHandler<S, V> handler) {
      add(new ArgParser<>(option, subject, handler));
      return this;
    }

    public <S> Builder add(CliOptionNoArg option, S subject,
        NoArgHandler<S> handler) {
      add(new NoArgParser<>(option, subject, handler));
      return this;
    }

    void checkDuplicateOption(String name) {
      checkArgument(!optionNames.contains(name),
          "Duplicate options are not allowed, found duplicate: '%s'.", name,
          optionNames);
    }

    void add(OptionParser e) {
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
      checkState(!buildingGroup);
      final CliOptionNoArg option = CliOption.builder(sn)
          .longName(ln)
          .description(desc)
          .buildHelpOption();
      add(new HelpParser(option));
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

    /**
     * Add the specified menu as a sub menu into the menu that this builder
     * instance is constructing. Each option from the specified menu will be
     * added to this builder with the specified prefixes. Help options are
     * ignored and will not be added to the new menu.
     * <p>
     * <b>Example:</b><br/>
     * If a menu with options <code>(a, add), (b), (c, construct)</code> is
     * added with <code>shortPrefix = 's', longPrefix = 'sub.'</code>, the
     * resulting menu will be
     * <code>(sa, sub.add),(sb),(sc, sub.construct)</code>.
     * 
     * @param shortPrefix The prefix to use for the short option names.
     * @param longPrefix The prefix to use for the long option names.
     * @param menu The menu to add as a sub menu.
     * @return This, as per the builder pattern.
     */
    public Builder addSubMenu(String shortPrefix, String longPrefix,
        CliMenu menu) {
      checkArgument(!shortPrefix.trim().isEmpty(),
          "The short prefix may not be an empty string.");
      checkArgument(!longPrefix.trim().isEmpty(),
          "The long prefix may not be an empty string.");
      checkState(
          !buildingGroup,
          "A submenu can not be added inside a group. First close the group before adding a submenu.");

      final Set<OptionParser> newOptions = newLinkedHashSet(menu.optionMap
          .values());
      for (final Set<CliOption> group : menu.groups) {
        openGroup();
        for (final CliOption option : group) {
          final OptionParser exec = menu.optionMap.get(option
              .getShortName());
          add(adapt(exec, shortPrefix, longPrefix));
          newOptions.remove(exec);
        }
        closeGroup();
      }
      for (final OptionParser exec : newOptions) {
        if (!exec.getOption().isHelpOption()) {
          add(adapt(exec,
              shortPrefix,
              longPrefix));
        }
      }
      return this;
    }

    static <T extends CliOption.Builder<?>> T adaptNames(T b, String sn,
        String ln) {
      b.shortName(sn + b.shortName);
      if (b.longName.isPresent()) {
        b.longName(ln + b.longName.get());
      }
      return b;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static OptionParser adapt(OptionParser exec, String shortPrefix,
        String longPrefix) {
      final CliOption opt = exec.getOption();
      if (opt instanceof CliOptionArg<?>) {
        final CliOptionArg<?> adapted =
            adaptNames(
                CliOption.builder((CliOptionArg<?>) opt),
                shortPrefix, longPrefix)
                .build();
        return ((ArgParser) exec).newInstance(adapted);
      } else {
        final CliOptionNoArg adapted =
            adaptNames(
                CliOption.builder((CliOptionNoArg) opt),
                shortPrefix, longPrefix)
                .build();
        return ((NoArgParser<?>) exec).newInstance(adapted);
      }
    }

    /**
     * Construct a new {@link CliMenu}.
     * @return A new instance containing the options as defined by this builder.
     */
    public CliMenu build() {
      return new CliMenu(this);
    }
  }

  interface OptionParser {

    CliOption getOption();

    void parse(List<String> arguments);
  }

  static class HelpParser implements OptionParser {
    final CliOption option;

    HelpParser(CliOption opt) {
      option = opt;
    }

    @Override
    public CliOption getOption() {
      return option;
    }

    @Override
    public void parse(List<String> arguments) {
      unexpectedArgument(arguments, option);
    }
  }

  static void unexpectedArgument(List<String> argument, CliOption option) {
    if (!argument.isEmpty()) {
      final String msg = new StringBuilder()
          .append("The option ")
          .append(option)
          .append(" does not support an argument. Found '")
          .append(argument)
          .append("'.")
          .toString();
      throw new CliException(msg, CauseType.UNEXPECTED_ARG,
          option);
    }
  }

  static class NoArgParser<S> implements OptionParser {
    private final CliOptionNoArg option;
    private final S subject;
    private final NoArgHandler<S> handler;

    NoArgParser(CliOptionNoArg o, S s, NoArgHandler<S> h) {
      option = o;
      subject = s;
      handler = h;
    }

    @Override
    public void parse(List<String> argument) {
      unexpectedArgument(argument, option);
      handler.execute(subject);
    }

    @Override
    public CliOption getOption() {
      return option;
    }

    OptionParser newInstance(CliOptionNoArg o) {
      return new NoArgParser<S>(o, subject, handler);
    }
  }

  static class ArgParser<S, V> implements OptionParser {
    private final CliOptionArg<V> option;
    private final S subject;
    private final ArgHandler<S, V> handler;

    ArgParser(CliOptionArg<V> o, S s, ArgHandler<S, V> h) {
      option = o;
      subject = s;
      handler = h;
    }

    @Override
    public void parse(List<String> arguments) {
      Optional<V> value;
      if (!arguments.isEmpty()) {
        value = Optional
            .of(option.argumentType.parse(option,
                Joiner.on(ArgumentParser.ARG_LIST_SEPARATOR).join(arguments)));
      } else if (!option.isArgOptional()) {
        throw new CliException("The option " + option + " requires a "
            + option.argumentType.name() + " argument.",
            CauseType.MISSING_ARG,
            option);
      } else {
        value = Optional.absent();
      }
      handler.execute(subject, value);
    }

    // static Optional<String> asString(CliOptionArg<?> option,
    // CommandLine commandLine) {
    // final String[] vals = commandLine.getOptionValues(option.getShortName());
    //
    // if (vals == null) {
    // if (option.isArgOptional) {
    // return Optional.absent();
    // }
    // throw new CliException("The option " + option + " requires a "
    // + option.argumentType.name() + " argument.",
    // CauseType.MISSING_ARG,
    // option);
    // }
    //
    // return Optional.of(Joiner.on(CliOption.ARG_LIST_SEPARATOR).join(vals));
    // }

    @Override
    public CliOption getOption() {
      return option;
    }

    OptionParser newInstance(CliOptionArg<V> o) {
      return new ArgParser<S, V>(o, subject, handler);
    }
  }
}
