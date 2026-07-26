# utils/images 배치 처리 Flow DSL 설계 — Issue #135

- **Issue**: #135 `[utils/images] 성능 / 배치 처리 (Flow DSL / 썸네일 파이프라인 / Tile 처리)`
- **모듈**: `utils/images` (`bluetape4k-images`)
- **작성일**: 2026-04-28
- **브랜치**: `feat/issue-135-images-batch-flow`
- **Worktree**: `.worktrees/feat/issue-135-images-batch-flow`
- **Research**: `docs/superpowers/research/2026-04-28-issue-135-images-batch-research.md`
- **상태**: Draft (Step 2-R review 대상)

---

## 1. 문제, 제약, 미확정 사항

### 1.1 문제

`utils/images`는 scrimage 기반 이미지 로딩, 저장, 필터 DSL, 변환, 분석 기능을 갖추고 있다. 그러나 대량 이미지 입력을 처리할 때 호출자는 다음을 직접 조합해야 한다.

- `Flow<Path>` 또는 `Flow<File>`를 이미지로 로딩
- 필터/변환 DSL 적용
- 출력 writer 선택 및 저장
- 병렬 처리 동시성 제한
- 깨진 입력 이미지 처리
- 썸네일 크기별 output path 계산
- 큰 이미지의 tile split/merge 처리

이 조합을 호출자가 매번 직접 작성하면 error handling과 dispatcher 선택이 흔들리고, batch pipeline 코드가 서비스마다 반복된다.

### 1.2 제약

- 새 외부 의존성은 추가하지 않는다. 기존 `scrimage`, `bluetape4k-coroutines`, `kotlinx-coroutines`로 구현한다.
- blocking 이미지 I/O와 writer 호출은 `Dispatchers.IO` 경로에서 실행한다.
- CPU-bound pixel/tile transform은 `Dispatchers.Default` 경로에서 실행한다. public API는 `ioDispatcher`와 `transformDispatcher`를 분리해 호출자가 실수로 blocking I/O를 CPU dispatcher에 올리거나 CPU-heavy transform을 IO pool에 몰지 않게 한다.
- 대용량/악성 이미지 입력은 fail-fast guard를 둔다. Path/File 입력은 가능한 경우 ImageIO reader로 decode 전 dimensions를 probe하고, decode 후에도 `width * height`를 다시 검증한다. decode 전 probe는 `ImageInputStream.use {}`와 `ImageReader.dispose()`를 보장한다.
- 기존 `ImageFilterChain`, `suspendImmutableImageOf`, `SuspendImageWriter`, `smartCropTo`, `mapParallel`를 우선 재사용한다.
- public API는 한국어 KDoc을 가진다.
- 결과 순서 보장은 기본 목표가 아니다. batch 처리량을 우선하며, 순서 의존 요구는 명시 옵션이 생기기 전까지 비목표로 둔다.

### 1.3 미확정/범위 결정

- Issue 예시는 `Flow<File>.processImages { ... }`를 보여주지만, Java NIO 사용성을 위해 `Flow<Path>`를 1차 API로 두고 `Flow<File>`는 위임 overload로 제공한다.
- `toJpeg(quality = 85)`는 DSL 안에서 "쓰기 가능한 결과"를 만들기 위한 writer 지정으로만 해석한다. 파일은 자동 저장하지 않으며, 저장은 반드시 `WritableImage.writeTo(path)` 또는 thumbnail pipeline이 담당한다. writer를 지정하지 않은 결과와 지정한 결과는 sealed subtype으로 분리해 nullable writer를 피한다.
- 얼굴/객체 인식 기반 smart crop은 비목표다. 기존 `SmartCrop`의 Sobel saliency 휴리스틱을 재사용한다.

---

## 2. 설계 위험 및 실패 모드

### Risk-1: per-image 실패가 전체 Flow를 중단

깨진 이미지 하나가 `ImmutableImage.loader()`에서 예외를 던지면 기본 Flow는 중단된다.

- **실패 모드**: 100장 중 1장이 깨진 경우 99장 처리 결과도 손실된다.
-

**완화**: 결과 타입을 `ImageBatchResult` sealed interface로 모델링하고 `skipFailures = true` 기본값에서는 `Failure`를 방출한 뒤 다음 입력을 계속 처리한다. `Failure`는 실패 stage, source/output, message, validation details를 포함하며, 기본 구현은 `log.warn(e) { ... }`로 skipped failure를 남긴다. 호출자는 `onFailure` callback으로 counter/metrics를 붙일 수 있다. `CancellationException`은 failure로 감싸지 않고 항상 전파한다.
- **DoD**: 깨진 파일 포함 batch 테스트에서 success N건 + failure 1건이 모두 방출되어야 한다.

