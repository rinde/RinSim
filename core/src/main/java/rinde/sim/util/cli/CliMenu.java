package rinde.sim.util.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CliMenu<T> {

  final String header;
  final String footer;
  final String cmdLineSyntax;
  final Options options;
  final Map<String, MenuOption<T>> optionMap;
  final T subject;

  CliMenu(Options op, Builder<T> b) {
    header = b.header;
    footer = b.footer;
    cmdLineSyntax = b.cmdLineSyntax;
    options = op;
    optionMap = ImmutableMap.copyOf(b.optionMap);
    subject = b.subject;
  }

  public boolean safeExecute(String[] args) {
    try {
      return execute(args);
    } catch (final CliException e) {
      System.err.println(e.getMessage());
      printHelp();
      return false;
    }
  }

  public boolean execute(String[] args)
      throws CliException {
    final CommandLineParser parser = new BasicParser();
    final CommandLine line;
    try {
      line = parser.parse(options, args);
    } catch (final MissingArgumentException e) {
      throw new CliException(e.getMessage(), e, optionMap.get(e
          .getOption().getOpt()));
    } catch (final AlreadySelectedException e) {
      throw new CliException(e.getMessage(), e, optionMap.get(e
          .getOption().getOpt()));
    } catch (final ParseException e) {
      throw new CliException("Parsing failed. Reason: " + e.getMessage(), e);
    }

    for (final MenuOption<T> option : optionMap.values()) {
      if (line.hasOption(option.getShortName())
          || line.hasOption(option.getLongName())) {
        final Value v = new Value(line, option);
        try {
          if (!option.execute(subject, v)) {
            printHelp();
            return false;
          }
        } catch (final IllegalArgumentException | IllegalStateException e) {
          throw new CliException("Problem with " + v.optionUsed()
              + " option: " + e.getMessage(), e, option);
        }
      }
    }
    return true;
  }

  public void printHelp() {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(cmdLineSyntax, header, options, footer);
  }

  public static <T> Builder<T> builder(T subject) {
    return new Builder<>(subject);
  }

  public static final class Builder<T> {
    String header;
    String footer;
    String cmdLineSyntax;
    Map<String, MenuOption<T>> optionMap;
    List<Set<MenuOption<T>>> groups;
    final T subject;

    Builder(T t) {
      subject = t;
      header = "";
      footer = "";
      cmdLineSyntax = "java";
      optionMap = newLinkedHashMap();
      groups = newArrayList();
    }

    public Builder<T> add(Iterable<MenuOption<T>> options) {
      for (final MenuOption<T> mo : options) {
        checkArgument(!optionMap.containsKey(mo.getShortName()),
            "An option with %s already exists.", mo.getShortName());
        optionMap.put(mo.getShortName(), mo);
      }
      return this;
    }

    @SafeVarargs
    public final Builder<T> add(MenuOption<T>... options) {
      return add(asList(options));
    }

    public Builder<T> addGroup(Iterable<MenuOption<T>> options) {
      groups.add(ImmutableSet.copyOf(options));
      add(options);
      return this;
    }

    @SafeVarargs
    public final Builder<T> addGroup(MenuOption<T>... options) {
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

    public <U> Builder<T> addSubMenu(String subMenuShortPrefix,
        String subMenuLongPrefix,
        CliMenu.Builder<U> subMenuBuilder) {
      checkArgument(!subMenuShortPrefix.trim().isEmpty());
      checkArgument(!subMenuLongPrefix.trim().isEmpty());
      for (final Set<MenuOption<U>> group : subMenuBuilder.groups) {
        final List<MenuOption<T>> adaptedOptions = newArrayList();
        for (final MenuOption<U> mo : group) {
          adaptedOptions.add(new MenuOptionAdapter<T, U>(subMenuShortPrefix,
              subMenuLongPrefix, mo, subMenuBuilder.subject));
        }
        addGroup(adaptedOptions);
      }
      for (final MenuOption<U> mo : subMenuBuilder.optionMap.values()) {
        if (!optionMap.containsKey(mo.getShortName())) {
          add(new MenuOptionAdapter<T, U>(subMenuShortPrefix,
              subMenuLongPrefix, mo, subMenuBuilder.subject));
        }
      }
      return this;
    }

    public CliMenu<T> build() {
      final Options op = new Options();
      for (final Set<MenuOption<T>> g : groups) {
        final OptionGroup og = new OptionGroup();
        for (final MenuOption<T> mo : g) {
          og.addOption(mo.createOption(subject));
        }
        op.addOptionGroup(og);
      }

      for (final MenuOption<T> mo : optionMap.values()) {
        if (!op.hasOption(mo.getShortName())) {
          op.addOption(mo.createOption(subject));
        }
      }

      return new CliMenu<T>(op, this);
    }
  }

  static class MenuOptionAdapter<T, U> extends AbstractMenuOption<T> {
    private final MenuOption<U> delegate;
    private final U subject;

    public MenuOptionAdapter(String shortNamePrefix, String longNamePrefix,
        MenuOption<U> deleg, U subj) {
      super(shortNamePrefix + deleg.getShortName(), longNamePrefix
          + deleg.getLongName());
      delegate = deleg;
      subject = subj;
    }

    @Override
    public Option createOption(T builder) {
      return OptionBuilder.optionBuilder(this)
          .set(delegate.createOption(subject))
          .build();
    }

    @Override
    public boolean execute(T builder, Value value) {
      return delegate.execute(subject, value);
    }
  }
}
