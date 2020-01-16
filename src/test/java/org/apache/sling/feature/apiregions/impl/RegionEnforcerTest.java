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
import org.osgi.framework.hooks.resolver.ResolverHook;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.APIREGIONS_JOINGLOBAL;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.BUNDLE_FEATURE_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.DEFAULT_REGIONS;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.FEATURE_REGION_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.IDBSNVER_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.PROPERTIES_FILE_LOCATION;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.PROPERTIES_RESOURCE_PREFIX;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.REGION_PACKAGE_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RegionEnforcerTest {
    @Test
    public void testRegionEnforcerNoConfiguration() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);

        try {
            new RegionEnforcer(ctx, new Hashtable<String, Object>());
            fail("Expected exception. Enforcer is enabled but is missing configuration");
        } catch (Exception e) {
            // good
        }
    }

    @Test
    public void testLoadBSNVerMap() throws Exception {
        String e = getClass().getResource("/empty.properties").getFile();
        String f = getClass().getResource("/idbsnver1.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(ctx, props);
        assertEquals(2, re.bsnVerMap.size());
        assertEquals(Collections.singletonList("g:b1:1"),
                re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b1", new Version(1,0,0))));
        assertEquals(new HashSet<>(Arrays.asList("g:b2:1.2.3", "g2:b2:1.2.4")),
                new HashSet<>(re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b2", new Version(1,2,3)))));
        assertEquals(new File(f).toURI().toString(), props.get(IDBSNVER_FILENAME));
    }

    @Test
    public void testLoadBundleFeatureMap() throws Exception {
        String e = getClass().getResource("/empty.properties").getFile();
        String f = getClass().getResource("/bundles1.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(ctx, props);
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
        String e = getClass().getResource("/empty.properties").getFile();
        String f = getClass().getResource("/features1.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(f);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(e);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(ctx, props);
        assertEquals(2, re.featureRegionMap.size());
        assertEquals(Collections.singleton("global"),
                re.featureRegionMap.get("an.other:feature:123"));
        assertEquals(new HashSet<>(Arrays.asList("global", "internal")),
                re.featureRegionMap.get("org.sling:something:1.2.3"));
        assertEquals(new File(f).toURI().toString(), props.get(FEATURE_REGION_FILENAME));
    }

    @Test
    public void testLoadRegionPackageMap() throws Exception {
        String e = getClass().getResource("/empty.properties").getFile();
        String f = getClass().getResource("/regions1.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(ctx, props);
        assertEquals(2, re.regionPackageMap.size());
        assertEquals(Collections.singleton("xyz"),
                re.regionPackageMap.get("internal"));
        assertEquals(new HashSet<>(Arrays.asList("a.b.c", "d.e.f", "test")),
                re.regionPackageMap.get("global"));
        assertEquals(new File(f).toURI().toString(), props.get(REGION_PACKAGE_FILENAME));
    }

    @Test
    public void testJoinRegionsToGlobal() throws Exception {
        String e = getClass().getResource("/empty.properties").getFile();
        String f = getClass().getResource("/regions2.properties").getFile();
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(APIREGIONS_JOINGLOBAL)).thenReturn("obsolete,deprecated");
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).thenReturn(e);
        Mockito.when(ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).thenReturn(f);

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>());
        assertEquals(1, re.regionPackageMap.size());
        assertEquals(new HashSet<>(Arrays.asList("xyz", "a.b.c", "d.e.f", "test")),
                re.regionPackageMap.get("global"));
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

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>());
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

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>());
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

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>());
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

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>());
        assertEquals(Arrays.asList("r0", "r1", "r2", "r3"),
                new ArrayList<>(re.featureRegionMap.get("org.sling:something:1.2.3")));
    }

    @Test
    public void testUnModifiableMaps() throws Exception {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
            thenReturn("classloader://props1");

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>());
        assertTrue(re.bsnVerMap.size() > 0);
        assertBSNVerMapUnmodifiable(re.bsnVerMap);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertMapUnmodifiable(re.bundleFeatureMap);
        assertTrue(re.featureRegionMap.size() > 0);
        assertMapUnmodifiable(re.featureRegionMap);
        assertTrue(re.regionPackageMap.size() > 0);
        assertMapUnmodifiable(re.regionPackageMap);
    }

    @Test
    public void testDefaultRegions() throws Exception {
        testDefaultRegions("foo.bar,foo.zar", new HashSet<>(Arrays.asList("foo.bar", "foo.zar")));
        testDefaultRegions("test", Collections.singleton("test"));
        testDefaultRegions("", Collections.emptySet());
        testDefaultRegions(null, Collections.emptySet());
    }

    @SuppressWarnings("unchecked")
    private void testDefaultRegions(String defProp, Set<String> expected)
            throws IOException, URISyntaxException, NoSuchFieldException, IllegalAccessException {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(DEFAULT_REGIONS)).thenReturn(defProp);
        Mockito.when(ctx.getProperty(PROPERTIES_FILE_LOCATION)).
        thenReturn("classloader://props1");

        RegionEnforcer re = new RegionEnforcer(ctx, new Hashtable<String, Object>());
        ResolverHook hook = re.begin(Collections.emptySet());
        Field f = ResolverHookImpl.class.getDeclaredField("defaultRegions");
        f.setAccessible(true);

        assertEquals(expected, f.get(hook));
    }

    private void assertBSNVerMapUnmodifiable(Map<Map.Entry<String, Version>, List<String>> m) {
        Map.Entry<Map.Entry<String, Version>, List<String>> entry = m.entrySet().iterator().next();
        try {
            List<String> c = entry.getValue();
            c.add("test");
            fail("Changing a value should have thrown an exception");
        } catch (Exception ex) {
            // good
        }

        try {
            m.put(new AbstractMap.SimpleEntry<>("hi", Version.parseVersion("1.2.3")),
                    Collections.singletonList("xyz"));
            fail("Adding a new value should have thrown an exception");
        } catch (Exception ex) {
            // good
        }
    }

    private void assertMapUnmodifiable(Map<String, Set<String>> m) {
        Map.Entry<String, Set<String>> entry = m.entrySet().iterator().next();
        try {
            Set<String> s = entry.getValue();
            s.add("testing");
            fail("Changing a value should have thrown an exception");
        } catch (Exception ex) {
            // good
        }

        try {
            m.put("foo", Collections.<String>emptySet());
            fail("Adding a new value should have thrown an exception");
        } catch (Exception ex) {
            // good
        }
    }

}
