# bluetape4k-images-vips Design Spec

- **Issue**: #136
- **Module**: `utils/images-vips-api`, `utils/images-vips-java21`, `utils/images-vips-java25`
- **Branch**: `feat/images-vips`
- **Worktree**: `.worktrees/feat/images-vips`
- **Date**: 2026-04-29
- **Status**: Draft — Phase 1 + Phase 2 in scope; Phase 3 deferred
- **Related**: `utils/images` (existing scrimage module), `virtualthread/` (structural pattern)

---

## 1. Background and Goals

### 1.1 Why libvips

The existing `utils/images` module wraps **scrimage** (Java2D / BufferedImage). Convenient but has known production limits:

| Limitation | Impact |
|---|---|
| Whole-image pixel decode | Large JPEGs/PNGs hit OOM at ~50MP+ even with `-Xmx4g` |
| No demand-driven pipeline | Resize-then-crop reads every pixel even when 99% discarded |
| Format gap | AVIF write / HEIC read are `@IncubatingImageApi` with no implementation |
| Speed | Scrimage thumbnails 5–20× slower than libvips on typical photo workloads |

[libvips](https://www.libvips.org/) is a streaming, demand-driven image library powering Wikipedia thumbnailing, Cloudinary, and `sharp` (Node.js). Its pipeline reads only the pixels needed for the output — processing 100 MP photos in under 200 ms and ~50 MB RSS.

### 1.2 Goals

- Provide **3 modules** under `utils/`:
  - `bluetape4k-images-vips-api` — common interfaces and types (binding-neutral)
  - `bluetape4k-images-vips-java21` — JVips (Java 21+) implementation
  - `bluetape4k-images-vips-java25` — vips-ffm (Java 25, FFM API) implementation
- Hide underlying binding types (`com.criteo.vips.*`, `app.photofox.vipsffm.*`) behind bluetape4k API types.
- Implement the AVIF/HEIC interfaces from `utils/images` (deferred to Phase 3).
- Skip Vips tests cleanly when `libvips` is not installed — never fail with native loader errors.

### 1.3 Non-Goals

- Replacing scrimage. `utils/images` stays as the default; `images-vips-*` are opt-in.
- Re-implementing Vips operators in pure Kotlin.
- Building libvips from source.
- Animated WebP / GIF encoding (Phase 3+).

## 1.4 Choosing a Module

| Consumer JDK | Module to use | Binding | Notes |
|---|---|---|---|
| Java 21–24 | `bluetape4k-images-vips-java21` | JVips (JNI) | Default choice; Linux bundle included |
| Java 25+ | `bluetape4k-images-vips-java25` | vips-ffm (FFM) | Requires `--enable-native-access=ALL-UNNAMED` |

Both modules expose identical API via `bluetape4k-images-vips-api`. Switching is a one-line Gradle change.

> **Note on naming**: `java21` / `java25` names mirror `virtualthread/jdk21` / `jdk25` convention, not a minimum Java version requirement for JVips (which is Java 8+). The JVips module chooses Java 21 toolchain to match the bluetape4k baseline.

---

## 2.0 Considered Alternatives (binding selection)

| Option | Description | Rejected because |
|---|---|---|
| **JVips (adopted for java21)** | JNI wrapper, Java 8+, Linux `.so` bundle | Adopted |
| **vips-ffm (adopted for java25)** | Java FFM API, JDK 23+, Maven Central | Adopted |
| **libvips-java (deftrue)** | Alternative JNI wrapper | Maven coordinates unverified; lower adoption than JVips |
| **sharp-java / subprocess** | Run Node.js sharp via ProcessBuilder | Cross-runtime overhead; lifecycle complexity; unnecessary dependency |
| **GraalVM native image** | Ahead-of-time native compilation | Not in bluetape4k JVM-only baseline; increases build complexity |
| **Hand-rolled JNI** | Write own JNI glue | High maintenance; security exposure; defeats purpose of adopting proven library |

## 2. Architecture Decisions

| # | Decision | Choice | Rationale |
|---|---|---|---|
| **D1** | Module structure | **3 sibling modules** (api + java21 + java25) | Mirrors `virtualthread/api` + `jdk21` + `jdk25` pattern. `settings.gradle.kts` auto-registers sibling dirs under `utils/` without changes. |
| **D2** | java21 binding | **JVips** `com.criteo:jvips:8.10.2-38fe1f6` | Java 8+, ships Linux native `.so` bundle (no `apt-get` for pure-JNI use), Criteo production-proven. Module named `java21` for naming parity with `virtualthread/jdk21` pattern, NOT because JVips requires Java 21. **Version pinned to `8.10.2-38fe1f6`** (latest verified on Maven Central as of 2026-04-29; `8.12.x` builds with hash suffixes exist on GitHub but are not yet published to a public Maven repo — will upgrade when available). |
| **D3** | java25 binding | **vips-ffm** `app.photofox.vips-ffm:vips-ffm-core:1.9.6` | JDK 23+ required (FFM finalized in Java 22, but vips-ffm 1.9.x targets JDK 23+). Java 25 toolchain per `virtualthread/jdk25` precedent. Requires `--enable-native-access=ALL-UNNAMED` JVM flag. Confirmed on Maven Central. |
| **D4** | PR scope | **Phase 1 + Phase 2** | Phase 3 (AVIF/HEIC impl, DZI) deferred to follow-up issue. Keeps this PR reviewable. |
| **D5** | Test isolation | `@Tag("vips-required")` + `excludeTags` | Mirrors `utils/science` `slow-netcdf` pattern. Default build skips; CI opts in via `-PincludeTags=vips-required`. |
| **D6** | CI strategy | **Dedicated `test-images-vips` job** | Real libvips codec plugins are not fully bundled in JVips; integration tests need `libvips42` from apt. Separate job avoids slowing main matrix. |
| **D7** | Package | **`io.bluetape4k.images.vips.*`** | Colocates with `images.avif`, `images.heic`, `IncubatingImageApi`. |
| **D8** | Binding hiding | All binding types `internal` | Consumer ABI independent of JVips / vips-ffm; swap is possible in future. |

### D1 — Module Layout Detail

```
utils/
├── images/               → bluetape4k-images        (existing — scrimage)
├── images-vips-api/      → bluetape4k-images-vips-api   (NEW — binding-neutral API)
├── images-vips-java21/   → bluetape4k-images-vips-java21 (NEW — JVips, toolchain 21)
└── images-vips-java25/   → bluetape4k-images-vips-java25 (NEW — vips-ffm, toolchain 25)
```

`settings.gradle.kts` auto-registration: `includeModules("utils", withBaseDir = false)` resolves
`bluetape4k + "-" + dir.name` for each subdirectory, so no `settings.gradle.kts` changes needed.

Dependency chain:
```
bluetape4k-images-vips-java21  ──api──► bluetape4k-images-vips-api
bluetape4k-images-vips-java25  ──api──► bluetape4k-images-vips-api
bluetape4k-images-vips-api     ──api──► bluetape4k-images
```

---

## 3. Module Layouts

### 3.1 images-vips-api

```
utils/images-vips-api/
├── build.gradle.kts
└── src/main/kotlin/io/bluetape4k/images/vips/
    ├── VipsImage.kt              # interface, AutoCloseable
    ├── VipsRuntime.kt            # interface: init(concurrency, maxPixels), shutdown(), isInitialized
    ├── VipsEncodeOptions.kt      # data class : Serializable (quality, lossless, effort)
    ├── VipsImageFormat.kt        # enum (JPEG, PNG, WEBP, @IncubatingImageApi AVIF★, HEIC★)
    └── coroutines/
        └── SuspendVipsOps.kt     # suspend extension fns on VipsImage
```

★ = declared with `@IncubatingImageApi` for Phase 3 completeness; no implementation in this PR.

### 3.2 images-vips-java21

```
utils/images-vips-java21/
├── build.gradle.kts              # toolchain 21, JVips dep
└── src/
    ├── main/kotlin/io/bluetape4k/images/vips/java21/
    │   ├── JVipsImage.kt         # implements VipsImage via com.criteo.vips
    │   ├── JVipsRuntime.kt       # implements VipsRuntime (Vips.init/shutdown)
    │   ├── JVipsImageSupport.kt  # vipsImageOf(File|Path|ByteArray|InputStream)
    │   ├── ops/
    │   │   ├── JVipsResize.kt    # resize extension on JVipsImage
    │   │   └── JVipsThumbnail.kt # thumbnail (most efficient path)
    │   ├── writer/
    │   │   ├── JVipsJpegWriter.kt  # Vips-native writer (VipsImage → ByteArray/Path)
    │   │   ├── JVipsPngWriter.kt
    │   │   └── JVipsWebpWriter.kt
    │   └── internal/
    │       └── NativeHandle.kt   # ref-count guard for JVips handles
    └── test/kotlin/io/bluetape4k/images/vips/java21/
        ├── AbstractJVipsTest.kt  # @BeforeAll Vips.init() probe
        ├── JVipsImageTest.kt     # @Tag("vips-required")
        └── ops/
            └── JVipsResizeTest.kt
```

### 3.3 images-vips-java25

```
utils/images-vips-java25/
├── build.gradle.kts              # toolchain 25, vips-ffm dep
└── src/
    ├── main/kotlin/io/bluetape4k/images/vips/java25/
    │   ├── FfmVipsImage.kt       # implements VipsImage via app.photofox.vipsffm
    │   ├── FfmVipsRuntime.kt     # implements VipsRuntime
    │   ├── FfmVipsImageSupport.kt
    │   ├── ops/
    │   │   ├── FfmVipsResize.kt
    │   │   └── FfmVipsThumbnail.kt
    │   └── writer/
    │       ├── FfmVipsJpegWriter.kt  # Vips-native writer (VipsImage → ByteArray/Path)
    │       ├── FfmVipsPngWriter.kt
    │       └── FfmVipsWebpWriter.kt
    └── test/kotlin/io/bluetape4k/images/vips/java25/
        ├── AbstractFfmVipsTest.kt
        ├── FfmVipsImageTest.kt   # @Tag("vips-required")
        └── ops/
            └── FfmVipsResizeTest.kt
```

> **Writer design note**: The `writer/` classes are **Vips-native** — they operate on `VipsImage`, not on `ImmutableImage`/`AwtImage`. They do **not** implement `SuspendImageWriter` from `bluetape4k-images` (that interface is scrimage-coupled). Each writer exposes a single suspend function, e.g.:
> ```kotlin
> suspend fun JVipsJpegWriter.write(image: VipsImage, dest: Path, options: VipsEncodeOptions = VipsEncodeOptions.Default)
> ```
> Scrimage bridge adapters (wrapping a `VipsImage` behind `SuspendImageWriter`) are out of scope for this PR.

---

## 4. API Design

### 4.1 VipsImage (in api module)

```kotlin
/**
 * libvips 이미지 핸들을 나타내는 인터페이스입니다.
 *
 * 네이티브 리소스를 보유하므로 반드시 [use] 블록 또는 try-finally로 해제해야 합니다.
 *
 * ```kotlin
 * vipsImageOf(file).use { img ->
 *     val resized = img.resize(800, 600)
 *     resized.toBytes(VipsImageFormat.WEBP, VipsEncodeOptions(quality = 80))
 * }
 * ```
 */
interface VipsImage : AutoCloseable {
    val width: Int
    val height: Int
    val bands: Int

    fun resize(width: Int, height: Int): VipsImage
    fun thumbnail(maxDimension: Int): VipsImage
    fun crop(left: Int, top: Int, width: Int, height: Int): VipsImage

    fun toBytes(format: VipsImageFormat, options: VipsEncodeOptions = VipsEncodeOptions.Default): ByteArray
    fun writeTo(path: Path, options: VipsEncodeOptions = VipsEncodeOptions.Default)
    fun writeTo(out: OutputStream, format: VipsImageFormat, options: VipsEncodeOptions = VipsEncodeOptions.Default)
}
```

### 4.2 VipsRuntime (in api module)

```kotlin
/**
 * libvips 런타임 초기화/종료 및 전역 설정을 관리하는 인터페이스입니다.
 */
interface VipsRuntime {
    // init() must be internally thread-safe (AtomicBoolean or synchronized).
    // Calling init() when isInitialized == true is a no-op.
    fun init(concurrency: Int = 4, maxPixels: Long = 150_000_000L)
    fun shutdown()
    val isInitialized: Boolean
}
```

### 4.3 Factory Functions

```kotlin
// VipsImageSupport.kt (both implementations mirror this signature)

/**
 * [file]에서 [VipsImage]를 생성합니다.
 */
fun vipsImageOf(file: File): VipsImage

fun vipsImageOf(path: Path): VipsImage

fun vipsImageOf(bytes: ByteArray): VipsImage

fun vipsImageOf(stream: InputStream): VipsImage

// Suspend variants — distinct names required (Kotlin cannot overload on suspend modifier alone)
suspend fun suspendVipsImageOf(file: File): VipsImage =
    withContext(Dispatchers.IO) { vipsImageOf(file) }

suspend fun suspendVipsImageOf(path: Path): VipsImage =
    withContext(Dispatchers.IO) { vipsImageOf(path) }

suspend fun suspendVipsImageOf(bytes: ByteArray): VipsImage =
    withContext(Dispatchers.IO) { vipsImageOf(bytes) }
```

> **Naming decision**: Kotlin cannot overload on the `suspend` modifier alone. Suspend factory variants therefore use distinct names: `suspendVipsImageOf(...)`. Both blocking and suspend variants are first-class API; neither is deprecated.

### 4.4 VipsEncodeOptions

```kotlin
/**
 * 이미지 인코딩 옵션입니다.
 *
 * @param quality JPEG/WebP 품질 (0–100). PNG는 무시됩니다.
 * @param lossless WebP/AVIF 무손실 인코딩 여부.
 * @param effort 인코딩 연산량 (1=fastest, 9=smallest). WebP/AVIF에 적용.
 */
data class VipsEncodeOptions(
    val quality: Int = 80,
    val lossless: Boolean = false,
    val effort: Int = 4,
) : Serializable {
    init {
        require(quality in 0..100) { "quality must be 0..100, was $quality" }
        require(effort in 1..9) { "effort must be 1..9, was $effort" }
    }

    @Suppress("unused")
    private fun readResolve(): Any {
        require(quality in 0..100) { "quality must be 0..100, was $quality" }
        require(effort in 1..9) { "effort must be 1..9, was $effort" }
        return this
    }

    companion object : KLogging() {
        private const val serialVersionUID = 1L
        val Default = VipsEncodeOptions()
        val HighQuality = VipsEncodeOptions(quality = 95, effort = 6)
        val LowBandwidth = VipsEncodeOptions(quality = 60, effort = 3)
    }
}
```

### 4.5 SuspendVipsOps (coroutines extension in api module)

```kotlin
/**
 * Coroutines 방식으로 [VipsImage]를 [ByteArray]로 인코딩합니다.
 *
 * I/O 작업은 [Dispatchers.IO]에서 실행됩니다.
 */
suspend fun VipsImage.suspendToBytes(
    format: VipsImageFormat,
    options: VipsEncodeOptions = VipsEncodeOptions.Default,
): ByteArray = withContext(Dispatchers.IO) { toBytes(format, options) }

/**
 * Coroutines 방식으로 [VipsImage]를 [path]에 씁니다.
 */
suspend fun VipsImage.suspendWriteTo(
    path: Path,
    options: VipsEncodeOptions = VipsEncodeOptions.Default,
) = withContext(Dispatchers.IO) { writeTo(path, options) }

suspend fun VipsImage.suspendWriteTo(
    out: OutputStream,
    format: VipsImageFormat,
    options: VipsEncodeOptions = VipsEncodeOptions.Default,
) = withContext(Dispatchers.IO) { writeTo(out, format, options) }
```

---

## 4.6 Resource Lifecycle

### Native Handle Cleanup
- `JVipsImage` / `FfmVipsImage` MUST register their native handle with `java.lang.ref.Cleaner`
  as a safety net. If `close()` is not called, the Cleaner logs a warning and releases the handle.
- Callers MUST use `use { }` blocks. Relying on Cleaner for normal cleanup is prohibited.

### Coroutine Cancellation Safety
- Deterministic cleanup on `CancellationException` is NOT guaranteed. Relying on Cleaner is the
  safety net, not a contract. The only guaranteed cleanup path is `use { }`.
- Callers in coroutine contexts MUST use `use {}`:
  ```kotlin
  suspendVipsImageOf(file).use { img ->
      img.resize(800, 600).suspendToBytes(VipsImageFormat.WEBP)
  }
  ```
- If the coroutine is cancelled after `suspendVipsImageOf` returns but before `close()` is called,
  the `Cleaner` will eventually reclaim the native handle (non-deterministic, logged as warning).
- Long-running operations (`resize`, `toBytes`) inside `use {}` are safe because `close()` runs
  in the `finally` block regardless of cancellation.

### VipsRuntime Shutdown Lifecycle
- `VipsRuntime.shutdown()` is NOT registered as a JVM shutdown hook by default (multi-tenant JVM safety).
- Spring Boot consumers: register via `@PreDestroy` or `DisposableBean.destroy()`.
- Standalone JVM consumers: call `Runtime.getRuntime().addShutdownHook(Thread { runtime.shutdown() })`.
- Failing to call `shutdown()` is safe (no data loss) but may delay JVM exit by up to 5 s due to libvips worker threads.

---

## 4.7 Exception Hierarchy

All binding-specific exceptions MUST be translated to bluetape4k types before crossing the API boundary.

```kotlin
/**
 * libvips 작업 중 발생한 예외를 나타냅니다.
 */
open class VipsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * 이미지 디코딩 실패 시 발생합니다.
 */
class VipsDecodeException(message: String, cause: Throwable? = null) : VipsException(message, cause)

/**
 * 이미지 인코딩 실패 시 발생합니다.
 */
class VipsEncodeException(message: String, cause: Throwable? = null) : VipsException(message, cause)

/**
 * VipsRuntime 초기화 실패 시 발생합니다.
 */
class VipsInitializationException(message: String, cause: Throwable? = null) : VipsException(message, cause)
```

Implementations:
- JVips: catch `com.criteo.vips.VipsException` → wrap as `VipsDecodeException` / `VipsEncodeException`
- vips-ffm: catch `app.photofox.vipsffm.*` errors → wrap as appropriate
- All factory functions declare `@throws VipsDecodeException` in KDoc.
- All encode operations declare `@throws VipsEncodeException` in KDoc.

---

## 4.8 Concurrency Policy

### Thread Count Math
- `Dispatchers.IO` default: 64 threads
- `VIPS_CONCURRENCY` default: 4 workers per operation
- Under sustained load: up to 64 × 4 = **256 native threads**
- Native stack: ~512 KB each = **128 MB native memory** (outside JVM heap)

### Recommended Pattern
```kotlin
// Create a bounded dispatcher for vips operations
val Dispatchers.Vips: CoroutineDispatcher
    get() = Dispatchers.IO.limitedParallelism(8)

// In VipsRuntime.init():
init(concurrency = 2)  // 8 IO threads × 2 vips workers = 16 native threads max
```

### VipsRuntime Fallback
- If libvips is unavailable at runtime, `VipsRuntime.init()` throws `VipsInitializationException`.
- Application-level pattern for graceful fallback to scrimage:
  ```kotlin
  val processor: ImageProcessor = runCatching { JVipsRuntime.init(); VipsImageProcessor }
      .getOrElse { ScrimageImageProcessor }
  ```

---

## 5. Build Configuration

### 5.1 images-vips-api/build.gradle.kts

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // api() required: @IncubatingImageApi and SuspendImageWriter are public types from bluetape4k-images
    // that this module's API surface exposes. Consumers need them transitively.
    api(project(":bluetape4k-images"))
    api(project(":bluetape4k-coroutines"))
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))

    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

