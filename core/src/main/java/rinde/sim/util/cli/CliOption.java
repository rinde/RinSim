package rinde.sim.util.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.PatternOptionBuilder;

import com.google.common.base.Joiner;

// TODO convert to abstract class
public final class CliOption<T> implements ICliOption<T> {

  private final Builder builder;
  private final OptionHandler<T> handler;

  CliOption(Builder b, OptionHandler<T> h) {
    builder = b;
    handler = h;
  }

  @Override
  public boolean execute(T ref, Value value) {
    return handler.execute(ref, value);
  }

  @Override
  public String getShortName() {
    return builder.option.getOpt();
  }

  @Override
  public String getLongName() {
    return builder.option.getLongOpt();
  }

  @Override
  public Option create() {
    return new Builder(getShortName()).setLongName(getLongName()).set(
        builder.option).option;
  }

  public static Builder builder(String shortName) {
    return new Builder(shortName);
  }

  public static class Builder {
    private static final int NUM_ARGS_IN_LIST = 100;
    private static final String ARG_LIST_NAME = "list";
    private static final char ARG_LIST_SEPARATOR = ',';

    private static final String NUM_NAME = "num";
    private static final String STRING_NAME = "string";
    final Option option;

    Builder(String sn) {
      option = new Option(sn, "");
    }

    public Builder setLongName(String ln) {
      option.setLongOpt(ln);
      return this;
    }

    // TODO convert the methods to use an enum property system
    public Builder argNumberList() {
      option.setArgs(NUM_ARGS_IN_LIST);
      option.setArgName(ARG_LIST_NAME);
      option.setType(PatternOptionBuilder.NUMBER_VALUE);
      option.setValueSeparator(ARG_LIST_SEPARATOR);
      return this;
    }

    public Builder argNumber() {
      option.setArgs(1);
      option.setArgName(NUM_NAME);
      option.setType(PatternOptionBuilder.NUMBER_VALUE);
      return this;
    }

    public Builder argStringList() {
      option.setArgs(NUM_ARGS_IN_LIST);
      option.setArgName(ARG_LIST_NAME);
      option.setType(PatternOptionBuilder.STRING_VALUE);
      option.setValueSeparator(ARG_LIST_SEPARATOR);
      return this;
    }

    public Builder argString() {
      option.setArgs(1);
      option.setArgName(STRING_NAME);
      option.setType(PatternOptionBuilder.NUMBER_VALUE);
      return this;
    }

    public Builder argOptional() {
      option.setOptionalArg(true);
      return this;
    }

    public Builder description(Object... desc) {
      option.setDescription(Joiner.on("").join(desc));
      return this;
    }

    /**
     * Sets all variables from the specified {@link Option} except the short and
     * long name.
     * @param opt
     * @return
     */
    public Builder set(Option opt) {
      option.setArgs(opt.getArgs());
      option.setArgName(opt.getArgName());
      option.setDescription(opt.getDescription());
      option.setOptionalArg(opt.hasOptionalArg());
      option.setType(opt.getType());
      option.setValueSeparator(opt.getValueSeparator());
      option.setRequired(opt.isRequired());
      return this;
    }

    public <T> CliOption<T> build(OptionHandler<T> handler) {
      // defensive copy, to make sure that any changes in the builder after this
      // point don't write through to the underlying option instance.
      return new CliOption<>(
          builder(option.getOpt())
              .setLongName(option.getLongOpt())
              .set(option),
          handler);
    }

    public <T> CliOption<T> buildHelpOption() {
      return build(new HelpHandler<T>());
    }
  }

  static class HelpHandler<T> implements OptionHandler<T> {
    @Override
    public boolean execute(T ref, Value value) {
      return false;
    }
  }

}
