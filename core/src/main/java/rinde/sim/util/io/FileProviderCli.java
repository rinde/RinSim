package rinde.sim.util.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import rinde.sim.util.cli.CliMenu;
import rinde.sim.util.cli.CliOption;
import rinde.sim.util.cli.OptionHandler;
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
        .add(createHelpOption())
        .add(createAddOption())
        .add(createFilterOption(builder));

    if (!pathMap.isEmpty()) {
      cliBuilder.addGroup(createIncludeOption(pathMap, builder),
          createExcludeOption(pathMap, builder));
    }
    return cliBuilder;
  }

  private static ImmutableMap<String, Path> createPathMap(Builder b) {
    final Map<String, Path> pathMap = newLinkedHashMap();
    for (int i = 0; i < b.paths.size(); i++) {
      pathMap.put("p" + i, b.paths.get(i));
    }
    return ImmutableMap.copyOf(pathMap);
  }

  static CliOption<Builder> createIncludeOption(Map<String, Path> pathMap,
      Builder builder) {
    final StringBuilder sb = new StringBuilder();
    sb.append("The following paths can be included. If this option is not used all paths are automatically included. The current paths:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
    sb.append("\nThe options should be given as a comma ',' separated list. This option "
        + "can not be used together with --exclude.");
    return CliOption.builder("i")
        .setLongName("include")
        .description(sb.toString())
        .argStringList()
        .build(new IncludeHandler(pathMap));
  }

  static CliOption<Builder> createExcludeOption(Map<String, Path> pathMap,
      Builder builder) {
    final StringBuilder sb = new StringBuilder();
    sb.append("The following paths can be excluded. If this option is not used all paths are automatically "
        + "included. The current paths:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
    sb.append("\nThe options should be given as a comma ',' separated list. This option "
        + "can not be used together with --include.");
    return CliOption.builder("e")
        .setLongName("exclude")
        .description(sb.toString())
        .argStringList()
        .build(new ExcludeHandler(pathMap));
  }

  static CliOption<Builder> createHelpOption() {
    return CliOption.builder("h")
        .setLongName("help")
        .description("Print this message.")
        .buildHelpOption();
  }

  static CliOption<Builder> createAddOption() {
    return CliOption
        .builder("a")
        .setLongName("add")
        .description(
            "Adds the specified paths. A path may be a file or a directory. "
                + "If it is a directory it will be searched recursively.")
        .argStringList()
        .build(Handlers.ADD);
  }

  static CliOption<Builder> createFilterOption(Builder ref) {
    return CliOption
        .builder("f")
        .setLongName("filter")
        .description(
            "Sets a filter of which paths to include. The filter is a string "
                + "of the form 'syntax:pattern', where 'syntax' is either 'glob' "
                + "or 'regex'.  The current filter is '"
                + ref.pathPredicate
                + "', there are "
                + ref.getNumberOfFiles()
                + " files that satisfy this filter. For more information about"
                + " the supported syntax please review the documentation of the "
                + "java.nio.file.FileSystem.getPathMatcher(String) method.")
        .argString()
        .build(Handlers.FILTER);
  }

  static class IncludeHandler implements OptionHandler<Builder> {
    final Map<String, Path> pathMap;

    IncludeHandler(Map<String, Path> map) {
      pathMap = map;
    }

    @Override
    public boolean execute(Builder ref, Value value) {
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
      ref.paths.retainAll(paths);
      return true;
    }
  }

  static class ExcludeHandler implements OptionHandler<Builder> {
    final Map<String, Path> pathMap;

    ExcludeHandler(Map<String, Path> map) {
      pathMap = map;
    }

    @Override
    public boolean execute(Builder ref, Value value) {
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
      ref.paths.removeAll(paths);
      return true;
    }
  }

  public enum Handlers implements OptionHandler<FileProvider.Builder> {
    ADD {
      @Override
      public boolean execute(FileProvider.Builder ref, Value value) {
        final List<String> paths = value.asList();
        for (final String p : paths) {
          ref.add(Paths.get(p));
        }
        return true;
      }
    },
    FILTER {
      @Override
      public boolean execute(FileProvider.Builder ref, Value value) {
        ref.filter(value.stringValue());
        return true;
      }
    }
  }

}