### 5.2 images-vips-java21/build.gradle.kts

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
kotlin {
    jvmToolchain(21)
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    useJUnitPlatform {
        val include = (project.findProperty("includeTags") as String?)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val excludeProp = (project.findProperty("excludeTags") as String?)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        // Always exclude vips-required by default unless user explicitly opts in via includeTags.
        // User-provided excludeTags ADDS to default exclusion (does not replace it).
        val vipsExclude = if (include.contains("vips-required")) emptyList() else listOf("vips-required")
        val exclude = when {
            excludeProp != null -> (excludeProp + vipsExclude).distinct()
            include.isNotEmpty() -> vipsExclude
            else -> vipsExclude
        }
        if (include.isNotEmpty()) includeTags(*include.toTypedArray())
        if (exclude.isNotEmpty()) excludeTags(*exclude.toTypedArray())
    }
}

dependencies {
    api(project(":bluetape4k-images-vips-api"))
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))

    // JVips — bundles libvips.so on Linux; macOS requires `brew install vips`
    api(Libs.jvips)

    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

### 5.3 images-vips-java25/build.gradle.kts

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}
kotlin {
    jvmToolchain(25)
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
}
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    // vips-ffm requires --enable-native-access for FFM API
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    useJUnitPlatform {
        // same include/exclude pattern as java21
        val include = (project.findProperty("includeTags") as String?)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val excludeProp = (project.findProperty("excludeTags") as String?)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        // Always exclude vips-required by default unless user explicitly opts in via includeTags.
        // User-provided excludeTags ADDS to default exclusion (does not replace it).
        val vipsExclude = if (include.contains("vips-required")) emptyList() else listOf("vips-required")
        val exclude = when {
            excludeProp != null -> (excludeProp + vipsExclude).distinct()
            include.isNotEmpty() -> vipsExclude
            else -> vipsExclude
        }
        if (include.isNotEmpty()) includeTags(*include.toTypedArray())
        if (exclude.isNotEmpty()) excludeTags(*exclude.toTypedArray())
    }
}

