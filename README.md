[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-apiregions/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-apiregions/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-apiregions/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-apiregions/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-apiregions&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-apiregions)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-apiregions&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-apiregions)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.apiregions.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.feature.apiregions)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.apiregions/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.apiregions%22)&#32;[![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/feature.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling API Regions runtime component

The API Regions runtime component is implemented as an OSGi Framework Extension bundle.
This ensures that the runtime component is always present early in the startup process.
For more information about API Regions see https://github.com/apache/sling-org-apache-sling-feature/blob/master/apicontroller.md

This component registers an OSGi resolver hook service which enforces the API regions at runtime. The component looks for properties files that provide the configuration of the API regions. The properties files are generated by the https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions component during the extension post-processing.

As the component has no dependencies on any other component, the properties files are obtained via Framework Properties lookups:

* `sling.feature.apiregions.resource.idbsnver.properties` - provides the location of the `idbsnver.properties` file.
* `sling.feature.apiregions.resource.bundles.properties` - provides the location of the `bundles.properties` file.
* `sling.feature.apiregions.resource.features.properties` - provides the location of the `features.properties` file.
* `sling.feature.apiregions.resource.regions.properties` - provides the location of the `regions.properties` file.

Alternatively, the directory where all of the above files can be found can be specified using one Framework Property instead of using
the above framework properties:

* `sling.feature.apiregions.location` - provides the location where all properties file can be found. If this property is specified the above properties are not necessary. However if both are provided the file-specific properties take precedence.

File locations are either provided as an absolute file path or by URL. The URL handling mechanism supports one special pseudo-protocol:
`classloader://`. URLs specified with this protocol are passed through the framework classloader's `getResource()` method to obtain
the actual URL. 

## Enabling / disabling this component
The component is enabled by setting the following framework property:

    org.apache.sling.feature.apiregions.regions=*

If this framework property is not set the component will be disabled.

## Additional Configuration

The following framework properties are also recognised:

* `sling.feature.apiregions.default` - a comma-separated list of region names. Each bundle installed will be added to these regions, regardless of whether it's installed in a feature or not.
* `sling.feature.apiregions.joinglobal` - a comma-separated list of region names. All packages exported by these regions are added to the `global` region.

## Runtime Configuration
If this component runs in a framework with Configuration Admin present, and it is set to be enabled using the framework property, it can be disabled at runtime
through Configuration Admin configuration.

Runtime configuration supported:

**PID**: `org.apache.sling.feature.apiregions.impl`

Key | Value  
--- | ---
`disable` | if `true` then the API Regions component is disabled. Otherwise the component is enabled.

No meta type is defined for this configuration since it's not a typical user setting. However, when using the web console
the configuration can be created using `curl`: 

    curl -u <user>:<pass> -X POST -d "apply=true" -d "propertylist=disable" -d "disable=true" http://localhost:8080/system/console/configMgr/org.apache.sling.feature.apiregions.impl

## Configuration Files

* `idbsnver.properties` contains a mapping from Maven artifact ID to BSN+Version in the following format: `groupid:artifactId:version=bsn~1.0.0`
* `bundles.properties` lists what feature a bundle (by Maven ID) is defined in, could be more than one feature, so the value is comma-separated e.g.: `org.sling:mybundles:1=some.other:feature:123,org.sling:something:1.2.3:slingosgifeature:myclassifier`
* `features.properties` lists for a feature ID what regions this feature is in, also comma separated, e.g: `org.sling:myfeature:1.2.3=internal,global`
* `regions.properties` contains for each region a list of package names that are exported in this region, e.g. `global=d.e.f,test,a.b.c`
