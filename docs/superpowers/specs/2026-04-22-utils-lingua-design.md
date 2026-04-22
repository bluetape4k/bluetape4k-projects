# utils/lingua Design Spec

## Summary

Promote the deprecated `x-obsoleted/lingua` module into `utils/lingua` as `bluetape4k-lingua`.
This work will restore the previous Kotlin DSL wrapper around `com.github.pemistahl:lingua` and add a new mixed-language convenience API that returns all detected languages as a `Set`.

The implementation intentionally uses the upstream `lingua` dependency again instead of a clean-room rewrite. This decision keeps scope small, preserves upstream detection quality, and avoids building a new language-detection engine and model pipeline.

## Goals

- Restore a supported `utils/lingua` module in the active multi-module build.
- Reuse `com.github.pemistahl:lingua` as the detection engine.
- Preserve the previous Bluetape4k-friendly Kotlin DSL for creating detectors.
- Add a new `detectAllLanguagesOf(text)` API that returns all detected languages as a `Set<Language>` for mixed-language text.
- Restore README and root module listings so `lingua` is no longer shown as dropped.

## Non-Goals

- Clean-room reimplementation of Lingua.
- Custom language models or custom training pipelines.
- Confidence-based candidate language APIs.
- Public span/segment APIs for mixed-language detection results.
- Additional abstraction layer hiding upstream `Language` types.

## Context and Research Findings

### Existing Repository State

- `TODO.md:81` explicitly tracks `lingua -> utils/lingua` promotion.
- `x-obsoleted/lingua` contains three main Kotlin source files and tests:
  - `LanguageDetector.kt` — Kotlin DSL wrapper around upstream `lingua`
  - `UnicodeDetector.kt` — script filtering helper with `KLogging`
  - `UnicodeSupport.kt` — character classification extensions
- `x-obsoleted/lingua/build.gradle.kts` previously depended on `com.github.pemistahl:lingua:1.2.2`.
- `README.md:372` and `README.ko.md:369` still list `lingua` as dropped.
- `settings.gradle.kts` auto-registers subdirectories under `utils/`, so `utils/lingua` will become `bluetape4k-lingua` automatically.
- `buildSrc/src/main/kotlin/Libs.kt` currently has no `lingua` entry, so this work must either add `Versions.lingua` / `Libs.lingua` or deliberately inline the dependency version with justification.
- Existing `utils/geo` provides the canonical README and test-resource template for this promotion.

### Upstream Capability Findings

Repository/source verification against upstream `pemistahl/lingua` `v1.2.2` and `main` confirmed these relevant capabilities:

- Builder-based detector creation with language subset selection.
- `detectLanguageOf(text)` for single-language detection.
- `computeLanguageConfidenceValues(text)` for confidence inspection on a single text input.
- `withPreloadedLanguageModels()` for eager model loading.
- `withLowAccuracyMode()` for lower memory usage on long texts.
- `withMinimumRelativeDistance(...)` to tune conservative detection behavior.

The same source verification also confirmed that upstream does **not** expose a public `detectMultipleLanguagesOf(text)` mixed-language segmentation API in the released version used by this module.

### Baseline Verification

Worktree baseline was created at `.worktrees/feat-utils-lingua`.
A baseline validation command succeeded from the worktree context:

```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-core:test
```

## Design Risks and Failure Modes

1. Mixed-language detection can be conservative on short or ambiguous text.
2. Upstream `lingua` `v1.2.2` has no public mixed-language segmentation API, so the wrapper must use a token-based heuristic and pin its behavior with tests. Short Latin tokens can also produce false positives (for example, `Hello -> SOTHO`), so the heuristic may need a narrow ambiguity correction.
3. Module promotion can be incomplete if README, README.ko, root README, testlog, superpowers index, or TODO updates are missed.
4. `withPreloadedLanguageModels()` can increase memory usage significantly, so eager loading must remain opt-in and never be forced by the wrapper.
5. Upstream model resources increase artifact size and may slow cold startup, so compile/test and README guidance must reflect that detectors should be reused instead of rebuilt repeatedly.

## Approach Options Considered

### Option A — Thin wrapper restoration around upstream Lingua (recommended)

Restore the old module structure, keep upstream public types such as `Language`, and add one new extension API for mixed-language `Set` results.

