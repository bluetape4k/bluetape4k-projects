---
manualId: bluetape4k-bom
title: Bluetape4k bill of materials
description: Align published bluetape4k-projects module versions without repeating a version on every dependency.
kind: library
group: foundation
---

# Bluetape4k bill of materials

## Problem {#problem}

An application often uses several bluetape4k modules. Repeating versions makes upgrades noisy and can produce a classpath assembled from releases that were not tested together. `bluetape4k-bom` publishes Gradle platform constraints for the repository's publishable modules.

## When to use {#when-to-use}

Import the BOM when a build uses two or more bluetape4k artifacts or when a shared convention plugin should choose one release line for many services. A single-module experiment may pin its dependency directly, but moving to the BOM early makes later module additions safer.

This BOM aligns modules from `bluetape4k-projects`; the ecosystem aggregator BOM can be a better entry point when the build also consumes separate repositories such as Exposed extensions.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-core")
    implementation("io.github.bluetape4k:bluetape4k-coroutines")
}
```

Use `enforcedPlatform(...)` only when the application intentionally wants BOM constraints to override every competing version. Library builds should normally use `platform(...)` so consumers retain dependency-resolution control.

## Concepts {#concepts}

The module applies Gradle's `java-platform` plugin. Its publication contains dependency constraints, not runtime classes. The build derives constraints from root subprojects and excludes the BOM itself, workshops, `examples`, and `-demo` projects.

## Quick start {#quick-start}

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-logging")
}
```

After import, omit the version only for artifacts constrained by the BOM. Keep explicit versions for unrelated libraries unless another platform owns them.

## API by task {#api-by-task}

| Task | Build expression |
| --- | --- |
| Align modules while allowing downstream resolution | `platform(...)` |
| Force the selected line in an application | `enforcedPlatform(...)` |
| Inspect the selected constraints | `./gradlew dependencyInsight --dependency bluetape4k-core` |
| Inspect a configuration's resolved graph | `./gradlew dependencies --configuration runtimeClasspath` |

## Patterns {#patterns}

Declare the platform once in a convention plugin or version catalog bundle used by all service modules. Keep the BOM version explicit at that boundary and omit versions on individual bluetape4k dependencies. Upgrade the BOM as one reviewed change, then run the application's compile and integration tests.

## Integrations {#integrations}

Spring's dependency-management plugin and Gradle platforms can both influence resolution. Prefer native Gradle platform import unless a Spring-managed build has a documented convention. A version catalog can alias the BOM coordinate, but the catalog does not replace the platform constraints.

## Configuration {#configuration}

The BOM has no runtime properties. Snapshot consumption requires the Sonatype Central snapshots repository in the consuming build. Release consumers normally need only Maven Central.

## Failures {#failures}

A missing repository produces dependency-resolution errors before compilation. Importing a BOM version that does not contain a newly added module leaves that module without a constraint, so its dependency still needs a version. Conflicting enforced platforms can make resolution fail or select a line the application did not test.

## Operations {#operations}

There is no runtime component to monitor. Operational work happens during dependency governance: inspect resolved versions, track vulnerability upgrades, and test the whole application after changing the platform version.

## Testing {#testing}

The module has no Kotlin test source. Validate its published model with Gradle dependency reports and the repository publication configuration:

```bash
./gradlew :bluetape4k-bom:dependencies --no-configuration-cache
./gradlew :bluetape4k-bom:generatePomFileForBluetape4kPublication --no-configuration-cache
```

The second command generates publication metadata locally; it does not publish an artifact.

## Workshops {#workshops}

No dedicated workshop is registered. To verify alignment, create a small consumer build with the BOM and two modules, then inspect `runtimeClasspath` and repeat after upgrading only the BOM version.

## Limitations {#limitations}

The BOM aligns versions; it does not guarantee that every module fits the application's framework, JDK, database, or native-image constraints. Demos and examples are intentionally excluded because they are not published libraries.

## Sources {#sources}

- [Module README](../../../../bluetape4k/bom/README.md)
- [Platform constraint build](../../../../bluetape4k/bom/build.gradle.kts)
- [Repository publication group](../../../../gradle.properties)
- [Manual manifest](../../manifest.yaml)