dependencies {
    api(project(":bluetape4k-images-vips-api"))
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))

    // vips-ffm — Java 25 FFM API; requires system libvips on all platforms
    api(Libs.vips_ffm)

    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

### 5.4 Libs.kt additions

```kotlin
// Versions object:
const val jvips = "8.10.2-38fe1f6"    // https://mvnrepository.com/artifact/com.criteo/jvips
const val vips_ffm = "1.9.6"           // https://mvnrepository.com/artifact/app.photofox.vips-ffm/vips-ffm-core

// Libs object:
val jvips = "com.criteo:jvips:${Versions.jvips}"
val vips_ffm = "app.photofox.vips-ffm:vips-ffm-core:${Versions.vips_ffm}"
```

---

## 6. Test Design

### 6.1 Test Isolation Strategy

Both `java21` and `java25` modules use `@Tag("vips-required")`. Default `./gradlew test`
excludes Vips tests. CI activates them with `-PincludeTags=vips-required`.

```kotlin
// In build.gradle.kts — vips tests must run in isolated JVM forks
tasks.withType<Test>().configureEach {
    forkEvery = 1  // each test class in its own JVM fork (prevents init() TOCTOU)
    // ...
}
```

```kotlin
// AbstractJVipsTest.kt (java21) / AbstractFfmVipsTest.kt (java25)
@Tag("vips-required")
abstract class AbstractVipsTest {
    companion object : KLogging() {
        @JvmStatic
        @BeforeAll
        fun initVips() {
            // Guard idempotency: Vips.init() must only be called once per JVM.
            // Parallel Gradle test forks each run in a separate JVM, so this is safe.
            // Within a single JVM, multiple test classes share the same static state.
            if (!JVipsRuntime.isInitialized) {
                runCatching { JVipsRuntime.init() }
                    .onFailure {
                        assumeTrue(false, "libvips not available: ${it.message}")
                    }
            }
        }
    }
}
```

