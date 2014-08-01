package rinde.sim.util.cli;

public final class Value<T> {

  final String stringValue;
  final String usedOption;
  final T value;

  Value(String str, String opt, T val) {
    stringValue = str;
    usedOption = opt;
    value = val;
  }

  public String asString() {
    return stringValue;
  }

  public String usedOption() {
    return usedOption;
  }

  public T asValue() {
    return value;
  }

  // public Optional<Long> longValue() {
  // try {
  // final Long i = (Long) commandLine
  // .getParsedOptionValue(option.getShortName());
  // return Optional.of(i);
  // } catch (final ParseException e) {
  // return Optional.absent();
  // }
  // }
  //
  // public String optionUsed() {
  // if (commandLine.hasOption(option.getShortName())) {
  // return "-" + option.getShortName();
  // } else if (commandLine.hasOption(option.getLongName())) {
  // return "--" + option.getLongName();
  // } else {
  // throw new IllegalArgumentException();
  // }
  // }
  //
  // public boolean hasValue() {
  // return commandLine.getOptionValue(option.getShortName()) != null;
  // }
  //
  // public String stringValue() {
  // return Joiner.on(",").join(
  // commandLine.getOptionValues(option.getShortName()));
  // }
  //
  // public ImmutableList<String> asList() {
  // return ImmutableList.copyOf(commandLine.getOptionValues(option
  // .getShortName()));
  // }

}
