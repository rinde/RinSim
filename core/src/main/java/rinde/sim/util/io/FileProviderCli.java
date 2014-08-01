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
import rinde.sim.util.cli.CliOption.OptionArgType;
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

  static CliOption createIncludeOption(
      Map<String, Path> pathMap,
      Builder builder) {
    final StringBuilder sb = new StringBuilder();
    sb.append("The following paths can be included. If this option is not used all paths are automatically included. The current paths:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
    sb.append("\nThe options should be given as a comma ',' separated list. This option "
        + "can not be used together with --exclude.");
    return CliOption
        .builder("i", OptionArgType.STRING_LIST)
        .longName("include")
        .description(sb.toString())
        .build(new IncludeHandler(pathMap));
  }

  static CliOption createExcludeOption(Map<String, Path> pathMap,
      Builder builder) {
    final StringBuilder sb = new StringBuilder();
    sb.append("The following paths can be excluded. If this option is not used all paths are automatically "
        + "included. The current paths:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
    sb.append("\nThe options should be given as a comma ',' separated list. This option "
        + "can not be used together with --include.");
    return CliOption.builder("e", OptionArgType.STRING_LIST)
        .longName("exclude")
        .description(sb.toString())
        .build(new ExcludeHandler(pathMap));
  }

  static CliOption createHelpOption() {
    return CliOption.builder("h")
        .longName("help")
        .description("Print this message.")
        .buildHelpOption();
  }

  static CliOption createAddOption() {
    return CliOption
        .builder("a", OptionArgType.STRING_LIST)
        .longName("add")
        .description(
            "Adds the specified paths. A path may be a file or a directory. "
                + "If it is a directory it will be searched recursively.")
        .build(ADD);
  }

  static CliOption createFilterOption(Builder ref) {
    return CliOption
        .builder("f", OptionArgType.STRING)
        .longName("filter")
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
        .build(FILTER);
  }

  static class IncludeHandler implements OptionHandler<Builder, List<String>> {
    final Map<String, Path> pathMap;

    IncludeHandler(Map<String, Path> map) {
      pathMap = map;
    }

    @Override
    public boolean execute(Builder ref, Value<List<String>> value) {
      final List<String> keys = value.asValue();
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

  static class ExcludeHandler implements OptionHandler<Builder, List<String>> {
    final Map<String, Path> pathMap;

    ExcludeHandler(Map<String, Path> map) {
      pathMap = map;
    }

    @Override
    public boolean execute(Builder ref, Value<List<String>> value) {
      final List<String> keys = value.asValue();
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

  private static final OptionHandler<FileProvider.Builder, List<String>> ADD = new OptionHandler<FileProvider.Builder, List<String>>() {
    @Override
    public boolean execute(FileProvider.Builder ref, Value<List<String>> value) {
      final List<String> paths = value.asValue();
      for (final String p : paths) {
        ref.add(Paths.get(p));
      }
      return true;
    }
  };

  private static final OptionHandler<FileProvider.Builder, String> FILTER = new OptionHandler<FileProvider.Builder, String>() {
    @Override
    public boolean execute(FileProvider.Builder ref, Value<String> value) {
      ref.filter(value.asValue());
      return true;
    }
  };

}
