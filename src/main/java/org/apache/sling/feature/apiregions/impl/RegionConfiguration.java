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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

class RegionConfiguration {
    private static final String BUNDLE_LOCATION_TO_FEATURE_FILE = "bundleLocationToFeature.properties";
    private static final String REGION_ORDER = "__region.order__";

    volatile Map<Map.Entry<String, Version>, List<String>> bsnVerMap;
    volatile Map<String, Set<String>> bundleFeatureMap;
    volatile Map<String, List<String>> featureRegionMap;
    volatile Map<String, Set<String>> regionPackageMap;

    final Set<String> defaultRegions;

    private final Dictionary<String, Object> regProps = new Hashtable<>();
    private final Map<String, Dictionary<String, Object>> factoryConfigs = new ConcurrentHashMap<>();

    private final Map<Map.Entry<String, Version>, List<String>> baseBsnVerMap;
    private final Map<String, Set<String>> baseBundleFeatureMap;
    private final Map<String, List<String>> baseFeatureRegionMap;
    private final Map<String, Set<String>> baseRegionPackageMap;
    private final List<String> globalRegionOrder;

    // This field stores the association between bundle location and the configuration
    // to be used. The configuration is based on bsn+version. If the bundle is updated
    // the original bsn+version associated with the location still needs to be used.
    // It is populated dynamically as bundles are getting resolved.
    private final ConcurrentMap<String, Map.Entry<String, Version>> bundleLocationConfigMap =
            new ConcurrentHashMap<>();

    private final String toGlobalConfig;

    RegionConfiguration(Map<Entry<String, Version>, List<String>> bsnVerMap, Map<String, Set<String>> bundleFeatureMap,
                        Map<String, List<String>> featureRegionMap, Map<String, Set<String>> regionPackageMap, Set<String> defaultRegions) {
        this.defaultRegions = defaultRegions;

        this.baseBsnVerMap = new HashMap<>(bsnVerMap);
        this.baseBundleFeatureMap = new HashMap<>(bundleFeatureMap);
        this.baseFeatureRegionMap = new HashMap<>(featureRegionMap);
        this.baseRegionPackageMap = new HashMap<>(regionPackageMap);
        this.globalRegionOrder = new ArrayList<>(this.baseFeatureRegionMap.getOrDefault(REGION_ORDER, Collections.emptyList()));
        this.baseFeatureRegionMap.remove(REGION_ORDER);

        this.toGlobalConfig = null;

        updateConfiguration();
    }

    RegionConfiguration(final BundleContext context)
            throws IOException, URISyntaxException {
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
        Map<String, List<String>> frm = populateFeatureRegionMap(featuresFile);

        URI regionsFile = getDataFileURI(context, RegionConstants.REGION_PACKAGE_FILENAME);
        // Register the location as a service property for diagnostic purposes
        regProps.put(RegionConstants.REGION_PACKAGE_FILENAME, regionsFile.toString());
        Map<String, Set<String>> rpm = populateRegionPackageMap(regionsFile);

        // store base configuration
        this.baseBsnVerMap = bvm;
        this.baseBundleFeatureMap = bfm;
        this.baseFeatureRegionMap = frm;
        this.baseRegionPackageMap = rpm;
        this.globalRegionOrder = new ArrayList<>(this.baseFeatureRegionMap.getOrDefault(REGION_ORDER, Collections.emptyList()));
        this.baseFeatureRegionMap.remove(REGION_ORDER);

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

        loadLocationToConfigMap(context);
        updateConfiguration();
    }

