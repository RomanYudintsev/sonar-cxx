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
package org.sonar.cxx.sensors.compiler;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.sensors.utils.CxxReportSensor;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * compiler for C++ with advanced analysis features (e.g. for VC 2008 team
 * edition or 2010/2012/2013/2015/2017 premium edition)
 *
 * @author Bert
 */
public class CxxCompilerVcSensor extends CxxReportSensor {
  private static final Logger LOG = Loggers.get(CxxCompilerVcSensor.class);
  public static final String REPORT_PATH_KEY = "compiler.vc.reportPath";
  public static final String REPORT_REGEX_DEF = "compiler.vc.regex";
  public static final String REPORT_CHARSET_DEF = "compiler.vc.charset";
  public static final String PARSER_KEY_DEF = "compiler.vc.parser";
  public static final String DEFAULT_PARSER_DEF = CxxCompilerVcParser.COMPILER_KEY;
  public static final String DEFAULT_CHARSET_DEF = "UTF-8";
  public static final String COMPILER_KEY = "Compiler.vc";

  private CompilerParser parser;

  /**
   * {@inheritDoc}
   * @param settings for report sensor
   */
  public CxxCompilerVcSensor(CxxLanguage language, Settings settings) {
    super(language.inst(), settings);

    parser = new CxxCompilerVcParser();
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(this.language.getKey()).name(language.getName() + " CompilerSensor");
  }

  @Override
  public String getReportPathKey() {
    return language.getPluginProperty(REPORT_PATH_KEY);
  }

  /**
   * Get string property from configuration. If the string is not set or empty,
   * return the default value.
   *
   * @param name Name of the property
   * @param def Default value
   * @return Value of the property if set and not empty, else default value.
   */
  public String getParserStringProperty(String name, String def) {
    String s = this.settings.getString(name);
    if (s == null || s.isEmpty()) {
      return def;
    }
    return s;
  }

  @Override
  protected void processReport(final SensorContext context, File report)
    throws javax.xml.stream.XMLStreamException {
    final String reportCharset = getParserStringProperty(this.language.getPluginProperty(REPORT_CHARSET_DEF), parser.defaultCharset());
    final String reportRegEx = getParserStringProperty(this.language.getPluginProperty(REPORT_REGEX_DEF), parser.defaultRegexp());
    final List<CompilerParser.Warning> warnings = new LinkedList<>();
    final Map<String,Integer> filesWithWarnings = new HashMap<String,Integer>();

    // Iterate through the lines of the input file
    LOG.info("Scanner '{}' initialized with report '{}', CharSet= '{}'",
      new Object[]{parser.key(), report, reportCharset});
    Integer warnCount = 0;
    Metric<Integer> metric = (Metric<Integer>) this.language.getMetric(getSensorKey());
    try {
      parser.processReport(context, report, reportCharset, reportRegEx, warnings);
      for (CompilerParser.Warning w : warnings) {
        if (isInputValid(w)) {
          saveUniqueViolation(context, parser.rulesRepositoryKey(), w.filename, w.line, w.id, w.msg);
          filesWithWarnings.putIfAbsent(w.filename, 0);
          filesWithWarnings.put(w.filename, filesWithWarnings.get(w.filename)+1);
        } else {
          LOG.warn("C-Compiler warning: '{}''{}'", w.id, w.msg);
        }
      }
      filesWithWarnings.forEach((filename, count) -> {
        if (! saveUniqueMeasure(context, filename, metric, count) ) {
          filesWithWarnings.put(filename, 0);
        }
      });
    } catch (java.io.FileNotFoundException|IllegalArgumentException e) {
      LOG.error("processReport Exception: {} - not processed '{}'", report, e);
    }
    for (Integer c : filesWithWarnings.values()) {
      warnCount+= c;
    }
    saveUniqueMeasure(context, null, metric, warnCount);
  }

  private boolean isInputValid(CompilerParser.Warning warning) {
    return !warning.toString().isEmpty();
  }
  
  @Override
  protected String getSensorKey() {
    return COMPILER_KEY;
  }  
}
