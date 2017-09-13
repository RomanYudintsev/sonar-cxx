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

import org.sonar.cxx.sensors.compiler.CxxCompilerGccParser;
import org.sonar.cxx.sensors.compiler.CxxCompilerVcParser;
import org.sonar.cxx.sensors.compiler.CxxCompilerSensor;
import org.sonar.cxx.sensors.compiler.CompilerParser;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Settings;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.sensors.utils.TestUtils;

public class CxxCompilerSensorTest {

  private DefaultFileSystem fs;
  private RulesProfile profile;
  private CxxLanguage language;


  @Before
  public void setUp() {
    fs = TestUtils.mockFileSystem();
    profile = mock(RulesProfile.class);
    language = TestUtils.mockCxxLanguage();
    when(language.getPluginProperty(CxxCompilerSensor.REPORT_PATH_KEY)).thenReturn("sonar.cxx." + CxxCompilerSensor.REPORT_PATH_KEY);
    when(language.getPluginProperty(CxxCompilerSensor.PARSER_KEY_DEF)).thenReturn("sonar.cxx." + CxxCompilerSensor.PARSER_KEY_DEF);
    when(language.getPluginProperty(CxxCompilerSensor.REPORT_CHARSET_DEF)).thenReturn("sonar.cxx." + CxxCompilerSensor.REPORT_CHARSET_DEF);
    when(language.getPluginProperty(CxxCompilerSensor.REPORT_REGEX_DEF)).thenReturn("sonar.cxx." + CxxCompilerSensor.REPORT_REGEX_DEF);
  }

  @Test
  public void shouldReportCorrectGccViolations() {    
    SensorContextTester context = SensorContextTester.create(fs.baseDir());

    Settings settings = new Settings();
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_PATH_KEY), "compiler-reports/build.gcclog");
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.PARSER_KEY_DEF), CxxCompilerGccParser.COMPILER_KEY);
    context.setSettings(settings);

    context.fileSystem().add(new DefaultInputFile("ProjectKey", "src/zipmanager.cpp")
      .setLanguage("cpp").initMetadata("asd\nasdas\nasda\n"));
    
    CxxCompilerSensor sensor = new CxxCompilerSensor(language, settings);
    sensor.execute(context);
    assertThat(context.allIssues()).hasSize(4);
  }

  @Test
  public void shouldReportACorrectVcViolations() {
    SensorContextTester context = SensorContextTester.create(fs.baseDir());

    Settings settings = new Settings();
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_PATH_KEY), "compiler-reports/BuildLog.htm");
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.PARSER_KEY_DEF), CxxCompilerVcParser.COMPILER_KEY);
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_CHARSET_DEF), "UTF-16");
    context.setSettings(settings);

    context.fileSystem().add(new DefaultInputFile("ProjectKey", "zipmanager.cpp")
      .setLanguage("cpp").initMetadata("asd\nasdas\nasda\n"));

    CxxCompilerSensor sensor = new CxxCompilerSensor(language, settings);
    sensor.execute(context);
    assertThat(context.allIssues()).hasSize(9);
  }

  @Test
  public void shouldReportBCorrectVcViolations() {
    when(language.getStringOption(language.getPluginProperty(CxxCompilerSensor.REPORT_CHARSET_DEF))).thenReturn("UTF-8");
    SensorContextTester context = SensorContextTester.create(fs.baseDir());

    Settings settings = new Settings();
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_PATH_KEY), "compiler-reports/VC-report.vclog");
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.PARSER_KEY_DEF), CxxCompilerVcParser.COMPILER_KEY);
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_CHARSET_DEF), "UTF-8");
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_REGEX_DEF), "^.*>(?<filename>.*)\\((?<line>\\d+)\\):\\x20warning\\x20(?<id>C\\d+):(?<message>.*)$");
    context.setSettings(settings);

    context.fileSystem().add(new DefaultInputFile("ProjectKey", "Server/source/zip/zipmanager.cpp")
      .setLanguage("cpp").initMetadata("asd\nasdas\nasda\n"));

    CxxCompilerSensor sensor = new CxxCompilerSensor(language, settings);
    sensor.execute(context);
    assertThat(context.allIssues()).hasSize(9);
  }

  @Test
  public void shouldReportCorrectVcViolations() {
    SensorContextTester context = SensorContextTester.create(fs.baseDir());

    Settings settings = new Settings();
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_PATH_KEY), "compiler-reports/VC-report.vclog");
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.PARSER_KEY_DEF), CxxCompilerVcParser.COMPILER_KEY);
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_CHARSET_DEF), "UTF-8");
    settings.setProperty(language.getPluginProperty(CxxCompilerSensor.REPORT_REGEX_DEF), "^(.*)\\((\\d+)\\):\\x20warning\\x20(C\\d+):(.*)$");
    context.setSettings(settings);

    context.fileSystem().add(new DefaultInputFile("ProjectKey", "Server/source/zip/zipmanager.cpp")
      .setLanguage("cpp").initMetadata("asd\nasdas\nasda\n"));
    
    CxxCompilerSensor sensor = new CxxCompilerSensor(language, settings);
    sensor.execute(context);
    assertThat(context.allIssues()).hasSize(9);
  }

  @Test
  public void shouldReportWarningsWithoutFileAndLineInformation() throws XMLStreamException {
    List<CompilerParser.Warning> warnings = Arrays.asList(
        new CompilerParser.Warning("filename1", "line1", "id1", "msg2"),
        new CompilerParser.Warning("filename1", null, "id2", "msg1"),
        new CompilerParser.Warning(null, null, "id3", "msg1"),
        new CompilerParser.Warning(null, null, "id4", null)
        );

    Settings settings = new Settings();

    MockCxxCompilerSensor sensor = new MockCxxCompilerSensor(language, settings, fs, profile, warnings);
      SensorContextTester context = SensorContextTester.create(fs.baseDir());
      sensor.processReport(context, null);

    Assert.assertTrue(warnings.containsAll(sensor.savedWarnings));
    Assert.assertTrue(sensor.savedWarnings.containsAll(warnings));
  }
}

