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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleRevision;

public class PlatformIsolationEnforcer extends AbstractResolverHookFactory<String, Version> {

    final Map<String, Version> bsnVerMap;

    PlatformIsolationEnforcer(BundleContext context) throws IOException, URISyntaxException {
        Map<String, Version> bsnVerMap = readBsnVerMap(context);
        this.bsnVerMap = Collections.unmodifiableMap(bsnVerMap);
    }

    @Override
    protected void addBsnVerArtifact(Map<String, Version> bsnVerMap,
                                     String artifactId,
                                     String bundleSymbolicName,
                                     Version bundleVersion) {
        bsnVerMap.put(bundleSymbolicName, bundleVersion);
    }

    @Override
    public ResolverHook begin(Collection<BundleRevision> triggers) {
        return new PlatformIsolationHook(bsnVerMap);
    }

}