### 6.2 Test Coverage Requirements (Phase 1 + 2)

| Test | Assertion |
|---|---|
| `vipsImageOf(file)` | width/height match known fixture |
| `resize(800, 600)` | output dimensions exactly 800×600 |
| `thumbnail(300)` | longest side ≤ 300 |
| `toBytes(JPEG)` | non-empty, valid JPEG header `FF D8 FF` |
| `toBytes(PNG)` | non-empty, valid PNG header `89 50 4E 47` |
| `toBytes(WEBP)` | non-empty, `RIFF...WEBP` header |
| `suspendToBytes(JPEG)` | same via coroutine, runs on IO dispatcher |
| `use { }` not leaking | VipsImage closed → subsequent call throws |

---

## 7. CI/CD

### 7.1 New workflow job (ci.yml + nightly-tests.yml)

```yaml
test-images-vips:
  name: Test images-vips modules
  runs-on: ubuntu-24.04  # pin Ubuntu version — libvips42 package stable on 24.04
  needs: build
  steps:
    - uses: actions/checkout@v4
    - name: Set up Java 21
      uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: '21'
    - name: Set up Java 25
      uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: '25'
    - name: Install libvips
      run: |
        sudo apt-get update -qq
        sudo apt-get install -y libvips-tools  # libvips-tools depends on runtime; Ubuntu 24.04 ships libvips42t64
    - name: Test images-vips-java21
      run: ./gradlew :bluetape4k-images-vips-java21:test -PincludeTags=vips-required --no-daemon
    - name: Test images-vips-java25
      run: ./gradlew :bluetape4k-images-vips-java25:test -PincludeTags=vips-required --no-daemon
```

