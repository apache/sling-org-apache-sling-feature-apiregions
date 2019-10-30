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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class PlatformIsolationHook implements ResolverHook {

    protected static final String OSGI_WIRING_PACKAGE_NAMESPACE = "osgi.wiring.package";

    private final Map<String, Version> bsnVerMap;

    PlatformIsolationHook(Map<String, Version> bsnVerMap) {
        this.bsnVerMap = bsnVerMap;
    }

    @Override
    public void filterResolvable(Collection<BundleRevision> candidates) {
        // not used in this version
    }

    @Override
    public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
        // not used in this version
    }

    @Override
    public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
        for (Iterator<BundleCapability> it = candidates.iterator(); it.hasNext();) {
            BundleCapability candidate = it.next();

            BundleRevision rev = candidate.getRevision();

            // bundle is allowed to wire to itself
            if (requirement.getRevision().getBundle().getBundleId() == rev.getBundle().getBundleId()) {
                continue;
            }

            // is it a restricted bundle?
            if (filter(requirement, candidate)) {
                it.remove();
                // LOG.info("Prevented {} from resolving to {}", requirement, candidate);
            }
        }
    }

    private boolean filter(BundleRequirement requirement, BundleCapability candidate) {
        String requirementNamespace = requirement.getNamespace();
        String candidateNamespace = candidate.getNamespace();
        if (!OSGI_WIRING_PACKAGE_NAMESPACE.equals(requirementNamespace)
                || !requirementNamespace.equals(candidateNamespace)) {
            return false; // checking wiring packages only
        }

        BundleRevision candidateRevision = candidate.getRevision();
        String candidateSymbolicName = candidateRevision.getSymbolicName();
        Version candidateVersion = candidateRevision.getVersion();

        Version expectedVersion = bsnVerMap.get(candidateSymbolicName);

        return expectedVersion != null && expectedVersion.equals(candidateVersion);
    }

    @Override
    public void end() {
        // not used in this version
    }

}
