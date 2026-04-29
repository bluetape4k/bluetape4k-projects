# bluetape4k-images-vips Implementation Plan

- **Spec**: `docs/superpowers/specs/2026-04-29-images-vips-design.md`
- **Issue**: #136
- **Branch**: `feat/images-vips`
- **Worktree**: `.worktrees/feat/images-vips`
- **Date**: 2026-04-29
- **Scope**: Phase 1 (skeleton) + Phase 2 (core API). Phase 3 (AVIF/HEIC, DZI, AutoImageProcessor) deferred.
- **Modules**: `utils/images-vips-api`, `utils/images-vips-java21`, `utils/images-vips-java25`

---

## Plan Overview

This plan delivers three new sibling modules under `utils/` mirroring the `virtualthread/{api,jdk21,jdk25}` pattern. Work proceeds in 6 task groups (T0–T5) with 41 atomic tasks total (38 original + T2.0 spike + T3.0 spike + T4.10 concurrency test). Each task carries a `complexity` label and explicit dependencies.

### Critical Path

```
T0 (scaffolding) → T1 (api) → T2 (java21) ─┐
                              T3 (java25) ─┼→ T4 (tests) → T5 (docs/CI)
                                            ┘
```

T2 and T3 can be developed **in parallel** once T1 is complete.

**Within T2**: T2.1 and T2.2 run in parallel → both must complete before T2.3 starts (T2.3 is the T2 bottleneck). T2.4–T2.9 run in parallel after T2.3.
**Within T3**: T3.1 → T3.2 (bottleneck) → T3.3–T3.8 in parallel.

### Task Count by Complexity

| Complexity | Count |
|---|---:|
| high | 11 |
| medium | 19 |
| low | 11 |
| **Total** | **41** |

> Note: T2.4, T3.3 complexity upgraded from `medium` → `high` (factory functions with security caps, exception safety, ~200+ LoC). T1.6 converted from incoherent "skip" to decision-recording task (low). Added T2.0 (JVips API spike), T3.0 (vips-ffm API spike), T4.10 (concurrent init test) — all `low` complexity.

---

## T0 — Build Scaffolding

Goal: Create the three module directories with `build.gradle.kts`, package skeletons, and `Libs.kt` entries so the project resolves before any source is written.

### T0.1 — Add Libs.kt versions and dependencies
- **complexity**: low
- **deps**: none
- **files**: `buildSrc/src/main/kotlin/Libs.kt`
- **details**:
  - Add `Versions.jvips = "8.10.2-38fe1f6"` and `Versions.vips_ffm = "1.9.6"`
  - Add `Libs.jvips = "com.criteo:jvips:${Versions.jvips}"` and `Libs.vips_ffm = "app.photofox.vips-ffm:vips-ffm-core:${Versions.vips_ffm}"`
- **DoD**: Project compiles with new Libs entries (referenced by T0.3/T0.4).

### T0.2 — Create images-vips-api module skeleton + build.gradle.kts
- **complexity**: medium
- **deps**: T0.1
- **files**:
  - `utils/images-vips-api/build.gradle.kts`
  - `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/.gitkeep`
  - `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/coroutines/.gitkeep`
  - `utils/images-vips-api/src/test/kotlin/io/bluetape4k/images/vips/.gitkeep`
  - `utils/images-vips-api/src/test/resources/junit-platform.properties`
  - `utils/images-vips-api/src/test/resources/logback-test.xml`
- **details**: Per spec §5.1. `api(project(":bluetape4k-images"))` — `api()` is correct here because `VipsImage.kt` and `VipsRuntime.kt` expose types from `bluetape4k-images` (specifically `@IncubatingImageApi`) as part of their own public API surface; consumers of `images-vips-api` need these types transitively on their compile classpath. `api(project(":bluetape4k-coroutines"))` — similarly, `SuspendVipsOps.kt` exposes suspend extension functions that require `Dispatchers.IO` from the coroutines API. `implementation(project(":bluetape4k-logging"))`, `testImplementation(project(":bluetape4k-junit5"))`.
- **DoD**: `./gradlew :bluetape4k-images-vips-api:build -x test` succeeds.

### T0.3 — Create images-vips-java21 module skeleton + build.gradle.kts
- **complexity**: medium
- **deps**: T0.1, T0.2
- **files**:
  - `utils/images-vips-java21/build.gradle.kts`
  - `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/{ops,writer,internal}/.gitkeep`
  - `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/.gitkeep`
  - `utils/images-vips-java21/src/test/resources/{junit-platform.properties,logback-test.xml}`
- **details**: Per spec §5.2. Java 21 toolchain, `kotlin { jvmToolchain(21) }`, `options.release.set(21)`, JUnit Platform `vips-required` exclude/include logic, `forkEvery = 1`, `maxParallelForks = 1` (prevents concurrent libvips JVMs on same host — forkEvery=1 alone does not prevent parallel forks), `implementation(Libs.jvips)` (**NOT** `api()` — D8: binding types are `internal`, must not leak to consumer compile classpath), `implementation(Libs.commons_io)`.
- **Pre-flight gate (MUST pass before T0.3 starts)**: Verify `com.criteo:jvips:8.10.2-38fe1f6` resolves from `mavenCentral()`. Run `./gradlew :bluetape4k-images-vips-java21:dependencies --configuration runtimeClasspath` and confirm `com.criteo:jvips:8.10.2-38fe1f6` appears in the output. If it fails to resolve: (a) check JitPack (`com.github.criteo:JVips:<commit>`) or GitHub Packages as fallback, (b) add the required repository to `buildSrc/` or root `build.gradle.kts`, (c) document the actual source in T0.1. **Do NOT proceed if resolution fails — the entire build depends on it.**
- **DoD**: `./gradlew :bluetape4k-images-vips-java21:build -x test` succeeds; default `:test` skips vips-required tests; JVips artifact resolution confirmed in pre-flight output.

### T0.4 — Create images-vips-java25 module skeleton + build.gradle.kts
- **complexity**: medium
- **deps**: T0.1, T0.2
- **files**:
  - `utils/images-vips-java25/build.gradle.kts`
  - `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/{ops,writer}/.gitkeep`
  - `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/.gitkeep`
  - `utils/images-vips-java25/src/test/resources/{junit-platform.properties,logback-test.xml}`
- **details**: Per spec §5.3. Java 25 toolchain, `kotlin { jvmToolchain(25) }`, `options.release.set(25)`, `jvmTargetValidationMode = WARNING`, `implementation(Libs.vips_ffm)` (**NOT** `api()` — D8: binding types are `internal`), `implementation(Libs.commons_io)`, `forkEvery = 1`, `maxParallelForks = 1`. **Important**: `jvmArgs("--enable-native-access=ALL-UNNAMED")` in ALL `Test` task config blocks — this is **NOT** in `virtualthread/jdk25` precedent; it is new for vips-ffm. Kotlin `KotlinCompile` does NOT need this flag.
- **DoD**: `./gradlew :bluetape4k-images-vips-java25:build -x test` succeeds; `jvmArgs` present in test config.

