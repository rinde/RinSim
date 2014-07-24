package rinde.sim.util.io;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.PatternOptionBuilder;

import com.google.common.base.Joiner;

public class OptionBuilder {
  private static final int NUM_ARGS_IN_LIST = 100;
  private static final String ARG_LIST_NAME = "list";
  private static final char ARG_LIST_SEPARATOR = ',';

  private static final String NUM_NAME = "num";
  private static final String STRING_NAME = "string";
  private final Option option;

  OptionBuilder(MenuOption im) {
    this(im.getShortName(), im.getLongName());
  }

  OptionBuilder(String sn, String ln) {
    option = new Option(sn, "");
    option.setLongOpt(ln);
  }

  public OptionBuilder numberArgList() {
    option.setArgs(NUM_ARGS_IN_LIST);
    option.setArgName(ARG_LIST_NAME);
    option.setType(PatternOptionBuilder.NUMBER_VALUE);
    option.setValueSeparator(ARG_LIST_SEPARATOR);
    return this;
  }

  public OptionBuilder numberArg() {
    option.setArgs(1);
    option.setArgName(NUM_NAME);
    option.setType(PatternOptionBuilder.NUMBER_VALUE);
    return this;
  }

  public OptionBuilder stringArgList() {
    option.setArgs(NUM_ARGS_IN_LIST);
    option.setArgName(ARG_LIST_NAME);
    option.setType(PatternOptionBuilder.STRING_VALUE);
    option.setValueSeparator(ARG_LIST_SEPARATOR);
    return this;
  }

  public OptionBuilder stringArg() {
    option.setArgs(1);
    option.setArgName(STRING_NAME);
    option.setType(PatternOptionBuilder.NUMBER_VALUE);
    return this;
  }

  public OptionBuilder optionalArg() {
    option.setOptionalArg(true);
    return this;
  }

  public OptionBuilder description(Object... desc) {
    option.setDescription(Joiner.on("").join(desc));
    return this;
  }

  public Option build() {
    return option;
  }

  public static OptionBuilder optionBuilder(MenuOption im) {
    return new OptionBuilder(im);
  }
}
