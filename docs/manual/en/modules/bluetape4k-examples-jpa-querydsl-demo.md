---
manualId: bluetape4k-examples-jpa-querydsl-demo
title: "JPA with Querydsl"
description: "A collection of examples for learning database query patterns using JPA and Querydsl."
kind: example
group: examples
learningOrder: 1430
---

# JPA with Querydsl

## Problem {#problem}

A collection of examples for learning database query patterns using JPA and Querydsl. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-examples-jpa-querydsl-demo` when the application needs the runnable entry point, required services, expected behavior, and the production pattern demonstrated. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This example project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:bluetape4k-examples-jpa-querydsl-demo`. Source directory: `examples/jpa-querydsl-demo`.

## Concepts {#concepts}

The first source-level concepts to inspect are `MemberDto`, `MemberSearchCondition`, `MemberTeamDto`, `TeamDto`, `dto-mapping`, `Member`, `Team`, and `MemberRepository`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :bluetape4k-examples-jpa-querydsl-demo:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`MemberDto`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/dto/MemberDto.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberSearchCondition`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/dto/MemberSearchCondition.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberTeamDto`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/dto/MemberTeamDto.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TeamDto`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/dto/TeamDto.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`dto-mapping`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/mapper/dto-mapping.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Member`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/model/Member.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Team`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/model/Team.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberRepository`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/repository/MemberRepository.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberRepositoryCustom`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/repository/MemberRepositoryCustom.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MemberRepositoryImpl`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/repository/MemberRepositoryImpl.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Examples**, **Querydsl Basics (examples/)**, **Common Query Patterns**, **Basic Queries**, **JPQL vs Querydsl**, **Projections (DTO Queries)**, **Dynamic Queries**, **Subqueries**, **Domain Model**, and **Entities (domain/model/)**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(project(":bluetape4k-hibernate"))
implementation(libs.jakarta.annotation.api)
implementation(libs.jakarta.persistence.api)
implementation(libs.hibernate.core)
implementation(libs.querydsl.jpa)
implementation(libs.hibernate.validator)
runtimeOnly(libs.jakarta.validation.api)
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-validation")
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
./gradlew :bluetape4k-examples-jpa-querydsl-demo:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractQuerydslTest`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/AbstractQuerydslTest.kt)
- [`QuerydslApplication`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/QuerydslApplication.kt)
- [`TestEntityManager`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/TestEntityManager.kt)
- [`AbstractDomainTest`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/AbstractDomainTest.kt)
- [`JpaRepositoryTest`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/repository/JpaRepositoryTest.kt)
- [`QuerydslExamples`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/examples/QuerydslExamples.kt)
- [`InitMemberService`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/services/InitMemberService.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### JPA Querydsl demo structure

[![JPA Querydsl demo structure](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-jpa-querydsl-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-jpa-querydsl-demo-diagram-01.svg)

_Release README: [`examples/jpa-querydsl-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/jpa-querydsl-demo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../examples/jpa-querydsl-demo/README.md)
- [Module build](../../../../examples/jpa-querydsl-demo/build.gradle.kts)
- [`MemberDto`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/dto/MemberDto.kt)
- [`MemberSearchCondition`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/dto/MemberSearchCondition.kt)
- [`MemberTeamDto`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/dto/MemberTeamDto.kt)
- [`TeamDto`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/dto/TeamDto.kt)
- [`dto-mapping`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/mapper/dto-mapping.kt)
- [`Member`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/model/Member.kt)
- [`Team`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/model/Team.kt)
- [`MemberRepository`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/repository/MemberRepository.kt)
- [`MemberRepositoryCustom`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/repository/MemberRepositoryCustom.kt)
- [`MemberRepositoryImpl`](../../../../examples/jpa-querydsl-demo/src/main/kotlin/io/bluetape4k/examples/jpa/querydsl/domain/repository/MemberRepositoryImpl.kt)
- [`AbstractQuerydslTest`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/AbstractQuerydslTest.kt)
- [`QuerydslApplication`](../../../../examples/jpa-querydsl-demo/src/test/kotlin/io/bluetape4k/examples/jpa/querydsl/QuerydslApplication.kt)
