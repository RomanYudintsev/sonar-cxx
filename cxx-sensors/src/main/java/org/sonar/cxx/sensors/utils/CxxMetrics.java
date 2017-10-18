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
package org.sonar.cxx.sensors.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.sensors.compiler.CxxCompilerClangSensor;
import org.sonar.cxx.sensors.compiler.CxxCompilerGccSensor;
import org.sonar.cxx.sensors.compiler.CxxCompilerSensor;
import org.sonar.cxx.sensors.compiler.CxxCompilerVcSensor;
import org.sonar.cxx.sensors.cppcheck.CxxCppCheckSensor;
import org.sonar.cxx.sensors.drmemory.CxxDrMemorySensor;
import org.sonar.cxx.sensors.other.CxxOtherSensor;
import org.sonar.cxx.sensors.pclint.CxxPCLintSensor;
import org.sonar.cxx.sensors.rats.CxxRatsSensor;
import org.sonar.cxx.sensors.squid.CxxSquidSensor;
import org.sonar.cxx.sensors.valgrind.CxxValgrindSensor;
import org.sonar.cxx.sensors.veraxx.CxxVeraxxSensor;

/**
 * {@inheritDoc}
 */
public class CxxMetrics implements Metrics {
  private final CxxLanguage language;

  private static final Logger LOG = Loggers.get(CxxCompilerGccSensor.class);

  /**
  * CxxMetrics
  * @param language
  **/
  public CxxMetrics(CxxLanguage language) {
    this.language = language;
    language.bindInst();
    
    this.buildMetric(CxxCompilerSensor.COMPILER_KEY, "Compiler issues", language);
    this.buildMetric(CxxCompilerClangSensor.COMPILER_KEY, "Compiler Clang issues", language);
    this.buildMetric(CxxCompilerGccSensor.COMPILER_KEY, "Compiler Gcc issues", language);
    this.buildMetric(CxxCompilerVcSensor.COMPILER_KEY, "Compiler Vc issues", language);
    this.buildMetric(CxxCppCheckSensor.KEY, "CppCheck issues", language);
    this.buildMetric(CxxOtherSensor.KEY, "Other tools issues", language);
    this.buildMetric(CxxPCLintSensor.KEY, "PC-Lint issues", language);
    this.buildMetric(CxxRatsSensor.KEY, "Rats issues", language);    
    this.buildMetric(CxxSquidSensor.KEY, "Squid issues", language);      
    this.buildMetric(CxxValgrindSensor.KEY, "Valgrind issues", language);    
    this.buildMetric(CxxVeraxxSensor.KEY, "Vera issues", language);    
    this.buildMetric(CxxDrMemorySensor.KEY, "DrMemory issues", language);
  }

  /**
  * GetKey
  * @param key
  * @param language
  * @return String
  **/
  public static String getKey(String key, CxxLanguage language) {
    return language.getPropertiesKey().toUpperCase(Locale.ENGLISH) + "-" + key.toUpperCase(Locale.ENGLISH);
  }


  
  @Override
  public List<Metric> getMetrics() {
    return new ArrayList(this.language.getMetricsCache());
  }

  private void buildMetric(String key, String description, CxxLanguage language) {
    String effectiveKey = CxxMetrics.getKey(key, language);
    Metric<?> metric = new Metric.Builder(effectiveKey, description, Metric.ValueType.INT)
    .setDescription(description)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(Boolean.TRUE)
    .setDomain(language.getKey().toUpperCase(Locale.ENGLISH))
    .create();
    
    language.SaveMetric(metric, key);    
  }
}
