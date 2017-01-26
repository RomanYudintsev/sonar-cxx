/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2016 SonarOpenCommunity
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
package org.sonar.plugins.cxx.scanbuild;

import org.apache.commons.io.FilenameUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.cxx.CxxLanguage;
import org.sonar.plugins.cxx.utils.CxxMetrics;
import org.sonar.plugins.cxx.utils.CxxReportSensor;
import org.sonar.api.config.Settings;
import org.sonar.plugins.cxx.utils.CxxUtils;
import org.sonar.plugins.cxx.utils.EmptyReportException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class CxxScanbuildSensor extends CxxReportSensor {
    public static final Logger LOG = Loggers.get(ScanbuildParser.class);
    public static final String REPORT_PATH_KEY = "sonar.cxx.scanbuild.reportPath";

    private final List<ScanbuildParser> parsers = new LinkedList<>();

    private final Metric metric;

    /**
     * {@inheritDoc}
     */
    public CxxScanbuildSensor(Settings settings) {
        super(settings, CxxMetrics.SCANBUILD);
        metric = CxxMetrics.SCANBUILD;
        parsers.add(new ScanbuildParser(this));
    }

    @Override
    protected String reportPathKey() {
        return REPORT_PATH_KEY;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.onlyOnLanguage(CxxLanguage.KEY).name("CxxScanbuildSensor");
    }

    @Override
    public void execute(SensorContext context) {
        try {
            LOG.info("Searching reports by relative path with basedir '{}' and search prop '{}'", context.fileSystem().baseDir(), reportPathKey());
            List<File> reports = getRecursiveReports(settings, context.fileSystem().baseDir(), reportPathKey());
            int violationsCount = 0;
            LOG.info("'{}' reports found", reports.size());
            for (File report : reports) {
                int prevViolationsCount = violationsCount;
                LOG.info("Processing report '{}'", report);
                try {
                    processReport(context, report);
                    LOG.debug("{} processed = {}", metric == null ? "Issues" : metric.getName(),
                            violationsCount - prevViolationsCount);
                } catch (EmptyReportException e) {
                    LOG.warn("The report '{}' seems to be empty, ignoring.", report);
                    CxxUtils.ValidateRecovery(e, settings);
                }
            }

            LOG.info("{} processed = {}", metric == null ? "Issues" : metric.getName(),
                    violationsCount);

            if (metric != null) {
                context.<Integer>newMeasure()
                        .forMetric(metric)
                        .on(context.module())
                        .withValue(violationsCount)
                        .save();
            }
        } catch (Exception e) {
            String msg = new StringBuilder()
                    .append("Cannot feed the data into sonar, details: '")
                    .append(e)
                    .append("'")
                    .toString();
            throw new IllegalStateException(msg, e);
        }
    }

    protected List<File> getRecursiveReports(Settings settings, final File moduleBaseDir, String reportPathPropertyKey)
    {
        List<File> reports = new ArrayList<File>();

        List<String> reportsPaths = Arrays.asList(settings.getStringArray(reportPathPropertyKey));
        if (!reportsPaths.isEmpty()) {
            for (String reportPaths : reportsPaths) {
                for (File reportsDir : new File(reportPaths).listFiles()) {
                    if (!reportsDir.isDirectory() || !reportsDir.exists()) {
                        LOG.info("Dir '{}' not found", reportsDir.getName());
                        continue;
                    }
                    File[] listFiles = reportsDir.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            LOG.info("Check file: '{}'", name);
                            LOG.info("Result : '{}'", name.endsWith(".plist"));
                            return name.endsWith(".plist");
                        }
                    });

                    for (File rep : listFiles) {
                        reports.add(rep);
                    }
                }

                LOG.debug("Not a valid report path '{}'", reportPaths);
            }

            LOG.info("Scanner found '{}' report files", reports.size());

            if (reports.isEmpty()) {
                LOG.warn("Cannot find a report for '{}'", reportPathPropertyKey);
            } else {
                LOG.info("Parser will parse '{}' report files", reports.size());
            }
        } else {
            LOG.info("Undefined report path value for key '{}'", reportPathPropertyKey);
        }
        return reports;
    }

    @Override
    protected void processReport(final org.sonar.api.batch.sensor.SensorContext context, File report)
            throws javax.xml.stream.XMLStreamException {
        boolean parsed = false;

        for (ScanbuildParser parser : parsers) {
            try {
                parser.processReport(context, report);
                LOG.info("Added report '{}' (parsed by: {})", report, parser);
                parsed = true;
                break;
            } catch (XMLStreamException e) {
                LOG.trace("Report {} cannot be parsed by {}", report, parser);
            }
        }

        if (!parsed) {
            LOG.error("Report {} cannot be parsed", report);
        }
    }
}