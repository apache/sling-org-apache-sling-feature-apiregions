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

import static org.junit.Assert.*;
import static org.apache.sling.feature.apiregions.impl.AbstractResolverHookFactory.IDBSNVER_FILENAME;
import static org.apache.sling.feature.apiregions.impl.AbstractResolverHookFactory.PROPERTIES_RESOURCE_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

public class PlatformIsolationEnforcerTest {

    @Test
    public void testLoadBSNVerMap() throws Exception {
        String propertiesFile = getClass().getResource("/idbsnver1.properties").getFile();

        BundleContext context = mock(BundleContext.class);
        when(context.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).thenReturn(propertiesFile);

        PlatformIsolationEnforcer enforcer = new PlatformIsolationEnforcer(context);

        assertFalse(enforcer.bsnVerMap.isEmpty());
        assertEquals(2, enforcer.bsnVerMap.size());

        Map<String, Version> expected = new HashMap<>();
        expected.put("b2", new Version(1, 2, 3));
        expected.put("b1", new Version(1, 0, 0));

        assertEquals(expected, enforcer.bsnVerMap);
    }

}