### Risk-2: parallelism 검증 부재

`parallelism = 0`이 조용히 1로 보정되면 호출자 설정 오류가 숨겨진다.

-

**완화**: public batch API의 `parallelism`은 `requirePositiveNumber("parallelism")`로 즉시 거부한다. 내부 재사용 함수가 보정하더라도 외부 API 계약은 명시적으로 실패시킨다.
- **DoD**: `parallelism <= 0` 입력 검증 테스트.

### Risk-3: 결과 순서 오해

`mapParallel`은 `flatMapMerge(concurrency)` 기반이라 결과 순서를 보장하지 않는다.

- **완화**: KDoc/README에 "입력 순서 보장 없음"을 명시한다. 순서가 필요한 사용자는 `parallelism = 1`을 선택하게 한다.
- **Rejected**: 기본 순서 보장 `AsyncFlow` 적용. 처리량 이슈 해결이 목적이며, 순서 보장을 기본으로 삼으면 API 목적과 맞지 않는다.

### Risk-4: output path 충돌

다중 크기 썸네일 생성 시 `image.jpg`에서 `150x150`, `300x300` 결과가 같은 파일명으로 쓰이면 덮어쓰기 위험이 있다.

-

**완화**: 기본 파일명 정책을 `{sourceHash}_{stem}_{width}x{height}.{ext}`로 고정해 서로 다른 디렉터리의 같은 파일명 충돌을 줄인다. 사용자 지정 `ThumbnailOutputName` 전략을 제공하며, 최종 output path set에 중복이 생기면 쓰기 전에 실패한다.

- **보안
  완화**: output name과 extension은 path separator, absolute path, `..`, Windows drive prefix를 거부한다. 최종 경로는 `outputDir.resolve(name).normalize()` 뒤 `outputDir.toAbsolutePath().normalize()` 하위인지 검증한다.
- **DoD**: 동일 source에서 3개 size를 생성하면 output path 3개가 모두 달라야 한다.

### Risk-5: JPEG 품질 기반 테스트의 픽셀 비교 flaky

JPEG는 손실 압축이라 pixel identity 비교가 불가능하다.

-

**완화**: tile split/merge pixel identity는 in-memory `ImmutableImage` 또는 PNG writer 기준으로만 수행한다. JPEG thumbnail은 dimensions/file existence/bytes size를 검증한다.

### Risk-6: tile merge 경계 오차

tile 좌표나 마지막 tile 크기 계산이 틀리면 경계 픽셀이 중복되거나 빠진다.

- **완화**: `ImageTile(x, y, width, height, image)`에 원본 좌표를 보존하고 merge는 좌표 기반으로 그린다.
- **DoD**: `splitToTiles(tileSize).mergeTiles()`가 원본과 모든 픽셀 동일해야 한다.

### Risk-7: 대용량 이미지 메모리 사용량

scrimage `ImmutableImage`는 in-memory 이미지이므로 gigapixel 이미지를 한 번에 merge하면 메모리 사용량이 크다.

-

**완화**: 이번 범위는 in-memory tile 처리 API로 명시하되, `maxPixels`, `maxInFlightPixels`, `maxTileCount` guard를 둔다. 기본값은 `DEFAULT_MAX_PIXELS = 16_777_216L`(16M pixels, ARGB 약 64MB), `DEFAULT_MAX_IN_FLIGHT_PIXELS = DEFAULT_MAX_PIXELS * 2`, `DEFAULT_MAX_TILE_COUNT = 65_536` 같은 named constant로 정의한다. `maxInFlightPixels / maxPixels`로 decode/process 동시성을 상한 조정해 `availableProcessors()`개의 대형 이미지를 동시에 디코딩하지 않는다. 초과 시 decode/processing 전후에 `IllegalArgumentException` 또는 `Failure(stage = VALIDATION)`로 fail-fast 한다. 진짜 out-of-core streaming tile은 별도 이슈로 분리한다.
- **KDoc 경고**: `TileProcessor`는 메모리 절감이 아니라 tile 단위 병렬 처리 API임을 명시한다.

### Risk-9: timeout/cancellation 계약 불명확

이미지 decode/encode/filter는 파일시스템이나 malformed input에 따라 매우 오래 걸릴 수 있다.

-

