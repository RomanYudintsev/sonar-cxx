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

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.cxx.utils.CxxReportSensor;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * compiler for C++ with advanced analysis features (e.g. for VC 2008 team
 * edition or 2010/2012/2013 premium edition)
 *
 * @author Bert
 */
public abstract class CxxCompilerSensor extends CxxReportSensor {
    public static final Logger LOG = Loggers.get(CxxCompilerSensor.class);

    private final CompilerParser activeParser;
    /**
     * {@inheritDoc}
     */
    public CxxCompilerSensor(Settings settings, CompilerParser parser, Metric metric) {
        super(settings, metric);
        activeParser = parser;
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
        String s = getStringProperty(name, "");
        if (s == null || s.isEmpty()) {
            return def;
        }
        return s;
    }


    protected String getReportCharsetDef()
    {
        return activeParser.defaultCharset();
    }

    protected String getReportRegexpDef()
    {
        return activeParser.defaultRegexp();
    }

    @Override
    protected void processReport(final SensorContext context, File report)
            throws javax.xml.stream.XMLStreamException {
        final String reportCharset = getReportCharsetDef();
        final String reportRegEx = getReportRegexpDef();
        final List<CompilerParser.Warning> warnings = new LinkedList<>();

        // Iterate through the lines of the input file
        LOG.info("Scanner '{}' initialized with report '{}', CharSet= '{}'",
                new Object[]{activeParser.key(), report, reportCharset});
        try {
            activeParser.processReport(context, report, reportCharset, reportRegEx, warnings);
            for (CompilerParser.Warning w : warnings) {
                if (isInputValid(w)) {
                    saveUniqueViolation(context, activeParser.rulesRepositoryKey(), w.filename, w.line, w.id, w.msg);
                } else {
                    LOG.warn("C-Compiler warning: '{}''{}'", w.id, w.msg);
                }
            }
        } catch (java.io.FileNotFoundException|java.lang.IllegalArgumentException e) {
            LOG.error("processReport Exception: {} - not processed '{}'", report, e);
        }
    }

    private boolean isInputValid(CompilerParser.Warning warning) {
        return warning != null && !warning.toString().isEmpty();
    }
}
