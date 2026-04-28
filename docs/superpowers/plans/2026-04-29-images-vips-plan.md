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

This plan delivers three new sibling modules under `utils/` mirroring the `virtualthread/{api,jdk21,jdk25}` pattern. Work proceeds in 6 task groups (T0–T5) with 38 atomic tasks total. Each task carries a `complexity` label and explicit dependencies.

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
| medium | 18 |
| low | 9 |
| **Total** | **38** |

> Note: T2.4, T3.3 complexity upgraded from `medium` → `high` (factory functions with security caps, exception safety, ~200+ LoC). T1.6 converted from incoherent "skip" to decision-recording task (low).

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
- **details**: Per spec §5.1. `api(project(":bluetape4k-images"))`, `api(project(":bluetape4k-coroutines"))`, `implementation(project(":bluetape4k-logging"))`, `testImplementation(project(":bluetape4k-junit5"))`.
- **DoD**: `./gradlew :bluetape4k-images-vips-api:build -x test` succeeds.

### T0.3 — Create images-vips-java21 module skeleton + build.gradle.kts
- **complexity**: medium
- **deps**: T0.1, T0.2
- **files**:
  - `utils/images-vips-java21/build.gradle.kts`
  - `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/{ops,writer,internal}/.gitkeep`
  - `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/.gitkeep`
  - `utils/images-vips-java21/src/test/resources/{junit-platform.properties,logback-test.xml}`
- **details**: Per spec §5.2. Java 21 toolchain, `kotlin { jvmToolchain(21) }`, `options.release.set(21)`, JUnit Platform `vips-required` exclude/include logic, `forkEvery = 1`, `api(Libs.jvips)`, `implementation(Libs.commons_io)` (required for `BoundedInputStream(50MB)` in T2.4).
- **DoD**: `./gradlew :bluetape4k-images-vips-java21:build -x test` succeeds; default `:test` skips vips-required tests.

### T0.4 — Create images-vips-java25 module skeleton + build.gradle.kts
- **complexity**: medium
- **deps**: T0.1, T0.2
- **files**:
  - `utils/images-vips-java25/build.gradle.kts`
  - `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/{ops,writer}/.gitkeep`
  - `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/.gitkeep`
  - `utils/images-vips-java25/src/test/resources/{junit-platform.properties,logback-test.xml}`
- **details**: Per spec §5.3. Java 25 toolchain, `kotlin { jvmToolchain(25) }`, `options.release.set(25)`, `jvmTargetValidationMode = WARNING`, `api(Libs.vips_ffm)`, `implementation(Libs.commons_io)` (required for `BoundedInputStream(50MB)` in T3.3), `forkEvery = 1` (FfmVipsRuntime.init() is once-per-JVM, same as java21). **Important**: `jvmArgs("--enable-native-access=ALL-UNNAMED")` must be added to ALL `Test` task config blocks — this is **NOT** present in `virtualthread/jdk25` precedent; it is a new requirement for vips-ffm. Also verify: Kotlin `KotlinCompile` task does NOT need this flag (compile-time only; not required).
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
- **details**: BOM uses `rootProject.subprojects` auto-aggregation — new modules are included automatically. **No manual edit needed.** Verify by reading `bluetape4k/bom/build.gradle.kts` and confirming the aggregation pattern. Then run `./gradlew :bluetape4k-bom:generatePomFileForBluetape4kPublication` and grep output POM for `images-vips`.
- **DoD**: Generated BOM POM XML includes all three `bluetape4k-images-vips-*` entries without manual edit.

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
- **details**: Per spec §4.4. `Serializable`, `serialVersionUID = 1L`, `companion object : KLogging()`, `init` validates `quality in 0..100` and `effort in 1..9`, `readResolve()` re-validates, presets `Default/HighQuality/LowBandwidth`. Use `requireXxx()` (NOT `assertXxx()`) per project rule.
- **DoD**: KDoc with `@param` for each field; serialization round-trip test in T4.1.