> ⚠️ **Rule**: `nightly-tests.yml` must be updated in the same PR (project memory rule).

### 7.2 macOS developer setup

```bash
brew install vips
./gradlew :bluetape4k-images-vips-java21:test -PincludeTags=vips-required
```

---

## 8. Risks and Mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| libvips not installed → `UnsatisfiedLinkError` | High (dev env) | `@BeforeAll` probe + `assumeTrue(false)` skip |
| JVips native handle leak | Medium | `NativeHandle` ref-count guard + `use { }` enforced |
| vips-ffm requiring Java 25 breaks consumers | Low | Separate module — consumers opt in explicitly |
| JVips libvips 8.10.2 missing a codec needed by test | Low | Test against common formats (JPEG/PNG/WebP) only in Phase 2 |
| `VIPS_CONCURRENCY` thread explosion | Medium | Default to 4; document `VipsRuntime.init(concurrency=N)` |
| JVips Linux bundle missing transitive codec libs | Medium | CI job installs `libvips42 libvips-tools` from apt for integration tests |
| vips-ffm GH Packages only (not Maven Central) | Medium | Verify Maven Central availability before plan phase; fallback to JitPack |
| Malformed image → JVM crash via native code | High | Sandbox/subprocess for untrusted input |
| Image bomb / pixel flood via crafted file | High | maxPixels cap in VipsRuntime.init() |
| Path traversal via vipsImageOf(path) | Medium | Caller responsibility — document in KDoc |

