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

import com.dd.plist.*;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ScanbuildParser {
    public static final Logger LOG = Loggers.get(ScanbuildParser.class);
    private final CxxScanbuildSensor sensor;

    public ScanbuildParser(CxxScanbuildSensor sensor) {
        this.sensor = sensor;
    }

    /**
     * {@inheritDoc}
     */
    public void processReport(final SensorContext context, File report)
            throws javax.xml.stream.XMLStreamException {
        LOG.debug("Parsing Clang plist format");
        plistParse(context, report);
    }

    private void plistParse(final SensorContext context, final File file)
    {
        try {
            // Clang report is NSDictionary, which converts to a Map
            Map<String, Object> report = (Map<String, Object>) XMLPropertyListParser.parse(file).toJavaObject();

            List<String> files = new ArrayList<String>();
            for (Object obj : (Object[]) report.get("files")) {
                files.add((String) obj);
            }
            List<Map<String, Object>> diagnostics = new ArrayList<Map<String, Object>>();
            for (Object obj : (Object[]) report.get("diagnostics")) {
                diagnostics.add((Map<String, Object>) obj);
            }

            for (Map<String, Object> diagnostic : diagnostics) {
                Map<String, Object> location = (Map<String, Object>) diagnostic.get("location");

                // diagnostic.keySet().forEach(key -> LOG.warn("diagnostic ::  {}",key + "->" + diagnostic.get(key)));

                String issue_context = (String) diagnostic.get("issue_context");
                if (!keyInRepository(issue_context, context))
                {
                    issue_context = "unknownError";
                }
                String msg = (String) diagnostic.get("description");
                String line = location.get("line").toString();
                String fileName = files.get((int) location.get("file")).replace("../../", "");

                LOG.warn("context.fileSystem().baseDir() - '{}'", context.fileSystem().baseDir());


                LOG.warn("file + line + issue_context + msg - '{} {} {} {}'", fileName,line,issue_context,msg);
                if (isInputValid(fileName, line, issue_context, msg)) {
                    sensor.saveUniqueViolation(context, CxxScanbuildRuleRepository.KEY, fileName, line, issue_context, msg);
                } else {
                    LOG.warn("Skipping invalid violation: msg - '{}'", msg);
                    LOG.warn("Skipping invalid violation: id - '{}'", issue_context);
                }
            }

        } catch (final IOException e) {
            LOG.error("Error processing file named {}", file, e);
        } catch (final ParserConfigurationException e) {
            LOG.error("Error processing file named {}", file, e);
        } catch (final ParseException e) {
            LOG.error("Error processing file named {}", file, e);
        } catch (final SAXException e) {
            LOG.error("Error processing file named {}", file, e);
        } catch (final PropertyListFormatException e) {
            LOG.error("Error processing file named {}", file, e);
        }

    }

    private boolean keyInRepository(String key, final SensorContext context)
    {
        if (key == null || key.isEmpty())
            return false;
        LOG.warn("find key {} in rules repository {}", key, CxxScanbuildRuleRepository.KEY);
        ActiveRule result = context.activeRules().findByInternalKey(CxxScanbuildRuleRepository.KEY, key);
        LOG.warn("result :: {}", null != result);
        return null != result;
    }

    private boolean isInputValid(String file, String line, String id, String msg) {
        return id != null && !id.isEmpty() && msg != null && !msg.isEmpty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
