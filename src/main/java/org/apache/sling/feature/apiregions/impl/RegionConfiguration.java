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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

class RegionConfiguration {
    private static final String BUNDLE_LOCATION_TO_FEATURE_FILE = "bundleLocationToFeature.properties";

    volatile Map<Map.Entry<String, Version>, List<String>> bsnVerMap;
    volatile Map<String, Set<String>> bundleFeatureMap;
    volatile Map<String, Set<String>> featureRegionMap;
    volatile Map<String, Set<String>> regionPackageMap;

    final Set<String> defaultRegions;

    private final BundleContext bundleContext;
    private final Dictionary<String, Object> regProps = new Hashtable<>();
    private final Map<String, Dictionary<String, Object>> factoryConfigs = new ConcurrentHashMap<>();

    private final Map<Map.Entry<String, Version>, List<String>> baseBsnVerMap;
    private final Map<String, Set<String>> baseBundleFeatureMap;
    private final Map<String, Set<String>> baseFeatureRegionMap;
    private final Map<String, Set<String>> baseRegionPackageMap;

    // This field stores the association between bundle location and features.
    // It is populated dynamically as bundles are getting resolved. If a feature
    // cannot be found for a location, it is looked up through the other maps in
    // this class.
    private final ConcurrentMap<String, Set<String>> bundleLocationFeatureMap =
            new ConcurrentHashMap<>();

    private final String toGlobalConfig;

    RegionConfiguration(Map<Entry<String, Version>, List<String>> bsnVerMap, Map<String, Set<String>> bundleFeatureMap,
                        Map<String, Set<String>> featureRegionMap, Map<String, Set<String>> regionPackageMap, Set<String> defaultRegions) {
        this.bundleContext = null;
        this.defaultRegions = defaultRegions;

        this.baseBsnVerMap = new HashMap<>(bsnVerMap);
        this.baseBundleFeatureMap = new HashMap<>(bundleFeatureMap);
        this.baseFeatureRegionMap = new HashMap<>(featureRegionMap);
        this.baseRegionPackageMap = new HashMap<>(regionPackageMap);

        this.toGlobalConfig = null;

        updateConfiguration();
    }