### T0.5 — Verify settings.gradle.kts auto-registers all 3 modules
- **complexity**: low
- **deps**: T0.2, T0.3, T0.4
- **files**: read-only check of `settings.gradle.kts`
- **details**: Confirm `includeModules("utils", ...)` picks up `bluetape4k-images-vips-api/java21/java25`. Use orchestrator-level `./gradlew projects` (Bash) and check output — do NOT use `rg` from a subagent prompt (subagents must use Read/Grep tools only, not Bash).
- **DoD**: Gradle project list shows all three new modules.

### T0.6 — Verify BOM auto-aggregation includes new modules
- **complexity**: low
- **deps**: T0.2, T0.3, T0.4
- **files**: read-only check of `bluetape4k/bom/build.gradle.kts`
- **details**: BOM uses `rootProject.subprojects` auto-aggregation — new modules are included automatically. **No manual edit needed.** Verify by reading `bluetape4k/bom/build.gradle.kts` and confirming the aggregation pattern. Then run `./gradlew :bluetape4k-bom:generatePomFileForBluetape4kPublication` and grep output POM for `images-vips`. **Fallback if verification fails**: inspect `subprojects` filter logic in `bluetape4k/bom/build.gradle.kts` — if modules are excluded by name pattern, add explicit entries for `bluetape4k-images-vips-{api,java21,java25}`.
- **DoD**: Generated BOM POM XML includes all three entries (auto or explicit).

**T0 parallelism**: T0.3 and T0.4 can run in parallel after T0.2.

---

## T1 — API Module (`bluetape4k-images-vips-api`)

Goal: Define binding-neutral interfaces, value types, factory signatures, exceptions, and coroutine extensions.

### T1.1 — VipsImageFormat enum
- **complexity**: low
- **deps**: T0.2
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsImageFormat.kt`
- **details**: Enum with `JPEG, PNG, WEBP`. Mark `AVIF, HEIC` with `@IncubatingImageApi` (imported from `bluetape4k-images`).
- **DoD**: KDoc on enum and each value; `@IncubatingImageApi` import resolves.

### T1.2 — VipsEncodeOptions data class
- **complexity**: medium
- **deps**: T0.2
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsEncodeOptions.kt`
- **details**: Per spec §4.4. `Serializable`, `companion object : KLogging()`, `init` validates `quality in 0..100` and `effort in 1..9`, presets `Default/HighQuality/LowBandwidth`. Use `requireXxx()` (NOT `assertXxx()`) per project rule.
  - **`serialVersionUID` placement (CRITICAL)**: must be declared with `@JvmStatic` inside `companion object`, otherwise Kotlin compiles it into the companion's bytecode and Java serialization cannot find it → JVM uses a generated UID → version drift across rebuilds:
    ```kotlin
    companion object : KLogging() {
        @JvmStatic private val serialVersionUID: Long = 1L
        val Default = VipsEncodeOptions()
        val HighQuality = VipsEncodeOptions(quality = 95, effort = 6)
        val LowBandwidth = VipsEncodeOptions(quality = 60, effort = 3)
    }
    ```
  - **`readResolve()` exception type**: use `InvalidObjectException` (not `require()` → `IllegalArgumentException`) so `ObjectInputStream` wraps it correctly per Java serialization spec:
    ```kotlin
    @Suppress("unused")
    private fun readResolve(): Any {
        if (quality !in 0..100) throw InvalidObjectException("quality out of range: $quality")
        if (effort !in 1..9) throw InvalidObjectException("effort out of range: $effort")
        return this
    }
    ```
- **DoD**: KDoc with `@param` for each field; `@JvmStatic serialVersionUID` confirmed; serialization round-trip test in T4.1 including tampered-stream `InvalidObjectException` assertion.

### T1.3 — Exception hierarchy
- **complexity**: low
- **deps**: T0.2
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsExceptions.kt`
- **details**: `open class VipsException`, `VipsDecodeException`, `VipsEncodeException`, `VipsInitializationException` per spec §4.7. **Exception message sanitization policy**: `message` must contain only safe, non-path-disclosing information (format name, operation type). The `cause` parameter preserves the original binding exception (with full libvips error buffer) for server-side logging, but callers should never return `e.message` to end users. Pattern:
  ```kotlin
  // BAD — leaks internal libvips path + error buffer in message
  throw VipsDecodeException(jvipsException.message ?: "decode failed", jvipsException)
  // GOOD — safe message, cause preserved for server logs
  throw VipsDecodeException("Image decode failed: unsupported format or corrupted input", jvipsException)
  ```
  Document this policy in KDoc of `VipsException`.
- **DoD**: All four classes compile; KDoc documents sanitization policy; cause chain preserved.

### T1.4 — VipsImage interface
- **complexity**: high
- **deps**: T1.1, T1.2, T1.3
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsImage.kt`
- **details**: Per spec §4.1. `interface VipsImage : AutoCloseable`. Properties `width/height/bands`. Methods `resize/thumbnail/crop/toBytes/writeTo(path)/writeTo(out, format)`. Korean KDoc with `use {}` example. Document `@throws VipsDecodeException/VipsEncodeException` where appropriate. `writeTo(Path)` KDoc MUST include: "호출자는 `path`가 허용된 디렉토리 내에 있음을 사전에 검증해야 합니다. 이 함수는 경로 탐색(path traversal)을 방지하지 않습니다." — Note: bluetape4k-core does not provide a `requireContainedIn` utility; path validation is the caller's responsibility at the application layer (per spec §8.5).
- **DoD**: Interface compiles; exhaustive KDoc; `writeTo(Path)` KDoc contains path traversal warning per spec §8.5.