**완화**: API 내부에서 임의 timeout을 강제하지 않는다. 대신 모든 suspend 경로는 구조적 concurrency 안에서 실행하고 cancellation을 삼키지 않는다. README/KDoc에 per-image timeout이 필요하면 호출자가 `withTimeout`으로 감싸야 함을 명시한다.

### Risk-8: 내부 DSL visibility

`ImageFilterChain.compactAndApply`는 `internal`이라 batch 패키지에서 직접 호출할 수 없다.

-

**완화**: `processImages` 구현은 public `applyFilters`를 `withContext(options.transformDispatcher)` 안에서 호출한다. `suspendApplyFilters`는 `Dispatchers.Default`를 고정하므로 이 feature의 dispatcher 주입 계약에는 사용하지 않는다. `compactAndApply` visibility 확장은 하지 않는다.

---

## 3. 접근 방법 비교

### 접근 A: 기존 DSL을 그대로 호출자가 조합

```kotlin
files.asFlow()
    .mapParallel(parallelism, Dispatchers.IO) { path ->
        suspendImmutableImageOf(path)
            .suspendApplyFilters { gaussianBlur(1) }
            .suspendWrite(SuspendJpegWriter.Default, output(path))
    }
```

- **장점**: 새 API가 거의 필요 없다.
- **단점**: 실패 이미지 skip, output naming, result metadata, thumbnail 다중 size 처리가 모두 호출자 반복 코드가 된다.
- **판정**: 스코프 미충족.

### 접근 B: 단일 만능 `ImageBatchPipeline`

```kotlin
ImageBatchPipeline.builder()
    .input(files)
    .transform { ... }
    .thumbnails { ... }
    .tiles { ... }
    .run()
```

- **장점**: batch 기능을 한 객체에 모을 수 있다.
- **단점**: Flow 확장 함수 요구사항과 멀어지고, thumbnail/tile까지 한 builder에 섞여 API가 무거워진다.
- **판정**: 과한 중앙집중 구조라 bluetape4k의 extension/DSL 관례와 맞지 않는다.

### 접근 C: 작고 독립적인 세 API 표면 — **선택**

1. `Flow<Path>.processImages(...)` / `Flow<File>.processImages(...)`
2. `ThumbnailPipeline`
3. `TileProcessor`

- **장점**:
    - Issue의 3개 요구를 각각 독립 API로 제공한다.
    - 기존 DSL/Writer/Flow helper를 조합하므로 새 추상화가 얇다.
    - thumbnail과 tile 처리를 batch DSL에 억지로 넣지 않는다.
- **단점**: 사용자가 세 API를 함께 쓰려면 조합 코드가 필요하다.
- **판정**: 기존 `utils/images`의 기능별 패키지 분리와 가장 잘 맞는다.

---

## 4. 아키텍처 옵션

### Option 1: `batch/` 패키지 중심

```
io.bluetape4k.images.batch
├── ImageBatchProcessing.kt
├── ImageBatchResult.kt
├── ImageProcessingDsl.kt
├── ThumbnailPipeline.kt
└── TileProcessor.kt
```

- 단일 batch 패키지에 관련 타입을 모은다.
- 호출자는 `io.bluetape4k.images.batch.*` 하나만 import하면 된다.
- tile 처리가 batch와 약간 다른 성격인데도 같은 패키지에 들어간다.

### Option 2: 목적별 패키지 분리

```
io.bluetape4k.images.batch
io.bluetape4k.images.thumbnail
io.bluetape4k.images.tiles
```

- 기능별 응집도가 높다.
- README/문서도 기능별로 나누기 쉽다.
- 파일 수와 import가 늘어난다.

### Option 3: 기존 패키지에 흡수

- `filters.dsl`에 batch DSL 추가
- `splitter`에 tile 처리 추가
- top-level에 thumbnail 추가

장점은 파일 이동이 적지만, package responsibility가 흐려진다.

### 선택

**Option 2를 선택**한다.

- `batch`: Flow 입력 처리와 result model
- `thumbnail`: 다중 size output pipeline
- `tiles`: tile split/process/merge

기존 `splitter/ImageSplitter`는 긴 이미지를 height 기준으로 byte stream 분할하는 API이므로 2D tile processor와 같은 패키지에 섞지 않는다.

---

## 5. API 설계

### 5.1 Batch Flow DSL

