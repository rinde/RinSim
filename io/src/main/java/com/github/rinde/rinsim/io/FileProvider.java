/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Configurable supplier of files.
 *
 * @param <T> The type of object that this file provider provides. See
 *          {@link Builder#build()} and {@link Builder#build(Function)} for more
 *          details.
 * @author Rinde van Lon
 */
public final class FileProvider<T> implements Supplier<ImmutableSet<T>> {
  final ImmutableList<Path> roots;
  final Predicate<Path> pathPredicate;
  final Function<Path, T> pathReader;

  FileProvider(ImmutableList<Path> rootPaths, Predicate<Path> predicate,
      Function<Path, T> reader) {
    roots = rootPaths;
    pathPredicate = predicate;
    pathReader = reader;
  }

  @Override
  public ImmutableSet<T> get() {
    try {
      final PathCollector<T> pc = new PathCollector<>(pathPredicate,
          pathReader);
      for (final Path path : roots) {
        Files.walkFileTree(path, pc);
      }
      return pc.getResults();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @return A new {@link Builder} for creating a {@link FileProvider} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating {@link FileProvider} instances. Via this builder
   * files and directories can be added and filtered. The resulting
   * {@link FileProvider} will provide all specified files.
   *
   * @author Rinde van Lon
   */
  public static class Builder {
    Predicate<Path> pathPredicate;
    final List<Path> paths;

    Builder() {
      pathPredicate = Predicates.alwaysTrue();
      paths = newArrayList();
    }

    /**
     * Add the file or directory that is represented by this {@link Path}
     * instance. If it is a directory it will be added recursively. Each added
     * file will be included at maximum once.
     *
     * @param path The file or directory to add.
     * @return This, as per the builder pattern.
     */
    public Builder add(Path path) {
      checkArgument(Files.exists(path), "Invalid path: '%s'.", path);
      paths.add(path);
      return this;
    }

    /**
     * Adds all files or directories that are represented by the specified
     * {@link Path}s. Directories will be added recursively. Each added file
     * will be included at maximum once.
     *
     * @param ps The files and/or directories to add.
     * @return This, as per the builder pattern.
     */
    public Builder add(Iterable<Path> ps) {
      for (final Path p : ps) {
        add(p);
      }
      return this;
    }

    /**
     * Apply a filter to all added and to be added paths. The expected syntax is
     * the same as for the
     * {@link java.nio.file.FileSystem#getPathMatcher(String)} method. Only one
     * filter can be applied at one time.
     *
     * @param syntaxAndPattern The syntax and pattern.
     * @return This, as per the builder pattern.
     */
    public Builder filter(String syntaxAndPattern) {
      checkNotNull(syntaxAndPattern);
      pathPredicate = new PredicateAdapter(syntaxAndPattern, FileSystems
          .getDefault().getPathMatcher(syntaxAndPattern));
      return this;
    }

    /**
     * Apply the specified {@link PathMatcher} as a filter. Only files that
     * satisfy the filter will be included.Only one filter can be applied at one
     * time.
     *
     * @param matcher The matcher to use as filter.
     * @return This, as per the builder pattern.
     */
    public Builder filter(PathMatcher matcher) {
      checkNotNull(matcher);
      pathPredicate = new PredicateAdapter(matcher);
      return this;
    }

    /**
     * Apply the specified {@link Predicate} as a filter. Only files that
     * satisfy the filter will be included. Only one filter can be applied at
     * one time.
     *
     * @param predicate The predicate to use as filter.
     * @return This, as per the builder pattern.
     */
    public Builder filter(Predicate<Path> predicate) {
      checkNotNull(predicate);
      pathPredicate = predicate;
      return this;
    }

    /**
     * Activates the command-line interface for this builder. If an invalid
     * option is given the help will be printed automatically to
     * {@link System#out}.
     *
     * @param stream The stream to write error messages to if any.
     * @param args The command-line arguments.
     * @return This, as per the builder pattern.
     */
    public Builder cli(PrintStream stream, String... args) {
      final Optional<String> error = FileProviderCli.execute(this, args);
      if (error.isPresent()) {
        stream.println(error.get());
      }
      return this;
    }

    /**
     * Create a new {@link FileProvider} which will provide the {@link Path} s
     * as specified by this builder.
     *
     * @return The new {@link FileProvider} instance.
     */
    public FileProvider<Path> build() {
      return build(Functions.<Path>identity());
    }

    /**
     * Create a new {@link FileProvider} which will provide the converted
     * {@link Path}s as specified by this builder. Each path will be converted
     * to type <code>T</code> using the specified <code>converter</code>.
     *
     * @param converter A {@link Function} that converts {@link Path}s.
     * @param <T> The type to which {@link Path}s are converted and which will
     *          be provided by the {@link FileProvider}.
     * @return The new {@link FileProvider} instance.
     */
    public <T> FileProvider<T> build(Function<Path, T> converter) {
      checkNotNull(converter);
      checkArgument(!paths.isEmpty(), "No paths are specified.");
      return new FileProvider<>(ImmutableList.copyOf(paths),
          pathPredicate, converter);
    }

    int getNumberOfFiles() {
      if (paths.isEmpty()) {
        return 0;
      }
      return build().get().size();
    }
  }

  static class PathCollector<T> extends SimpleFileVisitor<Path> {
    final Set<T> convertedPaths;
    final Predicate<Path> pathPredicate;
    final Function<Path, T> pathReader;

    PathCollector(Predicate<Path> predicate, Function<Path, T> reader) {
      convertedPaths = newLinkedHashSet();
      pathPredicate = predicate;
      pathReader = reader;
    }

    @Override
    public FileVisitResult visitFile(@Nullable Path file,
        @Nullable BasicFileAttributes attrs)
            throws IOException {
      if (pathPredicate.apply(file)) {
        convertedPaths.add(verifyNotNull(pathReader.apply(file), "%s",
            pathReader));
      }
      return FileVisitResult.CONTINUE;
    }

    public ImmutableSet<T> getResults() {
      return ImmutableSet.copyOf(convertedPaths);
    }
  }

  static class PredicateAdapter implements Predicate<Path> {
    private final PathMatcher delegate;
    private final Optional<String> name;

    PredicateAdapter(String nm, PathMatcher matcher) {
      name = Optional.of(nm);
      delegate = matcher;
    }

    PredicateAdapter(PathMatcher matcher) {
      name = Optional.absent();
      delegate = matcher;
    }

    @Override
    public boolean apply(@Nullable Path input) {
      return delegate.matches(input);
    }

    @Override
    public String toString() {
      if (name.isPresent()) {
        return name.get();
      }
      return delegate.toString();
    }
  }
}