### T1.5 — VipsRuntime interface
- **complexity**: medium
- **deps**: T1.3
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsRuntime.kt`
- **details**: Per spec §4.2 (updated). Add `isShutdown: Boolean` to the interface — the terminal-state contract is part of the public API, not an implementation detail. Interface definition:
  ```kotlin
  interface VipsRuntime {
      fun init(concurrency: Int = 4, maxPixels: Long = 150_000_000L)
      fun shutdown()
      val isInitialized: Boolean
      val isShutdown: Boolean  // true after shutdown(); init() call after this throws
  }
  ```
  KDoc: "shutdown() is terminal — VIPS_INIT() must not be called after vips_shutdown(). `init()` after `shutdown()` throws `VipsInitializationException`." Thread safety: `AtomicReference<State>` (no `@Synchronized` per project virtual-thread rule).
- **DoD**: Interface compiles with `isShutdown`; KDoc documents terminal contract; `spec §4.2` updated to match.

### T1.6 — Resolve Open Questions from spec + document decisions
- **complexity**: low
- **deps**: T1.4
- **files**: no code file; spec update only (if needed)
- **details**: Record decisions for spec Open Questions resolved during planning:
  - **OQ#1** (Maven Central for JVips 8.10.2): Confirmed available.
  - **OQ#3** (prefix: `JVips*` vs `Vips21*`): Chose `JVips*` — communicates the underlying binding name (JVips library) rather than the Java version (which the module name already conveys). Consistent with Criteo's naming.
  - **OQ#4** (shared test fixtures location): Resolved by T4.2 — `testFixtures` in api module.
  - **OQ#2** (JVips 8.10.2 codec completeness): Remains **open**. JVips 8.10.2 bundles libvips core + libwebp but may lack libaom (AV1/AVIF) and libheif. Tests that require AVIF/HEIC use `@Tag("vips-required")` + `assumeTrue` to skip gracefully. Track as a follow-up issue comment in the PR.
  - **Factory contract**: Factory functions (`vipsImageOf`, `suspendVipsImageOf`) live only in impl modules with identical signatures; no api-module abstract holder possible in Kotlin JVM (no `expect/actual` without MPP). Api module hosts only the suspend extension wrappers (T1.7).
- **DoD**: All OQ decisions recorded in this plan entry; PR description references OQ#2 as open follow-up.

### T1.7 — SuspendVipsOps coroutine extensions
- **complexity**: medium
- **deps**: T1.4
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/coroutines/SuspendVipsOps.kt`
- **details**: Per spec §4.5. `suspend fun VipsImage.suspendToBytes(...)`, two `suspendWriteTo(...)` overloads. All wrap `withContext(Dispatchers.IO) { ... }`. Korean KDoc.
- **DoD**: All three extensions compile against `VipsImage`; KDoc complete.

**T1 parallelism**: T1.1, T1.2, T1.3 can run in parallel; T1.4 and T1.5 can run in parallel after T1.1–T1.3; T1.7 follows T1.4.

---

## T2 — java21 Implementation (JVips, JNI)

Goal: Implement `VipsImage` and `VipsRuntime` against `com.criteo:jvips`, hide all `com.criteo.vips.*` types behind the API.

### T2.0 — JVips 8.10.2 API spike (**blocking gate for T2.4**)
- **complexity**: low
- **deps**: T0.3
- **files**: no code file; findings recorded in this plan as a comment
- **details**: Before writing T2.4 (factory functions), confirm the following from JVips 8.10.2 sources/Javadoc (`com.criteo.vips.*`):
  1. Does a header-only read API exist? (e.g., `VipsImage.getWidth()` without triggering pixel decode). Record the exact method name.
  2. If no header-only API exists, document the fallback: perform a full open + dimension check + `close()` if over limit. This is less efficient but acceptable.
  3. What exceptions can `VipsImage.newFromFile/newFromByteArray` throw? Document the mapping to `VipsDecodeException`.
  4. **[P3-R1] Streaming source API**: Does JVips 8.10.2 expose `VipsSource` / `VipsSourceCustom` or equivalent (analogous to `vips_source_new_from_descriptor`)? If yes, `vipsImageOf(stream)` MUST use the streaming path instead of reading the full stream to a `ByteArray` (50 MB × N concurrent = N×50 MB heap). Document the exact API if found.
  5. **[P3-R5] Lazy decode verification**: For `VipsImage.newFromByteArray(bytes)` with JPEG/PNG/WebP, use `-Dvips.leak=1` (or equivalent VIPS_LEAK env) + JVM heap snapshot before/after to confirm libvips performs header-only parse (lazy decode) rather than full pixel allocation at construction. If full decode occurs immediately, the maxPixels check window in T2.4 must move to use `VipsImage.getWidth()` on a probe object + immediate close.
- **DoD**: Confirmed JVips API surface documented in T2.4 details; streaming source API decision recorded (use or skip with reason); lazy decode behavior confirmed with evidence. ⛔ T2.4 MUST NOT start before this task is complete.