```kotlin
package io.bluetape4k.images.batch

sealed interface ImageBatchResult {
    val source: Path

    sealed interface Success : ImageBatchResult {
        val image: ImmutableImage
    }

    data class Image(
        override val source: Path,
        override val image: ImmutableImage,
    ) : Success

    data class WritableImage(
        override val source: Path,
        override val image: ImmutableImage,
        val writer: SuspendImageWriter,
    ) : Success

    data class Failure(
        override val source: Path,
        val stage: ImageBatchFailureStage,
        val cause: Throwable,
        val message: String,
        val output: Path? = null,
        val details: Map<String, String> = emptyMap(),
    ) : ImageBatchResult
}

enum class ImageBatchFailureStage {
    VALIDATION,
    LOAD,
    TRANSFORM,
    WRITE,
}

data class ImageProcessingOptions(
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val transformDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val parallelism: Int = defaultImageBatchParallelism(),
    val maxPixels: Long = DEFAULT_MAX_PIXELS,
    val maxInFlightPixels: Long = DEFAULT_MAX_IN_FLIGHT_PIXELS,
    val skipFailures: Boolean = true,
    val onFailure: suspend (ImageBatchResult.Failure) -> Unit = {},
)

@ImageFilterDsl
class ImageProcessingDsl internal constructor() {
    fun resize(width: Int, height: Int)
    fun fit(width: Int, height: Int)
    fun gaussianBlur(radius: Int = 2)
    fun watermark(text: String, ...)
    fun watermark(logo: ImmutableImage, position: Position = Position.BottomRight, alpha: Float = 1.0f)
    fun smartCrop(width: Int, height: Int)
    fun toJpeg(quality: Int = 80, progressive: Boolean = false)
    fun writer(writer: SuspendImageWriter)
    fun filters(block: ImageFilterChain.() -> Unit)
}

fun Flow<Path>.processImages(
    options: ImageProcessingOptions = ImageProcessingOptions(),
    block: ImageProcessingDsl.() -> Unit,
): Flow<ImageBatchResult>

fun Flow<File>.processImages(...): Flow<ImageBatchResult>
```

설계 규칙:

- 기본값과 검증 기준은 magic number로 직접 쓰지 않는다. `DEFAULT_MAX_PIXELS`, `DEFAULT_MAX_IN_FLIGHT_PIXELS`, `DEFAULT_MAX_TILE_COUNT`, `JPEG_QUALITY_MIN`, `JPEG_QUALITY_MAX`, `PERFORMANCE_SAMPLE_IMAGE_COUNT`처럼 의미가 드러나는 top-level `const val` 또는 `val`로 정의한다. `Runtime.getRuntime().availableProcessors()`처럼 런타임 계산이 필요한 값은 `defaultImageBatchParallelism()` 같은 named helper로 감싼다.
- `ImageProcessingDsl`은 내부적으로 `(ImmutableImage) -> ImmutableImage` steps와 optional writer를 보관한다.
- `filters { ... }`를 통해 기존 `ImageFilterChain`을 그대로 포함할 수 있다. 구현은 `suspendApplyFilters`를 그대로 호출하지 않고 `withContext(options.transformDispatcher) { image.applyFilters(block) }` 경로를 사용한다. 기존 `suspendApplyFilters`는 `Dispatchers.Default`를 고정하므로 dispatcher 주입 API의 구현 근거로 쓰지 않는다.
- 선언 순서대로 transform step을 실행한다. `filters {}`도 선언 위치의 한 step으로 적용된다.
- `watermark(text, ...)`는 기존 text watermark DSL을 위임한다. `watermark(logo, position, alpha)`는 Issue #135 예시의 로고 워터마크를 1차 범위에 포함하며, `ImmutableImage.copy().awt().createGraphics()` 또는 기존 `BufferedImage.drawImage` helper를 사용해 위치 기반 overlay를 구현한다. logo 크기 조정은 caller 책임이며, `alpha`는 `0.0f..1.0f` 범위로 검증한다.
- `toJpeg`/`writer`를 호출하면 결과는 `ImageBatchResult.WritableImage`가 된다. writer를 지정하지 않으면 `ImageBatchResult.Image`가 된다. 이로써 nullable writer로 인한 런타임 footgun을 제거한다.
- `toJpeg`/`writer`를 여러 번 호출하면 마지막 값으로 덮어쓰지 않고 `IllegalArgumentException`으로 거부한다.
- `toJpeg(quality)`는 `JPEG_QUALITY_MIN..JPEG_QUALITY_MAX` 범위로 검증한다.
- `toJpeg`/`writer`는 writer 선택만 수행하고 파일을 저장하지 않는다. KDoc/README 예제는 반드시 collection + `writeTo`까지 보여준다.
- 로딩/쓰기 I/O는 `options.ioDispatcher`, resize/filter/smartCrop 등 CPU-bound transform은 `options.transformDispatcher`에서 실행한다.
- 기존 `SuspendImageWriter.suspendWrite`는 `Dispatchers.IO`를 고정하므로 `writeTo(..., ioDispatcher)` 구현은 `withContext(ioDispatcher) { Files.newOutputStream(path).use { writer.write(image, metadata, it) } }` 방식으로 직접 blocking writer를 감싼다. dispatcher-aware writer overload를 별도 추가하지 않는다.
- `options.skipFailures = false`이면 첫 실패를 Flow 예외로 전파한다.
- 실패를 전파할 때는 source/stage context를 가진 `ImageBatchException`으로 감싸되, `CancellationException`은 감싸지 않고 그대로 전파한다.
- `options.maxPixels`를 decode 전 probe와 decode 후 image dimensions 검증에 적용한다.
- `options.maxInFlightPixels`는 `WeightedSemaphore` 성격의 내부 pixel permit으로 구현한다. decode 전 probe로 `width * height`를 계산하고, `min(pixelCount, maxPixels)`만큼 permit을 획득한 뒤 load/transform/write result 생성이 끝나면 `finally`에서 해제한다. encoded output bytes와 thumbnail 결과 파일 크기는 계산에 포함하지 않는다. decode 전 dimensions를 알 수 없는 입력은 `maxPixels` permit을 획득한 뒤 decode 후 실제 pixel count로 검증한다. permit 획득 중 cancellation이 발생하면 permit을 누수하지 않는다.
- ImageIO dimension probe는 `ImageInputStream.use {}`와 `ImageReader.dispose()`를 항상 수행한다.

