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

import static org.junit.Assert.assertEquals;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class ResolverHookImplTest {
    @Test
    public void testProvidingFeatureHasNoRegionsSoEveryoneCanAccess() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle", new Version(1,0,0)), Collections.singletonList("b1"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b1", Collections.singleton("f1"));
        bfmap.put("b2", Collections.singleton("f2"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f2", Collections.singletonList("r2"));
        frmap.put("__region.order__", Arrays.asList("global", "r2"));

        Map<String, Set<String>> rpmap = new HashMap<>();

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("*")));

        // b2 is in r2, it requires a capability that is provided by b1.
        // b1 is in a feature, but that feature is not in any region so it can provide access.
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.apache.sling.test", "b1", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1));
        rh.filterMatches(req1, candidates1);
        assertEquals(Collections.singletonList(cap1), candidates1);
    }

    @Test
    public void testProvidingBundleHasNoFeatureSoEveryoneCanAccess() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle", new Version(1,0,0)), Collections.singletonList("b1"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b2", Collections.singleton("f2"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f2", Collections.singletonList("r2"));
        frmap.put("__region.order__", Arrays.asList("global", "r2"));

        Map<String, Set<String>> rpmap = new HashMap<>();

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 is in r2, it requires a capability that is provided by b1.
        // b1 is not in any feature so it can provide access.
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.apache.sling.test", "b1", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1));
        rh.filterMatches(req1, candidates1);
        assertEquals(Collections.singletonList(cap1), candidates1);
    }

    @Test
    public void testProvidingFeatureHasNoRegionsSoEveryoneCanAccess3() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle", new Version(1,0,0)), Collections.singletonList("b1"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b1", Collections.singleton("f1"));
        bfmap.put("b2", Collections.singleton("f2"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f1", Collections.emptyList());
        frmap.put("f2", Collections.singletonList("r2"));
        frmap.put("__region.order__", Arrays.asList("global", "r2"));

        Map<String, Set<String>> rpmap = new HashMap<>();

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 is in r2, it requires a capability that is provided by b1.
        // b1 is in a feature that has an empty region set, any region so it can provide access.
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.apache.sling.test", "b1", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1));
        rh.filterMatches(req1, candidates1);
        assertEquals(Collections.singletonList(cap1), candidates1);
    }

    @Test
    public void testProvidingFeatureHasNoExportsSoOtherFeatureCannotAccess() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle", new Version(1,0,0)), Collections.singletonList("b1"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b1", Collections.singleton("f1"));
        bfmap.put("b2", Collections.singleton("f2"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f1", Collections.singletonList("r2"));
        frmap.put("f2", Collections.singletonList("r2"));
        frmap.put("__region.order__", Arrays.asList("global", "r2"));

        Map<String, Set<String>> rpmap = new HashMap<>();

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 is in r2, it requires a capability that is provided by b1.
        // b1 is also in r2 but is not exporting the package in there, so b2 should not see the package.
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.apache.sling.test", "b1", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1));
        rh.filterMatches(req1, candidates1);
        assertEquals("The exported package is not exported in any region, and b1 is in r2 so b2 should not see the package as its feature-private",
                0, candidates1.size());
    }

    @Test
    public void testRegionHasPrecedenceOverGlobal() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.otherfeature", new Version(1,0,0)), Collections.singletonList("b10"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.infeature", new Version(1,0,0)), Collections.singletonList("b11"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal", new Version(1,0,0)), Collections.singletonList("b19"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b10", Collections.singleton("f10"));
        bfmap.put("b11", Collections.singleton("f11"));
        bfmap.put("b2", Collections.singleton("f2"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f10", Arrays.asList("r2", "r3"));
        frmap.put("f11", Arrays.asList("r1", "r2"));
        frmap.put("f2", Collections.singletonList("r1"));
        frmap.put("__region.order__", Arrays.asList("global", "r1", "r2", "r3", "r4"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("r1", Collections.singleton("org.foo.bar"));
        rpmap.put("r2", new HashSet<>(Arrays.asList("xxx", "yyy", "zzz")));
        rpmap.put("r3", new HashSet<>(Arrays.asList("org.foo.bar", "zzz")));

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 needs to resolve to 'org.foo.bar' package. b2 is in region r1.
        // The package is provided by 3 bundles:
        //   b10 provides it but is not in a matching region
        //   b11 provides it and has a matching region
        //   b19 provides it in the global region
        // Only b11 should provide the capability. Even though b19 provides it from the global region, if there is an overlapping
        // specific region then the global region should not be used
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.foo.bar", "b10", bsnvermap);
        BundleCapability cap2 = mockCapability("org.foo.bar", "b11", bsnvermap);
        BundleCapability cap3 = mockCapability("org.foo.bar", "b19", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1, cap2, cap3));
        rh.filterMatches(req1, candidates1);

        assertEquals("Only the capability coming from bundle b11 should be selected, b10 is in a different region and b19 is in global "
                + "which should be excluded as there is a capability in a matching region.",
                Collections.singletonList(cap2), candidates1);
    }

    @Test
    public void testRegionHasPrecedenceOverGlobalRegionOrderNotSet() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.otherfeature", new Version(1,0,0)), Collections.singletonList("b10"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.infeature", new Version(1,0,0)), Collections.singletonList("b11"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal", new Version(1,0,0)), Collections.singletonList("b19"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b10", Collections.singleton("f10"));
        bfmap.put("b11", Collections.singleton("f11"));
        bfmap.put("b2", Collections.singleton("f2"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f10", Arrays.asList("r2", "r3"));
        frmap.put("f11", Arrays.asList("r1", "r2"));
        frmap.put("f2", Collections.singletonList("r1"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("r1", Collections.singleton("org.foo.bar"));
        rpmap.put("r2", new HashSet<>(Arrays.asList("xxx", "yyy", "zzz")));
        rpmap.put("r3", new HashSet<>(Arrays.asList("org.foo.bar", "zzz")));

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 needs to resolve to 'org.foo.bar' package. b2 is in region r1.
        // The package is provided by 3 bundles:
        //   b10 provides it but is not in a matching region
        //   b11 provides it and has a matching region
        //   b19 provides it in the global region
        // Only b11 should provide the capability. Even though b19 provides it from the global region, if there is an overlapping
        // specific region then the global region should not be used
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.foo.bar", "b10", bsnvermap);
        BundleCapability cap2 = mockCapability("org.foo.bar", "b11", bsnvermap);
        BundleCapability cap3 = mockCapability("org.foo.bar", "b19", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1, cap2, cap3));
        rh.filterMatches(req1, candidates1);

        assertEquals("Only the capability coming from bundle b11 should be selected, b10 is in a different region and b19 is in global "
                + "which should be excluded as there is a capability in a matching region.",
                Collections.singletonList(cap2), candidates1);
    }

    @Test
    public void testOwnBundleInRegionHasPrecedenceOverGlobal() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.infeature", new Version(1,0,0)), Collections.singletonList("b1"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b1", Collections.singleton("f1"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f1", Collections.emptyList());
        frmap.put("__region.order__", Arrays.asList("global", "r1"));

        Map<String, Set<String>> rpmap = new HashMap<>();

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        BundleRequirement req1 = mockRequirement("b1", bsnvermap);
        BundleCapability cap1 = mockCapability("org.foo.bar", "b1", bsnvermap);
        BundleCapability cap2 = mockCapability("org.foo.bar", "b2", bsnvermap);
        List<BundleCapability> candidates = new ArrayList<>(Arrays.asList(cap1, cap2));
        rh.filterMatches(req1, candidates);

        assertEquals("Only the candidate from b1 should be selected, even through b2 is in the global region, "
                + "because b1 is not exported and as such more specific",
                Collections.singletonList(cap1), candidates);
    }

    @Test
    public void testOwnFeatureHasPrecendenceOverGlobal() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.infeature", new Version(1,0,0)), Collections.singletonList("b11"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal", new Version(1,0,0)), Collections.singletonList("b19"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b11", Collections.singleton("f1"));
        bfmap.put("b2", Collections.singleton("f1"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f1", Collections.singletonList("r1"));
        frmap.put("__region.order__", Arrays.asList("global", "r1"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("r1", Collections.emptySet());

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 needs to resolve 'org.foo.bar' and there are 2 candidates:
        // b11 is in the same feature as b2
        // b19 is in the no feature
        // In this case b11 should be selected as its in the same region
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.foo.bar", "b11", bsnvermap);
        BundleCapability cap2 = mockCapability("org.foo.bar", "b19", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1, cap2));
        rh.filterMatches(req1, candidates1);

        assertEquals("Only b11 should be selected as its from the same region as b2",
                Collections.singletonList(cap1), candidates1);
    }

    @Test
    public void testGlobalAndNonAPIRegionsFeatureAreEqual() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();

        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.infeature", new Version(1,0,0)), Collections.singletonList("b101"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal", new Version(1,0,0)), Collections.singletonList("b102"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b99"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b99", Collections.singleton("f1"));
        bfmap.put("b101", Collections.singleton("f1"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f1", Arrays.asList("r1", "global"));
        frmap.put("__region.order__", Arrays.asList("global", "r1"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("r1", Collections.singleton("org.blah.blah"));
        rpmap.put(RegionConstants.GLOBAL_REGION, Collections.singleton("org.foo.bar"));

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 needs to resolve 'org.foo.bar' and there are 2 candidates:
        // b101 is in the same feature as b2
        // b102 is in the no feature
        // In this case both should still be available as both are in the global region
        BundleRequirement req1 = mockRequirement("b99", bsnvermap);
        BundleCapability cap1 = mockCapability("org.foo.bar", "b101", bsnvermap);
        BundleCapability cap2 = mockCapability("org.foo.bar", "b102", bsnvermap);
        List<BundleCapability> candidates = new ArrayList<>(Arrays.asList(cap1, cap2));
        Set<BundleCapability> orgCandidates = new HashSet<>(candidates);
        rh.filterMatches(req1, candidates);

        assertEquals(orgCandidates, new HashSet<>(candidates));
    }

    @Test
    public void testMultipleGlobalOptionsOnly() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.otherfeature", new Version(1,0,0)), Collections.singletonList("b10"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal1", new Version(1,0,0)), Collections.singletonList("b18"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal2", new Version(1,0,0)), Collections.singletonList("b19"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b10", Collections.singleton("f10"));
        bfmap.put("b2", Collections.singleton("f2"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f10", Arrays.asList("r2", "r3"));
        frmap.put("f2", Collections.singletonList("r1"));
        frmap.put("__region.order__", Arrays.asList("global", "r1", "r2", "r3"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("r1", Collections.emptySet());
        rpmap.put("r2", new HashSet<>(Arrays.asList("xxx", "yyy", "zzz")));
        rpmap.put("r3", new HashSet<>(Arrays.asList("org.foo.bar", "zzz")));

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 needs to resolve to 'org.foo.bar' package. b2 is in region r1.
        // However nobody in r1 exports the package
        // The package is exported by b10 in r3 but b2 is not in that region.
        // The package is exported by b18 and b19 which are both in the global region
        // Both these candidates should remain available.
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.foo.bar", "b10", bsnvermap);
        BundleCapability cap2 = mockCapability("org.foo.bar", "b18", bsnvermap);
        BundleCapability cap3 = mockCapability("org.foo.bar", "b19", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1, cap2, cap3));
        rh.filterMatches(req1, candidates1);

        assertEquals("Since there are no specific candidates in a matching region, the candidates from "
                + "the global region should be allowed.",
                Arrays.asList(cap2, cap3), candidates1);
    }

    @Test
    public void testMultipleGlobalOptionsOnly2() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal1", new Version(1,0,0)), Collections.singletonList("b18"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "providing.bundle.inglobal2", new Version(1,0,0)), Collections.singletonList("b19"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "requiring.bundle", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        Map<String, List<String>> frmap = new HashMap<>();
        Map<String, Set<String>> rpmap = new HashMap<>();

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 needs to resolve to 'org.foo.bar' package. b2 is in no region.
        // b18 and b19 export the package in the global region. They should both be allowed.
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("org.foo.bar", "b18", bsnvermap);
        BundleCapability cap2 = mockCapability("org.foo.bar", "b19", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1, cap2));
        rh.filterMatches(req1, candidates1);

        assertEquals(Arrays.asList(cap1, cap2), candidates1);
    }

    @Test
    public void testFilterMatches() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("system.bundle", new Version(3,2,1)),
                Collections.singletonList("b0"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("a.b.c", new Version(0,0,0)),
                Collections.singletonList("b7"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("a.bundle", new Version(1,0,0)),
                Collections.singletonList("b8"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("some.other.bundle", new Version(9,9,9,"suffix")),
                Collections.singletonList("b9"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("a-bundle", new Version(1,0,0,"SNAPSHOT")),
                Collections.singletonList("b10"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("not.in.a.feature", new Version(0,0,1)),
                Collections.singletonList("b11"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("also.not.in.a.feature", new Version(0,0,1)),
                Collections.singletonList("b12"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("a.b.c", new Version(1,2,3)),
                Collections.singletonList("b17"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("x.y.z", new Version(9,9,9)),
                Collections.singletonList("b19"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("zzz", new Version(1,0,0)),
                Collections.singletonList("b20"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b7", Collections.singleton("f"));
        bfmap.put("b8", Collections.singleton("f1"));
        bfmap.put("b9", Collections.singleton("f2"));
        bfmap.put("b10", Collections.singleton("f2"));
        bfmap.put("b17", Collections.singleton("f3"));
        bfmap.put("b19", Collections.singleton("f3"));
        bfmap.put("b20", Collections.singleton("f4"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f", Arrays.asList("r1", "r2", RegionConstants.GLOBAL_REGION));
        frmap.put("f1", Collections.singletonList("r1"));
        frmap.put("f2", Collections.singletonList("r2"));
        frmap.put("f3", Collections.singletonList("r3"));
        frmap.put("f4", Collections.singletonList("r3"));
        frmap.put("__region.order__", Arrays.asList("global", "r1", "r2", "r3"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("r0", Collections.singleton("org.bar"));
        rpmap.put("r1", new HashSet<>(Arrays.asList("org.blah", "org.foo")));
        rpmap.put(RegionConstants.GLOBAL_REGION, Collections.singleton("org.bar.tar"));
        rpmap.put("r3", Collections.singleton("xyz"));

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.emptySet()));

        // Check that we can get the capability from another bundle in the same region
        // where that region exports the package
        // Bundle 7 is in feature f with regions r1, r2. Bundle 8 is in feature f1 with regions r1
        // r1 exports the org.foo package
        BundleRequirement req0 = mockRequirement("b7", bsnvermap);
        BundleCapability bc0 = mockCapability("org.foo", "b8", bsnvermap);
        List<BundleCapability> candidates0 = new ArrayList<>(Arrays.asList(bc0));
        rh.filterMatches(req0, candidates0);
        assertEquals(Collections.singletonList(bc0), candidates0);

        // Check that we cannot get the capability from another bundle in the same region
        // but that region doesn't export the pacakge.
        // Bundle 7 is in feature f with regions r1, r2. Bundle 9 is in feature f2 with regions r2
        // r2 does not export any packages
        BundleRequirement req1 = mockRequirement("b7", bsnvermap);
        BundleCapability bc1 = mockCapability("org.foo", "b9", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(bc1));
        rh.filterMatches(req1, candidates1);
        assertEquals(Collections.emptyList(), candidates1);

        // Check that we cannot get the capability from another bundle in a different region
        // Bundle 9 is in feature f2 with region r2
        // Bundle 17 is in feature f3 with region r3
        BundleRequirement req2 = mockRequirement("b9", bsnvermap);
        BundleCapability bc2 = mockCapability("org.bar", "b17", bsnvermap);
        Collection<BundleCapability> c2 = new ArrayList<>(Arrays.asList(bc2));
        rh.filterMatches(req2, c2);
        assertEquals(0, c2.size());

        // Check that we can get the capability from the same bundle
        BundleRequirement req3 = mockRequirement("b9", bsnvermap);
        BundleCapability bc3 = mockCapability("abc.xyz", "b9", bsnvermap);
        Collection<BundleCapability> c3 = new ArrayList<>(Arrays.asList(bc3));
        rh.filterMatches(req3, c3);
        assertEquals(Collections.singletonList(bc3), c3);

        // Check that we can get the capability from the another bundle in the same feature
        BundleRequirement req4 = mockRequirement("b9", bsnvermap);
        BundleCapability bc4 = mockCapability("some.cool.package", "b10", bsnvermap);
        Collection<BundleCapability> c4 = new ArrayList<>(Arrays.asList(bc4));
        rh.filterMatches(req4, c4);
        assertEquals(Collections.singletonList(bc4), c4);

        // Check that we can get the capability from another bundle where the capability
        // is globally visible b7 exposes org.bar.tar in the global region, so b17 can see it
        BundleRequirement req5 = mockRequirement("b17", bsnvermap);
        BundleCapability bc5 = mockCapability("org.bar.tar", "b7", bsnvermap);
        Collection<BundleCapability> c5 = new ArrayList<>(Arrays.asList(bc5));
        rh.filterMatches(req5, c5);
        assertEquals(Collections.singletonList(bc5), c5);

        // Check that we cannot get at a capability in a region from a bundle not in a feature
        BundleRequirement req6 = mockRequirement(6, "bundle.not.in.feature", new Version(2,0,0));
        BundleCapability bc6 = mockCapability("org.foo", "b9", bsnvermap);
        Collection<BundleCapability> c6 = new ArrayList<>(Arrays.asList(bc6));
        rh.filterMatches(req6, c6);
        assertEquals(0, c6.size());

        // Check that capabilities in non-package namespaces are ignored
        BundleRequirement req7 = Mockito.mock(BundleRequirement.class);
        Mockito.when(req7.getNamespace()).thenReturn("some.other.namespace");
        BundleCapability bc7 = mockCapability("org.bar", "b17", bsnvermap);
        Collection<BundleCapability> c7 = new ArrayList<>(Arrays.asList(bc7));
        rh.filterMatches(req7, c7);
        assertEquals(Collections.singletonList(bc7), c7);

        // Check that we can get the capability from another provider in the same region
        BundleRequirement req8 = mockRequirement("b20", bsnvermap);
        BundleCapability bc8 = mockCapability("xyz", "b19", bsnvermap);
        Collection<BundleCapability> c8 = new ArrayList<>(Arrays.asList(bc8));
        rh.filterMatches(req8, c8);
        assertEquals(Collections.singletonList(bc8), c8);

        // A requirement from a bundle that has no feature cannot access one in a region
        // b17 provides package xyz which is in region r3, but b11 is not in any region.
        BundleRequirement req9 = mockRequirement("b11", bsnvermap);
        BundleCapability bc9 = mockCapability("xyz", "b17", bsnvermap);
        Collection<BundleCapability> c9 = new ArrayList<>(Arrays.asList(bc9));
        rh.filterMatches(req9, c9);
        assertEquals(0, c9.size());

        // A requirement from a bundle that has no feature can still access one in the global region
        // b7 exposes org.bar.tar in the global region, so b11 can see it
        BundleRequirement req10 = mockRequirement("b11", bsnvermap);
        BundleCapability bc10 = mockCapability("org.bar.tar", "b7", bsnvermap);
        Collection<BundleCapability> c10 = new ArrayList<>(Arrays.asList(bc10));
        rh.filterMatches(req10, c10);
        assertEquals(Collections.singletonList(bc10), c10);

        // A requirement from a bundle that has no feature can be satisfied by a capability
        // from a bundle that has no feature
        BundleRequirement req11 = mockRequirement("b11", bsnvermap);
        BundleCapability bc11 = mockCapability("ding.dong", "b12", bsnvermap);
        Collection<BundleCapability> c11 = new ArrayList<>(Arrays.asList(bc11));
        rh.filterMatches(req11, c11);
        assertEquals(Collections.singletonList(bc11), c11);

        // A capability from the system bundle is always accessible
        BundleRequirement req12 = mockRequirement("b11", bsnvermap);
        BundleCapability bc12 = mockCapability("ping.pong", "b0", bsnvermap);
        Collection<BundleCapability> c12 = new ArrayList<>(Arrays.asList(bc12));
        rh.filterMatches(req12, c12);
        assertEquals(Collections.singletonList(bc12), c12);

        // Check that anyone can get a capability from a bundle not in a feature
        BundleRequirement req13 = mockRequirement("b9", bsnvermap);
        BundleCapability bc13 = mockCapability("some.package", 999, "no.in.any.feature", new Version(1,0,0));
        Collection<BundleCapability> c13 = new ArrayList<>(Arrays.asList(bc13));
        rh.filterMatches(req13, c13);
        assertEquals(Collections.singletonList(bc13), c13);
    }

    @Test
    public void testMultipleRegionsNoneMatching() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("bundle.1", new Version(1,0,0)),
                Collections.singletonList("b1"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("bundle.2", new Version(1,0,0)),
                Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b1", Collections.singleton("f1"));
        bfmap.put("b2", Collections.singleton("f2"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f1", Arrays.asList(RegionConstants.GLOBAL_REGION, "org.foo.blah"));
        frmap.put("f2", Arrays.asList("org.foo.bar", RegionConstants.GLOBAL_REGION, "org.foo.blah"));
        frmap.put("__region.order__", Arrays.asList("global", "org.foo.blah", "org.foo.bar"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("org.foo.bar", Collections.singleton("org.test"));
        rpmap.put(RegionConstants.GLOBAL_REGION, Collections.singleton("org.something"));
        rpmap.put("org.foo.blah", Collections.singleton("org.something"));

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.emptySet()));

        BundleRequirement req0 = mockRequirement("b1", bsnvermap);
        BundleCapability cap0 = mockCapability("org.test", "b2", bsnvermap);
        List<BundleCapability> candidates0 = new ArrayList<>(Arrays.asList(cap0));
        rh.filterMatches(req0, candidates0);
        assertEquals(Collections.emptyList(), candidates0);
    }

    @Test
    public void testGetRegionsForPackage() {
        List<String> regions = Arrays.asList("r1", "r2", "r3");
        Map<String, List<String>> featureRegionMap = Collections.singletonMap("f2", regions);
        Map<String, Set<String>> regionPackageMap = new HashMap<>();

        regionPackageMap.put("r2", Collections.singleton("a.b.c"));
        Set<String> pkgs = new HashSet<>();
        pkgs.add("org.foo.bar");
        pkgs.add("org.foo.zar");
        regionPackageMap.put("r3", pkgs);

        ResolverHookImpl rh = new ResolverHookImpl(
                new RegionConfiguration(Collections.<Map.Entry<String, Version>, List<String>>emptyMap(),
                Collections.<String, Set<String>>emptyMap(), featureRegionMap, regionPackageMap, Collections.emptySet()));

        assertEquals(Collections.emptyList(), rh.getRegionsForPackage(null, "f1"));
        assertEquals(Collections.emptyList(), rh.getRegionsForPackage("org.foo", "f1"));
        assertEquals(Collections.emptyList(), rh.getRegionsForPackage(null, "f2"));
        assertEquals(Collections.emptyList(), rh.getRegionsForPackage("org.foo", "f2"));

        assertEquals(Collections.singletonList("r3"), rh.getRegionsForPackage("org.foo.bar", "f2"));
        assertEquals(Collections.singletonList("r2"), rh.getRegionsForPackage("a.b.c", "f2"));
    }

    @Test
    public void testDefaultRegions() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("b98", new Version(1, 0, 0)),
                Collections.singletonList("b98"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("b99", new Version(1, 2, 3)),
                Collections.singletonList("b99"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("b100", new Version(4, 5, 6)),
                Collections.singletonList("b100"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b98", Collections.singleton("f2"));
        bfmap.put("b100", Collections.singleton("f1"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("f1", Collections.singletonList("r1"));
        frmap.put("f2", Collections.singletonList("r2"));
        frmap.put("__region.order__", Arrays.asList("global", "r0", "r1", "r2", "r3"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("r1", Collections.singleton("org.test"));
        rpmap.put("r2", Collections.singleton("org.test"));

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap,
                new HashSet<>(Arrays.asList("r0", "r1"))));

        // b99 is not in any region itself and tries to resolve to b100 which is in r1
        // b99 can resolve to b100 because 'r1' is listed as a default region in the
        // ResolverHook.
        BundleRequirement req = mockRequirement("b99", bsnvermap);
        BundleCapability cap = mockCapability("org.test", "b100", bsnvermap);
        List<BundleCapability> candidates = new ArrayList<>(Arrays.asList(cap));
        rh.filterMatches(req, candidates);
        assertEquals("b99 should be able to wire to b100, as the default region is r1, which is what b100 is in. ",
                Collections.singletonList(cap), candidates);

        // b99 is not in any region and tries to resolve to b98, which is in r2
        // b99 can't resolve because b2 is not in the default regions and it's not in
        // any other regions itself.
        BundleRequirement req1 = mockRequirement("b99", bsnvermap);
        BundleCapability cap1 = mockCapability("org.test", "b98", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1));
        rh.filterMatches(req1, candidates1);
        assertEquals("b99 should not be able to wire to b98, as this is in region r2, which is not in the default regions r1 and r3",
                0, candidates1.size());
    }

    @Test
    public void testGetFeaturesForBundleMultiSession() {
        String bsn = "foo";
        Version bver = new Version(1,2,3);
        String blocation = "something://foobar";

        String bsn2 = "bar.bar";
        Version bver2 = new Version(1,0,0);

        Map<Map.Entry<String, Version>, List<String>> bsnVerMap = new HashMap<>();
        Map<String, Set<String>> bundleFeatureMap = new HashMap<>();
        bsnVerMap.put(new AbstractMap.SimpleEntry<String, Version>(bsn, bver),
                Collections.singletonList("myorg:foo:1.2.3"));
        bsnVerMap.put(new AbstractMap.SimpleEntry<String, Version>(bsn2, bver2),
                Collections.singletonList("myorg:bar.bar:1.0.0"));
        bundleFeatureMap.put("myorg:foo:1.2.3", new HashSet<>(Arrays.asList("feature1")));
        bundleFeatureMap.put("myorg:bar.bar:1.0.0", new HashSet<>(
                Arrays.asList("feature3", "feature4")));

        RegionConfiguration cfg = new RegionConfiguration(bsnVerMap, bundleFeatureMap,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());

        RegionEnforcer re = new RegionEnforcer(cfg);

        Bundle b = Mockito.mock(Bundle.class);
        Mockito.when(b.getSymbolicName()).thenReturn(bsn);
        Mockito.when(b.getVersion()).thenReturn(bver);
        Mockito.when(b.getLocation()).thenReturn(blocation);
        ResolverHookImpl rhi = (ResolverHookImpl) re.begin(Collections.emptySet());
        Set<String> features = rhi.getFeaturesForBundle(b);

        assertEquals(Collections.singleton("feature1"), features);

        // Try a bundle with a different version, same location
        Bundle b2 = Mockito.mock(Bundle.class);
        Mockito.when(b2.getSymbolicName()).thenReturn(bsn);
        Mockito.when(b2.getVersion()).thenReturn(new Version(1,4,0));
        Mockito.when(b2.getLocation()).thenReturn(blocation);

        // Obtain a new ResolverHookImpl to mimic multiple resolve sessions
        ResolverHookImpl rhi2 = (ResolverHookImpl) re.begin(Collections.emptySet());
        assertEquals(features, rhi2.getFeaturesForBundle(b2));

        // Try a bundle with the same bsn+version but different location
        Bundle b3 = Mockito.mock(Bundle.class);
        Mockito.when(b3.getSymbolicName()).thenReturn(bsn);
        Mockito.when(b3.getVersion()).thenReturn(bver);
        Mockito.when(b3.getLocation()).thenReturn("something://foobar2");
        ResolverHookImpl rhi3 = (ResolverHookImpl) re.begin(Collections.emptySet());
        assertEquals(features, rhi3.getFeaturesForBundle(b3));

        // Try a bundle with a different bsn+version and a different location
        Bundle b4 = Mockito.mock(Bundle.class);
        Mockito.when(b4.getSymbolicName()).thenReturn(bsn2);
        Mockito.when(b4.getVersion()).thenReturn(bver2);
        Mockito.when(b4.getLocation()).thenReturn("another://location");

        Set<String> expected = new HashSet<>(Arrays.asList("feature3", "feature4"));
        ResolverHookImpl rhi4 = (ResolverHookImpl) re.begin(Collections.emptySet());
        assertEquals(expected, rhi4.getFeaturesForBundle(b4));
    }

    @Test
    public void testRegionOrderInheritance() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "b1", new Version(1,0,0)), Collections.singletonList("b1"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "b2", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b1", Collections.singleton("g:f1:1"));
        bfmap.put("b2", Collections.singleton("g:f2:1"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("g:f1:1", Arrays.asList("deprecated", "internal"));
        frmap.put("g:f2:1", Collections.singletonList("internal"));
        frmap.put("__region.order__", Arrays.asList("global", "deprecated", "internal"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("internal", Collections.singleton("xyz"));
        rpmap.put("deprecated", Collections.singleton("abc"));

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));

        // b2 needs to resolve to the "abc" package, which is exported into the 'deprecated' region.
        // f2 doesn't directly see the 'deprecated' region (only internal), but since it's declared before
        // the internal region by f1, f2 implicitly gets visibility of it because it comes before the
        // internal region in the global region ordering.
        BundleRequirement req1 = mockRequirement("b2", bsnvermap);
        BundleCapability cap1 = mockCapability("abc", "b1", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(cap1));
        rh.filterMatches(req1, candidates1);

        assertEquals(Collections.singletonList(cap1), candidates1);
    }

    @Test
    public void testEmptyCandidates() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "b1", new Version(1,0,0)), Collections.singletonList("b1"));
        Map<String, Set<String>> bfmap = new HashMap<>();
        Map<String, List<String>> frmap = new HashMap<>();
        Map<String, Set<String>> rpmap = new HashMap<>();

        ResolverHookImpl rh = new ResolverHookImpl(new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap, Collections.singleton("global")));
        BundleRequirement req1 = mockRequirement("b1", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>();
        rh.filterMatches(req1, candidates1);
        assertEquals("There were no candidates, there still are none", 0, candidates1.size());
    }

    /* This test exposes a bug that exports the package from the wrong bundle. The situation can be reproduced as follows:
     * - Have a feature f1 that contains a bundle that exports a package, e.g. org.soup. Make sure that the feature doesn't
     *   export the package into an API region.
     * - Have another feature f2 that contains a different bundle that exports the same package. In this case the feature
     *   exports the package into the 'global' region.
     * Now when the resolver hook needs to handle the wiring for f2 it gets 2 candidates, because to OSGi both exporting
     * bundles are candidates.
     * The resolver hook finds that org.soup is exported into the global region (by f2). Then it looks at what bundles in
     * the global region and finds that both f1 and f2 are in the global region because they are by default.
     * Then the resolver hook thinks that both candidates are acceptible because both are in the global region, so it
     * produces a wiring to the candidate from f1, even though f1 doesn't export the package.
     *
     * It should really just return the candidate from f2, but currently the data provided to the API regions doesn't have
     * the richness to know this.
     */
    @Test
    public void testABCXYZ() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "b1", new Version(1,0,0)), Collections.singletonList("b1"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>(
                "b2", new Version(1,0,0)), Collections.singletonList("b2"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b1", Collections.singleton("g:feat-int:1"));
        bfmap.put("b2", Collections.singleton("g:feat-global:1"));

        Map<String, List<String>> frmap = new HashMap<>();
        frmap.put("g:feat-int:1", Arrays.asList("global", "org.foo.bar.deprecated"));
        frmap.put("g:feat-global:1", Arrays.asList("global", "org.foo.bar.deprecated"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("global", Collections.singleton("spaghetti.soup"));

        RegionConfiguration rc = new RegionConfiguration(bsnvermap, bfmap, frmap, rpmap,
                new HashSet<>(Arrays.asList("global", "org.foo.bar.deprecated")));

        ResolverHookImpl rh = new ResolverHookImpl(rc);
        BundleRequirement req = mockRequirement("b2", bsnvermap);
        List<BundleCapability> candidates = new ArrayList<>();

        BundleCapability capInt = mockCapability("spaghetti.soup", "b1", bsnvermap);
        BundleCapability capGlob = mockCapability("spaghetti.soup", "b2", bsnvermap);
        candidates.add(capInt);
        candidates.add(capGlob);

        rh.filterMatches(req, candidates);
        assertEquals("We should only get the second candidate here because that is the one that is exporting " +
                "spaghetti.soup. However we get both, because the data currently doesn't expose what feature exported a package " +
                "it just states that a package is exported in a certain region. So if there is a bundle in a feature that is part " +
                "of that region and happens to export it, it is automatically included, even if that package isn't exported in that feature",
                1, candidates.size());
    }

    private BundleCapability mockCapability(String pkgName, String bid, Map<Entry<String, Version>, List<String>> bsnvermap) {
        for (Map.Entry<Map.Entry<String, Version>, List<String>> entry : bsnvermap.entrySet()) {
            if (entry.getValue().contains(bid)) {
                // Remove first letter and use rest as bundle ID
                long id = Long.parseLong(bid.substring(1));
                return mockCapability(pkgName, id, entry.getKey().getKey(), entry.getKey().getValue());
            }
        }
        throw new IllegalStateException("Bundle not found " + bid);
    }

    private BundleCapability mockCapability(String pkg, long bundleID, String bsn, Version version) {
        Map<String, Object> attrs =
                Collections.<String, Object>singletonMap(PackageNamespace.PACKAGE_NAMESPACE, pkg);

        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(bundle.getBundleId()).thenReturn(bundleID);
        Mockito.when(bundle.getSymbolicName()).thenReturn(bsn);
        Mockito.when(bundle.getVersion()).thenReturn(version);
        Mockito.when(bundle.getLocation()).thenReturn("test://" + bsn + version + ".loc");

        BundleRevision br = Mockito.mock(BundleRevision.class);
        Mockito.when(br.getBundle()).thenReturn(bundle);

        BundleCapability cap = Mockito.mock(BundleCapability.class);
        Mockito.when(cap.getNamespace()).thenReturn(PackageNamespace.PACKAGE_NAMESPACE);
        Mockito.when(cap.getAttributes()).thenReturn(attrs);
        Mockito.when(cap.getRevision()).thenReturn(br);
        return cap;
    }

    private BundleRequirement mockRequirement(String bid, Map<Map.Entry<String, Version>, List<String>> bsnvermap) {
        for (Map.Entry<Map.Entry<String, Version>, List<String>> entry : bsnvermap.entrySet()) {
            if (entry.getValue().contains(bid)) {
                // Remove first letter and use rest as bundle ID
                long id = Long.parseLong(bid.substring(1));
                return mockRequirement(id, entry.getKey().getKey(), entry.getKey().getValue());
            }
        }
        throw new IllegalStateException("Bundle not found " + bid);
    }

    private BundleRequirement mockRequirement(long bundleID, String bsn, Version version) {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(bundle.getBundleId()).thenReturn(bundleID);
        Mockito.when(bundle.getSymbolicName()).thenReturn(bsn);
        Mockito.when(bundle.getVersion()).thenReturn(version);
        Mockito.when(bundle.getLocation()).thenReturn("test://" + bsn + version + ".loc");

        BundleRevision br = Mockito.mock(BundleRevision.class);
        Mockito.when(br.getBundle()).thenReturn(bundle);

        BundleRequirement req = Mockito.mock(BundleRequirement.class);
        Mockito.when(req.getNamespace()).thenReturn(PackageNamespace.PACKAGE_NAMESPACE);
        Mockito.when(req.getRevision()).thenReturn(br);

        return req;
    }
}
