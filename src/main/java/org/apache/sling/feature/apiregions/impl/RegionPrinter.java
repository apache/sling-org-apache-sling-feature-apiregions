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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

@SuppressWarnings("java:S3457") // adding platform specific endings makes the process harder and \n should be
                                // interpreted correctly across all
public class RegionPrinter {

    static final String HEADLINE = "Sling Feature API Regions";
    static final String PATH = "apiregions";
    private RegionConfiguration config;
    private BundleContext context;

    public RegionPrinter(BundleContext context, RegionConfiguration config) {
        this.context = context;
        this.config = config;
    }

    private void renderPackageMappings(PrintWriter pw) {
        Map<String, Set<String>> regionPackageMap = config.getRegionPackageMap();
        regionPackageMap.keySet().stream().sorted().forEach(region -> {
            pw.println(String.format("\n%s:", region));
            regionPackageMap.get(region).stream().sorted().forEach(pkg -> pw.println(" - " + pkg));
        });
    }

    private void renderBundleMappings(PrintWriter pw) {
        Map<String, List<String>> featureRegions = config.getFeatureRegionMap();
        Map<String, Set<String>> bundlesToFeatures = config.getBundleFeatureMap();

        Map<String, Entry<String, Version>> bundleLocations = config.getBundleLocationConfigMap();
        bundlesToFeatures.keySet().stream().sorted().forEach(bundle -> {
            Set<String> regions = new HashSet<>();
            bundlesToFeatures.get(bundle).stream()
                    .forEach(feature -> Optional.ofNullable(featureRegions.get(feature))
                            .ifPresent(regions::addAll));
            String location = Optional.ofNullable(bundleLocations.get(bundle))
                    .map(loc -> loc.getKey() + "v" + loc.getValue().toString()).orElse("null");
            pw.println(String.format(" - %s\n\t - features: %s\n\t - regions: %s\n\t - location: %s", bundle,
                    bundlesToFeatures.get(bundle).stream().collect(Collectors.joining(",")),
                    regions.stream().collect(Collectors.joining(",")), location));
        });
    }

    private void renderHeader(PrintWriter pw, String header) {
        pw.println("\n\n" + header + "\n-------------------\n");
    }

    private void renderProperties(PrintWriter pw) {
        String[] properties = new String[] { Activator.REGIONS_PROPERTY_NAME, RegionConstants.APIREGIONS_JOINGLOBAL,
                RegionConstants.DEFAULT_REGIONS, RegionConstants.PROPERTIES_FILE_LOCATION };
        Arrays.stream(properties).forEach(p -> pw.println(String.format(" - %s=%s", p, context.getProperty(p))));
    }

    /**
     * Print out the region information
     * 
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw) {
        pw.println(HEADLINE + "\n===========================");

        renderHeader(pw, "Default Regions");
        config.getDefaultRegions().stream().forEach(r -> pw.println(" - " + r));
        renderHeader(pw, "Region Order");
        config.getGlobalRegionOrder().stream().forEach(r -> pw.println(" - " + r));
        renderHeader(pw, "Properties");
        renderProperties(pw);
        renderHeader(pw, "Packages per Region");
        renderPackageMappings(pw);
        renderHeader(pw, "Bundle Mappings");
        renderBundleMappings(pw);
    }

}