### T1.3 — Exception hierarchy
- **complexity**: low
- **deps**: T0.2
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsExceptions.kt`
- **details**: `open class VipsException`, `VipsDecodeException`, `VipsEncodeException`, `VipsInitializationException` per spec §4.7.
- **DoD**: All four classes compile; KDoc on each.

### T1.4 — VipsImage interface
- **complexity**: high
- **deps**: T1.1, T1.2, T1.3
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsImage.kt`
- **details**: Per spec §4.1. `interface VipsImage : AutoCloseable`. Properties `width/height/bands`. Methods `resize/thumbnail/crop/toBytes/writeTo(path)/writeTo(out, format)`. Korean KDoc with `use {}` example. Document `@throws VipsDecodeException/VipsEncodeException` where appropriate.
- **DoD**: Interface compiles; exhaustive KDoc; security note about path traversal on `writeTo(Path)`.

### T1.5 — VipsRuntime interface
- **complexity**: medium
- **deps**: T1.3
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsRuntime.kt`
- **details**: Per spec §4.2. `init(concurrency = 4, maxPixels = 150_000_000L)`, `shutdown()`, `isInitialized`. KDoc must specify thread-safety contract (idempotent, AtomicBoolean/synchronized) and that `shutdown()` is NOT auto-registered as JVM hook.
- **DoD**: Interface compiles; thread-safety contract documented in KDoc.

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

### T2.1 — NativeHandle reference-count guard
- **complexity**: high
- **deps**: T0.3
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/internal/NativeHandle.kt`
- **details**: Internal class wrapping JVips native pointer with `AtomicInteger` ref count + `java.lang.ref.Cleaner` registration. `acquire()/release()` semantics. `Cleaner` callback logs warning if released via cleaner (i.e., user forgot `close()`). Used by `JVipsImage`.
- **DoD**: Unit-testable independent of libvips (mock the underlying release lambda); KDoc documents leak-detection contract.

### T2.2 — JVipsRuntime implementation
- **complexity**: high
- **deps**: T1.5, T0.3
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/JVipsRuntime.kt`
- **details**: Object implementing `VipsRuntime`. Use `AtomicBoolean` for `isInitialized` (not method-local atomicfu — class property only per project rule). Wrap `Vips.init(...)` from `com.criteo.vips`; on failure throw `VipsInitializationException`. Configure `concurrency` and `maxPixels` via JVips API. `shutdown()` calls `Vips.shutdown()`. **CRITICAL — shutdown is terminal**: per libvips docs (`vips_shutdown` page), `VIPS_INIT()` MUST NOT be called after `vips_shutdown()`. Therefore `shutdown()` sets `isInitialized = false` but also sets a separate `isShutdown: AtomicBoolean = true`; any subsequent `init()` call after shutdown throws `VipsInitializationException("libvips has been shut down — restart the process")`. **Init failure semantics** (before shutdown): if `init()` throws before native call completes, `isInitialized` remains `false` (retry allowed). Once shutdown, retry is permanently forbidden.
- **DoD**: Idempotent init; init under contention does not call `Vips.init()` twice; `init()` after `shutdown()` throws `VipsInitializationException`; `isInitialized` stays `false` on init failure (pre-shutdown only).

### T2.3 — JVipsImage implementation
- **complexity**: high
- **deps**: T1.4, T2.1
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/JVipsImage.kt`
- **details**: `internal class JVipsImage(private val handle: NativeHandle, ...) : VipsImage`. `width/height/bands` read once at construction (immutable per project rule). `resize(width, height)`, `thumbnail(maxDimension)`, and `crop(left, top, width, height)` all produce **new** `JVipsImage` instances (no mutation) — **`crop` is implemented directly in this file** (not a separate ops file; spec §3.2 module tree does not list `JVipsCrop.kt`). `toBytes(format, options)` dispatches to writer (T2.7–T2.9). `close()` releases the handle. Wrap all `com.criteo.vips.VipsException` → `VipsDecodeException` or `VipsEncodeException` per call site.
- **DoD**: All `VipsImage` methods implemented including `crop`; binding types confined to `internal`; immutability preserved.