**Pros**
- Smallest scope.
- Highest confidence in detection quality.
- Minimal migration risk.
- Keeps existing examples and DSL largely intact.

**Cons**
- Public API remains tied to upstream `Language` types.

### Option B — Facade wrapper over upstream Lingua

Wrap upstream internally but expose Bluetape4k-owned language types.

**Pros**
- Future engine replacement becomes easier.

**Cons**
- Larger scope.
- Adds conversion code, extra tests, and more API design work.
- Not necessary for the current promotion goal.

### Option C — Minimal heuristic helper over upstream single-language detection

Restore only the module dependency and expose a small helper that tokenizes text and runs upstream `detectLanguageOf(text)` repeatedly for each token.

**Pros**
- Uses only public upstream APIs that actually exist in `v1.2.2`.
- Keeps implementation scope relatively small.

**Cons**
- Heuristic behavior on short tokens must be pinned by wrapper tests.
- Does not provide segment/span details.
- Less direct than an upstream segmentation API would have been.

## Decision

Choose **Option A**.
The module will be restored as a thin Kotlin DSL wrapper over upstream `lingua`, with one new mixed-language convenience API.

## Module Structure

### Module Path

- `utils/lingua`

### Gradle Coordinates

- module name: `bluetape4k-lingua`

### Dependencies

- `api(project(":bluetape4k-core"))`
- `api(Libs.lingua)` backed by `Versions.lingua` / `Libs.lingua` in `buildSrc/src/main/kotlin/Libs.kt`
- `testImplementation(project(":bluetape4k-junit5"))`

The implementation should start from the previously used version `1.2.2` unless current repository dependency policy requires a newer pinned version. The exact version must be committed through `Libs.kt`, not left unresolved in module build scripts.

### Test Resources

The new module must include the standard Bluetape4k test resources:

- `src/test/resources/junit-platform.properties`
- `src/test/resources/logback-test.xml`

Additional test/resource setup should follow existing Bluetape4k module conventions.

## Scope of Restored Source Files

- `LanguageDetector.kt` — **Restore and extend**. This file is the core wrapper DSL and will host the new mixed-language extension or delegate to a focused extension file.
- `UnicodeDetector.kt` — **Restore unchanged or near-unchanged**. It is independently useful, already follows Bluetape4k logging style, and should not be silently dropped during promotion.
- `UnicodeSupport.kt` — **Restore unchanged or near-unchanged**. It is a small reusable helper with no dependency on upstream detector internals.

These files are part of the promotion scope because the TODO item refers to the existing deprecated module as a whole, not only the detector factory DSL.

## Bluetape4k Conventions

- All public classes, interfaces, and extension functions must have Korean KDoc.
- Any concrete class introduced in this module should use `companion object : KLogging()` when logging is needed.
- Existing `UnicodeDetector` logging should be preserved.
- The new `detectAllLanguagesOf(text)` contract intentionally returns `emptySet()` for blank input rather than throwing. This is a deliberate API choice and should be documented in KDoc as an exception to stricter `requireNotBlank` style validation.
- Public APIs should use immutable collections such as `Set<Language>`.

## Public API Design

### Restored Detector DSL APIs

The implementation should preserve the previous DSL style as much as possible:

- `allLanguageDetector(builder)`
- `allLanguageWithoutDetector(languages, builder)`
- `allSpokenLanguageDetector(builder)`
- `languageDetectorOf(languages, builder)` for `Set<Language>`
- `languageDetectorOf(isoCodes, builder)` for `Set<IsoCode639_1>`
- `languageDetectorOf(isoCodes, builder)` for `Set<IsoCode639_3>`
- Convenience overload restoring the simple builder-free call style if practical

### New Mixed-Language API

Add a new extension API:

```kotlin
fun LanguageDetector.detectAllLanguagesOf(text: String): Set<Language>
```

### Contract for `detectAllLanguagesOf`

- If `text.isBlank()`, return `emptySet()`.
- Split `text` into candidate tokens using Unicode-letter sequences while retaining in-word apostrophes and hyphens.
- Call upstream `detectLanguageOf(token)` for each candidate token.
- Remove `UNKNOWN` results.
- For short Latin tokens only, allow a narrow ambiguity correction when Lingua's top confidence candidates are close (for example, observed `"Hello" -> SOTHO` false positive). This correction remains internal to the wrapper and does not expose confidence values.
- If at least one non-`UNKNOWN` language remains, return unique values as `Set<Language>`.
- If no usable token result remains:
  - call `detectLanguageOf(text)` as fallback
  - if result is not `UNKNOWN`, return a singleton set
  - otherwise return `emptySet()`.