## 8.5 Security Boundaries

### Trust Model
`bluetape4k-images-vips-*` modules are designed for **trusted-input** scenarios (internal batch jobs, build pipelines). For **user-uploaded images** (untrusted input), additional hardening is required at the application layer.

### Image Bomb / Pixel Flood Defense
- The spec mandates a `maxPixels` limit enforced at the factory boundary before native decode:
  - Default: 150 MP (150,000,000 pixels)
  - Configurable via `VipsRuntime.init(maxPixels = ...)` or env var `VIPS_MAX_PIXELS`
- InputStream variant: MUST wrap with a `BoundedInputStream` capped at 50 MB default.
- These limits are checked BEFORE any native decode is attempted.

### Path Traversal
- `vipsImageOf(file/path)` and `writeTo(path)` do NOT canonicalize or confine paths.
- **Callers are responsible** for validating paths before passing to these APIs.
- KDoc on all path-accepting functions MUST state: "Caller must ensure path is within an allowed directory. This function does not prevent path traversal."

### Malformed Image / Native Crash
- JNI (java21) and FFM (java25) both call native C code. A crafted malformed image can crash the JVM.
- **Recommendation for untrusted input**: Run image processing in an isolated subprocess.
- Risk: documented in §8 risk matrix as HIGH (see Risk row update below).

