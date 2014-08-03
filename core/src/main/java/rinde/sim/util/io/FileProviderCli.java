package rinde.sim.util.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import rinde.sim.util.cli.ArgHandler;
import rinde.sim.util.cli.ArgumentParser;
import rinde.sim.util.cli.CliMenu;
import rinde.sim.util.cli.CliOption;
import rinde.sim.util.cli.CliOption.CliOptionArg;
import rinde.sim.util.io.FileProvider.Builder;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * Defines a command-line interface for the {@link FileProvider}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class FileProviderCli {

  private FileProviderCli() {}

  static Optional<String> execute(FileProvider.Builder builder, String[] args) {
    return createDefaultMenuBuilder(builder).build().execute(args);
  }

  /**
   * Creates the default {@link rinde.sim.util.cli.CliMenu.Builder} for creating
   * the {@link CliMenu} instances.
   * @param builder The {@link FileProvider.Builder} that should be controlled
   *          via CLI.
   * @return The new menu builder.
   */
  public static CliMenu.Builder createDefaultMenuBuilder(
      FileProvider.Builder builder) {
    final Map<String, Path> pathMap = createPathMap(builder);
    final CliMenu.Builder cliBuilder = CliMenu.builder()
        .addHelpOption("h", "help", "Print this message")
        .add(createAddOption(), builder, ADD)
        .add(createFilterOption(builder), builder, FILTER);

    if (!pathMap.isEmpty()) {
      cliBuilder
          .openGroup()
          .add(createIncludeOption(pathMap, builder), builder,
              new IncludeHandler(pathMap))
          .add(createExcludeOption(pathMap, builder), builder,
              new ExcludeHandler(pathMap))
          .closeGroup();
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

  static CliOptionArg<List<String>> createIncludeOption(
      Map<String, Path> pathMap,
      Builder builder) {
    final StringBuilder sb = new StringBuilder();
    sb.append("The following paths can be included. If this option is not used all paths are automatically included. The current paths:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
    sb.append("\nThe options should be given as a comma ',' separated list. This option "
        + "can not be used together with --exclude.");
    return CliOption
        .builder("i", ArgumentParser.STRING_LIST)
        .longName("include")
        .description(sb.toString())
        .build();
  }

  static CliOptionArg<List<String>> createExcludeOption(
      Map<String, Path> pathMap, Builder builder) {
    final StringBuilder sb = new StringBuilder();
    sb.append("The following paths can be excluded. If this option is not used all paths are automatically "
        + "included. The current paths:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
    sb.append("\nThe options should be given as a comma ',' separated list. This option "
        + "can not be used together with --include.");
    return CliOption.builder("e", ArgumentParser.STRING_LIST)
        .longName("exclude")
        .description(sb.toString())
        .build();
  }

  // static CliOption createHelpOption() {
  // return CliOption.builder("h")
  // .longName("help")
  // .description("Print this message.")
  // .buildHelpOption();
  // }

  static CliOptionArg<List<String>> createAddOption() {
    return CliOption
        .builder("a", ArgumentParser.STRING_LIST)
        .longName("add")
        .description(
            "Adds the specified paths. A path may be a file or a directory. "
                + "If it is a directory it will be searched recursively.")
        .build();
  }

  static CliOptionArg<String> createFilterOption(Builder ref) {
    return CliOption
        .builder("f", ArgumentParser.STRING)
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
        .build();
  }

  static class IncludeHandler implements ArgHandler<Builder, List<String>> {
    final Map<String, Path> pathMap;

    IncludeHandler(Map<String, Path> map) {
      pathMap = map;
    }

    @Override
    public void execute(Builder ref, Optional<List<String>> value) {
      final List<String> keys = value.get();
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
    }
  }

  static class ExcludeHandler implements ArgHandler<Builder, List<String>> {
    final Map<String, Path> pathMap;

    ExcludeHandler(Map<String, Path> map) {
      pathMap = map;
    }

    @Override
    public void execute(Builder ref, Optional<List<String>> value) {
      final List<String> keys = value.get();
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
    }
  }

  private static final ArgHandler<FileProvider.Builder, List<String>> ADD = new ArgHandler<FileProvider.Builder, List<String>>() {
    @Override
    public void execute(FileProvider.Builder ref, Optional<List<String>> value) {
      final List<String> paths = value.get();
      for (final String p : paths) {
        ref.add(Paths.get(p));
      }
    }
  };

  private static final ArgHandler<FileProvider.Builder, String> FILTER = new ArgHandler<FileProvider.Builder, String>() {
    @Override
    public void execute(FileProvider.Builder ref, Optional<String> value) {
      ref.filter(value.get());
    }
  };

}
