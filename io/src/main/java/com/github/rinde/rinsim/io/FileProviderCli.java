/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.github.rinde.rinsim.cli.ArgHandler;
import com.github.rinde.rinsim.cli.ArgumentParser;
import com.github.rinde.rinsim.cli.Menu;
import com.github.rinde.rinsim.cli.Option;
import com.github.rinde.rinsim.cli.Option.OptionArg;
import com.github.rinde.rinsim.io.FileProvider.Builder;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * Defines a command-line interface for {@link FileProvider}.
 * @author Rinde van Lon 
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
   * Creates the default {@link com.github.rinde.rinsim.cli.Menu.Builder} for creating
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

  abstract static class PathSelectorHandler implements
      ArgHandler<Builder, List<String>> {
    final Map<String, Path> pathMap;

    PathSelectorHandler(Map<String, Path> map) {
      pathMap = map;
    }

    @Override
    public void execute(Builder ref, Optional<List<String>> value) {
      final List<String> keys = value.get();
      final List<Path> paths = newArrayList();
      checkArgument(
          keys.size() <= pathMap.size(),
          "Too many paths, at most %s paths can be selected.",
          pathMap.size());
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
      super(map);
    }

    @Override
    void execute(Builder ref, List<Path> paths) {
      ref.paths.retainAll(paths);
    }
  }

  static class ExcludeHandler extends PathSelectorHandler {
    ExcludeHandler(Map<String, Path> map) {
      super(map);
    }

    @Override
    void execute(Builder ref, List<Path> paths) {
      ref.paths.removeAll(paths);
    }
  }
}
