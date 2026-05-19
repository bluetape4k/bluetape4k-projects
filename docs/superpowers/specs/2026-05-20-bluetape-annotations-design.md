# Bluetape API maturity annotations design

## Context

The bluetape4k ecosystem needs a small, stable annotation surface for public API
maturity and implementation-risk contracts. The annotations should support the
Kotlin library first, while leaving room for a future broader Bluetape brand
such as `bluetape.io` and non-Kotlin libraries.

## Goals

- Add a dependency-light `:bluetape4k-annotations` module.
- Provide opt-in markers for experimental, beta, internal, delicate, and
  implementation-only API contracts.
- Keep annotation names brand-oriented (`Bluetape`) instead of Kotlin-artifact
  oriented (`Bluetape4k`).
- Make the module usable by low-level modules such as logging, virtualthread,
  assertions, and future modules without depending on `:bluetape4k-core`.
- Register the module in build, CI, nightly, README, and BOM coverage.

## Non-goals

- Do not annotate existing bluetape4k APIs in this change.
- Do not add runtime annotation scanning behavior.
- Do not introduce external dependencies.
- Do not move existing modules or Maven coordinates.

## Module Placement

Use `bluetape4k/annotations`, which becomes Gradle project
`:bluetape4k-annotations` through the existing `includeModules("bluetape4k",
true, false)` rule.

Rationale:

- `:bluetape4k-core` already depends on `:bluetape4k-logging` and
  `:bluetape4k-virtualthread-api`, so putting annotations in core would force
  low-level modules upward.
- `testing/annotations` would imply test scope, but maturity annotations are
  general library API contracts.
- A standalone module can be depended on directly by any module that exposes
  these annotations in public signatures.

## Public API

Package: `io.bluetape4k.annotations`.

- `@ExperimentalBluetapeApi`: error-level opt-in for APIs that may change or be
  removed without compatibility guarantees.
- `@BetaBluetapeApi`: warning-level opt-in for APIs intended to stabilize, but
  still subject to minor source or behavior changes.
- `@InternalBluetapeApi`: error-level opt-in for public declarations exposed
  only for technical reasons.
- `@DelicateBluetapeApi`: warning-level opt-in for APIs requiring lifecycle,
  concurrency, security, or resource-management expertise.
- `@BluetapeImplementationApi`: warning-level marker intended for
  `@SubclassOptInRequired` on public SPI that is stable to use but not stable to
  implement or subclass.

All marker annotations must:

- use `@RequiresOptIn`;
- use `AnnotationRetention.BINARY`;
- use `@MustBeDocumented`;
- have no constructor parameters;
- target only declaration sites allowed for Kotlin opt-in markers.

The normal use-site marker target set is:

- `CLASS`
- `ANNOTATION_CLASS`
- `CONSTRUCTOR`
- `FUNCTION`
- `PROPERTY`
- `TYPEALIAS`

This target set applies to:

- `ExperimentalBluetapeApi`
- `BetaBluetapeApi`
- `InternalBluetapeApi`
- `DelicateBluetapeApi`

`BluetapeImplementationApi` is different. It exists for
`@SubclassOptInRequired` on public SPI classes and interfaces. Its target set
must stay class-oriented:

- `CLASS`
- `ANNOTATION_CLASS`

Its KDoc must state that it is not a generic function or property marker. Use
the other markers for ordinary unstable use-site APIs.

`FILE`, `TYPE`, `EXPRESSION`, and `TYPE_PARAMETER` are intentionally excluded
because Kotlin opt-in marker annotations cannot use them.

## Documentation

- Add `README.md` and `README.ko.md` for library users.
- Add the new module to the root README module list.
- Update the BOM README module count and examples list.
- KDoc for public annotations must be English and include usage guidance.

## Build And CI

- `./gradlew projects` should include `:bluetape4k-annotations`.
- `./gradlew :bluetape4k-annotations:compileKotlin
  :bluetape4k-annotations:test` should pass.
- Compile-time smoke tests should prove the marker annotations are usable with
  `@OptIn` and `@SubclassOptInRequired`.
- CI `core` path filter and test job should include `bluetape4k/annotations/**`
  and `:bluetape4k-annotations:test`.
- Nightly core job and coverage job should include `:bluetape4k-annotations`.
- BOM constraints should include the module automatically through existing
  `rootProject.subprojects` logic.

## Compatibility

This change adds a new artifact and does not change existing public APIs.
Consumers opt in only when future API declarations are annotated with these
markers.

Existing clients are unaffected unless they choose to depend on
`bluetape4k-annotations`.

When an annotated API graduates to stable, remove the marker from the API
declaration only after checking source and binary compatibility. Keep the marker
annotation class itself in the artifact unless a major version deliberately
removes it, because downstream code may still reference it in `@OptIn`
declarations.

## Source Evidence

- Kotlin official opt-in documentation: opt-in marker annotations require
  `@RequiresOptIn`, `BINARY` or `RUNTIME` retention, no parameters, and must not
  target `EXPRESSION`, `FILE`, `TYPE`, or `TYPE_PARAMETER`.
- Kotlin official `@SubclassOptInRequired` documentation: use it for interfaces
  and open/abstract classes that are stable to use but unstable, delicate, or
  closed to external implementation.
