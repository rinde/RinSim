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
import rinde.sim.util.cli.Menu;
import rinde.sim.util.cli.Option;
import rinde.sim.util.cli.Option.OptionArg;
import rinde.sim.util.io.FileProvider.Builder;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * Defines a command-line interface for {@link FileProvider}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class FileProviderCli {

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

  private FileProviderCli() {}

  static Optional<String> execute(FileProvider.Builder builder, String[] args) {
    return createDefaultMenu(builder).execute(args);
  }

  /**
   * Creates the default {@link rinde.sim.util.cli.Menu.Builder} for creating
   * the {@link Menu} instances.
   * @param builder The {@link FileProvider.Builder} that should be controlled
   *          via CLI.
   * @return The new menu builder.
   */
  public static Menu createDefaultMenu(
      FileProvider.Builder builder) {
    final Map<String, Path> pathMap = createPathMap(builder);
    final Menu.Builder cliBuilder = Menu.builder()
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
    return cliBuilder.build();
  }

  private static ImmutableMap<String, Path> createPathMap(Builder b) {
    final Map<String, Path> pathMap = newLinkedHashMap();
    for (int i = 0; i < b.paths.size(); i++) {
      pathMap.put("p" + i, b.paths.get(i));
    }
    return ImmutableMap.copyOf(pathMap);
  }

  static OptionArg<List<String>> createIncludeOption(
      Map<String, Path> pathMap,
      Builder builder) {
    final StringBuilder sb = new StringBuilder();
    printPathOptions(pathMap, sb);
    sb.append("This option can not be used together with --exclude.");
    return Option
        .builder("i", ArgumentParser.STRING_LIST)
        .longName("include")
        .description(sb.toString())
        .build();
  }

  static void printPathOptions(Map<String, Path> pathMap, StringBuilder sb) {
    sb.append("The following paths can be excluded. If this option is not used all paths are automatically "
        + "included. The current paths:\n");
    Joiner.on("\n").withKeyValueSeparator(" = ").appendTo(sb, pathMap);
    sb.append("\nThe options should be given as a comma ',' separated list.");
  }

  static OptionArg<List<String>> createExcludeOption(
      Map<String, Path> pathMap,
      Builder builder) {
    final StringBuilder sb = new StringBuilder();
    printPathOptions(pathMap, sb);
    sb.append("This option can not be used together with --include.");
    return Option.builder("e", ArgumentParser.STRING_LIST)
        .longName("exclude")
        .description(sb.toString())
        .build();
  }

  static OptionArg<List<String>> createAddOption() {
    return Option
        .builder("a", ArgumentParser.STRING_LIST)
        .longName("add")
        .description(
            "Adds the specified paths. A path may be a file or a directory. "
                + "If it is a directory it will be searched recursively.")
        .build();
  }

  static OptionArg<String> createFilterOption(Builder ref) {
    return Option
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

  static abstract class PathSelectorHandler implements
      ArgHandler<Builder, List<String>> {
    final Map<String, Path> pathMap;
    String verb;

    PathSelectorHandler(Map<String, Path> map, String v) {
      pathMap = map;
      verb = v;
    }

    @Override
    public void execute(Builder ref, Optional<List<String>> value) {
      final List<String> keys = value.get();
      final List<Path> paths = newArrayList();
      checkArgument(
          keys.size() <= pathMap.size(),
          "Too many paths, at most %s paths can be %sd.",
          pathMap.size(), verb);
      for (final String k : keys) {
        checkArgument(pathMap.containsKey(k),
            "The key '%s' is not valid. Valid keys: %s.", k, pathMap.keySet());
        paths.add(pathMap.get(k));
      }
      execute(ref, paths);
    }

    abstract void execute(Builder ref, List<Path> paths);
  }

  static class IncludeHandler extends PathSelectorHandler {
    IncludeHandler(Map<String, Path> map) {
      super(map, "include");
    }

    @Override
    void execute(Builder ref, List<Path> paths) {
      ref.paths.retainAll(paths);
    }
  }

  static class ExcludeHandler extends PathSelectorHandler {
    ExcludeHandler(Map<String, Path> map) {
      super(map, "exclude");
    }

    @Override
    void execute(Builder ref, List<Path> paths) {
      ref.paths.removeAll(paths);
    }
  }
}
