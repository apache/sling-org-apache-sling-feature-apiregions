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

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.BUNDLE_FEATURE_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.FEATURE_REGION_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.IDBSNVER_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.PROPERTIES_FILE_LOCATION;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.PROPERTIES_RESOURCE_PREFIX;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.REGION_PACKAGE_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.SYNTHESIZED_BUNDLES_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegionEnforcerTest {
    @Test
    public void testRegionEnforcerNoConfiguration() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>(), "*");
        assertEquals(0, re.bsnVerMap.size());
        assertEquals(0, re.bundleFeatureMap.size());
        assertEquals(0, re.featureRegionMap.size());
        assertEquals(0, re.regionPackageMap.size());
    }

    @Test
    public void testLoadBSNVerMap() throws Exception {
        String f = getClass().getResource("/idbsnver1.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(ctx, props, "*");
        assertEquals(2, re.bsnVerMap.size());
        assertEquals(Collections.singletonList("g:b1:1"),
                re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b1", new Version(1,0,0))));
        assertEquals(new HashSet<>(Arrays.asList("g:b2:1.2.3", "g2:b2:1.2.4")),
                new HashSet<>(re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b2", new Version(1,2,3)))));
        assertEquals(new File(f).toURI().toString(), props.get(IDBSNVER_FILENAME));
    }

    @Test
    public void testLoadBundleFeatureMap() throws Exception {
        String f = getClass().getResource("/bundles1.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(ctx, props, "*");
        assertEquals(3, re.bundleFeatureMap.size());
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b1:1"));
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b2:1"));
        assertEquals(new HashSet<>(Arrays.asList("some.other:feature:123", "org.sling:something:1.2.3:slingosgifeature:myclassifier")),
                re.bundleFeatureMap.get("org.sling:b3:1"));
        assertEquals(new File(f).toURI().toString(), props.get(BUNDLE_FEATURE_FILENAME));
    }

    @Test
    public void testLoadFeatureRegionMap() throws Exception {
        String f = getClass().getResource("/features1.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(ctx, props, "*");
        assertEquals(2, re.featureRegionMap.size());
        assertEquals(Collections.singleton("global"),
                re.featureRegionMap.get("an.other:feature:123"));
        assertEquals(new HashSet<>(Arrays.asList("global", "internal")),
                re.featureRegionMap.get("org.sling:something:1.2.3"));
        assertEquals(new File(f).toURI().toString(), props.get(FEATURE_REGION_FILENAME));
    }

    @Test
    public void testLoadRegionPackageMap() throws Exception {
        String f = getClass().getResource("/regions1.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(ctx, props, "*");
        assertEquals(2, re.regionPackageMap.size());
        assertEquals(Collections.singleton("xyz"),
                re.regionPackageMap.get("internal"));
        assertEquals(new HashSet<>(Arrays.asList("a.b.c", "d.e.f", "test")),
                re.regionPackageMap.get("global"));
        assertEquals(new File(f).toURI().toString(), props.get(REGION_PACKAGE_FILENAME));
    }

    @Test
    public void testBegin() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).
            thenReturn(getClass().getResource("/idbsnver1.properties").getFile());
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).
            thenReturn(getClass().getResource("/bundles1.properties").getFile());
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).
            thenReturn(getClass().getResource("/features1.properties").getFile());
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).
            thenReturn(getClass().getResource("/regions1.properties").getFile());

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>(), "*");
        assertTrue(re.bsnVerMap.size() > 0);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertTrue(re.featureRegionMap.size() > 0);
        assertTrue(re.regionPackageMap.size() > 0);

        ResolverHookImpl hook = (ResolverHookImpl) re.begin(null);
        assertEquals(re.bsnVerMap, hook.bsnVerMap);
        assertEquals(re.bundleFeatureMap, hook.bundleFeatureMap);
        assertEquals(re.featureRegionMap, hook.featureRegionMap);
        assertEquals(re.regionPackageMap, hook.regionPackageMap);
    }

    @Test
    public void testURLs() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        String location = new File(getClass().getResource("/props1/idbsnver.properties").
                getFile()).getParentFile().toURI().toString();
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).thenReturn(location);

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>(), "*");
        assertTrue(re.bsnVerMap.size() > 0);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertTrue(re.featureRegionMap.size() > 0);
        assertTrue(re.regionPackageMap.size() > 0);
    }

    @Test
    public void testClassloaderURLs() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
            thenReturn("classloader://props1");

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>(), "*");
        assertTrue(re.bsnVerMap.size() > 0);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertTrue(re.featureRegionMap.size() > 0);
        assertTrue(re.regionPackageMap.size() > 0);
    }

    @Test
    public void testOrderingOfRegionsInFeatures() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
            thenReturn("classloader://props2");

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>(), "*");
        assertEquals(Arrays.asList("r0", "r1", "r2", "r3"),
                new ArrayList<>(re.featureRegionMap.get("org.sling:something:1.2.3")));
    }

    @Test
    public void testFrameworkPropertyBundleRegions() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(SYNTHESIZED_BUNDLES_KEY)).thenReturn(
                "org.foo.bar:1.2.3=org.foo:bar:1.2.3;myregion," +
                "org.foo.jar:1.0.0=org.foo:jar:1.0.0;myregion");

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>(), "*");

        assertTrue(re.bsnVerMap.size() == 2);
        List<String> al1 = re.bsnVerMap.get(new AbstractMap.SimpleEntry<>(
                "org.foo.bar", Version.valueOf("1.2.3")));
        assertEquals(1, al1.size());
        String a1 = al1.iterator().next();

        List<String> al2 = re.bsnVerMap.get(new AbstractMap.SimpleEntry<>(
                "org.foo.jar", Version.valueOf("1.0.0")));
        assertEquals(1, al2.size());
        String a2 = al2.iterator().next();

        Set<String> fl1 = re.bundleFeatureMap.get(a1);
        assertEquals(1, fl1.size());
        String f1 = fl1.iterator().next();
        Set<String> fl2 = re.bundleFeatureMap.get(a2);
        assertEquals(1, fl2.size());
        String f2 = fl2.iterator().next();
        assertEquals(f1, f2);

        Set<String> rl = re.featureRegionMap.get(f1);
        assertEquals(1, rl.size());
        String r = rl.iterator().next();
        assertEquals("myregion", r);
    }
}
