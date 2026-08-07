---
manualId: bluetape4k-bom
title: "Bluetape4k Bill of Materials"
description: Understand the bluetape4k-projects constraints composed by the central BOM and how maintainers validate them.
kind: library
group: foundation
learningOrder: 100
---

# Bluetape4k Bill of Materials

## Problem {#problem}

An application often uses several bluetape4k modules. Repeating versions makes upgrades noisy and can produce a classpath assembled from releases that were not tested together. `bluetape4k-bom` publishes Gradle platform constraints for the repository's publishable modules.

## When to use {#when-to-use}

Normal applications do not choose this repository BOM directly. They import `bluetape4k-dependencies`, which composes `bluetape4k-bom` and the other repository BOMs as a tested set.

Work with `bluetape4k-bom` directly when publishing the repository, inspecting platform constraints, or reproducing a dependency conflict. The rest of this page describes that maintainer-facing surface.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-core")
    implementation("io.github.bluetape4k:bluetape4k-coroutines")
}
```

The `<version>` above is the published version of `bluetape4k-dependencies`. Applications do not declare the internal `bluetape4k-bom` version separately.

Use `enforcedPlatform(...)` only when the application intentionally wants BOM constraints to override every competing version. Library builds should normally use `platform(...)` so consumers retain dependency-resolution control.

## Concepts {#concepts}

The module applies Gradle's `java-platform` plugin. Its publication contains dependency constraints, not runtime classes. The build derives constraints from root subprojects and excludes the BOM itself, workshops, `examples`, and `-demo` projects.

## Quick start {#quick-start}

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
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

Declare the central platform once in a convention plugin or version catalog bundle used by all service modules. Keep only the `bluetape4k-dependencies` version explicit at that boundary and omit versions on individual bluetape4k dependencies. Upgrade the central BOM as one reviewed change, then run the application's compile and integration tests.

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

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### bluetape4k-bom platform constraints diagram

[![bluetape4k-bom platform constraints diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-bom-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/bluetape4k-bom-diagram-01.svg)

_Release README: [`bluetape4k/bom/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/bluetape4k/bom/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../bluetape4k/bom/README.md)
- [Platform constraint build](../../../../bluetape4k/bom/build.gradle.kts)
- [Repository publication group](../../../../gradle.properties)
- [Manual manifest](../../manifest.yaml)
