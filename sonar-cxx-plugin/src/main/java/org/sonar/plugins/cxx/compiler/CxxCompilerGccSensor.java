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
package org.sonar.plugins.cxx.compiler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.cxx.CxxLanguage;
import org.sonar.plugins.cxx.utils.CxxMetrics;
import org.sonar.plugins.cxx.utils.CxxReportSensor;

/**
 * compiler for C++ with advanced analysis features (e.g. for VC 2008 team
 * edition or 2010/2012/2013 premium edition)
 *
 * @author Bert
 */
public class CxxCompilerGccSensor extends CxxCompilerSensor {
  public static final Logger LOG = Loggers.get(CxxCompilerGccSensor.class);
  public static final String REPORT_PATH_KEY = "sonar.cxx.compiler-gcc.reportPath";
  public static final String REPORT_REGEX_DEF = "sonar.cxx.compiler-gcc.regex";
  public static final String REPORT_CHARSET_DEF = "sonar.cxx.compiler-gcc.charset";

  /**
   * {@inheritDoc}
   */
  public CxxCompilerGccSensor(Settings settings) {
    super(settings, new CxxCompilerGccParser(), CxxMetrics.GCC_COMPILER);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(CxxLanguage.KEY).name("CxxCompilerGccSensor");
  }
  
  @Override
  protected String reportPathKey() {
    return REPORT_PATH_KEY;
  }

  @Override
  protected String getReportCharsetDef()
  {
    return getParserStringProperty(REPORT_CHARSET_DEF, super.getReportCharsetDef());
  }

  @Override
  protected String getReportRegexpDef()
  {
    return getParserStringProperty(REPORT_REGEX_DEF, super.getReportRegexpDef());
  }
}