    private void loadLocationToConfigMap(BundleContext context) {
        File file = context.getBundle().getDataFile(BUNDLE_LOCATION_TO_FEATURE_FILE);

        if (file != null && file.exists()) {
            Properties p = new Properties();
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                p.load(is);
            } catch (IOException e) {
                Activator.LOG.log(Level.WARNING, "Unable to load " + BUNDLE_LOCATION_TO_FEATURE_FILE, e);
            }

            for (String k : p.stringPropertyNames()) {
                Map.Entry<String, Version> bsnver = parseBSNVer(p.getProperty(k));
                if (bsnver != null) {
                    bundleLocationConfigMap.put(k, bsnver);
                }
            }
        }
    }

    private Map.Entry<String, Version> parseBSNVer(String val) {
        String[] bsnver = val.split("~");
        if (bsnver.length == 2) {
            String bsn = bsnver[0].trim();
            Version ver = null;
            try {
                ver = Version.parseVersion(bsnver[1].trim());
            } catch (Exception e) {
                Activator.LOG.log(Level.WARNING, "Problem parsing " + BUNDLE_LOCATION_TO_FEATURE_FILE, e);
            }

            if (ver != null) {
                return new AbstractMap.SimpleEntry<String,Version>(bsn, ver);
            }
        }
        return null;
    }

    void storeLocationToConfigMap(BundleContext context) {
        File file = context.getBundle().getDataFile(BUNDLE_LOCATION_TO_FEATURE_FILE);
        if (file == null) {
            Activator.LOG.warning("Cannot store " + BUNDLE_LOCATION_TO_FEATURE_FILE
                    + " Persistence not supported by this framework.");
            return;
        }

        Properties p = new Properties();
        for (Map.Entry<String, Map.Entry<String, Version>> entry : bundleLocationConfigMap.entrySet()) {
            p.setProperty(entry.getKey(), entry.getValue().getKey() + "~" + entry.getValue().getValue());
        }
        if (p.size() > 0) {
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
                p.store(os, "Bundle Location to Feature Map");
            } catch (IOException e) {
                Activator.LOG.log(Level.WARNING, "Unable to store " + BUNDLE_LOCATION_TO_FEATURE_FILE, e);
            }
        }
    }

    private synchronized void updateConfiguration() {
        final Map<Entry<String, Version>, List<String>> bvm = cloneMapOfLists(this.baseBsnVerMap);
        final Map<String, Set<String>> bfm = cloneMapOfSets(this.baseBundleFeatureMap);
        final Map<String, List<String>> frm = cloneMapOfLists(this.baseFeatureRegionMap);
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
                }
            }

            // bundle id to features
            valObj = props.get(RegionConstants.PROP_bundleFeatures);
            if ( valObj != null ) {
                handleMapConfig(valObj, bfm, HashSet::new);
            }

            // feature id to regions
            valObj = props.get(RegionConstants.PROP_featureRegions);
            if ( valObj != null ) {
                handleMapConfig(valObj, frm, ArrayList::new);
            }

            // region to packages
            valObj = props.get(RegionConstants.PROP_regionPackage);
            if ( valObj != null ) {
                handleMapConfig(valObj, rpm, HashSet::new);
            }
        }

        // join regions
        if (this.toGlobalConfig != null) {
            joinRegionsWithGlobal(this.toGlobalConfig, rpm);
        }

        // Make all maps and their contents unmodifiable
        bsnVerMap = unmodifiableMapToList(bvm);
        bundleFeatureMap = unmodifiableMapToSet(bfm);
        featureRegionMap = unmodifiableMapToList(frm);
        regionPackageMap = unmodifiableMapToSet(rpm);
    }

    private <T extends Collection<String>> void handleMapConfig(Object valObj, Map<String, T> map, Supplier<T> constructor) {
        for(final String val : convert(valObj)) {
            final String[] parts = val.split("=");
            final String n = parts[0];
            final List<String> features = parts.length == 1 ? Collections.emptyList() : Arrays.asList(parts[1].split(","));
            addValuesToMap(map, n, features, constructor);
        }
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
            newMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
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

            addValuesToMap(rpm, RegionConstants.GLOBAL_REGION, packages, HashSet::new);
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
        return loadMap(bundlesFile, HashSet::new);
    }

    private static Map<String, List<String>> populateFeatureRegionMap(URI featuresFile) throws IOException {
        return loadMap(featuresFile, ArrayList::new);
    }

    private static Map<String, Set<String>> populateRegionPackageMap(URI regionsFile) throws IOException {
        return loadMap(regionsFile, HashSet::new);
    }

    private static <T extends Collection<String>> Map<String, T> loadMap(URI propsFile, Supplier<T> constructor) throws IOException {
        Map<String, T> m = new HashMap<>();

        Properties p = new Properties();
        try (InputStream is = propsFile.toURL().openStream()) {
            p.load(is);
        }

        for (String n : p.stringPropertyNames()) {
            String[] values = p.getProperty(n).split(",");
            addValuesToMap(m, n, Arrays.asList(values), constructor);
        }

        return m;
    }

    private static <T extends Collection<String>> void addValuesToMap(Map<String, T> map, String key, Collection<String> values, Supplier<T> constructor) {
        T bf = map.get(key);
        if (bf == null) {
            bf = constructor.get();
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
     * bundle location and their configuration, which is the original
     * Symbolic Name and Version. This map is persisted in bundle private
     * storage. Initially this map is empty but as more bundles get resolved
     * this map is gradually filled in. <p>
     *
     * This map is used to find the API Regions information for a bundle even
     * if the bundle is updated at some point. After the update the location
     * will still be the same, so the original configuration can be found by
     * looking up the original bsn+version associated with the location. That's
     * what this method enables.
     * @return The bundle location to bsnver map.
     */
    public ConcurrentMap<String, Map.Entry<String, Version>> getBundleLocationConfigMap() {
        return bundleLocationConfigMap;
    }

    public Map<String, Set<String>> getBundleFeatureMap() {
        return bundleFeatureMap;
    }

    public Map<String, List<String>> getFeatureRegionMap() {
        return featureRegionMap;
    }

    public Map<String, Set<String>> getRegionPackageMap() {
        return regionPackageMap;
    }

    public Set<String> getDefaultRegions() {
        return defaultRegions;
    }

    public List<String> getGlobalRegionOrder() {
        return globalRegionOrder;
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