### T2.1 — NativeHandle reference-count guard
- **complexity**: high
- **deps**: T0.3
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/internal/NativeHandle.kt`
- **details**: Internal class wrapping JVips native pointer with `AtomicInteger` ref count + `java.lang.ref.Cleaner` registration. `acquire()/release()` semantics. `Cleaner` callback logs warning if released via cleaner (i.e., user forgot `close()`). Used by `JVipsImage`.

  **[P3-R4] CRITICAL — Cleaner lambda MUST NOT capture `this`**: If the cleanup lambda captures `this` (the `NativeHandle` instance) or any object holding a strong reference to it, the Cleaner can never fire and native handles will leak permanently. The correct pattern captures only the raw native pointer (`Long`):
  ```kotlin
  internal class NativeHandle(private val ptr: Long, private val release: (Long) -> Unit) {
      companion object {
          private val CLEANER: Cleaner = Cleaner.create()  // shared, NOT per-instance
      }

      private val cleanable: Cleaner.Cleanable

      init {
          val capturedPtr = ptr         // capture primitive, not 'this'
          val capturedRelease = release // capture lambda, not 'this'
          cleanable = CLEANER.register(this) {
              capturedRelease(capturedPtr)  // no reference to NativeHandle
          }
      }
  }
  ```
  - `CLEANER` must be `companion object` (static) — per-instance `Cleaner.create()` creates a new daemon thread each time.
  - `release` lambda must be a top-level or companion function, NOT an instance method reference (`this::releaseNative` captures `this`).
- **DoD**: Unit-testable independent of libvips (mock the underlying release lambda); KDoc documents leak-detection contract; Cleaner lambda does NOT capture `this` (verified by T4.6 WeakReference GC test — must FAIL the build, not just warn, if capture detected).

### T2.2 — JVipsRuntime implementation
- **complexity**: high
- **deps**: T1.5, T0.3
- **files**:
  - `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/JVipsRuntime.kt`
  - `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/internal/JVipsNativeRuntime.kt`
- **details**: Object implementing `VipsRuntime`. Thread safety via `AtomicReference<RuntimeState>` — a single atomic field encodes the full lifecycle (replaces dual-AtomicBoolean which has TOCTOU window). ⚠️ Do NOT use `@Synchronized` (blocked on Virtual Threads). Use `AtomicReference` at class-property level only (not method-local, per project atomicfu rule). State machine:
  ```kotlin
  private enum class RuntimeState { UNINITIALIZED, INITIALIZING, INITIALIZED, SHUTDOWN }
  private val state = AtomicReference(RuntimeState.UNINITIALIZED)

  override fun init(concurrency: Int, maxPixels: Long) {
      // Fast path
      when (state.get()) {
          RuntimeState.INITIALIZED -> return
          RuntimeState.SHUTDOWN -> throw VipsInitializationException(
              "libvips has been shut down — restart the process"
          )
          else -> {}
      }
      if (!state.compareAndSet(RuntimeState.UNINITIALIZED, RuntimeState.INITIALIZING)) {
          // Another thread won the CAS. Spin until it finishes (INITIALIZING → INITIALIZED or UNINITIALIZED).
          // ⚠️ Do NOT just return here — the caller must not proceed before init is complete.
          while (state.get() == RuntimeState.INITIALIZING) {
              Thread.onSpinWait()  // CPU hint; no blocking, no @Synchronized
          }
          when (state.get()) {
              RuntimeState.INITIALIZED -> return
              RuntimeState.SHUTDOWN -> throw VipsInitializationException(
                  "libvips was shut down during concurrent init"
              )
              RuntimeState.UNINITIALIZED -> throw VipsInitializationException(
                  "Concurrent init attempt failed — retry"
              )
              else -> {} // unreachable: INITIALIZING loop above exited
          }
          return
      }
      // This thread owns INITIALIZING slot
      try {
          nativeRuntime.nativeInit(concurrency)  // calls Vips.init() via adapter seam (see M2)
          // configure maxPixels...
          state.set(RuntimeState.INITIALIZED)
      } catch (e: Exception) {
          state.set(RuntimeState.UNINITIALIZED)  // allow retry
          throw VipsInitializationException("libvips init failed", e)
      }
  }

  override fun shutdown() {
      if (state.getAndSet(RuntimeState.SHUTDOWN) == RuntimeState.INITIALIZED) {
          nativeRuntime.nativeShutdown()  // calls Vips.shutdown() via adapter seam
      }
  }
  ```
  `isInitialized` returns `state.get() == RuntimeState.INITIALIZED`.
  `isShutdown` returns `state.get() == RuntimeState.SHUTDOWN`.

  **M2 adapter seam** (required for T4.10 testability): Define an `internal interface JVipsNativeRuntime` with `nativeInit(concurrency: Int)` and `nativeShutdown()`. The production impl (`DefaultJVipsNativeRuntime`) delegates to `Vips.init()`/`Vips.shutdown()`. `JVipsRuntime` holds `internal var nativeRuntime: JVipsNativeRuntime = DefaultJVipsNativeRuntime` — `var` allows test injection without PowerMock/bytecode manipulation. File: `internal/JVipsNativeRuntime.kt`.

  **[P3-R2] `resetForTest()` REQUIRED for test isolation**: `JVipsRuntime` is a Kotlin `object` (JVM singleton). Without a reset method, once `state` reaches `INITIALIZED` or `SHUTDOWN`, it cannot be reset between tests — causing test bleed across classes in the same JVM fork. Provide:
  ```kotlin
  /** Test-only. Resets state to UNINITIALIZED and restores default nativeRuntime. */
  @VisibleForTesting
  internal fun resetForTest() {
      state.set(RuntimeState.UNINITIALIZED)
      nativeRuntime = DefaultJVipsNativeRuntime
  }
  ```
  T4.10 MUST call this in `@AfterEach`. All other test classes that depend on `JVipsRuntime` state should call it in `@BeforeEach` as defensive cleanup.

  **[P3-R2] Spring devtools WARNING**: Do NOT register `JVipsRuntime.shutdown()` as a Spring `@PreDestroy` bean method. Spring Boot devtools restarts the `LaunchedClassLoader` (ApplicationContext) but keeps the native `.so` in the JVM's `SystemClassLoader`. If `shutdown()` is called via `@PreDestroy`, the next devtools restart calls `init()` on a permanently SHUTDOWN runtime → `VipsInitializationException("restart the process")`. Only use `Runtime.getRuntime().addShutdownHook(...)` for auto-cleanup. This warning MUST appear prominently in `README.md` §Lifecycle.

- **DoD**: `init()` exactly-once under concurrency; second caller spins until first finishes; retry after pre-shutdown failure; `init()` after `shutdown()` throws `VipsInitializationException`; no `@Synchronized`; adapter seam in place; `resetForTest()` present and used in T4.10.

### T2.3 — JVipsImage implementation
- **complexity**: high
- **deps**: T1.4, T2.1
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/JVipsImage.kt`
- **details**: `internal class JVipsImage(private val handle: NativeHandle, ...) : VipsImage`. `width/height/bands` read once at construction (immutable per project rule). `resize(width, height)`, `thumbnail(maxDimension)`, and `crop(left, top, width, height)` all produce **new** `JVipsImage` instances (no mutation) — **`crop` is implemented directly in this file** (not a separate ops file; spec §3.2 module tree does not list `JVipsCrop.kt`). `toBytes(format, options)` dispatches to writer (T2.7–T2.9). `close()` releases the handle. Wrap all `com.criteo.vips.VipsException` → `VipsDecodeException` or `VipsEncodeException` per call site.
- **DoD**: All `VipsImage` methods implemented including `crop`; binding types confined to `internal`; immutability preserved. **Decomposition option**: if executor needs to unblock T2.4–T2.9 early, `crop` may be stubbed as `throw UnsupportedOperationException("crop: T2.3b")` and completed in a follow-up commit before T4.4 starts.

### T2.4 — JVipsImageSupport factory functions
- **complexity**: high
- **deps**: T2.3
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/JVipsImageSupport.kt`
- **details**: (Implement ONLY after T2.0 spike is complete.) Top-level `vipsImageOf(File|Path|ByteArray|InputStream): VipsImage` and `suspendVipsImageOf(File|Path|ByteArray): VipsImage` per spec §4.3. Security controls:
  1. **Format allowlist**: sniff first 12 bytes against `VipsImageFormat` magic bytes (JPEG: `FF D8 FF`, PNG: `89 50 4E 47`, WebP: `52 49 46 46..57 45 42 50`). Throw `VipsDecodeException("Unsupported format")` for anything else. Alternatively, enable `VIPS_BLOCK_UNTRUSTED=1` in `JVipsRuntime.init()` to block high-risk loaders at libvips level — document which approach is used.
  2. **InputStream 50MB cap**: wrap with `BoundedInputStream` using **`setOnMaxCount` callback** (Commons IO 2.16+ API — method is `setOnMaxCount`, NOT `setOnMaxLength`). The callback fires when byte `maxCount+1` would be read — i.e., only on genuine overflow, never on exactly-50MB input:
     ```kotlin
     val bounded = BoundedInputStream.builder()
         .setInputStream(stream)
         .setMaxCount(50L * 1024 * 1024)   // 50 MB ceiling
         .setPropagateClose(false)
         .setOnMaxCount { throw VipsDecodeException("Input stream exceeds 50 MB limit") }
         .get()
     ```
     Do NOT use `bounded.count >= MAX_BYTES` check: that incorrectly rejects exactly-50MB inputs AND fails to confirm overflow if callback is absent. The callback approach is the only correct pattern.
  3. **maxPixels check**: read header dimensions (confirmed method from T2.0 spike). Compute `width * height * bands` (not just `width * height` — high-depth RGBA is 4× larger). If over limit, throw `VipsDecodeException`. Header parse uses native parser but avoids full pixel allocation.
  4. **Exception safety**: `try/finally` to release stream + partial native handle on decode failure.
  5. Korean KDoc: document path traversal risk on File/Path variants; document format allowlist behavior.
- **deps**: T2.3, T2.0 (spike)
- **DoD**: All 4 blocking + 3 suspend factories compile; format allowlist enforced; BoundedInputStream throws on overflow; `maxPixels` uses `width*height*bands`; exception path cleans up.

### T2.5 — JVipsResize / JVipsThumbnail internal helpers
- **complexity**: medium
- **deps**: T2.3
- **files**:
  - `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/ops/JVipsResize.kt`
  - `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/ops/JVipsThumbnail.kt`
- **details**: ⚠️ **Kotlin extensions cannot satisfy interface methods.** `resize()` and `thumbnail()` on `VipsImage` are interface methods, so they MUST be implemented directly in `JVipsImage` (T2.3). These `ops/` files contain **internal helper functions** (not extensions, not interface impls) — e.g., `internal fun resizeWithJVips(image: JVipsHandle, width: Int, height: Int): JVipsHandle` — called by `JVipsImage.resize()`/`thumbnail()`. Separating the JVips API call logic improves testability without violating Kotlin's dispatch model.
- **DoD**: `JVipsImage.resize()` and `thumbnail()` delegate to helpers in ops files; ops files have no `implements VipsImage` annotation; unit-testable independently.

### T2.7 — JVipsJpegWriter
- **complexity**: medium
- **deps**: T2.3, T1.2
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/writer/JVipsJpegWriter.kt`
- **details**: `object JVipsJpegWriter` with `write(image: VipsImage, dest: Path|OutputStream, options)` and a suspend variant. Use JVips `writeToJpeg(quality)`. Wrap binding exceptions → `VipsEncodeException`.
- **DoD**: Output starts with `FF D8 FF` (JPEG magic).

