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

import static org.apache.sling.feature.apiregions.impl.RegionConstants.BUNDLE_FEATURE_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.FEATURE_REGION_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.IDBSNVER_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.PROPERTIES_RESOURCE_PREFIX;
import static org.apache.sling.feature.apiregions.impl.RegionConstants.REGION_PACKAGE_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ActivatorTest {
    private Properties savedProps;

    @Before
    public void setup() {
        savedProps = new Properties(); // note that new Properties(props) doesn't copy
        savedProps.putAll(System.getProperties());
    }

    @After
    public void teardown() {
        System.setProperties(savedProps);
        savedProps = null;
    }

    @Test
    public void testStart() throws Exception {
        String i = getClass().getResource("/idbsnver1.properties").toURI().toString();
        String b = getClass().getResource("/bundles1.properties").toURI().toString();
        String f = getClass().getResource("/features1.properties").toURI().toString();
        String r = getClass().getResource("/regions1.properties").toURI().toString();

        Dictionary<String, Object> expectedProps = new Hashtable<>();
        expectedProps.put(IDBSNVER_FILENAME, i);
        expectedProps.put(BUNDLE_FEATURE_FILENAME, b);
        expectedProps.put(FEATURE_REGION_FILENAME, f);
        expectedProps.put(REGION_PACKAGE_FILENAME, r);

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(bc.getProperty(Activator.REGIONS_PROPERTY_NAME)).thenReturn("*");
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).
            thenReturn(i);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).
            thenReturn(b);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).
            thenReturn(f);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).
            thenReturn(r);

        Activator a = new Activator();
        a.start(bc);

        Mockito.verify(bc, Mockito.times(1)).registerService(
                Mockito.eq(ResolverHookFactory.class),
                Mockito.isA(RegionEnforcer.class),
                Mockito.eq(expectedProps));

        Mockito.verify(bc).addFrameworkListener(a);
    }

    @Test
    public void testRegistryHookNotEnabled() {
        BundleContext bc = Mockito.mock(BundleContext.class);

        Activator a = new Activator();
        a.bundleContext = bc;
        a.registerHook();

        assertNull(a.hookRegistration);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegistryHookAlreadyPresent() {
        BundleContext bc = Mockito.mock(BundleContext.class);

        Activator a = new Activator();
        a.bundleContext = bc;
        a.hookRegistration = Mockito.mock(ServiceRegistration.class);
        a.registerHook();

        assertNotNull(a.hookRegistration);
        Mockito.verifyZeroInteractions(bc);
    }

    @Test
    public void testUnregisterHook() {
        Activator a = new Activator();
        a.unregisterHook(); // Should not throw an exception
        assertNull(a.hookRegistration);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testUnregisterHook2() {
        ServiceRegistration reg = Mockito.mock(ServiceRegistration.class);

        Activator a = new Activator();
        a.hookRegistration = reg;

        a.unregisterHook();
        Mockito.verify(reg).unregister();
        assertNull(a.hookRegistration);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFrameworkEvent() throws Exception {
        String resourceDir = new File(getClass().getResource("/props3/idbsnver.properties").getFile()).getParent();

        BundleWiring bw = Mockito.mock(BundleWiring.class);
        Mockito.when(bw.getClassLoader()).thenReturn(getClass().getClassLoader());
        BundleRevision rev = Mockito.mock(BundleRevision.class);
        Mockito.when(rev.getWiring()).thenReturn(bw);
        BundleCapability cap = Mockito.mock(BundleCapability.class);
        Mockito.when(cap.getRevision()).thenReturn(rev);

        FrameworkWiring wiring = Mockito.mock(FrameworkWiring.class);
        Mockito.when(wiring.findProviders(Mockito.any(Requirement.class))).thenAnswer(
            new Answer<Collection<BundleCapability>>() {
                @Override
                public Collection<BundleCapability> answer(InvocationOnMock invocation) throws Throwable {
                    Requirement req = invocation.getArgument(0);
                    if ("osgi.wiring.package".equals(req.getNamespace())) {
                        if ("(osgi.wiring.package=org.osgi.service.cm)".equals(req.getDirectives().get("filter"))) {
                            return Collections.singleton(cap);
                        }
                    }
                    return null;
                }
            });


        Bundle fw = Mockito.mock(Bundle.class);
        Mockito.when(fw.adapt(FrameworkWiring.class)).thenReturn(wiring);

        List<Object> managedServices = new ArrayList<Object>();
        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundle()).thenReturn(fw);
        Mockito.when(bc.getProperty(Activator.REGIONS_PROPERTY_NAME)).thenReturn("*");
        Mockito.when(bc.getProperty(RegionConstants.PROPERTIES_FILE_LOCATION)).thenReturn(resourceDir);
        Mockito.when(bc.registerService(
            Mockito.eq("org.osgi.service.cm.ManagedService"),
            Mockito.any(),
            Mockito.any(Dictionary.class))).thenAnswer(new Answer<ServiceRegistration<?>>() {
                @Override
                public ServiceRegistration<?> answer(InvocationOnMock invocation) throws Throwable {
                    Dictionary<String,?> dict = invocation.getArgument(2);
                    if ("org.apache.sling.feature.apiregions.impl".equals(dict.get("service.pid"))) {
                        managedServices.add(invocation.getArgument(1));
                    }
                    return Mockito.mock(ServiceRegistration.class);
                }
            });
        Mockito.when(bc.registerService(
                Mockito.eq(ResolverHookFactory.class),
                Mockito.isA(RegionEnforcer.class),
                Mockito.any(Dictionary.class))).thenReturn(Mockito.mock(ServiceRegistration.class));


        Activator a = new Activator();
        a.bundleContext = bc;
        a.configuration = new RegionConfiguration(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
        FrameworkEvent ev = Mockito.mock(FrameworkEvent.class);
        Mockito.when(ev.getType()).thenReturn(FrameworkEvent.STARTED);

        a.frameworkEvent(ev);
        Mockito.verify(bc).removeFrameworkListener(a);
        assertEquals(1, managedServices.size());
        ManagedService managedService = (ManagedService) managedServices.get(0);

        assertNull("Precondition", a.hookRegistration);
        managedService.updated(null);
        assertNotNull(a.hookRegistration);

        ServiceRegistration<ResolverHookFactory> hookReg = a.hookRegistration;
        Mockito.verifyZeroInteractions(hookReg);
        managedService.updated(new Hashtable<>(Collections.singletonMap("disable", "true")));
        Mockito.verify(hookReg).unregister();
        assertNull(a.hookRegistration);

        managedService.updated(new Hashtable<>(Collections.singletonMap("disable", "false")));
        assertNotNull(a.hookRegistration);
    }

    @Test
    public void testFrameworkEventNoCMProviders() {
        FrameworkWiring wiring = Mockito.mock(FrameworkWiring.class);

        Bundle fw = Mockito.mock(Bundle.class);
        Mockito.when(fw.adapt(FrameworkWiring.class)).thenReturn(wiring);

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundle()).thenReturn(fw);

        Activator a = new Activator();
        a.bundleContext = bc;

        FrameworkEvent ev = Mockito.mock(FrameworkEvent.class);
        Mockito.when(ev.getType()).thenReturn(FrameworkEvent.STARTED);

        a.frameworkEvent(ev);
        Mockito.verify(bc).removeFrameworkListener(a);
    }

    @Test
    public void testFrameworkEventNoSystemBundle() {
        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundle()).thenReturn(Mockito.mock(Bundle.class));

        Activator a = new Activator();
        a.bundleContext = bc;

        FrameworkEvent ev = Mockito.mock(FrameworkEvent.class);
        Mockito.when(ev.getType()).thenReturn(FrameworkEvent.STARTED);

        a.frameworkEvent(ev);
        Mockito.verify(bc).removeFrameworkListener(a);
    }

    @Test
    public void testCreatePackageRequirement() {
        Requirement req = Activator.createCMPackageRequirement();
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, req.getNamespace());
        assertEquals(1, req.getDirectives().size());
        String directive = req.getDirectives().get("filter");
        assertEquals("(osgi.wiring.package=org.osgi.service.cm)", directive);
        assertEquals(0, req.getAttributes().size());
        assertNull(req.getResource());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConfigAdminListener() throws Exception {
        String i = getClass().getResource("/idbsnver1.properties").toURI().toString();
        String b = getClass().getResource("/bundles1.properties").toURI().toString();
        String f = getClass().getResource("/features1.properties").toURI().toString();
        String r = getClass().getResource("/regions1.properties").toURI().toString();

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundle()).thenReturn(Mockito.mock(Bundle.class));
        Mockito.when(bc.getProperty(Activator.REGIONS_PROPERTY_NAME)).thenReturn("*");
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + IDBSNVER_FILENAME)).
            thenReturn(i);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + BUNDLE_FEATURE_FILENAME)).
            thenReturn(b);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + FEATURE_REGION_FILENAME)).
            thenReturn(f);
        Mockito.when(bc.getProperty(PROPERTIES_RESOURCE_PREFIX + REGION_PACKAGE_FILENAME)).
            thenReturn(r);

        ConfigurationAdmin cm = Mockito.mock(ConfigurationAdmin.class);
        Mockito.when(cm.listConfigurations("(service.factoryPid=org.apache.sling.feature.apiregions.factory)")).
            thenAnswer(new Answer<Configuration[]>() {
                @Override
                public Configuration[] answer(InvocationOnMock invocation) throws Throwable {
                    Dictionary<String, Object> props = new Hashtable<>();
                    props.put("foo", "bar");

                    Configuration cfg = Mockito.mock(Configuration.class);
                    Mockito.when(cfg.getPid()).thenReturn("org.apache.sling.feature.apiregions.factory~123");
                    Mockito.when(cfg.getProperties()).thenReturn(props);

                    return new Configuration[] {cfg};
                }
            });

        ServiceReference<Object> caRef = Mockito.mock(ServiceReference.class);
        Mockito.when(bc.getService(caRef)).thenReturn(cm);


        Activator a = new Activator();
        a.start(bc);
        ServiceTracker<Object, Object> st = a.configAdminTracker;
        Field custField = st.getClass().getDeclaredField("customizer");
        custField.setAccessible(true);

        @SuppressWarnings("rawtypes")
        ServiceTrackerCustomizer customizer = (ServiceTrackerCustomizer) custField.get(st);

        Field fcf = a.configuration.getClass().getDeclaredField("factoryConfigs");
        fcf.setAccessible(true);
        @SuppressWarnings("rawtypes")
        Map<String, Dictionary<String, Object>> factoryConfigs = (Map) fcf.get(a.configuration);
        assertEquals("Precondition", 0, factoryConfigs.size());

        customizer.addingService(caRef);
        Dictionary<String, Object> dict = factoryConfigs.get("org.apache.sling.feature.apiregions.factory~123");
        assertEquals("bar", dict.get("foo"));
    }
}
