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

abstract class RegionConstants {

    static final String GLOBAL_REGION = "global";

    static final String CLASSLOADER_PSEUDO_PROTOCOL = "classloader://";
    static final String APIREGIONS_JOINGLOBAL = "sling.feature.apiregions.joinglobal";
    static final String DEFAULT_REGIONS = "sling.feature.apiregions.default";
    static final String PROPERTIES_RESOURCE_PREFIX = "sling.feature.apiregions.resource.";
    static final String PROPERTIES_FILE_LOCATION = "sling.feature.apiregions.location";

    static final String IDBSNVER_FILENAME = "idbsnver.properties";
    static final String BUNDLE_FEATURE_FILENAME = "bundles.properties";
    static final String FEATURE_REGION_FILENAME = "features.properties";
    static final String REGION_PACKAGE_FILENAME = "regions.properties";

    static final String PROP_idbsnver = "mapping.bundleid.bsnver";
    static final String PROP_bundleFeatures = "mapping.bundleid.features";
    static final String PROP_featureRegions = "mapping.featureid.regions";
    static final String PROP_regionPackage = "mapping.region.packages";
}