### 5.2 Processed image convenience

`ImageBatchResult.WritableImage`는 쓰기 편의를 제공한다.

```kotlin
suspend fun ImageBatchResult.WritableImage.writeTo(
    path: Path,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): Long

suspend fun ImageBatchResult.Image.writeTo(
    path: Path,
    writer: SuspendImageWriter,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): Long
```

- `WritableImage.writeTo(path)`는 DSL에서 지정된 writer를 사용한다.
- `Image.writeTo(path, writer)`는 호출 시점 writer를 필수로 받는다.
- 두 함수 모두 `ioDispatcher`에서 직접 `writer.write(image, outputStream)`을 호출한다. 기존 `ImmutableImage.suspendWrite(writer, path)`는 `Dispatchers.IO`를 고정하므로 이 API에서는 사용하지 않는다.

### 5.3 ThumbnailPipeline

```kotlin
package io.bluetape4k.images.thumbnail

data class ThumbnailSize(
    val width: Int,
    val height: Int,
    val crop: ThumbnailCrop = ThumbnailCrop.Fit,
)

sealed interface ThumbnailCrop {
    data object Fit : ThumbnailCrop
    data class Smart(val strategy: SaliencyStrategy = SaliencyStrategy.SobelEnergy) : ThumbnailCrop
    data object Cover : ThumbnailCrop
}

data class ThumbnailResult(
    val source: Path,
    val size: ThumbnailSize,
    val output: Path,
    val status: ThumbnailStatus,
)

sealed interface ThumbnailStatus {
    data class Success(val bytes: Long) : ThumbnailStatus
    data class Failure(
        val stage: ImageBatchFailureStage,
        val cause: Throwable,
        val message: String,
        val details: Map<String, String> = emptyMap(),
    ) : ThumbnailStatus
}

data class ThumbnailFormat(
    val writer: SuspendImageWriter,
    val extension: String,
)

fun interface ThumbnailOutputName {
    fun outputName(source: Path, size: ThumbnailSize, format: ThumbnailFormat): String
}

class ThumbnailPipeline private constructor(...) {
    fun process(sourceImages: Flow<Path>): Flow<ThumbnailResult>
    fun process(sourceImages: Iterable<Path>): Flow<ThumbnailResult>

    companion object {
        fun builder(): Builder
    }
}
```

Builder surface:

