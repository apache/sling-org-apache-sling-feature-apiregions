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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class PlatformIsolationHookTest {

    @Test
    public void doNotFilterRequirementWithDifferentNamespace() {
        BundleRequirement requirement = mock(BundleRequirement.class);
        when(requirement.getNamespace()).thenReturn("something.irrilevant");

        // won't be touched
        BundleCapability candidate = newBundleCapability();

        PlatformIsolationHook hook = new PlatformIsolationHook(new HashMap<String, Version>());
        assertFalse(hook.filter(requirement, candidate));
    }

    @Test
    public void doNotFilterCandidateWithDifferentNamespace() {
        BundleRequirement requirement = newRequirement();

        BundleCapability candidate = mock(BundleCapability.class);
        when(candidate.getNamespace()).thenReturn("something.irrilevant");

        PlatformIsolationHook hook = new PlatformIsolationHook(new HashMap<String, Version>());
        assertFalse(hook.filter(requirement, candidate));
    }

    @Test
    public void letUnownBundleBeResolved() {
        Map<String, Version> bsnVerMap = new HashMap<>();
        bsnVerMap.put("b2", new Version(1, 2, 3));
        bsnVerMap.put("b1", new Version(1, 0, 0));
        PlatformIsolationHook hook = new PlatformIsolationHook(bsnVerMap);

        BundleRequirement requirement = newRequirement();

        BundleCapability candidate = newBundleCapability();
        BundleRevision revision = newBundleRevision("asd", "1.0.0.CUSTOM");
        when(candidate.getRevision()).thenReturn(revision);

        assertFalse(hook.filter(requirement, candidate));
    }

    @Test
    public void letKnownBundleBeResolved() {
        Map<String, Version> bsnVerMap = new HashMap<>();
        bsnVerMap.put("b2", new Version(1, 2, 3));
        bsnVerMap.put("b1", new Version(1, 0, 0));
        PlatformIsolationHook hook = new PlatformIsolationHook(bsnVerMap);

        BundleRequirement requirement = newRequirement();

        BundleCapability candidate = newBundleCapability();
        BundleRevision revision = newBundleRevision("b1", "1.0.0");
        when(candidate.getRevision()).thenReturn(revision);

        assertFalse(hook.filter(requirement, candidate));
    }

    @Test
    public void upatedBundleFilteredOut() {
        Map<String, Version> bsnVerMap = new HashMap<>();
        bsnVerMap.put("b2", new Version(1, 2, 3));
        bsnVerMap.put("b1", new Version(1, 0, 0));
        PlatformIsolationHook hook = new PlatformIsolationHook(bsnVerMap);

        BundleRequirement requirement = newRequirement();

        BundleCapability candidate = newBundleCapability();
        BundleRevision revision = newBundleRevision("b1", "1.0.0.CUSTOM");
        when(candidate.getRevision()).thenReturn(revision);

        assertTrue(hook.filter(requirement, candidate));
    }

    private static BundleRequirement newRequirement() {
        BundleRequirement requirement = mock(BundleRequirement.class);
        when(requirement.getNamespace()).thenReturn(PlatformIsolationHook.OSGI_WIRING_PACKAGE_NAMESPACE);
        return requirement;
    }

    private static BundleCapability newBundleCapability() {
        BundleCapability candidate = mock(BundleCapability.class);
        when(candidate.getNamespace()).thenReturn(PlatformIsolationHook.OSGI_WIRING_PACKAGE_NAMESPACE);
        return candidate;
    }

    private static BundleRevision newBundleRevision(String symbolicName, String version) {
        BundleRevision revision = mock(BundleRevision.class);
        when(revision.getSymbolicName()).thenReturn(symbolicName);
        when(revision.getVersion()).thenReturn(Version.valueOf(version));
        return revision;
    }

}
