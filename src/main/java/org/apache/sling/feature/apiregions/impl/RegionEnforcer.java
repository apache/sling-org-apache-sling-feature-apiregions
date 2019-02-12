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
    static final String PROPERTIES_RESOURCE_PREFIX = "sling.feature.apiregions.resource.";
    static final String PROPERTIES_FILE_LOCATION = "sling.feature.apiregions.location";
    static final String SYNTHESIZED_BUNDLES_KEY = "sling.feature.apiregions.bundles";
    static final String SYNTHESIZED_FEATURE = "org.apache.sling:org.apache.sling.feature.synthesized:0.0.0-SNAPSHOT";

    static final String IDBSNVER_FILENAME = "idbsnver.properties";
    static final String BUNDLE_FEATURE_FILENAME = "bundles.properties";
    static final String FEATURE_REGION_FILENAME = "features.properties";
    static final String REGION_PACKAGE_FILENAME = "regions.properties";

    static final Logger LOG = Logger.getLogger(ResolverHookImpl.class.getName());

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

        loadRegionsFromProperties(context, bsnVerMap, bundleFeatureMap, featureRegionMap);
        // TODO fix all collections
    }

    private static void loadRegionsFromProperties(BundleContext context,
            Map<Entry<String, Version>, List<String>> bsnVerMap,
            Map<String, Set<String>> bundleFeatureMap,
            Map<String, Set<String>> featureRegionMap) {
        String prop = context.getProperty(SYNTHESIZED_BUNDLES_KEY);
        if (prop == null)
            return;

        for (String bundle : prop.split(",")) {
            String[] bundleinfo = bundle.split("=");
            if (bundleinfo.length != 2) {
                LOG.severe("Incorrect bundle info '" + bundle + "' in " + prop);
                continue;
            }

            String bsnver = bundleinfo[0];
            String info = bundleinfo[1];

            String[] bsnver1 = bsnver.split(":");
            if (bsnver1.length != 2) {
                LOG.severe("Incorrect bsn and version '" + bsnver + "' in " + prop);
                continue;
            }

            String bsn = bsnver1[0];
            String ver = bsnver1[1];

            String[] aidregion = info.split(";");
            if (aidregion.length != 2) {
                LOG.severe("Incorrect artifact and region '" + aidregion + "' in " + prop);
                continue;
            }

            String aid = aidregion[0];
            String region = aidregion[1];

            addBsnVerArtifact(bsnVerMap, bsn, ver, aid);
            addValuesToMap(bundleFeatureMap, aid, SYNTHESIZED_FEATURE);
            addValuesToMap(featureRegionMap, SYNTHESIZED_FEATURE, region);

            LOG.info("Added bundle " +  bsnver + " as " + aid + " to feature " + region);
        }
    }

    private static Map<Map.Entry<String, Version>, List<String>> populateBSNVerMap(URI idbsnverFile) throws IOException {
        if (idbsnverFile == null) {
            return new HashMap<>();
        }

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
        if (propsFile == null) {
            return new HashMap<>();
        }
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
        Set<String> bf = map.get(key);
        if (bf == null) {
            bf = new LinkedHashSet<>(); // It's important that the insertion order is maintained.
            map.put(key, bf);
        }
        bf.addAll(Arrays.asList(values));
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
