package rinde.sim.util.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class FileProvider<T> implements Supplier<ImmutableSet<T>> {
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
      final PathCollector<T> pc = new PathCollector<>(pathPredicate, pathReader);
      for (final Path path : roots) {
        Files.walkFileTree(path, pc);
      }
      return pc.getResults();
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    Predicate<Path> pathPredicate;
    final List<Path> paths;

    Builder() {
      pathPredicate = Predicates.alwaysTrue();
      paths = newArrayList();
    }

    // accepts files and directories
    // each file will be included at maximum once
    public Builder add(Path path) {
      checkArgument(Files.exists(path), "Invalid path: '%s'.", path);
      paths.add(path);
      return this;
    }

    public Builder add(Iterable<Path> ps) {
      for (final Path p : ps) {
        add(p);
      }
      return this;
    }

    public Builder filter(String syntaxAndPattern) {
      pathPredicate = new PredicateAdapter(syntaxAndPattern,
          FileSystems.getDefault().getPathMatcher(syntaxAndPattern));
      return this;
    }

    public Builder filter(PathMatcher matcher) {
      pathPredicate = new PredicateAdapter(matcher);
      return this;
    }

    public Builder filter(Predicate<Path> predicate) {
      pathPredicate = predicate;
      return this;
    }

    int getNumberOfFiles() {
      return build().get().size();
    }

    public Builder cli(String[] args) {
      // do CLI stuff
      return this;
    }

    public FileProvider<Path> build() {
      return build(Functions.<Path> identity());
    }

    public <T> FileProvider<T> build(Function<Path, T> converter) {
      return new FileProvider<T>(ImmutableList.copyOf(paths), pathPredicate,
          converter);
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
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException {
      if (pathPredicate.apply(file)) {
        convertedPaths.add(pathReader.apply(file));
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
    public boolean apply(Path input) {
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
