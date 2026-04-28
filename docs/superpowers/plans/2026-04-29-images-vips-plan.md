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

### Task Count by Complexity

| Complexity | Count |
|---|---:|
| high | 9 |
| medium | 18 |
| low | 11 |
| **Total** | **38** |

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
- **details**: Per spec §5.2. Java 21 toolchain, `kotlin { jvmToolchain(21) }`, `options.release.set(21)`, JUnit Platform `vips-required` exclude/include logic, `forkEvery = 1`, `api(Libs.jvips)`.
- **DoD**: `./gradlew :bluetape4k-images-vips-java21:build -x test` succeeds; default `:test` skips vips-required tests.

### T0.4 — Create images-vips-java25 module skeleton + build.gradle.kts
- **complexity**: medium
- **deps**: T0.1, T0.2
- **files**:
  - `utils/images-vips-java25/build.gradle.kts`
  - `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/{ops,writer}/.gitkeep`
  - `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/.gitkeep`
  - `utils/images-vips-java25/src/test/resources/{junit-platform.properties,logback-test.xml}`
- **details**: Per spec §5.3. Java 25 toolchain, `kotlin { jvmToolchain(25) }`, `options.release.set(25)`, `jvmTargetValidationMode = WARNING`, `jvmArgs("--enable-native-access=ALL-UNNAMED")`, `api(Libs.vips_ffm)`.
- **DoD**: `./gradlew :bluetape4k-images-vips-java25:build -x test` succeeds.

### T0.5 — Verify settings.gradle.kts auto-registers all 3 modules
- **complexity**: low
- **deps**: T0.2, T0.3, T0.4
- **files**: read-only check of `settings.gradle.kts`
- **details**: Confirm `includeModules("utils", ...)` picks up `bluetape4k-images-vips-api/java21/java25`. Run `./gradlew projects | rg images-vips`.
- **DoD**: Gradle project list shows all three new modules.

### T0.6 — Add modules to bluetape4k-bom
- **complexity**: low
- **deps**: T0.2, T0.3, T0.4
- **files**: `bluetape4k/bom/build.gradle.kts` (or wherever module list is maintained)
- **details**: Add three `:bluetape4k-images-vips-*` entries to BOM module aggregation.
- **DoD**: `./gradlew :bluetape4k-bom:build` succeeds; BOM POM lists new modules.

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

### T1.6 — Factory function signatures (declared, not implemented in api)
- **complexity**: low
- **deps**: T1.4
- **files**: `utils/images-vips-api/src/main/kotlin/io/bluetape4k/images/vips/VipsImageSupport.kt`
- **details**: This file is **api-side documentation** of expected signatures. The actual factory functions are package-private to each impl module. We document the contract via abstract factory holder OR leave only the suspend wrappers here. Decision: place `suspendVipsImageOf(...)` and `vipsImageOf(...)` as `expect`-style abstract via Kotlin reflection is NOT possible across JVM modules — instead, both impl modules provide identical top-level fns. The api module hosts only the suspend extension wrappers (T1.7).
- **details (revised)**: Skip — factory fns live in impl modules. This task becomes a contract note in spec only (no file).
- **DoD**: Decision recorded in plan: factory fns are duplicated per impl module with identical signatures and KDoc.

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
- **details**: Object implementing `VipsRuntime`. Use `AtomicBoolean` for `isInitialized` (not method-local atomicfu — class property only per project rule). Wrap `Vips.init(...)` from `com.criteo.vips`; on failure throw `VipsInitializationException`. Configure `concurrency` and `maxPixels` via JVips API. `shutdown()` calls `Vips.shutdown()` and resets the flag.
- **DoD**: Idempotent init; init under contention does not call `Vips.init()` twice; throws translated exception on failure.

