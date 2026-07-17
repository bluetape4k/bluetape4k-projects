---
manualId: bluetape4k-annotations
title: "API Maturity Annotations"
description: Apply explicit opt-in contracts to experimental, internal, delicate, obsolete, beta, and implementation-sensitive APIs.
kind: library
group: foundation
learningOrder: 120
---

# API Maturity Annotations

## Problem {#problem}

Kotlin visibility cannot express every compatibility boundary. A declaration may need to stay public for inlining or framework integration while still being unsafe to call, unstable to implement, or scheduled for removal. `bluetape4k-annotations` makes that boundary visible to the compiler with `@RequiresOptIn` markers.

![Bluetape API marker selection map](../../assets/annotations/annotation-decision-map.svg)

### Reading path

| Question | Chapter |
| --- | --- |
| Which marker fits this contract? | [Marker selection](./bluetape4k-annotations/marker-selection.md) |
| Should `@OptIn` live on a function, class, or file? | [Opt-in scope](./bluetape4k-annotations/opt-in-scope.md) |
| How do I permit calls but restrict third-party implementations? | [Implementation-sensitive SPI](./bluetape4k-annotations/implementation-spi.md) |
| What happens when an API stabilizes or becomes obsolete? | [Compatibility lifecycle](./bluetape4k-annotations/compatibility-lifecycle.md) |
| Where are complete declaration and caller examples? | [Practical recipes](./bluetape4k-annotations/recipes.md) |

## When to use {#when-to-use}

Use a marker when a public declaration needs a contract stronger than prose. Choose `BluetapeExperimentalApi` for unstable use sites, `BluetapeInternalApi` for technically public internals, `BluetapeDelicateApi` for lifecycle or security-sensitive calls, `BluetapeObsoleteApi` for migration-only APIs, and `BluetapeBetaApi` for APIs expected to stabilize. Use `BluetapeImplementationApi` on SPI types that are safe to call but not yet safe to subclass.

Do not annotate a stable API merely to avoid compatibility work. If the restriction can be represented with Kotlin visibility, prefer visibility.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-annotations")
}
```

The annotations have binary retention, so downstream compilation sees them without adding runtime behavior.

## Concepts {#concepts}

Error-level markers (`Experimental`, `Internal`, `Obsolete`) require explicit `@OptIn`. Warning-level markers (`Beta`, `Delicate`) surface risk while allowing compilation. `BluetapeImplementationApi` uses `@SubclassOptInRequired`, which separates “calling an interface” from “implementing the interface.”

## Quick start {#quick-start}

```kotlin
import io.bluetape4k.annotations.BluetapeExperimentalApi

@BluetapeExperimentalApi
fun unstablePlan(): String = "draft"

@OptIn(BluetapeExperimentalApi::class)
fun evaluateDraft(): String = unstablePlan()
```

Keep the opt-in scope as narrow as practical; a file-level opt-in can hide unrelated unstable calls.

## API by task {#api-by-task}

| Task | Marker |
| --- | --- |
| Publish an API that may change without compatibility guarantees | `BluetapeExperimentalApi` |
| Expose a declaration only for technical reasons | `BluetapeInternalApi` |
| Warn about lifecycle, concurrency, resource, or security obligations | `BluetapeDelicateApi` |
| Keep an API only for migration | `BluetapeObsoleteApi` |
| Signal a likely-to-stabilize API | `BluetapeBetaApi` |
| Allow calls but restrict third-party implementations | `BluetapeImplementationApi` |

## Patterns {#patterns}

Put the marker on the declaration that owns the unstable contract. Propagation is intentional: a public wrapper around an experimental API should either opt in internally and expose a stable contract, or carry the marker itself. For SPI, annotate the base class or interface so implementations—not callers—receive the warning.

## Integrations {#integrations}

The module depends only on Kotlin annotation semantics and can be used by every other bluetape4k module. Java callers see the annotation metadata but do not receive Kotlin's opt-in diagnostics, so Java-facing documentation must still state the restriction.

## Configuration {#configuration}

There is no runtime configuration. Build configuration consists of dependency selection and, when deliberately accepting a contract across a bounded source set, Kotlin compiler opt-in flags. Prefer source-level `@OptIn` because it records the exact acceptance site.

## Failures {#failures}

Error-level markers fail Kotlin compilation when a use site has not opted in. Warning-level markers emit diagnostics. Removing a marker annotation class can break downstream source that names it in `@OptIn`, even after all bluetape4k declarations stop using that marker.

## Operations {#operations}

The artifact adds no threads, I/O, allocation lifecycle, or runtime service. Its operational risk is compatibility governance: teams should review opt-ins during upgrades instead of suppressing them globally and forgetting the accepted risk.

## Testing {#testing}

`BluetapeApiMarkersTest` verifies retention, targets, diagnostic level, and the special subclass opt-in contract. Run:

```bash
./gradlew :bluetape4k-annotations:test --no-configuration-cache
```

## Workshops {#workshops}

No dedicated workshop is registered. The smallest useful experiment is a two-module build where one module publishes a marked declaration and the other verifies the expected compiler diagnostic and narrow `@OptIn` fix.

## Limitations {#limitations}

Markers communicate policy; they do not provide binary compatibility analysis or enforce contracts for Java callers. They also do not replace deprecation annotations when a replacement API and migration message exist.

## Sources {#sources}

- [Module README](../../../../bluetape4k/annotations/README.md)
- [Annotation source directory](../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations)
- [Marker contract tests](../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
- [Module build](../../../../bluetape4k/annotations/build.gradle.kts)