    RegionConfiguration(final BundleContext context)
            throws IOException, URISyntaxException {
        this.bundleContext = context;

        URI idbsnverFile = getDataFileURI(context, RegionConstants.IDBSNVER_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(RegionConstants.IDBSNVER_FILENAME, idbsnverFile.toString());
        Map<Entry<String, Version>, List<String>> bvm = populateBSNVerMap(idbsnverFile);

        URI bundlesFile = getDataFileURI(context, RegionConstants.BUNDLE_FEATURE_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(RegionConstants.BUNDLE_FEATURE_FILENAME, bundlesFile.toString());
        Map<String, Set<String>> bfm = populateBundleFeatureMap(bundlesFile);

        URI featuresFile = getDataFileURI(context, RegionConstants.FEATURE_REGION_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(RegionConstants.FEATURE_REGION_FILENAME, featuresFile.toString());
        Map<String, Set<String>> frm = populateFeatureRegionMap(featuresFile);

        URI regionsFile = getDataFileURI(context, RegionConstants.REGION_PACKAGE_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(RegionConstants.REGION_PACKAGE_FILENAME, regionsFile.toString());
        Map<String, Set<String>> rpm = populateRegionPackageMap(regionsFile);

        // store base configuration
        this.baseBsnVerMap = bvm;
        this.baseBundleFeatureMap = bfm;
        this.baseFeatureRegionMap = frm;
        this.baseRegionPackageMap = rpm;

        this.toGlobalConfig = context.getProperty(RegionConstants.APIREGIONS_JOINGLOBAL);
        if ( this.toGlobalConfig != null ) {
            regProps.put(RegionConstants.APIREGIONS_JOINGLOBAL, this.toGlobalConfig);
        }

        String defRegProp = context.getProperty(RegionConstants.DEFAULT_REGIONS);
        if (defRegProp != null) {
            Set<String> defRegs = new HashSet<>();
            for (String region : Arrays.asList(defRegProp.split(","))) {
                if (region.length() > 0) {
                    defRegs.add(region);
                }
            }
            defaultRegions = Collections.unmodifiableSet(defRegs);
            if (defaultRegions.size() > 0) {
                regProps.put(RegionConstants.DEFAULT_REGIONS, defaultRegions.toString());
            }
        } else {
            defaultRegions = Collections.emptySet();
        }

        loadLocationToFeatureMap(context);
        updateConfiguration();
    }

    private void loadLocationToFeatureMap(BundleContext context) {
        File file = context.getBundle().getDataFile(BUNDLE_LOCATION_TO_FEATURE_FILE);

        if (file != null && file.exists()) {
            Properties p = new Properties();
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                p.load(is);
            } catch (IOException e) {
                Activator.LOG.log(Level.WARNING, "Unable to load " + BUNDLE_LOCATION_TO_FEATURE_FILE, e);
            }

            for (String k : p.stringPropertyNames()) {
                bundleLocationFeatureMap.put(k, stringToSetOfString(p.getProperty(k)));
            }
        }
    }

    void storePersistedConfiguration(BundleContext context) {
        File file = context.getBundle().getDataFile(BUNDLE_LOCATION_TO_FEATURE_FILE);
        if (file == null) {
            Activator.LOG.warning("Cannot store " + BUNDLE_LOCATION_TO_FEATURE_FILE
                    + " Persistence not supported by this framework.");
            return;
        }

        Properties p = new Properties();
        for (Map.Entry<String, Set<String>> entry : bundleLocationFeatureMap.entrySet()) {
            p.setProperty(entry.getKey(),
                    entry.getValue().stream().collect(Collectors.joining(",")));
        }
        if (p.size() > 0) {
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
                p.store(os, "Bundle Location to Feature Map");
            } catch (IOException e) {
                Activator.LOG.log(Level.WARNING, "Unable to store " + BUNDLE_LOCATION_TO_FEATURE_FILE, e);
            }
        }
    }

    private Set<String> stringToSetOfString(String prop) {
        Set<String> res = new HashSet<>();

        for (String s : prop.split(",")) {
            String ts = s.trim();
            if (ts.length() > 0) {
                res.add(ts);
            }
        }

        return res;
    }

    private synchronized void updateConfiguration() {
        final Map<Entry<String, Version>, List<String>> bvm = cloneMapOfLists(this.baseBsnVerMap);
        final Map<String, Set<String>> bfm = cloneMapOfSets(this.baseBundleFeatureMap);
        final Map<String, Set<String>> frm = cloneMapOfSets(this.baseFeatureRegionMap);
        final Map<String, Set<String>> rpm = cloneMapOfSets(this.baseRegionPackageMap);

        // apply configurations
        for(final Dictionary<String, Object> props : this.factoryConfigs.values()) {
            // bundle id to bsnver
            Object valObj = props.get(RegionConstants.PROP_idbsnver);
            if ( valObj != null ) {
                for(final String val : convert(valObj)) {
                    final String[] parts = val.split("=");
                    final String n = parts[0];
                    final String[] bsnver = parts[1].split("~");
                    String bsn = bsnver[0];
                    String bver = bsnver[1];
                    addBsnVerArtifact(bvm, bsn, bver, n);

                    // remove the locations for the updated bundles from the location to feature cache
                    for (Iterator<String> it = bundleLocationFeatureMap.keySet().iterator(); it.hasNext(); ) {
                        String loc = it.next();
                        Bundle b = bundleContext.getBundle(loc);
                        if (b != null && bsn.equals(b.getSymbolicName()) && bver.equals(b.getVersion().toString())) {
                            it.remove();
                            break;
                        }
                    }
                }
            }

            // bundle id to features
            valObj = props.get(RegionConstants.PROP_bundleFeatures);
            if ( valObj != null ) {
                List<String> updatedBundleIDs = handleMapConfig(valObj, bfm);

                // remove the locations for the updated bundles from the location to feature cache
                for (String updatedBundleID : updatedBundleIDs) {
                    for (Iterator<String> it = bundleLocationFeatureMap.keySet().iterator(); it.hasNext(); ) {
                        String loc = it.next();
                        Bundle b = bundleContext.getBundle(loc);
                        if (b != null) {
                            List<String> bundleIDs = bvm.get(new AbstractMap.SimpleEntry<>(b.getSymbolicName(), b.getVersion()));
                            if (bundleIDs != null && bundleIDs.contains(updatedBundleID)) {
                                it.remove();
                                break;
                            }
                        }
                    }
                }
            }

            // feature id to regions
            valObj = props.get(RegionConstants.PROP_featureRegions);
            if ( valObj != null ) {
                handleMapConfig(valObj, frm);
            }

            // region to packages
            valObj = props.get(RegionConstants.PROP_regionPackage);
            if ( valObj != null ) {
                handleMapConfig(valObj, rpm);
            }
        }

        // join regions
        if (this.toGlobalConfig != null) {
            joinRegionsWithGlobal(this.toGlobalConfig, rpm);
        }

        // Make all maps and their contents unmodifiable
        bsnVerMap = unmodifiableMapToList(bvm);
        bundleFeatureMap = unmodifiableMapToSet(bfm);
        featureRegionMap = unmodifiableMapToSet(frm);
        regionPackageMap = unmodifiableMapToSet(rpm);
    }

    private List<String> handleMapConfig(Object valObj, Map<String, Set<String>> map) {
        List<String> keys = new ArrayList<>();

        for(final String val : convert(valObj)) {
            final String[] parts = val.split("=");
            final String n = parts[0];
            final String[] features = parts[1].split(",");
            addValuesToMap(map, n, Arrays.asList(features));
            keys.add(n);
        }
        return keys;
    }

    private static <K,V> Map<K, List<V>> cloneMapOfLists(Map<K, List<V>> m) {
        final Map<K, List<V>> newMap = new HashMap<>();
        for (Map.Entry<K, List<V>> entry : m.entrySet()) {
            newMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return newMap;
    }

    private static <K,V> Map<K, Set<V>> cloneMapOfSets(Map<K, Set<V>> m) {
        final Map<K, Set<V>> newMap = new HashMap<>();
        for (Map.Entry<K, Set<V>> entry : m.entrySet()) {
            newMap.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        return newMap;
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

            addValuesToMap(rpm, RegionConstants.GLOBAL_REGION, packages);
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
        if (!l.contains(artifactId))
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
            String[] values = p.getProperty(n).split(",");
            addValuesToMap(m, n, Arrays.asList(values));
        }

        return m;
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
        String fn = ctx.getProperty(RegionConstants.PROPERTIES_RESOURCE_PREFIX + name);
        if (fn == null) {
            String loc = ctx.getProperty(RegionConstants.PROPERTIES_FILE_LOCATION);
            if (loc != null) {
                fn = loc + "/" + name;
            }
        }

        if (fn == null)
            throw new IOException("API Region Enforcement enabled, but no configuration found to find "
                    + "region definition resource: " + name);

        if (fn.contains(":")) {
            if (fn.startsWith(RegionConstants.CLASSLOADER_PSEUDO_PROTOCOL)) {
                // It's using the 'classloader:' protocol looks up the location from the classloader
                String loc = fn.substring(RegionConstants.CLASSLOADER_PSEUDO_PROTOCOL.length());
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

    public Map<Map.Entry<String, Version>, List<String>> getBsnVerMap() {
        return bsnVerMap;
    }

    /**
     * Obtain a mutable concurrent map that contains an association between
     * bundle location and features. This map is persisted in bundle private
     * storage. Initially this map is empty but as more bundles get resolved
     * this map is gradually filled in.
     * @return The bundle location to features map.
     */
    public ConcurrentMap<String, Set<String>> getBundleLocationFeatureMap() {
        return bundleLocationFeatureMap;
    }

    public Map<String, Set<String>> getBundleFeatureMap() {
        return bundleFeatureMap;
    }

    public Map<String, Set<String>> getFeatureRegionMap() {
        return featureRegionMap;
    }

    public Map<String, Set<String>> getRegionPackageMap() {
        return regionPackageMap;
    }

    public Set<String> getDefaultRegions() {
        return defaultRegions;
    }

    public Dictionary<String, Object> getRegistrationProperties() {
        return regProps;
    }

    private String[] convert(final Object obj) {
        if ( obj instanceof String[]) {
            return (String[])obj;
        }
        return new String[] {obj.toString()};
    }

    /**
     * Add a new factory configuration
     * @param pid The pid
     * @param props The properties
     */
    public void setConfig(final String pid, final Dictionary<String, Object> props) {
        this.factoryConfigs.put(pid, props);
        updateConfiguration();
    }

    /**
     * Remove a factory configuration
     * @param pid The pid
     */
    public void removeConfig(final String pid) {
        final Dictionary<String, Object> props = this.factoryConfigs.remove(pid);
        if ( props != null ) {
            updateConfiguration();
        }
    }
}
