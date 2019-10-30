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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleRevision;

public class PlatformIsolationEnforcer extends AbstractResolverHookFactory {

    final Map<String, Version> bsnVerMap;

    PlatformIsolationEnforcer(BundleContext context) throws IOException, URISyntaxException {
        bsnVerMap = readBsnVerMap(context);
    }

    private Map<String, Version> readBsnVerMap(BundleContext context) throws IOException, URISyntaxException {
        final Map<String, Version> bsnVerMap = new HashMap<>();

        URI idbsnverFile = getDataFileURI(context, IDBSNVER_FILENAME);

        Properties p = new Properties();
        try (InputStream is = idbsnverFile.toURL().openStream()) {
            p.load(is);
        }

        for (Object valueObject : p.values()) {
            if (valueObject != null) { // it shouldn't happen, but...
                String value = valueObject.toString();

                int splitIndex = value.indexOf('~');
                if (splitIndex != -1) { // again, it shouldn't happen...
                    String bundleSymbolicName = value.substring(0, splitIndex);
                    String bundleVersion = value.substring(splitIndex + 1);
                    Version version = Version.valueOf(bundleVersion);

                    bsnVerMap.put(bundleSymbolicName, version);
                }
            }
        }

        return Collections.unmodifiableMap(bsnVerMap);
    }

    @Override
    public ResolverHook begin(Collection<BundleRevision> triggers) {
        // TODO Auto-generated method stub
        return null;
    }

}
