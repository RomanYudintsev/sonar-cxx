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

import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.cxx.CxxLanguage;
import org.sonar.plugins.cxx.utils.CxxMetrics;
import org.sonar.plugins.cxx.utils.CxxReportSensor;

import javax.xml.stream.XMLStreamException;
import java.io.File;

public class CxxScanbuildSensor extends CxxReportSensor {
    public static final Logger LOG = Loggers.get(ScanbuildParser.class);
    public static final String REPORT_PATH_KEY = "sonar.cxx.scanbuild.reportPath";

    private final ScanbuildParser parser = new ScanbuildParser(this);

    /**
     * {@inheritDoc}
     */
    public CxxScanbuildSensor(Settings settings) {
        super(settings, CxxMetrics.SCANBUILD);
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
    protected void processReport(final org.sonar.api.batch.sensor.SensorContext context, File report)
            throws javax.xml.stream.XMLStreamException {
        boolean parsed = false;

        try {
            parser.processReport(context, report);
            LOG.info("Added report '{}' (parsed by: {})", report, parser);
            parsed = true;
        } catch (XMLStreamException e) {
            LOG.trace("Report {} cannot be parsed by {}", report, parser);
        }

        if (!parsed) {
            LOG.error("Report {} cannot be parsed", report);
        }
    }
}