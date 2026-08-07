---
manualId: bluetape4k-geo
title: "Geospatial Utilities"
description: "A unified module for geographic information processing. Provides Geocode, GeoHash, and GeoIP2 functionality."
kind: library
group: utilities
learningOrder: 1220
---

# Geospatial Utilities

## Problem {#problem}

A unified module for geographic information processing. Provides Geocode, GeoHash, and GeoIP2 functionality. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-geo` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-geo")
}
```

Gradle project path: `:bluetape4k-geo`. Source directory: `utils/geo`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Address`, `Geocode`, `GeocodeAddressFinder`, `SuspendGeocodeAddressFinder`, `BingAddress`, `BingAddressFinder`, `BingMapModel`, and `BingMapService`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Address`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/Address.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Address`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/Address.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Geocode`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/Geocode.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GeocodeAddressFinder`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/GeocodeAddressFinder.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendGeocodeAddressFinder`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/SuspendGeocodeAddressFinder.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BingAddress`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingAddress.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BingAddressFinder`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingAddressFinder.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BingMapModel`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingMapModel.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BingMapService`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingMapService.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GeoApiContextSupport`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/google/GeoApiContextSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GoogleAddress`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/google/GoogleAddress.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Module Overview**, **Class Diagram**, **GeoHash Encoding/Decoding Flow**, **Key Features**, **Geocode (formerly utils/geocode)**, **GeoHash**, **GeoIP2 (formerly utils/geoip2)**, **Usage Examples**, and **GeoHash Encoding/Decoding**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
compileOnly(project(":bluetape4k-jackson3"))
compileOnly(project(":bluetape4k-resilience4j"))
compileOnly(project(":bluetape4k-feign"))
compileOnly(libs.feign.core)
compileOnly(libs.feign.kotlin)
compileOnly(libs.feign.slf4j)
compileOnly(libs.feign.jackson)
compileOnly("com.google.maps:google-maps-services:2.2.0")
compileOnly(libs.httpclient5)
compileOnly(libs.httpclient5.cache)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`GeoLite2-ASN.mmdb`](../../../../utils/geo/src/main/resources/GeoLite2-ASN.mmdb)
- [`GeoLite2-City.mmdb`](../../../../utils/geo/src/main/resources/GeoLite2-City.mmdb)
- [`GeoLite2-Country.mmdb`](../../../../utils/geo/src/main/resources/GeoLite2-Country.mmdb)
- [`BingMapApi.http`](../../../../utils/geo/src/main/resources/bing/BingMapApi.http)
- [`location.json`](../../../../utils/geo/src/main/resources/bing/location.json)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Measure hot paths, bound input sizes, and monitor failures at the application boundary that calls the utility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-geo:test --no-configuration-cache
```

Representative test anchors:

- [`GeoReadmeContractTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geo/GeoReadmeContractTest.kt)
- [`AbstractGeocodeTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geocode/AbstractGeocodeTest.kt)
- [`JsonSerializationTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geocode/JsonSerializationTest.kt)
- [`BingAddressFinderTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geocode/bing/BingAddressFinderTest.kt)
- [`BingMapServiceTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geocode/bing/BingMapServiceTest.kt)
- [`GoogleAddressFinderTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geocode/google/GoogleAddressFinderTest.kt)
- [`AbstractGeoHashTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geohash/AbstractGeoHashTest.kt)
- [`BoudingBoxTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geohash/BoudingBoxTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Module Overview diagram

[![Module Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-geo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-geo-diagram-01.svg)

_Release README: [`utils/geo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/geo/README.md)_

### Geo Class Structure diagram

[![Geo Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-geo-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-geo-diagram-02.svg)

_Release README: [`utils/geo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/geo/README.md)_

### GeoHash Encoding/Decoding Flow diagram

[![GeoHash Encoding/Decoding Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-geo-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-geo-sequence-01.svg)

_Release README: [`utils/geo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/geo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/geo/README.md)
- [Module build](../../../../utils/geo/build.gradle.kts)
- [`Address`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/Address.kt)
- [`Geocode`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/Geocode.kt)
- [`GeocodeAddressFinder`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/GeocodeAddressFinder.kt)
- [`SuspendGeocodeAddressFinder`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/SuspendGeocodeAddressFinder.kt)
- [`BingAddress`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingAddress.kt)
- [`BingAddressFinder`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingAddressFinder.kt)
- [`BingMapModel`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingMapModel.kt)
- [`BingMapService`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/bing/BingMapService.kt)
- [`GeoApiContextSupport`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/google/GeoApiContextSupport.kt)
- [`GoogleAddress`](../../../../utils/geo/src/main/kotlin/io/bluetape4k/geocode/google/GoogleAddress.kt)
- [`GeoReadmeContractTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geo/GeoReadmeContractTest.kt)
- [`AbstractGeocodeTest`](../../../../utils/geo/src/test/kotlin/io/bluetape4k/geocode/AbstractGeocodeTest.kt)