- The wrapper does **not** expose segment/span data or confidence values in this PR.

### Rejected Output Shapes

- `List<Language>` ordered by first occurrence was rejected because the approved requirement is a set of detected languages, not ordered segments.
- Public segment/span exposure was rejected for this PR to keep the wrapper surface small; upstream `v1.2.2` does not expose a public mixed-language segmentation API, so richer span data would require separate custom wrapper design.

### Expected Behavior Examples

- `"Hello" -> {ENGLISH}`
- `"Hello 안녕" -> {ENGLISH, KOREAN}`
- `"Hello 안녕 こんにちは" -> {ENGLISH, KOREAN, JAPANESE}`
- `"" -> emptySet()`
- unrecognizable text -> `emptySet()`

## Data Flow

1. User creates a detector using the Kotlin DSL wrapper.
2. Detector creation delegates to upstream `LanguageDetectorBuilder`.
3. `detectLanguageOf(text)` remains upstream behavior.
4. `detectAllLanguagesOf(text)` tokenizes mixed text, detects each token with upstream single-language detection, then falls back to whole-text detection when needed.
5. The public output for mixed detection is intentionally simplified to `Set<Language>`.

## Testing Strategy

### Unit / API Tests

Create tests covering:

- detector builder DSL restoration
- detector creation from `Language`, `IsoCode639_1`, and `IsoCode639_3`
- convenience overload behavior
- single-language detection examples
- mixed-language `Set` detection
- blank input returning `emptySet()`
- unknown/unrecognized input returning `emptySet()`

### Required Mixed-Language Cases

- `"Hello 안녕" -> {ENGLISH, KOREAN}`
- `"Hello 안녕 こんにちは" -> {ENGLISH, KOREAN, JAPANESE}`

### Regression / Compatibility Checks

- old example-style usage should still compile
- no stale imports from deprecated module paths
- no unresolved references to removed wrappers

## Documentation Changes

Update all affected docs in sync:

- `utils/lingua/README.md`
- `utils/lingua/README.ko.md`
- root `README.md`
- root `README.ko.md`

README requirements:

- follow the `utils/geo/README.md` template
- use `# Module bluetape4k-lingua` / `# Module bluetape4k-lingua` style H1 headers
- language switch links directly below the title
- Mermaid UML section
- Architecture -> UML -> Features -> Examples structure
- include dependency installation snippet
- examples must be runnable and match the actual public API

## Required Project Hygiene Updates

Because this is superpowers work, the implementation must also update:

- `docs/testlogs/2026-04.md`
- `docs/superpowers/index/2026-04.md`
- `docs/superpowers/INDEX.md`
- `TODO.md:81` checkbox state

If module listings or module tables require it, update `CLAUDE.md` as well.

## Verification Commands

These commands must be reflected in the implementation and verification plan:

```bash
./gradlew :bluetape4k-lingua:compileKotlin
./gradlew :bluetape4k-lingua:compileTestKotlin
./bin/repo-test-summary -- ./gradlew :bluetape4k-lingua:test
```

Additional checks:

```bash
./gradlew -p . build -x test
```

The implementation should also verify that root documentation no longer marks `lingua` as dropped.

## Acceptance Criteria

The work is complete when all of the following are true:

1. `utils/lingua` is an active module in the build.
2. The module compiles and its tests pass.
3. The restored DSL APIs compile and work.
4. `LanguageDetector.detectAllLanguagesOf(text)` exists and returns correct `Set<Language>` results for mixed-language text.
5. Blank or unrecognized input returns `emptySet()`.
6. Module README and root README files are updated in both English and Korean.
7. `docs/testlogs/2026-04.md` contains the test execution record.
8. `docs/superpowers/index/2026-04.md` and `docs/superpowers/INDEX.md` are updated.

## Out of Scope for This PR

The following are intentionally deferred:

- candidate-language APIs based on confidence thresholds
- public span/segment result APIs
- internal replacement of upstream `Language` types
- clean-room engine implementation
- model training or packaging work