```kotlin
ThumbnailPipeline.builder()
    .sizes(listOf(ThumbnailSize(150, 150, ThumbnailCrop.Smart())))
    .format(ThumbnailFormat(SuspendJpegWriter.Default.withCompression(80), "jpg"))
    .outputDir(Path.of("/thumbnails"))
    .outputName(ThumbnailOutputName { source, size, format -> /* optional custom filename */ })
    .parallelism(defaultImageBatchParallelism())
    .ioDispatcher(Dispatchers.IO)
    .transformDispatcher(Dispatchers.Default)
    .maxPixels(DEFAULT_MAX_PIXELS)
    .maxInFlightPixels(DEFAULT_MAX_IN_FLIGHT_PIXELS)
    .skipFailures(true)
    .onFailure { failure -> /* metrics/log hook */ }
    .build()
```

설계 규칙:

- 기본 output name은 `{sourceHash}_{sourceStem}_{width}x{height}.{extension}`.
- 사용자 지정 `ThumbnailOutputName`은 source/size/format을 받아 파일명만 반환한다.
- `ThumbnailFormat`은 writer와 extension을 한 값 객체로 묶는다. `extension`은 blank를 거부하고 leading dot은 제거/정규화한다.
- output name과 extension은 path separator, absolute path, `..`, Windows drive prefix를 거부한다. 최종 output은 `outputDir.resolve(name).normalize()`가 normalized absolute outputDir 하위인지 확인한다.
- pipeline은 쓰기 전에 source/size별 output path를 계산해 중복 path가 있으면 전체 Flow를 시작하기 전 validation failure로 거부한다.
- 각 source/size 조합은 독립 결과를 방출한다.
- `skipFailures = true`이면 thumbnail 실패를 `ThumbnailStatus.Failure`로 방출하고 기본 `KLogging` warn 로그를 남긴다. `onFailure` callback은 failure마다 호출되어 metrics를 붙일 수 있다.
- `skipFailures = false`이면 source/size context를 가진 `ImageBatchException`으로 실패를 전파한다. `CancellationException`은 감싸지 않고 그대로 전파한다.
- Thumbnail pipeline도 `ioDispatcher`, `transformDispatcher`, `maxPixels`, `maxInFlightPixels` 옵션을 갖고 batch DSL과 동일한 pixel permit 계약을 사용한다.
- `process(...)`는 cold Flow factory이므로 `suspend`가 아니다.
- `ThumbnailSize.width/height`, builder `parallelism`, `maxPixels`, `maxInFlightPixels`는 양수여야 한다.

### 5.4 TileProcessor

```kotlin
package io.bluetape4k.images.tiles

data class TileSize(val width: Int, val height: Int)

data class ImageTile(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val image: ImmutableImage,
)

class TileProcessor(
    val tileSize: TileSize = TileSize(512, 512),
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val parallelism: Int = defaultImageBatchParallelism(),
    val maxPixels: Long = DEFAULT_MAX_PIXELS,
    val maxTileCount: Int = DEFAULT_MAX_TILE_COUNT,
) {
    fun splitToTiles(image: ImmutableImage): List<ImageTile>

    suspend fun processTiles(
        image: ImmutableImage,
        transform: suspend (ImageTile) -> ImageTile,
    ): List<ImageTile>

    fun mergeTiles(
        tiles: Iterable<ImageTile>,
        width: Int,
        height: Int,
    ): ImmutableImage
}
```

설계 규칙:

- `TileSize`, `parallelism`, `maxPixels`, `maxTileCount`는 모두 양수 검증.
- split 전 source image의 `width * height <= maxPixels`를 검증한다.
- split 결과 tile 수가 `maxTileCount`를 넘으면 즉시 실패한다.
- split은 마지막 row/column tile의 width/height를 원본 경계에 맞춰 줄인다.
- merge는 좌표 기반 draw로 수행한다.
- `processTiles`의 transform은 tile의 `image`만 바꿀 수 있도록 권장한다. 반환 tile이 좌표/크기까지 바꾸는 경우 `mergeTiles`가 duplicate, missing, out-of-bounds, image dimension mismatch를 검증하고 `IllegalArgumentException`으로 실패한다.
- `processTiles`는 `coroutineScope { async(dispatcher) { ... } }`와 `Semaphore(parallelism)`으로 동시 실행 tile 수를 제한한다. tile processor는 이미 단일 decoded image를 입력으로 받으므로 `maxInFlightPixels`를 별도로 두지 않는다.

---

## 6. 파일 계획

예상 추가/변경 파일:

