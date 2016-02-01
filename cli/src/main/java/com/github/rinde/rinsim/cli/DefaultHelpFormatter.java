/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.cli;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.apache.commons.lang3.text.WordUtils;

import com.google.common.base.Strings;

/**
 * Default implementation of {@link HelpFormatter}.
 * @author Rinde van Lon
 */
public enum DefaultHelpFormatter implements HelpFormatter {
  /**
   * The instance of the default {@link HelpFormatter}.
   */
  INSTANCE {
    private static final int HELP_WIDTH = 80;
    private static final int NAME_DESC_PADDING = 3;

    @Override
    public String format(Menu menu) {
      final List<Option> options = menu.getOptions();
      final List<String> optionNames = newArrayList();
      int maxLength = 0;
      for (final Option opt : options) {
        final StringBuilder sb = new StringBuilder();
        sb.append(" -").append(opt.getShortName());
        if (opt.getLongName().isPresent()) {
          sb.append(",--").append(opt.getLongName().get());
        }
        if (opt.getArgument().isPresent()) {
          sb.append(" <").append(opt.getArgument().get().name()).append(">");
        }
        optionNames.add(sb.toString());
        maxLength = Math.max(sb.length(), maxLength);
      }
      final int total = HELP_WIDTH;
      final int nameLength = maxLength + NAME_DESC_PADDING;
      final int descLength = total - nameLength;

      final StringBuilder sb = new StringBuilder();
      if (!menu.getCmdLineSyntax().trim().isEmpty()) {
        sb.append("usage: ").append(menu.getCmdLineSyntax())
            .append(System.lineSeparator());
      }
      if (!menu.getHeader().trim().isEmpty()) {
        sb.append(menu.getHeader()).append(System.lineSeparator());
      }

      for (int i = 0; i < options.size(); i++) {
        sb.append(Strings.padEnd(optionNames.get(i), nameLength, ' '));
        final String[] descParts = options.get(i).getDescription()
            .split(System.lineSeparator());
        for (int j = 0; j < descParts.length; j++) {
          if (j > 0) {
            sb.append(Strings.padEnd("", nameLength, ' '));
          }
          sb.append(
                    WordUtils.wrap(
                                   descParts[j],
                                   descLength,
                                   Strings.padEnd(System.lineSeparator(),
                                                  nameLength + 1, ' '),
                                   false))
              .append(System.lineSeparator());
        }
      }
      if (!menu.getFooter().trim().isEmpty()) {
        sb.append(menu.getFooter()).append(System.lineSeparator());
      }
      return sb.toString();
    }
  }
}
