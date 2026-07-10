# Floci Launcher Reuse Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure the standard Floci test Launcher never requests Testcontainers reuse.

**Architecture:** Preserve the public direct-construction default and apply the
non-reuse invariant only at the lazy Launcher boundary. This keeps one container
per module test JVM while preventing cross-process Docker reuse.

**Tech Stack:** Kotlin, Testcontainers 2.0.5, JUnit Jupiter, bluetape4k assertions.

---

### Task 1: Lock the Launcher policy with a regression test

**Files:**
- Modify: `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/aws/FlociServerTest.kt`

- [ ] **Step 1: Add a Launcher reuse-policy test**

```kotlin
@Test
fun `Floci Launcher disables Testcontainers reuse`() {
    val server = FlociServer.Launcher.floci
    server.isRunning.shouldBeTrue()
    server.isReuseRequested.shouldBeFalse()
}
```

- [ ] **Step 2: Run the focused test before the implementation**

Run: `./gradlew :bluetape4k-testcontainers:test --tests '*FlociServerTest' --no-daemon`

Expected: FAIL because the current Launcher constructs `FlociServer()` with
the public `reuse=true` default.

### Task 2: Make the Launcher non-reusable

**Files:**
- Modify: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/FlociServer.kt`

- [ ] **Step 1: Construct the lazy Launcher server explicitly**

```kotlin
val floci: FlociServer by lazy {
    FlociServer(reuse = false).apply {
        start()
        ShutdownQueue.register(this)
    }
}
```

- [ ] **Step 2: Run the focused Floci suite**

Run: `./gradlew :bluetape4k-testcontainers:test --tests '*FlociServerTest' --no-daemon`

Expected: PASS with the same startup behavior and no reusable Launcher request.

### Task 3: Verify and prepare snapshot consumption

**Files:**
- Verify: `gradle.properties`
- Verify: `testing/testcontainers/build.gradle.kts`

- [ ] **Step 1: Run compile and focused test checks sequentially**

Run: `./gradlew :bluetape4k-testcontainers:compileKotlin :bluetape4k-testcontainers:test --tests '*FlociServerTest' --no-daemon`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Validate the snapshot artifact coordinates**

Run: `./gradlew :bluetape4k-testcontainers:publishToMavenLocal -PsnapshotVersion=-SNAPSHOT --no-daemon`

Expected: the local Maven repository contains
`io.github.bluetape4k:bluetape4k-testcontainers:1.11.1-SNAPSHOT`.

- [ ] **Step 3: Check the final diff**

Run: `git diff --check`

Expected: no whitespace errors.