```
utils/images/src/main/kotlin/io/bluetape4k/images/batch/
├── ImageBatchResult.kt
├── ImageProcessingDsl.kt
└── ImageBatchProcessing.kt

utils/images/src/main/kotlin/io/bluetape4k/images/thumbnail/
├── ThumbnailPipeline.kt
├── ThumbnailResult.kt
└── ThumbnailSize.kt

utils/images/src/main/kotlin/io/bluetape4k/images/tiles/
└── TileProcessor.kt

utils/images/src/test/kotlin/io/bluetape4k/images/batch/
utils/images/src/test/kotlin/io/bluetape4k/images/thumbnail/
utils/images/src/test/kotlin/io/bluetape4k/images/tiles/

utils/images/README.md
utils/images/README.ko.md
docs/testlogs/2026-04.md
docs/superpowers/index/2026-04.md
docs/superpowers/INDEX.md
```

---

## 7. 테스트 전략

### 7.1 Batch Flow DSL

- 정상 이미지 5개를 처리해 `Success` 5건 방출.
- 깨진 파일 1개 + 정상 이미지 N개에서 `skipFailures = true`이면 `Failure` 1건 + `Success` N건 방출.
- `skipFailures = false`이면 Flow collection 중 예외 전파.
- `parallelism <= 0`은 `IllegalArgumentException`.
- `writeTo(path, SuspendJpegWriter.Default.withCompression(85))` 결과 파일이 생성되고 byte size > 0.
- `maxPixels`를 넘는 synthetic image는 validation failure 또는 `IllegalArgumentException`으로 실패.
- `maxInFlightPixels`에 따라 대형 이미지 batch의 in-flight 처리 수가 제한된다.
- `onFailure` callback이 failure마다 호출된다.

### 7.2 ThumbnailPipeline

- 한 source에서 3개 size를 생성하면 `ThumbnailResult` 3건과 파일 3개 생성.
- smart crop thumbnail은 정확한 width/height를 가진다.
- 깨진 source는 failure result를 방출하고 다른 source/size 처리를 계속한다.
- `skipFailures = true`에서 기본 warn logging과 `onFailure` callback이 failure마다 실행된다.
- `skipFailures = false`에서는 `ImageBatchException`이 전파되고 `CancellationException`은 그대로 전파된다.
- output name collision이 없어야 한다.
- `ThumbnailFormat(SuspendJpegWriter.Default, "")`와 `ThumbnailSize(width <= 0)`는 거부된다.
- output name이 `../evil.jpg`, absolute path, path separator, Windows drive prefix를 포함하면 거부된다.

### 7.3 TileProcessor

- `TILE_TEST_IMAGE_WIDTH x TILE_TEST_IMAGE_HEIGHT` synthetic image를 `TILE_TEST_SIZE` tile로 나누면 마지막 row/column 크기가 올바르다.
- 무변환 split/merge 결과가 원본과 픽셀 동일하다.
- tile별 transform을 적용하면 merge 결과가 해당 tile 영역에 반영된다.
- `tileSize <= 0`, `parallelism <= 0` 검증.
- duplicate/missing/out-of-bounds tile geometry는 merge에서 결정적으로 실패한다.
- `maxPixels`, `maxTileCount` 초과 입력은 fail-fast 한다.

### 7.4 성능 회귀

- 단일 스레드 대비 N배 개선을 하드 assertion으로 두지 않는다. CI machine 편차가 크기 때문이다.
- 대신 동시성 상한/결과 수/실패 지속성을 단위 테스트로 고정한다.
- `PERFORMANCE_SAMPLE_IMAGE_COUNT = 100` synthetic 이미지 또는 fixture 복제 입력으로 `parallelism = 1`과 `parallelism = min(4, availableProcessors)` 처리 시간을 non-gating test log에 기록한다.
- 성능 측정은 pass/fail assertion으로 쓰지 않고 `docs/testlogs/2026-04.md`에 입력 수, 이미지 크기, dispatcher, parallelism, 처리 시간만 남긴다.

---

## 8. 문서화

`utils/images/README.md`와 `README.ko.md`에 다음 섹션을 추가한다.

- Batch Image Processing
- Thumbnail Pipeline
- Tile Processing
- Error handling contract
- Ordering and parallelism notes
- Memory guard and caller-owned timeout/cancellation notes
- Output path containment notes

예시는 다음 흐름을 포함한다.

```kotlin
val results = imageFiles.asFlow()
    .processImages(
        options = ImageProcessingOptions(
            ioDispatcher = Dispatchers.IO,
            transformDispatcher = Dispatchers.Default,
            parallelism = 4,
            maxPixels = DEFAULT_MAX_PIXELS,
            maxInFlightPixels = DEFAULT_MAX_IN_FLIGHT_PIXELS,
        )
    ) {
        resize(800, 600)
        gaussianBlur(radius = 1)
    }
```

