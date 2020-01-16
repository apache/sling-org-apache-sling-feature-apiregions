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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

class RegionEnforcer implements ResolverHookFactory {
    public static final String GLOBAL_REGION = "global";

    static final String CLASSLOADER_PSEUDO_PROTOCOL = "classloader://";
    static final String APIREGIONS_JOINGLOBAL = "sling.feature.apiregions.joinglobal";
    static final String DEFAULT_REGIONS = "sling.feature.apiregions.default";
    static final String PROPERTIES_RESOURCE_PREFIX = "sling.feature.apiregions.resource.";
    static final String PROPERTIES_FILE_LOCATION = "sling.feature.apiregions.location";

    static final String IDBSNVER_FILENAME = "idbsnver.properties";
    static final String BUNDLE_FEATURE_FILENAME = "bundles.properties";
    static final String FEATURE_REGION_FILENAME = "features.properties";
    static final String REGION_PACKAGE_FILENAME = "regions.properties";

    static final Logger LOG = Logger.getLogger(ResolverHookImpl.class.getName());

    final Map<Map.Entry<String, Version>, List<String>> bsnVerMap;
    final Map<String, Set<String>> bundleFeatureMap;
    final Map<String, Set<String>> featureRegionMap;
    final Map<String, Set<String>> regionPackageMap;
    final Set<String> defaultRegions;

    RegionEnforcer(BundleContext context, Dictionary<String, Object> regProps)
            throws IOException, URISyntaxException {
        URI idbsnverFile = getDataFileURI(context, IDBSNVER_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(IDBSNVER_FILENAME, idbsnverFile.toString());
        Map<Entry<String, Version>, List<String>> bvm = populateBSNVerMap(idbsnverFile);

        URI bundlesFile = getDataFileURI(context, BUNDLE_FEATURE_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(BUNDLE_FEATURE_FILENAME, bundlesFile.toString());
        Map<String, Set<String>> bfm = populateBundleFeatureMap(bundlesFile);

        URI featuresFile = getDataFileURI(context, FEATURE_REGION_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(FEATURE_REGION_FILENAME, featuresFile.toString());
        Map<String, Set<String>> frm = populateFeatureRegionMap(featuresFile);

        URI regionsFile = getDataFileURI(context, REGION_PACKAGE_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(REGION_PACKAGE_FILENAME, regionsFile.toString());
        Map<String, Set<String>> rpm = populateRegionPackageMap(regionsFile);

        String toglobal = context.getProperty(APIREGIONS_JOINGLOBAL);
        if (toglobal != null) {
            joinRegionsWithGlobal(toglobal, rpm);
            regProps.put(APIREGIONS_JOINGLOBAL, toglobal);
        }

        String defRegProp = context.getProperty(DEFAULT_REGIONS);
        if (defRegProp != null) {
            Set<String> defRegs = new HashSet<>();
            for (String region : Arrays.asList(defRegProp.split(","))) {
                if (region.length() > 0) {
                    defRegs.add(region);
                }
            }
            defaultRegions = Collections.unmodifiableSet(defRegs);
            if (defaultRegions.size() > 0) {
                regProps.put(DEFAULT_REGIONS, defaultRegions.toString());
            }
        } else {
            defaultRegions = Collections.emptySet();
        }

        // Make all maps and their contents unmodifiable
        bsnVerMap = unmodifiableMapToList(bvm);
        bundleFeatureMap = unmodifiableMapToSet(bfm);
        featureRegionMap = unmodifiableMapToSet(frm);
        regionPackageMap = unmodifiableMapToSet(rpm);
    }

    private static <K,V> Map<K, List<V>> unmodifiableMapToList(Map<K, List<V>> m) {
        for (Map.Entry<K, List<V>> entry : m.entrySet()) {
            m.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(m);
    }

    private static <K,V> Map<K, Set<V>> unmodifiableMapToSet(Map<K, Set<V>> m) {
        for (Map.Entry<K, Set<V>> entry : m.entrySet()) {
            m.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
        return Collections.unmodifiableMap(m);
    }

    private void joinRegionsWithGlobal(String toglobal, Map<String, Set<String>> rpm) {
        for (String region : toglobal.split(",")) {
            Set<String> packages = rpm.get(region);
            if (packages == null)
                continue;

            addValuesToMap(rpm, GLOBAL_REGION, packages);
            rpm.remove(region);
        }
    }

    private static Map<Map.Entry<String, Version>, List<String>> populateBSNVerMap(URI idbsnverFile) throws IOException {
        Map<Map.Entry<String, Version>, List<String>> m = new HashMap<>();

        Properties p = new Properties();
        try (InputStream is = idbsnverFile.toURL().openStream()) {
            p.load(is);
        }

        for (String n : p.stringPropertyNames()) {
            String[] bsnver = p.getProperty(n).split("~");
            addBsnVerArtifact(m, bsnver[0], bsnver[1], n);
        }

        return m;
    }

    private static void addBsnVerArtifact(
            Map<Map.Entry<String, Version>, List<String>> bsnVerMap,
            String bundleSymbolicName, String bundleVersion,
            String artifactId) {
        Version version = Version.valueOf(bundleVersion);
        Map.Entry<String, Version> bsnVer = new AbstractMap.SimpleEntry<>(bundleSymbolicName, version);
        List<String> l = bsnVerMap.get(bsnVer);
        if (l == null) {
            l = new ArrayList<>();
            bsnVerMap.put(bsnVer, l);
        }
        l.add(artifactId);
    }

    private static Map<String, Set<String>> populateBundleFeatureMap(URI bundlesFile) throws IOException {
        return loadMap(bundlesFile);
    }

    private static Map<String, Set<String>> populateFeatureRegionMap(URI featuresFile) throws IOException {
        return loadMap(featuresFile);
    }

    private static Map<String, Set<String>> populateRegionPackageMap(URI regionsFile) throws IOException {
        return loadMap(regionsFile);
    }

    private static Map<String, Set<String>> loadMap(URI propsFile) throws IOException {
        Map<String, Set<String>> m = new HashMap<>();

        Properties p = new Properties();
        try (InputStream is = propsFile.toURL().openStream()) {
            p.load(is);
        }

        for (String n : p.stringPropertyNames()) {
            String[] features = p.getProperty(n).split(",");
            addValuesToMap(m, n, features);
        }

        return m;
    }

    private static void addValuesToMap(Map<String, Set<String>> map, String key, String ... values) {
        addValuesToMap(map, key, Arrays.asList(values));

    }
    private static void addValuesToMap(Map<String, Set<String>> map, String key, Collection<String> values) {
        Set<String> bf = map.get(key);
        if (bf == null) {
            bf = new LinkedHashSet<>(); // It's important that the insertion order is maintained.
            map.put(key, bf);
        }
        bf.addAll(values);
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
            throw new IOException("API Region Enforcement enabled, but no configuration found to find "
                    + "region definition resource: " + name);

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
        return new ResolverHookImpl(bsnVerMap, bundleFeatureMap, featureRegionMap, regionPackageMap, defaultRegions);
    }
}
