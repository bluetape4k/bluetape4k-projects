---
manualId: bluetape4k-rule-engine
title: "bluetape4k-rule-engine"
description: "A lightweight rule engine library for Kotlin. It follows the Easy Rules pattern and adds Kotlin DSLs, coroutine support (SuspendRule), and annotation-based rule definitions."
kind: library
group: utilities
---

# bluetape4k-rule-engine

## Problem {#problem}

A lightweight rule engine library for Kotlin. It follows the Easy Rules pattern and adds Kotlin DSLs, coroutine support (SuspendRule), and annotation-based rule definitions. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-rule-engine` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-rule-engine")
}
```

Gradle project path: `:bluetape4k-rule-engine`. Source directory: `utils/rule-engine`.

## Concepts {#concepts}

The first source-level concepts to inspect are `RuleDefaults`, `Action`, `Condition`, `Fact`, `Priority`, `Rule`, `Action`, and `Condition`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`RuleDefaults`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/RuleDefaults.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`RuleDefaults`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/RuleDefaults.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Action`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Action.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Condition`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Condition.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Fact`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Fact.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Priority`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Priority.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Rule`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Rule.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Action`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Action.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Condition`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Condition.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Facts`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Facts.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Rule`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Rule.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Concept Overview**, **Core Class Diagram**, **Rule Engine Class Diagram**, **Composite Rules**, **Rule Execution Sequence**, **InferenceRuleEngine (Forward Chaining)**, **Rule Engine Selection Guide**, **Core Features**, and **Usage Examples**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
implementation(project(":bluetape4k-coroutines"))
implementation(libs.kotlinx.coroutines.core)
implementation(platform(libs.spring.boot.dependencies))
compileOnly("org.springframework:spring-expression")
compileOnly(libs.mvel2)
compileOnly(libs.janino)
compileOnly(libs.janino.commons.compiler)
compileOnly(libs.groovy)
compileOnly(libs.kotlin.scripting.common)
compileOnly(libs.kotlin.scripting.jvm)
compileOnly(libs.kotlin.scripting.jvm.host)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Measure hot paths, bound input sizes, and monitor failures at the application boundary that calls the utility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-rule-engine:test --no-configuration-cache
```

Representative test anchors:

- [`ConditionTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/api/ConditionTest.kt)
- [`FactsTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/api/FactsTest.kt)
- [`RuleDefinitionTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/api/RuleDefinitionTest.kt)
- [`RuleEngineConfigTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/api/RuleEngineConfigTest.kt)
- [`ActionMethodOrderBeanTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/core/ActionMethodOrderBeanTest.kt)
- [`DefaultRuleEngineListenerTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/core/DefaultRuleEngineListenerTest.kt)
- [`DefaultRuleEngineTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/core/DefaultRuleEngineTest.kt)
- [`DefaultRuleListenerTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/core/DefaultRuleListenerTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../utils/rule-engine/README.md)
- [Module build](../../../../utils/rule-engine/build.gradle.kts)
- [`RuleDefaults`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/RuleDefaults.kt)
- [`Action`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Action.kt)
- [`Condition`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Condition.kt)
- [`Fact`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Fact.kt)
- [`Priority`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Priority.kt)
- [`Rule`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/annotation/Rule.kt)
- [`Action`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Action.kt)
- [`Condition`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Condition.kt)
- [`Facts`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Facts.kt)
- [`Rule`](../../../../utils/rule-engine/src/main/kotlin/io/bluetape4k/rule/api/Rule.kt)
- [`ConditionTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/api/ConditionTest.kt)
- [`FactsTest`](../../../../utils/rule-engine/src/test/kotlin/io/bluetape4k/rule/api/FactsTest.kt)
