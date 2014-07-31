package rinde.sim.util.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static rinde.sim.util.io.FileProviderCli.MenuOptions.ADD;
import static rinde.sim.util.io.FileProviderCli.MenuOptions.FILTER;
import static rinde.sim.util.io.FileProviderCli.MenuOptions.HELP;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;

import rinde.sim.util.cli.AbstractMenuOption;
import rinde.sim.util.cli.CliMenu;
import rinde.sim.util.cli.MenuOption;
import rinde.sim.util.cli.OptionBuilder;
import rinde.sim.util.cli.Value;
import rinde.sim.util.io.FileProvider.Builder;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

/**
 * Defines a command-line interface for the {@link FileProvider}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class FileProviderCli {

  private FileProviderCli() {}

  static void execute(FileProvider.Builder builder, String[] args) {
    createDefaultMenuBuilder(builder).build().execute(args);
  }

  /**
   * Creates the default {@link rinde.sim.util.cli.CliMenu.Builder} for creating
   * the {@link CliMenu} instances.
   * @param builder The {@link FileProvider.Builder} that should be controlled
   *          via CLI.
   * @return The new menu builder.
   */
  public static CliMenu.Builder<Builder> createDefaultMenuBuilder(
      FileProvider.Builder builder) {
    final Map<String, Path> pathMap = createPathMap(builder);
    final CliMenu.Builder<Builder> cliBuilder = CliMenu.builder(builder)
        .add(HELP)
        .add(ADD)
        .add(FILTER);

    if (!pathMap.isEmpty()) {
      cliBuilder.addGroup(new Include(pathMap), new Exclude(pathMap));
    }
    return cliBuilder;
  }

  enum MenuOptions implements MenuOption<FileProvider.Builder> {
    HELP("h", "help") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder.optionBuilder(this)
            .description("Print this message.").build();
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        return false;
      }
    },
    ADD("a", "add") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder
            .optionBuilder(this)
            .description(
                "Adds the specified paths. A path may be a file or a directory. "
                    + "If it is a directory it will be searched recursively.")
            .stringArgList()
            .build();
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        final List<String> paths = value.asList();
        for (final String p : paths) {
          builder.add(Paths.get(p));
        }
        return true;
      }
    },
    FILTER("f", "filter") {
      @Override
      public Option createOption(Builder builder) {
        return OptionBuilder
            .optionBuilder(this)
            .description(
                "Sets a filter of which paths to include. The filter is a string "
                    + "of the form 'syntax:pattern', where 'syntax' is either 'glob' "
                    + "or 'regex'.  The current filter is '"
                    + builder.pathPredicate
                    + "', there are "
                    + builder.getNumberOfFiles()
                    + " files that satisfy this filter. For more information about"
                    + " the supported syntax please review the documentation of the "
                    + "java.nio.file.FileSystem.getPathMatcher(String) method.")
            .stringArg()
            .build();
      }

      @Override
      public boolean execute(Builder builder, Value value) {
        builder.filter(value.stringValue());
        return true;
      }
    };

    private final String shortName;
    private final String longName;

    MenuOptions(String sn, String ln) {
      shortName = sn;
      longName = ln;
    }

    @Override
    public String getShortName() {
      return shortName;
    }

    @Override
    public String getLongName() {
      return longName;
    }
  }

  private static ImmutableMap<String, Path> createPathMap(Builder b) {
    final Map<String, Path> pathMap = newLinkedHashMap();
    for (int i = 0; i < b.paths.size(); i++) {
      pathMap.put("p" + i, b.paths.get(i));
    }
    return ImmutableMap.copyOf(pathMap);
  }

  static class Include extends AbstractMenuOption<Builder> {
    final Map<String, Path> pathMap;

    Include(Map<String, Path> map) {
      super("i", "include");
      pathMap = map;
    }

    @Override
    public Option createOption(Builder builder) {
      final StringBuilder sb = new StringBuilder();
      sb.append("The following paths can be included. If this option is not used all paths are automatically included. The current paths:\n");
      Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
      sb.append("\nThe options should be given as a comma ',' separated list. This option "
          + "can not be used together with --exclude.");
      return OptionBuilder.optionBuilder(this)
          .description(sb.toString())
          .stringArgList()
          .build();
    }

    @Override
    public boolean execute(Builder builder, Value value) {
      final List<String> keys = value.asList();
      final List<Path> paths = newArrayList();
      checkArgument(
          keys.size() <= pathMap.size(),
          "Too many paths, at most %s paths can be included.",
          pathMap.size());
      for (final String k : keys) {
        checkArgument(pathMap.containsKey(k),
            "The key '%s' is not valid. Valid keys: %s.", k, pathMap.keySet());
        paths.add(pathMap.get(k));
      }
      builder.paths.retainAll(paths);
      return true;
    }

  }

  static class Exclude extends AbstractMenuOption<Builder> {
    private final Map<String, Path> pathMap;

    protected Exclude(Map<String, Path> map) {
      super("e", "exclude");
      pathMap = map;
    }

    @Override
    public Option createOption(Builder builder) {
      final StringBuilder sb = new StringBuilder();
      sb.append("The following paths can be excluded. If this option is not used all paths are automatically "
          + "included. The current paths:\n");
      Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
      sb.append("\nThe options should be given as a comma ',' separated list. This option "
          + "can not be used together with --include.");
      return OptionBuilder.optionBuilder(this)
          .description(sb.toString())
          .stringArgList()
          .build();
    }

    @Override
    public boolean execute(Builder builder, Value value) {
      final List<String> keys = value.asList();
      final List<Path> paths = newArrayList();
      checkArgument(
          keys.size() < pathMap.size(),
          "Too many configurations, at most %s configurations can be excluded.",
          pathMap.size() - 1);
      for (final String k : keys) {
        checkArgument(pathMap.containsKey(k),
            "The key '%s' is not valid. Valid keys: %s.", k, pathMap.keySet());
        paths.add(pathMap.get(k));
      }
      builder.paths.removeAll(paths);
      return true;
    }
  }

}