### `--enable-native-access` Scope
- `ALL-UNNAMED` is required for vips-ffm until it ships as a named module.
- Consumers MUST add `--enable-native-access=ALL-UNNAMED` (or the module name if vips-ffm becomes modular) to their JVM startup args.
- This must be documented prominently in the java25 module README.

---

## 9. Phase 3 (Deferred — out of scope for this PR)

| ID | Feature |
|---|---|
| F1 | `VipsAvifWriter` implements `AvifWriter` (@IncubatingImageApi) |
| F2 | `VipsHeicReader` implements `HeicReader` (@IncubatingImageApi) |
| F3 | JXL (JPEG XL) encode/decode |
| F4 | DZI (Deep Zoom Image) tile generation |
| F5 | Animated WebP / multi-page TIFF |
| F6 | ICC profile embed/strip |
| F7 | vips-ffm migration when bluetape4k baseline moves to Java 25+ |

---

## 10. Definition of Done

### Build / Compile

- [ ] `./gradlew :bluetape4k-images-vips-api:build` — success
- [ ] `./gradlew :bluetape4k-images-vips-java21:build` — success (Java 21 toolchain)
- [ ] `./gradlew :bluetape4k-images-vips-java25:build` — success (Java 25 toolchain)
- [ ] Default `./gradlew :bluetape4k-images-vips-java21:test` — vips tests skipped (not failed)

