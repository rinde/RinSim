package rinde.sim.util.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class Value {
  private final CommandLine commandLine;
  private final MenuOption option;

  public Value(CommandLine cla, MenuOption opt) {
    checkNotNull(cla);
    checkNotNull(opt);
    commandLine = cla;
    option = opt;
  }

  public Optional<Long> longValue() {
    try {
      final Long i = (Long) commandLine
          .getParsedOptionValue(option.getShortName());
      return Optional.of(i);
    } catch (final ParseException e) {
      return Optional.absent();
    }
  }

  public String optionUsed() {
    if (commandLine.hasOption(option.getShortName())) {
      return "-" + option.getShortName();
    } else if (commandLine.hasOption(option.getLongName())) {
      return "--" + option.getLongName();
    } else {
      throw new IllegalArgumentException();
    }
  }

  public boolean hasValue() {
    return commandLine.getOptionValue(option.getShortName()) != null;
  }

  public String stringValue() {
    return Joiner.on(",").join(
        commandLine.getOptionValues(option.getShortName()));
  }

  public ImmutableList<String> asList() {
    return ImmutableList.copyOf(commandLine.getOptionValues(option
        .getShortName()));
  }
}