---

## 9. DoD

### API

- [ ] `Flow<Path>.processImages(...)` 공개 API가 추가된다.
- [ ] `Flow<File>.processImages(...)` 위임 overload가 추가된다.
- [ ] `ImageProcessingDsl`이 resize/fit/gaussianBlur/smartCrop/writer/toJpeg/filter bridge를 제공한다.
- [ ] `ImageProcessingDsl`이 text watermark와 logo image watermark를 제공한다.
- [ ] `ImageBatchResult.Image` / `WritableImage` / `Failure` 결과 타입이 추가된다.
- [ ] `ImageProcessingOptions`가 `ioDispatcher` / `transformDispatcher` / `maxPixels` / `maxInFlightPixels` / `onFailure`를 제공한다.
- [ ] `ThumbnailPipeline` builder와 `ThumbnailResult`가 추가된다.
- [ ] `ThumbnailPipeline`이 `onFailure`, dispatcher, maxPixels, maxInFlightPixels 옵션을 제공한다.
- [ ] `ThumbnailFormat(writer, extension)` 값 객체가 writer/extension 불일치를 줄인다.
- [ ] `TileProcessor` split/process/merge API가 추가된다.
- [ ] 기본값과 테스트 기준값이 magic number가 아니라 의미 있는 `const val`/named helper로 정의된다.

### Behavior

- [ ] 깨진 이미지 입력이 전체 batch를 중단하지 않고 failure result로 방출된다 (`skipFailures = true`).
- [ ] `skipFailures = false`에서는 원본 예외가 전파된다.
- [ ] `parallelism`, size, quality, extension, maxPixels, maxInFlightPixels, maxTileCount 등 public 입력값 검증이 있다.
- [ ] thumbnail output path containment 검증이 path traversal을 거부한다.
- [ ] 실패 result와 기본 warn logging 또는 `onFailure` callback으로 skipped failure가 관측 가능하다.
- [ ] I/O와 CPU transform dispatcher가 분리되어 있다.
- [ ] dispatcher 주입 API는 `suspendApplyFilters`/`SuspendImageWriter.suspendWrite`의 고정 dispatcher를 우회해 지정 dispatcher에서 실행된다.
- [ ] thumbnail output path는 size별로 충돌하지 않는다.
- [ ] tile split 후 무변환 merge는 원본과 픽셀 동일하다.
- [ ] tile merge geometry validation이 duplicate/missing/out-of-bounds/mismatched dimensions를 거부한다.

### Tests

- [ ] batch DSL 성공/실패/입력 검증 테스트가 있다.
- [ ] thumbnail 다중 size/SmartCrop/failure skip 테스트가 있다.
- [ ] tile split/merge/pixel identity/validation 테스트가 있다.
- [ ] maxPixels/maxInFlightPixels/maxTileCount guard 테스트가 있다.
- [ ] 100개 이미지 batch 처리 시간 non-gating 로그가 있다.
- [ ] `./bin/repo-test-summary -- ./gradlew :bluetape4k-images:test`가 통과한다.

### Documentation

- [ ] 모든 public API에 한국어 KDoc이 있다.
- [ ] `utils/images/README.md`와 `README.ko.md`가 batch/thumbnail/tile 사용법을 설명한다.
- [ ] `docs/superpowers/index/2026-04.md`와 `docs/superpowers/INDEX.md`가 갱신된다.
- [ ] `docs/testlogs/2026-04.md`에 검증 로그가 추가된다.

---

## 10. 초안 Task 목록

| Task | 내용                                                         | complexity |
|------|--------------------------------------------------------------|------------|
| T01  | `ImageBatchResult`와 `ImageProcessingDsl` 모델 추가          | medium     |
| T02  | `Flow<Path/File>.processImages` 구현                         | high       |
| T03  | batch DSL 테스트 작성                                        | medium     |
| T04  | `ThumbnailSize`, `ThumbnailResult`, `ThumbnailPipeline` 구현 | high       |
| T05  | thumbnail pipeline 테스트 작성                               | medium     |
| T06  | `TileProcessor` split/process/merge 구현                     | high       |
| T07  | tile processor pixel identity 테스트 작성                    | medium     |
| T08  | 100-image non-gating 성능 로그 작성                          | low        |
| T09  | README.md / README.ko.md 갱신                                | low        |
| T10  | testlog + superpowers index 갱신                             | low        |