### Functional (CI with `libvips42` installed)

- [ ] All test assertions in §6.2 pass for java21
- [ ] All test assertions in §6.2 pass for java25
- [ ] `use { }` leak test passes for both

### CI / Workflow

- [ ] `ci.yml` `test-images-vips` job added
- [ ] `nightly-tests.yml` mirrored

### Documentation

- [ ] `README.md` + `README.ko.md` created for all 3 modules
- [ ] Korean KDoc on all public APIs

### Process

- [ ] Work done inside `.worktrees/feat/images-vips`
- [ ] Spec + Plan committed to feature branch before implementation
- [ ] Code review (Step 6-R all Tiers) passed
- [ ] PR created with DoD checklist in body
- [ ] BOM 모듈 업데이트 (bluetape4k-bom에 3개 신규 모듈 추가)
- [ ] /wiki-update 실행
- [ ] README Mermaid UML 다이어그램 포함
- [ ] 코드 리뷰 실행 (oh-my-claudecode:code-reviewer, HIGH/CRITICAL 0 확인)

---

## 11. Open Questions

1. **vips-ffm Maven Central status**: RESOLVED: confirmed on mvnrepository. Latest `1.9.6` is available on Maven Central — no GitHub Packages dependency required.
2. **JVips bundled codec completeness**: Does `com.criteo:jvips:8.10.2-38fe1f6` include libheif/libaom in the Linux bundle for potential Phase 3 AVIF? Needs jar inspection.
3. **java21 module name prefix**: Does `JVips` prefix (e.g. `JVipsImage`) or `Vips21` prefix read better? Confirm during plan.
4. **Common test fixtures**: Shared test image files (JPEG/PNG/WebP samples) — place in `images-vips-api/src/test/resources/` as `testFixtures`, or duplicate per module?

---

## 12. References

- [libvips official docs](https://www.libvips.org/API/current/)
- [criteo/JVips GitHub](https://github.com/criteo/JVips)
- [lopcode/vips-ffm GitHub](https://github.com/lopcode/vips-ffm)
- [app.photofox.vips-ffm on mvnrepository](https://mvnrepository.com/artifact/app.photofox.vips-ffm/vips-ffm-core)
- `utils/science/build.gradle.kts` — `@Tag` + excludeTags pattern (precedent)
- `virtualthread/jdk21/build.gradle.kts` — Java 21 toolchain override pattern
- `virtualthread/jdk25/build.gradle.kts` — Java 25 toolchain override pattern
- `utils/images/src/main/kotlin/io/bluetape4k/images/avif/AvifWriter.kt` — Phase 3 target
- `utils/images/src/main/kotlin/io/bluetape4k/images/heic/HeicReader.kt` — Phase 3 target
