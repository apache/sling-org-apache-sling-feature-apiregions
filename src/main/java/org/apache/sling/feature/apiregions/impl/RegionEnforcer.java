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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class RegionEnforcer implements ResolverHookFactory {
    private static final String CLASSLOADER_PSEUDO_PROTOCOL = "classloader://";

    public static final String GLOBAL_REGION = "global";

    static final String PROPERTIES_RESOURCE_PREFIX = "sling.feature.apiregions.resource.";
    static final String PROPERTIES_FILE_LOCATION = "sling.feature.apiregions.location";

    static final String IDBSNVER_FILENAME = "idbsnver.properties";
    static final String BUNDLE_FEATURE_FILENAME = "bundles.properties";
    static final String FEATURE_REGION_FILENAME = "features.properties";
    static final String REGION_PACKAGE_FILENAME = "regions.properties";

    final Map<Map.Entry<String, Version>, List<String>> bsnVerMap;
    final Map<String, Set<String>> bundleFeatureMap;
    final Map<String, Set<String>> featureRegionMap;
    final Map<String, Set<String>> regionPackageMap;
    final Set<String> enabledRegions;

    RegionEnforcer(BundleContext context, Dictionary<String, Object> regProps, String regionsProp)
            throws IOException, URISyntaxException {
        URI idbsnverFile = getDataFileURI(context, IDBSNVER_FILENAME);
        bsnVerMap = populateBSNVerMap(idbsnverFile);
        if (idbsnverFile != null) {
            // Register the location as a service property for diagnostic purposes
            regProps.put(IDBSNVER_FILENAME, idbsnverFile.toString());
        }

        URI bundlesFile = getDataFileURI(context, BUNDLE_FEATURE_FILENAME);
        bundleFeatureMap = populateBundleFeatureMap(bundlesFile);
        if (bundlesFile != null) {
            // Register the location as a service property for diagnostic purposes
            regProps.put(BUNDLE_FEATURE_FILENAME, bundlesFile.toString());
        }

        URI featuresFile = getDataFileURI(context, FEATURE_REGION_FILENAME);
        featureRegionMap = populateFeatureRegionMap(featuresFile);
        if (featuresFile != null) {
            // Register the location as a service property for diagnostic purposes
            regProps.put(FEATURE_REGION_FILENAME, featuresFile.toString());
        }

        URI regionsFile = getDataFileURI(context, REGION_PACKAGE_FILENAME);
        regionPackageMap = populateRegionPackageMap(regionsFile);
        if (regionsFile != null) {
            // Register the location as a service property for diagnostic purposes
            regProps.put(REGION_PACKAGE_FILENAME, regionsFile.toString());
        }

        enabledRegions = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(regionsProp.split(","))));
    }

    private Map<Map.Entry<String, Version>, List<String>> populateBSNVerMap(URI idbsnverFile) throws IOException {
        if (idbsnverFile == null) {
            return Collections.emptyMap();
        }

        Map<Map.Entry<String, Version>, List<String>> m = new HashMap<>();

        Properties p = new Properties();
        try (InputStream is = idbsnverFile.toURL().openStream()) {
            p.load(is);
        }

        for (String n : p.stringPropertyNames()) {
            String[] bsnver = p.getProperty(n).split("~");
            Map.Entry<String, Version> key = new AbstractMap.SimpleEntry<>(bsnver[0], Version.valueOf(bsnver[1]));
            List<String> l = m.get(key);
            if (l == null) {
                l = new ArrayList<>();
                m.put(key, l);
            }
            l.add(n);
        }

        Map<Map.Entry<String, Version>, List<String>> m2 = new HashMap<>();

        for (Map.Entry<Map.Entry<String, Version>, List<String>> entry : m.entrySet()) {
            m2.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }

        return Collections.unmodifiableMap(m2);
    }

    private Map<String, Set<String>> populateBundleFeatureMap(URI bundlesFile) throws IOException {
        return loadMap(bundlesFile);
    }

    private Map<String, Set<String>> populateFeatureRegionMap(URI featuresFile) throws IOException {
        return loadMap(featuresFile);
    }

    private Map<String, Set<String>> populateRegionPackageMap(URI regionsFile) throws IOException {
        return loadMap(regionsFile);
    }

    private Map<String, Set<String>> loadMap(URI propsFile) throws IOException {
        if (propsFile == null) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> m = new HashMap<>();

        Properties p = new Properties();
        try (InputStream is = propsFile.toURL().openStream()) {
            p.load(is);
        }

        for (String n : p.stringPropertyNames()) {
            String[] features = p.getProperty(n).split(",");
            m.put(n, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(features))));
        }

        return Collections.unmodifiableMap(m);
    }

    private URI getDataFileURI(BundleContext ctx, String name) throws IOException, URISyntaxException {
        String fn = ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + name);
        if (fn == null) {
            String loc = ctx.getProperty(PROPERTIES_FILE_LOCATION);
            if (loc != null) {
                fn = loc + "/" + name;
            }
        }

        if (fn == null)
            return null;

        if (fn.contains(":")) {
            if (fn.startsWith(CLASSLOADER_PSEUDO_PROTOCOL)) {
                // It's using the 'classloader:' protocol looks up the location from the classloader
                String loc = fn.substring(CLASSLOADER_PSEUDO_PROTOCOL.length());
                if (!loc.startsWith("/"))
                    loc = "/" + loc;
                fn = getClass().getResource(loc).toString();
            }
            // It's already a URL
            return new URI(fn);
        } else {
            // It's a file location
            return new File(fn).toURI();
        }
    }

    @Override
    public ResolverHook begin(Collection<BundleRevision> triggers) {
        if (enabledRegions.isEmpty())
            return null;
        return new ResolverHookImpl(bsnVerMap, bundleFeatureMap, featureRegionMap, regionPackageMap);
    }
}
