# Bluetape API maturity annotations implementation plan

## Decision

Create `:bluetape4k-annotations` under `bluetape4k/annotations` as a standalone low-level module with no bluetape4k module dependencies.

## Tasks

1. Module skeleton
    - Complexity: small.
    - Skill: apply `$bluetape4k-patterns` for Gradle/test-resource conventions.
    - Expected files:
        - `bluetape4k/annotations/build.gradle.kts`
        - `bluetape4k/annotations/src/test/resources/junit-platform.properties`
        - `bluetape4k/annotations/src/test/resources/logback-test.xml`
    - Dependency assumptions:
        - production code has no bluetape4k module dependency;
        - test code may use existing project test conventions.
    - Verification:
        - `./gradlew projects --console=plain --no-configuration-cache` lists
          `:bluetape4k-annotations`.

2. Annotation implementation
    - Complexity: medium.
    - Skill: apply `$bluetape4k-patterns` for Kotlin API, KDoc, tests, and diagnostics.
    - Expected files:
        - `BluetapeExperimentalApi.kt`
        - `BluetapeBetaApi.kt`
        - `BluetapeInternalApi.kt`
        - `BluetapeDelicateApi.kt`
        - `BluetapeObsoleteApi.kt`
        - `BluetapeImplementationApi.kt`
    - Package: `io.bluetape4k.annotations`.
    - Shared requirements:
        - `@RequiresOptIn`;
        - `AnnotationRetention.BINARY`;
        - `@MustBeDocumented`;
        - no constructor parameters;
        - English KDoc with contract and usage guidance.
    - Marker levels:
        - `BluetapeExperimentalApi`: `RequiresOptIn.Level.ERROR`;
        - `BluetapeBetaApi`: `RequiresOptIn.Level.WARNING`;
        - `BluetapeInternalApi`: `RequiresOptIn.Level.ERROR`;
        - `BluetapeDelicateApi`: `RequiresOptIn.Level.WARNING`;
        - `BluetapeObsoleteApi`: `RequiresOptIn.Level.ERROR`;
        - `BluetapeImplementationApi`: `RequiresOptIn.Level.WARNING`.
    - Normal use-site marker targets:
        - `CLASS`, `ANNOTATION_CLASS`, `CONSTRUCTOR`, `FUNCTION`, `PROPERTY`,
          `TYPEALIAS`.
    - `BluetapeImplementationApi` targets:
        - `CLASS`, `ANNOTATION_CLASS`;
        - KDoc must say it is for `@SubclassOptInRequired`, not a generic function/property marker.
    - Verification:
        - IDE diagnostics on touched Kotlin files;
        - `./gradlew :bluetape4k-annotations:compileKotlin --console=plain --no-configuration-cache`.

3. Compile-time smoke tests
    - Complexity: small.
    - Skill: apply `$bluetape4k-patterns` and test conventions.
    - Expected files:
        - `bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt`
    - Test requirements:
        - reference all six marker types;
        - prove `@OptIn` compiles for normal markers;
        - prove `@SubclassOptInRequired(BluetapeImplementationApi::class)` compiles with a local opt-in implementation.
    - Verification:
        - `./gradlew :bluetape4k-annotations:test --console=plain --no-configuration-cache`.

4. Module and repository documentation
    - Complexity: medium.
    - Skill: apply `$bluetape4k-patterns` for public KDoc and README rules.
    - Expected files:
        - `bluetape4k/annotations/README.md`
        - `bluetape4k/annotations/README.ko.md`
        - `README.md`
        - `README.ko.md`
        - `bluetape4k/bom/README.md`
        - `bluetape4k/bom/README.ko.md`
    - Docs impact:
        - add language switch in module README pair;
        - explain dependency coordinates, marker catalog, retention, and
          `@SubclassOptInRequired` usage;
        - add root module-list entry;
        - update BOM family count and examples.
    - Verification:
        - grep source marker names against module README and BOM README examples.

5. Registration, CI, and Nightly
    - Complexity: medium.
    - Expected files:
        - `.github/workflows/ci.yml`
        - `.github/workflows/nightly-tests.yml`
    - Registration requirements:
        - `settings.gradle.kts` remains unchanged because
          `includeModules("bluetape4k", true, false)` auto-registers the module;
        - verify BOM constraints are automatic through `bluetape4k/bom/build.gradle.kts`;
        - add `bluetape4k/annotations/**` to CI `core` path filter;
        - add `:bluetape4k-annotations:test` to CI and nightly core test jobs;
        - add `:bluetape4k-annotations:koverXmlReport` to CI and nightly core coverage jobs;
        - keep coverage `needs` and artifact names unchanged because the module is included inside existing core artifacts.
    - Verification:
        - `actionlint`;
        - `rg -n "\\\\'" .github/workflows` returns no hits.

6. Final local verification
    - Complexity: small.
    - Verification:
        - `./gradlew projects --console=plain --no-configuration-cache`;
        - `./gradlew :bluetape4k-annotations:compileKotlin :bluetape4k-annotations:test --console=plain --no-configuration-cache`;
        - `git diff --check`.

7. Step 6-R current-session review
    - Complexity: small.
    - Review rules:
        - no Claude CLI;
        - native subagent allowed;
        - resolve all P0/P1 before publication.

8. Lesson and commit
    - Complexity: small.
    - Expected files:
        - `docs/lessons/2026-05-20-bluetape-annotations.md`
    - Commit:
        - use English Lore commit protocol.

9. Push, PR, Step 7-R, and CI watch
    - Complexity: small.
    - PR rules:
        - assignee `debop`;
        - English title/body;
        - add current-session Step 7-R PR review/comment;
        - watch CI and fix failures when recoverable.

## Review Gates

- Step 2-R spec review: current-session review only, no Claude/Codex CLI.
- Step 3-R plan review: current-session review only, no Claude/Codex CLI.
- Step 6-R code review: current-session review/native subagent allowed.
- Step 7-R PR review: current-session formal GitHub review and PR comment.

## Risks

- Wrong retention or targets can make Kotlin reject opt-in marker annotations. Keep `BINARY` retention and declaration-only targets.
- Placing the module under `testing` would mislead users and weaken reuse. Keep it under `bluetape4k/annotations`.
- Missing CI/nightly registration can leave the new publishable module untested in automation.