### T2.3 — JVipsImage implementation
- **complexity**: high
- **deps**: T1.4, T2.1
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/JVipsImage.kt`
- **details**: `internal class JVipsImage(private val handle: NativeHandle, ...) : VipsImage`. `width/height/bands` read once at construction (immutable per project rule). `resize/thumbnail/crop` produce **new** `JVipsImage` instances (no mutation). `toBytes(format, options)` dispatches to writer (T2.5–T2.7). `close()` releases the handle. Wrap all `com.criteo.vips.VipsException` → `VipsDecodeException` or `VipsEncodeException` per call site.
- **DoD**: All `VipsImage` methods implemented; binding types confined to `internal`; immutability preserved.

### T2.4 — JVipsImageSupport factory functions
- **complexity**: medium
- **deps**: T2.3
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/JVipsImageSupport.kt`
- **details**: Top-level `vipsImageOf(File|Path|ByteArray|InputStream): VipsImage` and `suspendVipsImageOf(File|Path|ByteArray): VipsImage` per spec §4.3. Each enforces `maxPixels` cap before native decode (read header first via JVips). InputStream variant wraps with `BoundedInputStream(50MB)` per spec §8.5. Catch JVips exceptions → `VipsDecodeException`. Korean KDoc with security note on path traversal.
- **DoD**: All four blocking + three suspend factories compile; security caps enforced; KDoc warns about path traversal.

### T2.5 — JVipsResize op
- **complexity**: medium
- **deps**: T2.3
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/ops/JVipsResize.kt`
- **details**: Extension implementation. Single-arg `Pair<Int,Int>` or two-int form. Implements via JVips `resize()` operator. Returns new `JVipsImage`.
- **DoD**: Output dimensions match requested values exactly (asserted in T4.4).

### T2.6 — JVipsThumbnail op
- **complexity**: medium
- **deps**: T2.3
- **files**: `utils/images-vips-java21/src/main/kotlin/io/bluetape4k/images/vips/java21/ops/JVipsThumbnail.kt`
- **details**: Use JVips `thumbnail` (most efficient — demand-driven decode). `maxDimension` → preserve aspect ratio, longest side ≤ N.
- **DoD**: Longest side ≤ maxDimension; aspect ratio preserved.

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
- **details**: Object implementing `VipsRuntime` via vips-ffm `VVips`/`Arena` lifecycle. `AtomicBoolean isInitialized`. Configure concurrency/maxPixels via vips-ffm config API. JVM args check (warn if `--enable-native-access=ALL-UNNAMED` missing). Wrap init failures → `VipsInitializationException`.
- **DoD**: Idempotent init; thread-safe; throws translated exception on failure.

### T3.2 — FfmVipsImage implementation
- **complexity**: high
- **deps**: T1.4, T0.4
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/FfmVipsImage.kt`
- **details**: `internal class FfmVipsImage` wrapping vips-ffm `VImage` and an `Arena`. Register `Cleaner` for `Arena.close()` safety net. `width/height/bands` immutable. Operations return new instances. Wrap `app.photofox.vipsffm.*` exceptions → `VipsDecodeException`/`VipsEncodeException`.
- **DoD**: All `VipsImage` methods implemented; `Arena` closed on `close()`; binding types `internal`.

### T3.3 — FfmVipsImageSupport factory functions
- **complexity**: medium
- **deps**: T3.2
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/FfmVipsImageSupport.kt`
- **details**: Same factory signatures as T2.4 (`vipsImageOf`/`suspendVipsImageOf`). Enforce `maxPixels` and `BoundedInputStream(50MB)`. Catch FFM exceptions → `VipsDecodeException`.
- **DoD**: All blocking + suspend factories compile; security caps enforced.

### T3.4 — FfmVipsResize op
- **complexity**: medium
- **deps**: T3.2
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/ops/FfmVipsResize.kt`
- **details**: Mirror T2.5 against vips-ffm.
- **DoD**: Output dimensions match requested values exactly.

