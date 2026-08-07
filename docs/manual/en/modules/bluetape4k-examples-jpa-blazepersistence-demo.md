---
manualId: bluetape4k-examples-jpa-blazepersistence-demo
title: "JPA with Blaze-Persistence"
description: "This module demonstrates JPA query patterns with Blaze Persistence: Criteria Builder, Entity Views, offset pagination, keyset pagination, and count metadata."
kind: example
group: examples
learningOrder: 1440
---

# JPA with Blaze-Persistence

## Problem {#problem}

This module demonstrates JPA query patterns with Blaze Persistence: Criteria Builder, Entity Views, offset pagination, keyset pagination, and count metadata. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-examples-jpa-blazepersistence-demo` when the application needs the runnable entry point, required services, expected behavior, and the production pattern demonstrated. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This example project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:bluetape4k-examples-jpa-blazepersistence-demo`. Source directory: `examples/jpa-blazepersistence-demo`.

## Concepts {#concepts}

The first source-level concepts to inspect are `BlazePersistenceConfiguration`, `MemberPage`, `MemberSearchCondition`, `Member`, `Team`, `MemberBlazeRepository`, `MemberSummaryView`, and `MemberTeamView`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`BlazePersistenceConfiguration`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/config/BlazePersistenceConfiguration.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberPage`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/dto/MemberPage.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberSearchCondition`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/dto/MemberSearchCondition.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Member`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/model/Member.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Team`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/model/Team.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberBlazeRepository`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/repository/MemberBlazeRepository.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberSummaryView`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/view/MemberSummaryView.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberTeamView`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/view/MemberTeamView.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Why Blaze Persistence**, **Example Coverage**, **Domain Model and Read Path**, **Core Usage**, **Entity View Registration**, **Dynamic Criteria Query**, **Entity View Pagination**, **Querydsl Migration Notes**, **Dependencies**, and **How to Run**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(project(":bluetape4k-hibernate"))
implementation(libs.jakarta.annotation.api)
implementation(libs.jakarta.persistence.api.v32)
implementation(libs.hibernate.core)
implementation(libs.blaze.persistence.core.api.jakarta)
runtimeOnly(libs.blaze.persistence.core.impl.jakarta)
implementation(libs.blaze.persistence.entity.view.api.jakarta)
runtimeOnly(libs.blaze.persistence.entity.view.impl.jakarta)
implementation(libs.blaze.persistence.jpa.criteria.api.jakarta)
runtimeOnly(libs.blaze.persistence.jpa.criteria.impl.jakarta)
runtimeOnly(libs.blaze.persistence.integration.hibernate7)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Run the example in an isolated environment and observe startup, dependency health, requests, and shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractBlazePersistenceTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/AbstractBlazePersistenceTest.kt)
- [`BlazePersistenceApplication`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/BlazePersistenceApplication.kt)
- [`TestEntityManager`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/TestEntityManager.kt)
- [`AbstractDomainTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/AbstractDomainTest.kt)
- [`MemberBlazeRepositoryTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/repository/MemberBlazeRepositoryTest.kt)
- [`InitMemberService`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/services/InitMemberService.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Domain Model and Read Path diagram

[![Domain Model and Read Path diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-jpa-blazepersistence-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-jpa-blazepersistence-demo-diagram-01.svg)

_Release README: [`examples/jpa-blazepersistence-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/jpa-blazepersistence-demo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../examples/jpa-blazepersistence-demo/README.md)
- [Module build](../../../../examples/jpa-blazepersistence-demo/build.gradle.kts)
- [`BlazePersistenceConfiguration`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/config/BlazePersistenceConfiguration.kt)
- [`MemberPage`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/dto/MemberPage.kt)
- [`MemberSearchCondition`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/dto/MemberSearchCondition.kt)
- [`Member`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/model/Member.kt)
- [`Team`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/model/Team.kt)
- [`MemberBlazeRepository`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/repository/MemberBlazeRepository.kt)
- [`MemberSummaryView`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/view/MemberSummaryView.kt)
- [`MemberTeamView`](../../../../examples/jpa-blazepersistence-demo/src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/view/MemberTeamView.kt)
- [`AbstractBlazePersistenceTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/AbstractBlazePersistenceTest.kt)
- [`BlazePersistenceApplication`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/BlazePersistenceApplication.kt)
- [`TestEntityManager`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/TestEntityManager.kt)
- [`AbstractDomainTest`](../../../../examples/jpa-blazepersistence-demo/src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/domain/AbstractDomainTest.kt)
