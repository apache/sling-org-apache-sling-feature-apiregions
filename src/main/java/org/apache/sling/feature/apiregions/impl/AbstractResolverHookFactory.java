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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

abstract class AbstractResolverHookFactory<K, V> implements ResolverHookFactory {

    static final String CLASSLOADER_PSEUDO_PROTOCOL = "classloader://";

    static final String PROPERTIES_RESOURCE_PREFIX = "sling.feature.apiregions.resource.";

    static final String PROPERTIES_FILE_LOCATION = "sling.feature.apiregions.location";

    static final String IDBSNVER_FILENAME = "idbsnver.properties";

    protected final URI getDataFileURI(BundleContext ctx, String name) throws IOException, URISyntaxException {
        String fn = ctx.getProperty(PROPERTIES_RESOURCE_PREFIX + name);
        if (fn == null) {
            String loc = ctx.getProperty(PROPERTIES_FILE_LOCATION);
            if (loc != null) {
                fn = loc + "/" + name;
            }
        }

        if (fn == null) {
            throw new IOException("API Region Enforcement enabled, but no configuration found to find "
                    + "region definition resource: " + name);
        }

        if (fn.contains(":")) {
            if (fn.startsWith(CLASSLOADER_PSEUDO_PROTOCOL)) {
                // It's using the 'classloader:' protocol looks up the location from the classloader
                String loc = fn.substring(CLASSLOADER_PSEUDO_PROTOCOL.length());
                if (!loc.startsWith("/")) {
                    loc = "/" + loc;
                }
                fn = getClass().getResource(loc).toString();
            }
            // It's already a URL
            return new URI(fn);
        }

        // It's a file location
        return new File(fn).toURI();
    }

    protected final Map<K, V> readBsnVerMap(BundleContext context) throws IOException, URISyntaxException {
        final Map<K, V> bsnVerMap = new HashMap<>();

        URI idbsnverFile = getDataFileURI(context, IDBSNVER_FILENAME);

        Properties properties = new Properties();
        try (InputStream is = idbsnverFile.toURL().openStream()) {
            properties.load(is);
        }

        for (String artifactId : properties.stringPropertyNames()) {
            String value = properties.getProperty(artifactId);
            if (value != null && !value.isEmpty()) { // it shouldn't happen, but...
                int splitIndex = value.indexOf('~');
                if (splitIndex != -1) { // again, it shouldn't happen...
                    String bundleSymbolicName = value.substring(0, splitIndex);
                    String bundleVersion = value.substring(splitIndex + 1);
                    Version version = Version.valueOf(bundleVersion);

                    addBsnVerArtifact(bsnVerMap, artifactId, bundleSymbolicName, version);
                }
            }
        }

        return bsnVerMap;
    }

    protected abstract void addBsnVerArtifact(Map<K, V> bsnVerMap,
                                              String artifactId,
                                              String bundleSymbolicName,
                                              Version bundleVersion);

}
