# Coroutines Test Bridge Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove deprecated `io.bluetape4k.coroutines.tests` helpers and migrate repository call sites to `bluetape4k-assertions` and `bluetape4k-junit5`.

**Architecture:** Flow assertions already belong to `testing/assertions`; dispatcher helpers move to `testing/junit5`. Coroutines tests and examples import those owner packages directly.

**Tech Stack:** Kotlin 2.3, Gradle, kotlinx.coroutines, bluetape4k-assertions, bluetape4k-junit5.

---

## File Structure

- Create/modify: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupport.kt`
  - Add `withSingleThread` and `withParallels`.
- Delete: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/FlowAssertions.kt`
- Delete: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/TestSupport.kt`
- Delete: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/tests/FlowAssertionsBridgeTest.kt`
- Modify: coroutines tests and examples importing `io.bluetape4k.coroutines.tests.*`
  - Flow assertions -> `io.bluetape4k.assertions.coroutines.*`
  - dispatcher helpers -> `io.bluetape4k.junit5.coroutines.*`
- Add: `docs/lessons/2026-06-23-issue-879-coroutines-test-bridges.md`
- Add: `docs/review/2026-06-23-issue-879-coroutines-test-bridges-review.md`

### Task 1: Add Dispatcher Helpers To JUnit5

**Files:**
- Modify: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupport.kt`

- [ ] **Step 1: Add imports**

Add the required imports:

```kotlin
import io.bluetape4k.support.forEachCatching
import io.bluetape4k.support.requirePositiveNumber
import java.util.concurrent.TimeUnit
```

- [ ] **Step 2: Add helpers**

Append `withSingleThread` and `withParallels` with the same behavior as the old
bridge package, returning dispatchers to the test block and shutting down
executors afterward.

- [ ] **Step 3: Compile JUnit5**

Run:

```bash
./gradlew :bluetape4k-junit5:compileKotlin
```

Expected: build succeeds.

### Task 2: Migrate Imports And Delete Bridge

**Files:**
- Modify: all active Kotlin test/example files from `rg "io\.bluetape4k\.coroutines\.tests" -g '*.kt'`
- Delete: bridge files under `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests`
- Delete: bridge-only test file under `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/tests`

- [ ] **Step 1: Replace Flow assertion imports**

Replace imports for `assertEmpty`, `assertResult`, `assertResultSet`,
`assertFailure`, and `assertError` with:

```kotlin
import io.bluetape4k.assertions.coroutines.<function>
```

- [ ] **Step 2: Replace dispatcher helper imports**

Replace imports for `withSingleThread` and `withParallels` with:

```kotlin
import io.bluetape4k.junit5.coroutines.<function>
```

- [ ] **Step 3: Delete old bridge files**

Remove:

```text
bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/FlowAssertions.kt
bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/TestSupport.kt
bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/tests/FlowAssertionsBridgeTest.kt
```

- [ ] **Step 4: Check active references**

Run:

```bash
rg "io\.bluetape4k\.coroutines\.tests" -g '*.kt' -g '*.kts'
```

Expected: no matches.

### Task 3: Validate Behavior

**Files:**
- Test reports under Gradle build directories.

- [ ] **Step 1: Compile owner modules**

Run:

```bash
./gradlew :bluetape4k-junit5:compileKotlin :bluetape4k-assertions:compileKotlin
```

Expected: build succeeds.

- [ ] **Step 2: Test owner modules**

Run:

```bash
./gradlew :bluetape4k-junit5:test :bluetape4k-assertions:test
```

Expected: tests pass.

- [ ] **Step 3: Compile and test coroutines**

Run:

```bash
./gradlew :bluetape4k-coroutines:compileTestKotlin :bluetape4k-coroutines:test
```

Expected: tests pass. If `assertResultSet` failures appear, inspect duplicate
semantics before changing expected values.

- [ ] **Step 4: Compile example tests**

Run:

```bash
./gradlew :examples:coroutines-demo:compileTestKotlin
```

Expected: build succeeds.

- [ ] **Step 5: Static checks**

Run:

```bash
rg "io\.bluetape4k\.coroutines\.tests"
git diff --check
```

Expected: active references removed; whitespace check passes.

### Task 4: Evidence, Commit, PR

**Files:**
- Add: `docs/lessons/2026-06-23-issue-879-coroutines-test-bridges.md`
- Add: `docs/review/2026-06-23-issue-879-coroutines-test-bridges-review.md`

- [ ] **Step 1: Write lesson and review evidence**

Record migration decisions, duplicate-aware assertion risk, validation commands,
and P0/P1 = 0 local review verdict.

- [ ] **Step 2: Commit**

Use Lore commit trailers and include validation evidence in `Tested:`.

- [ ] **Step 3: Create PR**

Create a PR against `develop` with `--body-file`. Set assignee `debop`, copy
issue #879 labels and milestone to the PR, then verify with `gh pr view`.

- [ ] **Step 4: CI gate**

Wait for GitHub Actions. Update the PR body DoD after checks pass.
