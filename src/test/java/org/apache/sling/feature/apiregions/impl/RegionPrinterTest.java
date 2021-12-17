/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.apiregions.impl;

import static org.apache.sling.feature.apiregions.impl.RegionConstants.BUNDLE_FEATURE_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.FEATURE_REGION_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.IDBSNVER_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.PROPERTIES_RESOURCE_PREFIX;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.REGION_PACKAGE_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.io.IOUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class RegionPrinterTest {

    private RegionConfiguration regionConfiguration;
    private BundleContext bundleContext;
    private PrintWriter pw;
    private StringWriter sw;

    @Before
    public void setup() {
        regionConfiguration = mock(RegionConfiguration.class);
        bundleContext = mock(BundleContext.class);
        sw = new StringWriter();
        pw = new PrintWriter(sw);
    }

    private String loadResource(String name) {
        return IOUtil.readLines(RegionPrinterTest.class.getClassLoader().getResourceAsStream(name)).stream()
                .collect(Collectors.joining(String.format("\n")));
    }

    @Test
    public void testBasic() {
        RegionPrinter printer = new RegionPrinter(bundleContext, regionConfiguration);
        printer.printConfiguration(pw);
        assertEquals(loadResource("printer/empty.txt"), sw.toString());
    }

    @Test
    public void testNoConfiguration() {
        RegionPrinter printer = new RegionPrinter(bundleContext, null);
        printer.printConfiguration(pw);
        assertEquals(loadResource("printer/noconfig.txt"), sw.toString());
    }

    @Test
    public void testWithData() throws URISyntaxException, IOException {

        String e = getClass().getResource("/empty.properties").toURI().toString();
        String b = getClass().getResource("/bundles1.properties").toURI().toString();
        String f = getClass().getResource("/features1.properties").toURI().toString();
        String r = getClass().getResource("/regions1.properties").toURI().toString();

        when(bundleContext.getBundle()).thenReturn(mock(Bundle.class));
        when(bundleContext.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        when(bundleContext.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(b);
        when(bundleContext.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(f);
        when(bundleContext.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(r);

        regionConfiguration = new RegionConfiguration(bundleContext);

        RegionPrinter printer = new RegionPrinter(bundleContext, regionConfiguration);
        printer.printConfiguration(pw);
        assertEquals(loadResource("printer/populated.txt"), sw.toString());
    }
}
