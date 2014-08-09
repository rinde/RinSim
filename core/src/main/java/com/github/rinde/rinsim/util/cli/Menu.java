package com.github.rinde.rinsim.util.cli;

import static com.github.rinde.rinsim.util.cli.CliException.checkAlreadySelected;
import static com.github.rinde.rinsim.util.cli.CliException.checkCommand;
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

import com.github.rinde.rinsim.util.cli.CliException.CauseType;
import com.github.rinde.rinsim.util.cli.Option.OptionArg;
import com.github.rinde.rinsim.util.cli.Option.OptionNoArg;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * A menu is the main class for a command-line interface. It contains all
 * options and via the {@link #execute(String...)} method the command-line
 * arguments are parsed and handled. Instances can be constructed via the
 * {@link #builder()} method.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Menu {
  final String header;
  final String footer;
  final String cmdLineSyntax;
  final ImmutableMap<String, OptionParser> optionMap;
  final ImmutableList<ImmutableSet<Option>> groups;
  final ImmutableMultimap<Option, Option> groupMap;
  final HelpFormatter helpFormatter;

  Menu(Builder b) {
    header = b.header;
    footer = b.footer;
    cmdLineSyntax = b.cmdLineSyntax;
    optionMap = ImmutableMap.copyOf(b.optionMap);
    helpFormatter = b.helpFormatter;

    final ImmutableList.Builder<ImmutableSet<Option>> groupsBuilder = ImmutableList
        .builder();
    final ImmutableMultimap.Builder<Option, Option> groups2Builder = ImmutableMultimap
        .builder();
    for (final Set<Option> group : b.groups) {
      groupsBuilder.add(ImmutableSet.copyOf(group));
      for (final Option opt : group) {
        final Set<Option> groupWithoutMe = newLinkedHashSet(group);
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
  public Optional<String> safeExecute(String... args) {
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
  public Optional<String> execute(String... args) {
    final PeekingIterator<String> it = Iterators.peekingIterator(Iterators
        .forArray(args));
    final Set<Option> selectedOptions = newLinkedHashSet();
    while (it.hasNext()) {
      final String arg = it.next();
      final Optional<OptionParser> optParser = parseOption(arg);

      checkCommand(optParser.isPresent(), "Found unrecognized command: '%s'.",
          arg);
      checkAlreadySelected(
          !selectedOptions.contains(optParser.get().getOption()),
          optParser.get().getOption(),
          "Option is already selected: %s.", optParser.get().getOption());

      if (groupMap.containsKey(optParser.get().getOption())) {
        // this option is part of a option group
        final SetView<Option> intersect = Sets.intersection(
            selectedOptions,
            newLinkedHashSet(groupMap.get(optParser.get().getOption())));

        checkAlreadySelected(
            intersect.isEmpty(),
            optParser.get().getOption(),
            "An option from the same group as '%s' has already been selected: '%s'.",
            optParser.get().getOption(), intersect);
      }

      selectedOptions.add(optParser.get().getOption());
      if (optParser.get().getOption().isHelpOption()) {
        return Optional.of(printHelp());
      }
      final List<String> arguments = newArrayList();
      // if a non-option string is following the current option, it must be
      // the argument of the current option.
      while (it.hasNext() && !parseOption(it.peek()).isPresent()) {
        arguments.add(it.next());
      }
      try {
        optParser.get().parse(arguments);
      } catch (IllegalArgumentException | IllegalStateException e) {
        throw new CliException(e.getMessage(), e, CauseType.HANDLER_FAILURE,
            optParser.get().getOption());
      }
    }
    return Optional.absent();
  }

  /**
   * @return The header of the menu.
   */
  public String getHeader() {
    return header;
  }

  /**
   * @return The footer of the menu.
   */
  public String getFooter() {
    return footer;
  }

  /**
   * @return The command-line syntax of the menu.
   */
  public String getCmdLineSyntax() {
    return cmdLineSyntax;
  }

  Optional<OptionParser> parseOption(String arg) {
    if (arg.charAt(0) == '-') {
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

  /**
   * @return The help message as defined by this menu.
   */
  public String printHelp() {
    return helpFormatter.format(this);
  }

  /**
   * @return A list containing all options sorted by their short name.
   */
  public ImmutableList<Option> getOptions() {
    final List<Option> options = newArrayList();
    for (final OptionParser exec : newLinkedHashSet(optionMap.values())) {
      options.add(exec.getOption());
    }
    Collections.sort(options, new Comparator<Option>() {
      @Override
      public int compare(Option o1, Option o2) {
        return o1.getShortName().compareTo(o2.getShortName());
      }
    });
    return ImmutableList.copyOf(options);
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

  static void unexpectedArgument(List<String> argument, Option option) {
    if (!argument.isEmpty()) {
      throw new CliException(String.format(
          "The option %s does not support an argument. Found '%s'.", option,
          argument), CauseType.UNEXPECTED_ARG,
          option);
    }
  }

  /**
   * Builder for creating {@link Menu} instances.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class Builder {
    HelpFormatter helpFormatter;
    String header;
    String footer;
    String cmdLineSyntax;
    Map<String, OptionParser> optionMap;
    List<Set<Option>> groups;
    boolean buildingGroup;
    Set<String> optionNames;
    boolean addedHelpOption;

    Builder() {
      header = "";
      footer = "";
      cmdLineSyntax = "java -jar jarname <options>";
      optionMap = newLinkedHashMap();
      groups = newArrayList();
      buildingGroup = false;
      addedHelpOption = false;
      optionNames = newLinkedHashSet();
      helpFormatter = new DefaultHelpFormatter();
    }

    /**
     * Add an command-line option that expects an argument.
     * @param option The option instance.
     * @param subject The subject which will be passed to the handler.
     * @param handler The handler which will be called when this option is
     *          activated in the menu. The handler will receive all parsed
     *          arguments belonging to this option.
     * @param <V> The type of argument.
     * @param <S> The type of the subject.
     * @return This, as per the builder pattern.
     */
    public <V, S> Builder add(OptionArg<V> option, S subject,
        ArgHandler<S, V> handler) {
      add(new ArgParser<>(option, subject, handler));
      return this;
    }

    /**
     * Add an command-line option that does not expect an argument.
     * @param option The option instance.
     * @param subject The subject which will be passed to the handler.
     * @param handler The handler which will be called when this option is
     *          activated in the menu.
     * @param <S> The type of the subject.
     * @return This, as per the builder pattern.
     */
    public <S> Builder add(OptionNoArg option, S subject,
        NoArgHandler<S> handler) {
      add(new NoArgParser<>(option, subject, handler));
      return this;
    }

    /**
     * Add a help option. A help option is a special option that will trigger
     * the display of the help menu. A help option may not be added to a group.
     * @param sn The short name of the help option.
     * @param ln The long name of the help option.
     * @param desc The description of the help option.
     * @return This, as per the builder pattern.
     */
    public Builder addHelpOption(String sn, String ln, String desc) {
      checkState(!buildingGroup, "A help option can not be added to a group.");
      final OptionNoArg option = Option.builder(sn)
          .longName(ln)
          .description(desc)
          .buildHelpOption();
      add(new HelpParser(option));
      addedHelpOption = true;
      return this;
    }

    /**
     * Sets a {@link HelpFormatter}. If this method is not called the
     * {@link DefaultHelpFormatter} will be used.
     * @param formatter The formatter to use.
     * @return This, as per the builder pattern.
     */
    public Builder helpFormatter(HelpFormatter formatter) {
      helpFormatter = formatter;
      return this;
    }

    /**
     * Flags the start of the creation of a new group. A group is a set of
     * options which may not be selected at the same time. All options that are
     * added after this method is called and before a call to
     * {@link #closeGroup()} are part of this group. A group must contain at
     * least 2 options, any attempt to create a group with less than 2 options
     * will throw an {@link IllegalArgumentException}. If a group has previously
     * been under construction this method will automatically call
     * {@link #closeGroup()} to close the previous group and start a new group.
     * <p>
     * <b>Example:</b><br/>
     * This code will construct two groups, one containing two options and one
     * containing three options.
     * 
     * <pre>
     * {@code
     * Builder b = Menu.builder();
     * 
     * b.openGroup()
     * .add(..).add(..)
     * .openGroup()
     * .add(..).add(..).add(..)
     * .closeGroup();
     * }
     * </pre>
     * 
     * @return This, as per the builder pattern.
     */
    public Builder openGroup() {
      if (buildingGroup) {
        closeGroup();
      }
      buildingGroup = true;
      groups.add(Sets.<Option> newLinkedHashSet());
      return this;
    }

    /**
     * Flags the end of the creation of a group which was previously started
     * with {@link #openGroup()}.
     * @return This, as per the builder pattern.
     */
    public Builder closeGroup() {
      buildingGroup = false;
      final int groupOptions = groups.get(groups.size() - 1).size();
      checkArgument(
          groupOptions >= 2,
          "At least two options need to be added to a group, found %s option(s).",
          groupOptions);
      return this;
    }

    /**
     * Sets the header which may be displayed in the help menu. How it is shown
     * depends on the {@link HelpFormatter} that is used.
     * @param string The string to use as header.
     * @return This, as per the builder pattern.
     */
    public Builder header(String string) {
      header = string;
      return this;
    }

    /**
     * Sets the footer which may be displayed in the help menu. How it is shown
     * depends on the {@link HelpFormatter} that is used.
     * @param string The string to use as footer.
     * @return This, as per the builder pattern.
     */
    public Builder footer(String string) {
      footer = string;
      return this;
    }

    /**
     * Sets the command-line syntax, this can be displayed in the help menu. How
     * it is shown depends on the {@link HelpFormatter} that is used.
     * @param string The string that shows the command-line syntax.
     * @return This, as per the builder pattern.
     */
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
        Menu menu) {
      checkArgument(shortPrefix.matches(Option.NAME_REGEX),
          "The short prefix may not be an empty string.");
      checkArgument(longPrefix.matches(Option.NAME_REGEX),
          "The long prefix may not be an empty string.");
      checkState(
          !buildingGroup,
          "A submenu can not be added inside a group. First close the group before adding a submenu.");

      final Set<OptionParser> newOptions = newLinkedHashSet(menu.optionMap
          .values());
      for (final Set<Option> group : menu.groups) {
        openGroup();
        for (final Option option : group) {
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

    void checkDuplicateOption(String name) {
      checkArgument(!optionNames.contains(name),
          "Duplicate options are not allowed, found duplicate: '%s'.", name,
          optionNames);
    }

    void add(OptionParser e) {
      final Option option = e.getOption();
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

    static <T extends Option.Builder<?>> T adaptNames(T b, String sn,
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
      final Option opt = exec.getOption();
      if (opt instanceof OptionArg<?>) {
        final OptionArg<?> adapted =
            adaptNames(
                Option.builder((OptionArg<?>) opt),
                shortPrefix, longPrefix)
                .build();
        return ((ArgParser) exec).newInstance(adapted);
      } else {
        final OptionNoArg adapted =
            adaptNames(
                Option.builder((OptionNoArg) opt),
                shortPrefix, longPrefix)
                .build();
        return ((NoArgParser<?>) exec).newInstance(adapted);
      }
    }

    /**
     * Construct a new {@link Menu}.
     * @return A new instance containing the options as defined by this builder.
     */
    public Menu build() {
      checkArgument(addedHelpOption,
          "At least one help option is required for creating a menu.");
      return new Menu(this);
    }
  }

  interface OptionParser {
    /**
     * @return The option of this parser.
     */
    Option getOption();

    /**
     * Parse the arguments.
     * @param arguments The arguments to parse.
     */
    void parse(List<String> arguments);
  }

  static class HelpParser extends NoArgParser<Object> {
    @SuppressWarnings("null")
    HelpParser(OptionNoArg opt) {
      super(opt, null, null);
    }
  }

  static class NoArgParser<S> implements OptionParser {
    private final OptionNoArg option;
    private final S subject;
    private final NoArgHandler<S> handler;

    NoArgParser(OptionNoArg o, S s, NoArgHandler<S> h) {
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
    public Option getOption() {
      return option;
    }

    OptionParser newInstance(OptionNoArg o) {
      return new NoArgParser<S>(o, subject, handler);
    }
  }

  static class ArgParser<S, V> implements OptionParser {
    private final OptionArg<V> option;
    private final S subject;
    private final ArgHandler<S, V> handler;

    ArgParser(OptionArg<V> o, S s, ArgHandler<S, V> h) {
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

    @Override
    public Option getOption() {
      return option;
    }

    OptionParser newInstance(OptionArg<V> o) {
      return new ArgParser<S, V>(o, subject, handler);
    }
  }
}
