/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2017 SonarOpenCommunity
 * http://github.com/SonarOpenCommunity/sonar-cxx
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.cxx.preprocessor;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.sonar.cxx.api.CxxTokenType;

import com.sonar.sslr.api.Preprocessor; //@todo: deprecated, see http://javadocs.sonarsource.org/4.5.2/apidocs/deprecated-list.html
import com.sonar.sslr.api.PreprocessorAction; //@todo: deprecated, see http://javadocs.sonarsource.org/4.5.2/apidocs/deprecated-list.html
import com.sonar.sslr.api.Token;

public class JoinStringsPreprocessor extends Preprocessor { //@todo deprecated Preprocessor

  @Override
  public PreprocessorAction process(List<Token> tokens) { //@todo deprecated PreprocessorAction
    Token token = tokens.get(0);

    if (token.getType() == CxxTokenType.STRING) {

      // Joining string literals (C++ Standard, "2.2 Phases of translation, Phase 6")
      StringBuilder newStr = null;
      int numberOfStrings = 1;

      for (;;) {
        Token nextToken = tokens.get(numberOfStrings);
        if (nextToken.getType() != CxxTokenType.STRING) {
          if (newStr != null) {
            newStr.append("\"");
          }
          break;
        }
        if (newStr == null) {
          newStr = new StringBuilder();
          newStr.append("\"");
          newStr.append(stripQuotes(token.getValue()));
        }
        newStr.append(stripQuotes(nextToken.getValue()));
        numberOfStrings++;
      }

      if (newStr != null) {
        List<Token> tokensToInject = new ArrayList<>();
        tokensToInject.add(
          Token.builder()
            .setLine(token.getLine())
            .setColumn(token.getColumn())
            .setURI(token.getURI())
            .setType(CxxTokenType.STRING)
            .setValueAndOriginalValue(newStr.toString())
            .build()
        );
        return new PreprocessorAction(numberOfStrings, Collections.EMPTY_LIST, tokensToInject); //@todo deprecated PreprocessorAction
      }

      return PreprocessorAction.NO_OPERATION; //@todo deprecated PreprocessorAction
    }
    return PreprocessorAction.NO_OPERATION; //@todo deprecated PreprocessorAction
  }

  private String stripQuotes(String str) {
    return str.substring(str.indexOf('"') + 1, str.lastIndexOf('"'));
  }
}