### T2.4 — JVipsImageSupport factory functions
- **complexity**: high
- **deps**: T2.3
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/JVipsImageSupport.kt`
- **details**: Top-level `vipsImageOf(File|Path|ByteArray|InputStream): VipsImage` and `suspendVipsImageOf(File|Path|ByteArray): VipsImage` per spec §4.3. Each enforces `maxPixels` cap: use JVips header-read API to get image dimensions before **full pixel evaluation/render** — note this still invokes the native parser for metadata, which is acceptable; the guard prevents memory exhaustion from full pixel decoding. InputStream variant wraps with `BoundedInputStream(50MB)` per spec §8.5. Catch JVips exceptions → `VipsDecodeException`. Korean KDoc with security note on path traversal. **Exception safety**: use `try/finally` or `runCatching` to ensure stream and any partial native handle are released on decode failure.
- **DoD**: All four blocking + three suspend factories compile; `maxPixels` check throws before full pixel decode; KDoc warns about path traversal; InputStream exception path cleans up resources.

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

### T3.1 — FfmVipsRuntime implementation
- **complexity**: high
- **deps**: T1.5, T0.4
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/FfmVipsRuntime.kt`
- **details**: Object implementing `VipsRuntime` via vips-ffm API. ⚠️ **API spike required before implementing**: current vips-ffm docs (https://vipsffm.photofox.app/) show libvips is **auto-initialized** by `VImage`, `VipsHelper`, or `VipsInvoker` — no manual `Vips.init()` call is documented. `VVips` may not exist in public API; `VImage` is the primary class. Executor must read vips-ffm 1.9.6 sources/docs before writing code and adjust `init()/shutdown()` to wrap whatever lifecycle the library actually exposes. **CRITICAL — shutdown is terminal**: same as T2.2 — libvips `vips_shutdown` cannot be followed by re-init; add `isShutdown: AtomicBoolean` guard, `init()` after shutdown throws `VipsInitializationException`. Init failure (pre-shutdown) keeps `isInitialized = false` for retry. JVM args check: warn if `--enable-native-access=ALL-UNNAMED` missing.
- **DoD**: Idempotent init; shutdown is terminal (`init()` after shutdown throws); JVM args warning in place; thread-safe.

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
- **details**: Same factory signatures as T2.4. Enforce `maxPixels` cap before **full pixel evaluation/render** (header-only read via vips-ffm — confirm API from T3.1 spike). `BoundedInputStream(50MB)`. Catch FFM exceptions → `VipsDecodeException`. **Exception safety**: use `try/finally` to ensure `Arena` and stream are closed on decode failure.
- **DoD**: All blocking + suspend factories compile; `maxPixels` check throws before full pixel decode; InputStream exception path cleans up Arena + stream.

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
- **details**: AAA pattern. Verify validation throws `IllegalArgumentException` for `quality = -1`, `quality = 101`, `effort = 0`, `effort = 10`. Verify `Default/HighQuality/LowBandwidth` constants. Serialization round-trip via Kotlin/Java. Use Kluent matchers (`shouldBeEqualTo`, `shouldThrow`). NO `@Tag("vips-required")` — pure logic test.
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
- **deps**: T2.4, T2.5, T2.6, T2.7, T2.8, T2.9, T4.3
- **files**: `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/JVipsImageTest.kt`
- **details**: Cover all assertions from spec §6.2 plus additional coverage for methods not in §6.2:
  - `vipsImageOf(file)` width/height match fixture
  - `resize(800, 600)` exact dimensions
  - `thumbnail(300)` longest side ≤ 300
  - `toBytes(JPEG)` magic `FF D8 FF`
  - `toBytes(PNG)` magic `89 50 4E 47`
  - `toBytes(WEBP)` `RIFF…WEBP`
  - `suspendToBytes(JPEG)` via coroutine on `Dispatchers.IO`
  - `use { }` not leaking — after close, subsequent op throws
  - `crop(0, 0, 100, 100)` result is 100×100 (covers spec §4.1 interface method not in §6.2)
  - `writeTo(OutputStream, JPEG, options)` produces valid JPEG in stream (covers OutputStream overload not in §6.2)
  - Use Kluent matchers (e.g., `bytes.size shouldBeGreaterThan 0`, `width shouldBeEqualTo 800`). Never `(x >= y).shouldBeTrue()` — use `shouldBeGreaterThanOrEqualTo`.
- **DoD**: All 10 assertions pass when libvips present; skipped when absent.

### T4.5 — JVipsResize ops tests (java21)
- **complexity**: low
- **deps**: T2.5, T2.6, T4.3
- **files**: `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/ops/JVipsResizeTest.kt`
- **details**: Focused tests for resize and thumbnail behavior (aspect ratio, edge cases like 1×1, very wide images).
- **DoD**: Passes under `-PincludeTags=vips-required`.

### T4.6 — NativeHandle leak-detection unit test (java21)
- **complexity**: medium
- **deps**: T2.1
- **files**: `utils/images-vips-java21/src/test/kotlin/io/bluetape4k/images/vips/java21/internal/NativeHandleTest.kt`
- **details**: NOT `@Tag("vips-required")` — uses a mock release lambda. Verifies ref-count semantics and Cleaner-triggered release logs warning. Needed because the leak-guard contract is critical.
- **DoD**: Runs in default `:test`; passes.

### T4.7 — AbstractFfmVipsTest base class (java25)
- **complexity**: medium
- **deps**: T3.1, T4.2
- **files**: `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/AbstractFfmVipsTest.kt`
- **details**: Mirror T4.3 against `FfmVipsRuntime`. **Substitution map** (spec §6.1 has a copy-paste artifact): replace `JVipsRuntime` → `FfmVipsRuntime`, `com.criteo.vips.Vips` → `app.photofox.vipsffm.VVips`. The JVM fork comment in spec §6.1 is correct and applies equally here.
- **DoD**: Skips cleanly when libvips absent; uses `FfmVipsRuntime` (not JVipsRuntime).

### T4.8 — FfmVipsImageTest (java25)
- **complexity**: medium
- **deps**: T3.3, T3.4, T3.5, T3.6, T3.7, T3.8, T4.7
- **files**: `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/FfmVipsImageTest.kt`
- **details**: Mirror T4.4 against the FFM implementation. Same 10 assertions (including `crop` and `writeTo(OutputStream)`). Note: spec §6.1 has a copy-paste artifact where `AbstractFfmVipsTest` shows `JVipsRuntime.isInitialized` — use `FfmVipsRuntime.isInitialized` instead.
- **DoD**: All 10 assertions pass when libvips present; skipped otherwise.

### T4.9 — FfmVipsResize ops tests (java25)
- **complexity**: low
- **deps**: T3.4, T3.5, T4.7
- **files**: `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/ops/FfmVipsResizeTest.kt`
- **details**: Mirror T4.5.
- **DoD**: Passes under `-PincludeTags=vips-required`.

**T4 parallelism**: T4.1 and T4.6 are independent of impl modules; T4.3–T4.5 parallel with T4.7–T4.9 (separate modules).

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
T1.4, T1.5 → T2.1, T2.2 (parallel) → T2.3 → T2.4, T2.5, T2.6, T2.7, T2.8, T2.9 (parallel)
T1.4, T1.5 → T3.1 (parallel) → T3.2 → T3.3, T3.4, T3.5, T3.6, T3.7, T3.8 (parallel)
T2.* → T4.3, T4.4, T4.5, T4.6 (parallel)
T3.* → T4.7, T4.8, T4.9 (parallel)
T1.2 → T4.1
T0.2 → T4.2
T4.* + T5.4 → T5.5 → T5.6
T1.7, T2.9, T3.8 → T5.1, T5.2, T5.3 (parallel)
ALL → T5.7, T5.8
```

## Parallelization Recommendations

Two natural parallel tracks once T1 finishes:
- **Track A (java21)**: T2.1 → T2.2 → T2.3 → T2.4–T2.9 → T4.3–T4.6 → T5.2
- **Track B (java25)**: T3.1 → T3.2 → T3.3–T3.8 → T4.7–T4.9 → T5.3

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

- [ ] All 38 tasks complete with their per-task DoD checked.
- [ ] `./gradlew :bluetape4k-images-vips-{api,java21,java25}:build` — green.
- [ ] Default `:test` skips vips tests cleanly (no native loader errors).
- [ ] `-PincludeTags=vips-required` passes all 10 assertions per impl in CI (spec §6.2 base 8 + crop + writeTo(OutputStream) added in plan).
- [ ] `ci.yml` + `nightly-tests.yml` both updated with `test-images-vips` job.
- [ ] All 6 README files (3 modules × 2 languages) complete with Mermaid diagrams.
- [ ] Korean KDoc on every public symbol.
- [ ] BOM module updated.
- [ ] `/wiki-update` executed.
- [ ] Code review (oh-my-claudecode:code-reviewer) passed with 0 HIGH/CRITICAL.
- [ ] Worktree-only commits on `feat/images-vips`.