### T2.8 — JVipsPngWriter
- **complexity**: medium
- **deps**: T2.3, T1.2
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/writer/JVipsPngWriter.kt`
- **details**: PNG writer. `quality` ignored (PNG is lossless); `effort` may map to compression level.
- **DoD**: Output starts with `89 50 4E 47` (PNG magic).

### T2.9 — JVipsWebpWriter
- **complexity**: medium
- **deps**: T2.3, T1.2
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/writer/JVipsWebpWriter.kt`
- **details**: WebP writer. Honor `quality`, `lossless`, `effort`.
- **DoD**: Output contains `RIFF` at offset 0 and `WEBP` at offset 8.

**T2 parallelism**: T2.5–T2.9 can run in parallel after T2.3.

---

## T3 — java25 Implementation (vips-ffm, FFM API)

Goal: Implement `VipsImage` and `VipsRuntime` against `app.photofox.vips-ffm`. Same external API as java21, FFM-based internals.

### T3.0 — vips-ffm 1.9.6 API spike (**blocking gate for T3.1–T3.8**)
- **complexity**: low
- **deps**: T0.4
- **files**: no code file; findings recorded in this plan as a comment
- **details**: Before writing any T3.x task, confirm the following from vips-ffm 1.9.6 sources (https://vipsffm.photofox.app/ or Maven source jar):
  1. Does `VVips` class exist in `app.photofox.vipsffm.*`? Current docs suggest it may not — `VImage`, `VipsHelper`, or `VipsInvoker` may be the primary classes.
  2. Is libvips auto-initialized on first `VImage` construction, or is an explicit init call required?
  3. How is `Arena` lifecycle managed? Per-image, per-session, or global?
  4. What is the header-only read path for dimension pre-checks?
  5. What exceptions does the library throw? Document the mapping.
  6. **[P3-R1] Streaming source API**: Does vips-ffm expose `VSource` / `VipsForeign.findLoadSource` or equivalent that avoids reading the entire stream to a `ByteArray`? If yes, `vipsImageOf(stream)` MUST use it (50 MB × N concurrent = N×50 MB heap otherwise). Document the API if found; document "ByteArray fallback required" if not found.
  7. **[P3-R5] Lazy decode verification**: Does `VImage(bytes, ...)` or equivalent perform header-only parse or full pixel allocation immediately? Test with `-Dvips.leak=1` env and heap snapshot to confirm.
  Record confirmed class names, initialization sequence, Arena ownership, streaming API, and lazy decode behavior in a comment block prepended to T3.1 before starting implementation.
- **DoD**: Confirmed vips-ffm API surface; VVips existence verified or refuted; initialization sequence documented; streaming source API decision recorded; lazy decode behavior confirmed. ⛔ T3.1 MUST NOT start before this task is complete.

### T3.1 — FfmVipsRuntime implementation
- **complexity**: high
- **deps**: T1.5, T0.4
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/FfmVipsRuntime.kt`
- **details**: Object implementing `VipsRuntime` via vips-ffm API. ⚠️ **Implement ONLY after T3.0 spike is complete** — confirm actual initialization API before writing. Use same `AtomicReference<RuntimeState>` state machine as T2.2 (see T2.2 details for pattern — copy and adapt to vips-ffm lifecycle). ⚠️ Do NOT use `@Synchronized`. `isShutdown` = `state.get() == SHUTDOWN`. JVM args check: if `--enable-native-access=ALL-UNNAMED` is missing at runtime, log a `WARN` with guidance (do not throw — the JVM may still work depending on JDK version).
- **DoD**: Same state machine contract as T2.2; no `@Synchronized`; shutdown terminal; JVM args warning present.

### T3.2 — FfmVipsImage implementation
- **complexity**: high
- **deps**: T1.4, T0.4
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/FfmVipsImage.kt`
- **details**: `internal class FfmVipsImage` wrapping vips-ffm `VImage` (confirmed public class per docs) and a memory `Arena`. ⚠️ **Implement only after T3.1 API spike** — confirm whether `Arena` is needed per-image or globally, and whether libvips auto-init means no explicit Runtime call is required before constructing `VImage`. Register `Cleaner` for `Arena.close()` safety net. `width/height/bands` immutable. `resize(width, height)`, `thumbnail(maxDimension)`, and `crop(left, top, width, height)` produce new instances — `crop` is implemented **directly in this class** (no separate file); `resize`/`thumbnail` delegate to T3.4 internal helpers. Wrap `app.photofox.vipsffm.*` exceptions → `VipsDecodeException`/`VipsEncodeException`.
- **DoD**: All `VipsImage` methods implemented including `crop`; `Arena` (or equivalent) closed on `close()`; binding types `internal`; no `VVips` reference (use confirmed vips-ffm API).

### T3.3 — FfmVipsImageSupport factory functions
- **complexity**: high
- **deps**: T3.2
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/FfmVipsImageSupport.kt`
- **details**: (Implement ONLY after T3.0 spike.) Mirror T2.4 security controls:
  1. **Format allowlist**: same magic-byte check as T2.4 (or `VIPS_BLOCK_UNTRUSTED=1` if vips-ffm exposes this).
  2. **InputStream 50MB cap**: same throwing `BoundedInputStream` as T2.4.
  3. **maxPixels**: `width * height * bands` (confirm header-read API from T3.0 spike).
  4. **Exception safety**: `try/finally` to close `Arena` + stream on failure.
- **deps**: T3.2, T3.0 (spike)
- **DoD**: Same contract as T2.4; Arena closed on failure.

### T3.4 — FfmVipsResize / FfmVipsThumbnail internal helpers
- **complexity**: medium
- **deps**: T3.2
- **files**:
  - `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/ops/FfmVipsResize.kt`
  - `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/ops/FfmVipsThumbnail.kt`
- **details**: Mirror T2.5 pattern — **internal helpers only**, not extension impls of interface methods. `resize()` and `thumbnail()` are implemented in `FfmVipsImage` (T3.2); these files hold the vips-ffm API call logic as `internal fun` helpers. ⚠️ Confirm actual vips-ffm API for resize/thumbnail from spike in T3.1 before implementing.
- **DoD**: `FfmVipsImage.resize()` and `thumbnail()` delegate to helpers; output dimensions correct.

### T3.6 — FfmVipsJpegWriter
- **complexity**: medium
- **deps**: T3.2, T1.2
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/writer/FfmVipsJpegWriter.kt`
- **details**: Mirror T2.7 against vips-ffm.
- **DoD**: Output JPEG magic verified.

### T3.7 — FfmVipsPngWriter
- **complexity**: medium
- **deps**: T3.2, T1.2
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/writer/FfmVipsPngWriter.kt`
- **details**: Mirror T2.8.
- **DoD**: Output PNG magic verified.

### T3.8 — FfmVipsWebpWriter
- **complexity**: medium
- **deps**: T3.2, T1.2
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/writer/FfmVipsWebpWriter.kt`
- **details**: Mirror T2.9.
- **DoD**: Output WebP magic verified.

**T3 parallelism**: All of T3 can run **in parallel with all of T2** once T1 is complete. Within T3, T3.4–T3.8 parallel after T3.2.

---

## T4 — Tests

Goal: Cover spec §6.2 assertions for both implementations + the `use {}` leak guard. All vips-touching tests carry `@Tag("vips-required")`.

### T4.1 — VipsEncodeOptions unit tests (api module)
- **complexity**: low
- **deps**: T1.2
- **files**: `utils/images-vips-api/src/test/kotlin/io/bluetape4k/images/vips/VipsEncodeOptionsTest.kt`
- **details**: AAA pattern. Verify validation throws `IllegalArgumentException` for `quality = -1`, `quality = 101`, `effort = 0`, `effort = 10`. Verify `Default/HighQuality/LowBandwidth` constants. **Java serialization round-trip only** (`ObjectOutputStream` / `ObjectInputStream`) — `VipsEncodeOptions` implements `java.io.Serializable`, NOT `kotlinx.serialization.Serializable`. Use Kluent matchers (`shouldBeEqualTo`, `shouldThrow`). NO `@Tag("vips-required")` — pure logic test.
- **DoD**: Runs in default `:test` (not skipped); passes.

### T4.2 — Test fixtures and AbstractVipsTest harness (api module)
- **complexity**: medium
- **deps**: T0.2, T0.3, T0.4
- **files**:
  - `utils/images-vips-api/src/testFixtures/kotlin/io/bluetape4k/images/vips/testfixtures/VipsTestFixtures.kt`
  - `utils/images-vips-api/src/testFixtures/resources/fixtures/{sample.jpg,sample.png,sample.webp}`
  - Update `utils/images-vips-api/build.gradle.kts` to enable `java-test-fixtures` plugin.
  - Update `utils/images-vips-java21/build.gradle.kts`: add `testImplementation(testFixtures(project(":bluetape4k-images-vips-api")))`.
  - Update `utils/images-vips-java25/build.gradle.kts`: add `testImplementation(testFixtures(project(":bluetape4k-images-vips-api")))`.
- **details**: Resolves Open Question #4 — shared fixtures via `testFixtures`. Provides `loadFixture(name): ByteArray` and known dimensions for assertions. Sample images small (~50–200 KB) and license-clean. ⚠️ **Both impl modules must add the `testFixtures` dep** or T4.4/T4.8 will fail to compile. **Publishing safety**: add `disableTestFixturesPublication()` if needed; verify with `./gradlew :bluetape4k-images-vips-api:publishToMavenLocal --dry-run`.
- **DoD**: `testFixtures(project(":bluetape4k-images-vips-api"))` declared in java21 AND java25 build files; fixtures compile in T4.4/T4.8; test-fixtures jar NOT in published artifact list.

### T4.3 — AbstractJVipsTest base class (java21)
- **complexity**: medium
- **deps**: T2.2, T4.2
- **files**: `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/AbstractJVipsTest.kt`
- **details**: Per spec §6.1. `@Tag("vips-required")` at class level. `@BeforeAll` calls `JVipsRuntime.init()` inside `runCatching`; on failure → `assumeTrue(false, message)` (skip, not fail). `companion object : KLogging()`.
- **DoD**: Skips cleanly when libvips absent; initializes once per JVM fork.

### T4.4 — JVipsImageTest (java21)
- **complexity**: medium
- **deps**: T2.4, T2.5, T2.7, T2.8, T2.9, T4.3
- **files**: `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/JVipsImageTest.kt`
- **details**: Cover 15 assertions — all must pass when libvips present, skipped otherwise:
  1. `vipsImageOf(file)` width/height match known fixture dimensions
  2. `resize(800, 600)` — libvips `thumbnail_image` with both dimensions forces exact size (verify from T2.0 spike which JVips method to call; if aspect-ratio-preserving instead, adapt assertion to `width shouldBeLessOrEqualTo 800 && height shouldBeLessOrEqualTo 600`)
  3. `thumbnail(300)` longest side ≤ 300 (boundary: test with a 300-pixel-long-side fixture to verify no-op passthrough; test with 301-pixel to verify clamp)
  4. `toBytes(JPEG)` non-empty, magic `FF D8 FF`
  5. `toBytes(PNG)` non-empty, magic `89 50 4E 47`
  6. `toBytes(WEBP)` non-empty, `RIFF` at offset 0 and `WEBP` at offset 8
  7. `suspendToBytes(JPEG)` returns same result as `toBytes(JPEG)` (validates coroutine wrapper outcome — do NOT assert which dispatcher was used; `Dispatchers.IO` is not replaced by `runTest`)
  8. `use { }` — after close, subsequent call throws `IllegalStateException` or similar
  9. `close()` called twice — second call must not throw (idempotent) and must not crash JVM
  10. `crop(0, 0, 100, 100)` → result dimensions exactly 100×100
  11. `writeTo(path, options)` → file exists, non-zero size, valid JPEG magic (covers Path overload)
  12. `writeTo(out, JPEG, options)` → stream bytes start with JPEG magic
  13. `resize(0, 600)` or `resize(-1, 600)` → throws (expect `VipsDecodeException` or `IllegalArgumentException`)
  14. `crop` out of bounds (e.g., `crop(0, 0, width+1, height)`) → throws
  15. `vipsImageOf(ByteArray(byteArrayOf(0,1,2,3)))` corrupt data → throws `VipsDecodeException`
  - Use Kluent matchers: `bytes.size shouldBeGreaterThan 0`, `width shouldBeEqualTo 100`. Never `(x >= y).shouldBeTrue()`.
- **DoD**: All 15 assertions pass when libvips present; skipped when absent. DoD count updated in final DoD section.

### T4.5 — JVipsResize ops tests (java21)
- **complexity**: low
- **deps**: T2.5, T4.3
- **files**: `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/ops/JVipsResizeTest.kt`
- **details**: Focused tests for resize and thumbnail behavior (aspect ratio, edge cases like 1×1, very wide images).
- **DoD**: Passes under `-PincludeTags=vips-required`.

### T4.6 — NativeHandle leak-detection unit test (java21)
- **complexity**: medium
- **deps**: T2.1
- **files**: `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/internal/NativeHandleTest.kt`
- **details**: NOT `@Tag("vips-required")` — uses a mock release lambda (no native libvips required). Two distinct test scopes:
  1. **Ref-count / AtomicReference state machine** (deterministic): verify `release()` is idempotent (second call does not double-free), `release()` transitions state to released, closed handle throws on subsequent use. These are unit-testable with a mock lambda.
  2. **Cleaner invocation** (non-deterministic): GC + Cleaner fire time is not guaranteed. Test strategy: make the handle object unreachable, call `System.gc()` + `Thread.sleep(100)` as best-effort hint, then check log output via log capture (e.g., Logback `ListAppender`). **Do NOT assert with `shouldBeTrue` on Cleaner-fired state** — mark this assertion `@Disabled` or `assumeTrue(false, "Cleaner timing non-deterministic")` so CI stays green.
  3. **Cleaner lambda capture check**: verify the lambda does NOT hold a strong reference to the `NativeHandle` wrapper (would prevent GC). Test by verifying `WeakReference<NativeHandle>` becomes `null` after GC hint.
- **DoD**: Ref-count tests pass deterministically in default `:test`; Cleaner invocation test is best-effort or disabled with comment; no strong-ref capture confirmed.

### T4.7 — AbstractFfmVipsTest base class (java25)
- **complexity**: medium
- **deps**: T3.1, T4.2
- **files**: `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/AbstractFfmVipsTest.kt`
- **details**: Mirror T4.3 against `FfmVipsRuntime`. **Substitution map** (spec §6.1 has copy-paste artifact — use corrected names from T3.0 spike): `JVipsRuntime` → `FfmVipsRuntime`. ⚠️ Spec §6.1 shows `com.criteo.vips.Vips` → replace with the actual vips-ffm init class confirmed by T3.0 spike (may NOT be `VVips`). The JVM fork comment is correct and applies equally here.
- **deps**: T3.1, T4.2 (and implicitly T3.0 spike for class name)
- **DoD**: Skips cleanly when libvips absent; uses `FfmVipsRuntime`; class names match T3.0 confirmed API.

### T4.8 — FfmVipsImageTest (java25)
- **complexity**: medium
- **deps**: T3.3, T3.4, T3.6, T3.7, T3.8, T4.7
- **files**: `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/FfmVipsImageTest.kt`
- **details**: Mirror T4.4 against the FFM implementation. Same 15 assertions. Note: spec §6.1 has copy-paste artifact — use `FfmVipsRuntime` everywhere `JVipsRuntime` appears.
- **DoD**: All 15 assertions pass when libvips present; skipped otherwise.

### T4.9 — FfmVipsResize ops tests (java25)
- **complexity**: low
- **deps**: T3.4, T4.7
- **files**: `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/ops/FfmVipsResizeTest.kt`
- **details**: Mirror T4.5.
- **DoD**: Passes under `-PincludeTags=vips-required`.

### T4.10 — Concurrent VipsRuntime.init() test (java21)
- **complexity**: medium
- **deps**: T2.2
- **files**: `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/JVipsRuntimeConcurrencyTest.kt`
- **details**: Verifies the `AtomicReference<State>` CAS + spin/wait pattern from T2.2 is race-free. Uses the `JVipsNativeRuntime` adapter seam (T2.2 internal interface) — substitute `DefaultJVipsNativeRuntime` with a counting test-double:
  ```kotlin
  val initCount = AtomicInteger(0)
  val testAdapter = object : JVipsNativeRuntime {
      override fun nativeInit(concurrency: Int) { initCount.incrementAndGet() }
      override fun nativeShutdown() {}
  }
  JVipsRuntime.nativeRuntime = testAdapter  // inject before concurrent calls
  ```
  Test: launch 10 coroutines simultaneously calling `JVipsRuntime.init()`. After all complete:
  - `initCount.get() shouldBeEqualTo 1` — called exactly once
  - `JVipsRuntime.isInitialized shouldBeEqualTo true`
  NOT `@Tag("vips-required")` — no real libvips needed.
  ⚠️ Restore `DefaultJVipsNativeRuntime` in `@AfterEach` (or use `@BeforeEach` reset) to prevent state bleed between tests.
- **DoD**: Exactly-once init confirmed under 10-way concurrency; adapter seam used (not real `Vips.init()`); state reset after test.

**T4 parallelism**: T4.1, T4.6, T4.10 are independent of impl modules; T4.3–T4.5 parallel with T4.7–T4.9 (separate modules).

---

## T5 — Documentation, CI, Wiki

Goal: README/KDoc completeness, CI workflow updates, project memory & wiki sync.

### T5.1 — README.md and README.ko.md for images-vips-api
- **complexity**: medium
- **deps**: T1.7
- **files**:
  - `utils/images-vips-api/README.md`
  - `utils/images-vips-api/README.ko.md`
- **details**: Architecture → Mermaid UML class diagram showing `VipsImage`/`VipsRuntime`/`VipsEncodeOptions`/exceptions → Features → Examples (`use {}` block, suspend ops). Top-of-page language switch links per project rule.
- **DoD**: Both READMEs include Mermaid diagram; language-switch links present; no Vega-Lite.

### T5.2 — README.md and README.ko.md for images-vips-java21
- **complexity**: medium
- **deps**: T2.9
- **files**:
  - `utils/images-vips-java21/README.md`
  - `utils/images-vips-java21/README.ko.md`
- **details**: Mermaid class diagram showing `JVipsImage` implements `VipsImage`. Setup section covers `brew install vips` (macOS) and `apt install libvips-tools` (Linux). Example: end-to-end resize+encode. Note that JVips bundles `.so` for Linux but requires brew/apt for full codec set. Language-switch links.
- **DoD**: Both READMEs complete with diagrams and runnable examples.

### T5.3 — README.md and README.ko.md for images-vips-java25
- **complexity**: medium
- **deps**: T3.8
- **files**:
  - `utils/images-vips-java25/README.md`
  - `utils/images-vips-java25/README.ko.md`
- **details**: Mermaid class diagram showing `FfmVipsImage` implements `VipsImage`. **Prominent note**: `--enable-native-access=ALL-UNNAMED` JVM flag required (per spec §8.5). Example with Spring Boot config snippet for the JVM arg. Language-switch links.
- **DoD**: Both READMEs complete; `--enable-native-access` documented prominently.

### T5.4 — KDoc audit pass
- **complexity**: low
- **deps**: T1.7, T2.9, T3.8
- **files**: all `.kt` source files in the three modules
- **details**: Verify Korean KDoc on every public class/interface/extension function. `@throws` declarations on factory and encode operations per spec §4.7. **Check command**: `rg -l "^(public |)?(class|interface|fun|object)" --type kotlin utils/images-vips-{api,java21,java25}/src/main` then spot-check each file for missing KDoc blocks. Alternatively enable Detekt `UndocumentedPublicClass`/`UndocumentedPublicFunction` rules in the module's `detekt.yml` for CI enforcement.
- **DoD**: No public symbol lacks KDoc; `@throws` present on all factory + encode functions.

### T5.5 — Update ci.yml with test-images-vips job
- **complexity**: medium
- **deps**: T2.9, T3.8, T4.4, T4.8
- **files**: `.github/workflows/ci.yml`
- **details**: Add `test-images-vips` job per spec §7.1. Pin `runs-on: ubuntu-24.04`. Install both Java 21 and Java 25 toolchains via `actions/setup-java@v4` (called twice — last call wins for `JAVA_HOME`, but Gradle Foojay Toolchain resolver auto-discovers both). Use `--no-daemon` (per spec) to avoid daemon JVM picking wrong toolchain. **Toolchain ordering caution**: run java21 test step before java25 to minimize confusion; Foojay `auto-detect=true` should find both. `apt-get install -y libvips-tools`. Run `:bluetape4k-images-vips-java21:test -PincludeTags=vips-required` and `:bluetape4k-images-vips-java25:test -PincludeTags=vips-required`.
- **DoD**: CI YAML valid (lint via `actionlint` if available); job present; both impl modules' tests run.

### T5.6 — Update nightly-tests.yml with test-images-vips job
- **complexity**: low
- **deps**: T5.5
- **files**: `.github/workflows/nightly-tests.yml`
- **details**: Read existing nightly job patterns first (Read `.github/workflows/nightly-tests.yml`). Mirror T5.5 job structure (ci.yml ↔ nightly-tests.yml sync per project memory). Nightly may have broader matrix (e.g., longer timeouts) — check and align accordingly; do not just copy-paste from ci.yml without verifying.
- **DoD**: Same job structure present; any nightly-specific settings (timeout, extended matrix) applied if applicable.

### T5.7 — Run /wiki-update for spec & plan
- **complexity**: low
- **deps**: T5.4
- **files**: Obsidian wiki (handled by skill)
- **details**: Invoke `oh-my-claudecode:wiki-update` skill per project memory rule (spec/plan triggers wiki update).
- **DoD**: Wiki page for `bluetape4k-images-vips` exists; qmd reindexed.

### T5.8 — DoD verification & code review
- **complexity**: medium
- **deps**: ALL above
- **files**: review-only
- **details**:
  - Run `./gradlew :bluetape4k-images-vips-api:build :bluetape4k-images-vips-java21:build :bluetape4k-images-vips-java25:build` — all green.
  - Run `./gradlew :bluetape4k-images-vips-java21:test -PincludeTags=vips-required` locally (if libvips installed) or document as CI-only.
  - Invoke `oh-my-claudecode:code-reviewer` skill — resolve all CRITICAL/HIGH issues.
- **DoD**: Spec §10 DoD all checked; code review HIGH/CRITICAL = 0.

**T5 parallelism**: T5.1, T5.2, T5.3 fully parallel; T5.5/T5.6 sequential; T5.4 after impl complete.

---

## Dependency Graph (summary)

```
T0.1 → T0.2 → T0.3, T0.4 (parallel) → T0.5, T0.6
T0.2 → T1.1, T1.2, T1.3 (parallel) → T1.4, T1.5 (parallel) → T1.7
T0.3 → T2.0 (spike) → T2.4
T0.4 → T3.0 (spike) → T3.1
T1.4, T1.5 → T2.1, T2.2 (parallel) → T2.3 → T2.4, T2.5, T2.7, T2.8, T2.9 (parallel)
T1.4, T1.5 → T3.1 → T3.2 → T3.3, T3.4, T3.6, T3.7, T3.8 (parallel)
T2.* → T4.3, T4.4, T4.5, T4.6, T4.10 (parallel)
T3.* → T4.7, T4.8, T4.9 (parallel)
T1.2 → T4.1
T0.2 → T4.2
T4.* + T5.4 → T5.5 → T5.6
T1.7, T2.9, T3.8 → T5.1, T5.2, T5.3 (parallel)
ALL → T5.7, T5.8
```

## Parallelization Recommendations

Two natural parallel tracks once T1 finishes:
- **Track A (java21)**: T2.0 (spike) → T2.1+T2.2 (parallel) → T2.3 → T2.4–T2.9 → T4.3–T4.6, T4.10 → T5.2
- **Track B (java25)**: T3.0 (spike) → T3.1 → T3.2 → T3.3–T3.8 → T4.7–T4.9 → T5.3

Note: T2.0 and T3.0 spikes can start before T1 is complete (they only need T0.3/T0.4).

These can be executed by separate agents/sessions concurrently.

## Out of Scope (deferred to Phase 3)

Per spec §9, the following are NOT in this PR:
- F1: `VipsAvifWriter` implementing `AvifWriter`
- F2: `VipsHeicReader` implementing `HeicReader`
- F3: JXL encode/decode
- F4: DZI tile generation
- F5: Animated WebP / multi-page TIFF
- F6: ICC profile embed/strip
- F7: vips-ffm migration once baseline moves to Java 25+
- AutoImageProcessor / scrimage bridge adapters

## Definition of Done (mirrors spec §10)

- [ ] All 41 tasks complete with their per-task DoD checked (38 original + T2.0 spike + T3.0 spike + T4.10 concurrent test).
- [ ] `./gradlew :bluetape4k-images-vips-{api,java21,java25}:build` — green.
- [ ] Default `:test` skips vips tests cleanly (no native loader errors).
- [ ] `-PincludeTags=vips-required` passes all 15 assertions per impl in CI.
- [ ] `ci.yml` + `nightly-tests.yml` both updated with `test-images-vips` job.
- [ ] All 6 README files (3 modules × 2 languages) complete with Mermaid diagrams.
- [ ] Korean KDoc on every public symbol.
- [ ] BOM module updated.
- [ ] `/wiki-update` executed.
- [ ] Code review (oh-my-claudecode:code-reviewer) passed with 0 HIGH/CRITICAL.
- [ ] Worktree-only commits on `feat/images-vips`.