### T3.5 — FfmVipsThumbnail op
- **complexity**: medium
- **deps**: T3.2
- **files**: `utils/images-vips-java25/src/main/kotlin/io/bluetape4k/images/vips/java25/ops/FfmVipsThumbnail.kt`
- **details**: Mirror T2.6.
- **DoD**: Longest side ≤ maxDimension.

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
- **deps**: T0.2
- **files**:
  - `utils/images-vips-api/src/testFixtures/kotlin/io/bluetape4k/images/vips/testfixtures/VipsTestFixtures.kt`
  - `utils/images-vips-api/src/testFixtures/resources/fixtures/{sample.jpg,sample.png,sample.webp}`
  - Update `utils/images-vips-api/build.gradle.kts` to enable `java-test-fixtures` plugin.
- **details**: Resolves Open Question #4 — shared fixtures via `testFixtures`. Provides `loadFixture(name): ByteArray` and known dimensions for assertions. Sample images small (~50–200 KB) and license-clean (generate with ImageMagick or check in pre-existing public-domain images).
- **DoD**: `testFixtures(project(":bluetape4k-images-vips-api"))` resolvable from java21/java25 modules; fixtures load successfully.

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
- **details**: Cover all assertions from spec §6.2:
  - `vipsImageOf(file)` width/height match fixture
  - `resize(800, 600)` exact dimensions
  - `thumbnail(300)` longest side ≤ 300
  - `toBytes(JPEG)` magic `FF D8 FF`
  - `toBytes(PNG)` magic `89 50 4E 47`
  - `toBytes(WEBP)` `RIFF…WEBP`
  - `suspendToBytes(JPEG)` via coroutine on `Dispatchers.IO`
  - `use { }` not leaking — after close, subsequent op throws.
  - Use Kluent matchers (e.g., `bytes.size shouldBeGreaterThan 0`, `format shouldBeEqualTo expected`).
- **DoD**: All 8 assertions pass when libvips present; skipped when absent.

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
- **details**: Mirror T4.3 against `FfmVipsRuntime`.
- **DoD**: Skips cleanly when libvips absent.

### T4.8 — FfmVipsImageTest (java25)
- **complexity**: medium
- **deps**: T3.3, T3.4, T3.5, T3.6, T3.7, T3.8, T4.7
- **files**: `utils/images-vips-java25/src/test/kotlin/io/bluetape4k/images/vips/java25/FfmVipsImageTest.kt`
- **details**: Mirror T4.4 against the FFM implementation. Same 8 assertions.
- **DoD**: All 8 assertions pass when libvips present; skipped otherwise.

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
- **details**: Verify Korean KDoc on every public class/interface/extension function. `@throws` declarations on factory and encode operations per spec §4.7.
- **DoD**: No public symbol lacks KDoc.

### T5.5 — Update ci.yml with test-images-vips job
- **complexity**: medium
- **deps**: T2.9, T3.8, T4.4, T4.8
- **files**: `.github/workflows/ci.yml`
- **details**: Add `test-images-vips` job per spec §7.1. Pin `runs-on: ubuntu-24.04`. Install both Java 21 and Java 25 toolchains. `apt-get install -y libvips-tools`. Run `:bluetape4k-images-vips-java21:test -PincludeTags=vips-required` and `:bluetape4k-images-vips-java25:test -PincludeTags=vips-required`.
- **DoD**: CI YAML valid (lint via `actionlint` if available); job present.

### T5.6 — Update nightly-tests.yml with test-images-vips job
- **complexity**: low
- **deps**: T5.5
- **files**: `.github/workflows/nightly-tests.yml`
- **details**: Mirror T5.5 (project memory rule: ci.yml ↔ nightly-tests.yml sync).
- **DoD**: Same job structure present.

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
- [ ] `-PincludeTags=vips-required` passes all 8 assertions per impl in CI.
- [ ] `ci.yml` + `nightly-tests.yml` both updated with `test-images-vips` job.
- [ ] All 6 README files (3 modules × 2 languages) complete with Mermaid diagrams.
- [ ] Korean KDoc on every public symbol.
- [ ] BOM module updated.
- [ ] `/wiki-update` executed.
- [ ] Code review (oh-my-claudecode:code-reviewer) passed with 0 HIGH/CRITICAL.
- [ ] Worktree-only commits on `feat/images-vips`.
