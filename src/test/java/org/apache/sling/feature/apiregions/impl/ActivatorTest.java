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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.BUNDLE_FEATURE_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.FEATURE_REGION_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.IDBSNVER_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.PROPERTIES_RESOURCE_PREFIX;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.REGION_PACKAGE_FILENAME;

public class ActivatorTest {
    private Properties savedProps;

    @Before
    public void setup() {
        savedProps = new Properties(); // note that new Properties(props) doesn't copy
        savedProps.putAll(System.getProperties());
    }

    @After
    public void teardown() {
        System.setProperties(savedProps);
        savedProps = null;
    }

    @Test
    public void testStart() throws Exception {
        String i = getClass().getResource("/idbsnver1.properties").getFile();
        String b = getClass().getResource("/bundles1.properties").getFile();
        String f = getClass().getResource("/features1.properties").getFile();
        String r = getClass().getResource("/regions1.properties").getFile();

        Dictionary<String, Object> expectedProps = new Hashtable<>();
        expectedProps.put(IDBSNVER_FILENAME, new File(i).toURI().toString());
        expectedProps.put(BUNDLE_FEATURE_FILENAME, new File(b).toURI().toString());
        expectedProps.put(FEATURE_REGION_FILENAME, new File(f).toURI().toString());
        expectedProps.put(REGION_PACKAGE_FILENAME, new File(r).toURI().toString());

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getProperty(Activator.REGIONS_PROPERTY_NAME)).thenReturn("*");
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).
            thenReturn(i);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).
            thenReturn(b);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).
            thenReturn(f);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).
            thenReturn(r);

        Activator a = new Activator();
        a.start(bc);

        Mockito.verify(bc, Mockito.times(1)).registerService(
                Mockito.eq(ResolverHookFactory.class),
                Mockito.isA(RegionEnforcer.class),
                Mockito.eq(expectedProps));
    }
}
