# Module bluetape4k-images

[English](./README.md) | 한국어

JPG, PNG, GIF, WebP, **TIFF/SVG** (Issue #134) 등의 이미지를 로드, 변환, 크기 조절, 분할, 필터 적용 등의 조작을 지원하는 라이브러리입니다.
[Scrimage](https://github.com/sksamuel/scrimage) 라이브러리를 기반으로 하며, Coroutines를 활용한 비동기 이미지 처리를 제공합니다.
AVIF·HEIC는 incubating 인터페이스로 제공되며, 구현체는 `bluetape4k-images-vips` 모듈에서 제공합니다.

## 아키텍처

### 처리 파이프라인

```mermaid
flowchart LR
    subgraph 입력["입력 소스"]
        BA[ByteArray]
        IS[InputStream]
        FILE[File / Path]
    end

    subgraph 이미지처리["이미지 처리 (Scrimage 기반)"]
        II["ImmutableImage<br/>(immutableImageOf)"]
        BI["BufferedImage<br/>(bufferedImageOf)"]
    end

    subgraph 조작["이미지 조작"]
        SC["크기 조절<br/>(ImageScaler)"]
        SP["이미지 분할<br/>(ImageSplitter)"]
        WM["워터마크<br/>(WatermarkFilter)"]
        CP["캡션<br/>(CaptionFilter)"]
        PD["패딩<br/>(PaddingSupport)"]
        TR["변환<br/>(AutoCrop/SmartCrop/회전/원근/CLAHE)"]
    end

    subgraph 출력["비동기 저장 (Coroutines)"]
        JPG["SuspendJpegWriter<br/>(손실 압축)"]
        PNG["SuspendPngWriter<br/>(무손실)"]
        WEBP["SuspendWebpWriter<br/>(최고 압축)"]
        GIF["SuspendGifWriter<br/>(애니메이션)"]
        ANIM["SuspendGif2WebpWriter<br/>(GIF→WebP 변환)"]
        TIFF["SuspendTiffWriter<br/>(단일 페이지)"]
        TIFFM["SuspendTiffMultiPageWriter<br/>(다중 페이지)"]
        SVG["BatikSvgRasterizer<br/>(SVG→래스터)"]
    end

    입력 --> 이미지처리
    이미지처리 --> 조작
    조작 --> 출력

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef utilStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000

    class BA,IS,FILE dataStyle
    class II,BI coreStyle
    class SC,SP,WM,CP,PD utilStyle
    class JPG,PNG,WEBP,GIF,ANIM,TIFF,TIFFM,SVG asyncStyle
```

### 클래스 다이어그램

```mermaid
classDiagram
    class ImmutableImage {
        +width: Int
        +height: Int
        +scale(width, height) ImmutableImage
        +output(writer) ByteArray
    }
    class ImageScaler {
        +scale(image, width, height) ImmutableImage
    }
    class ImageSplitter {
        +split(image, rows, cols) List~ImmutableImage~
    }
    class WatermarkFilter {
        +apply(image) ImmutableImage
    }
    class ImageFilterChain {
        +brightness(factor)
        +contrast(factor)
        +saturation(factor)
        +hue(deltaDegrees)
        +colorTemperature(kelvin)
        +gaussianBlur(radius)
        +medianBlur(radius, boundary)
        +sepia()
        +vignette()
        +roundedCorners(radius)
        +raw(filter)
        +pixel(block)
    }
    class ColorSpaceConverter {
        +rgbToHsv(r, g, b) FloatArray
        +hsvToRgb(h, s, v) IntArray
        +kelvinToRgb(kelvin) IntArray
    }
    class SaturationAdjustFilter {
        +factor: Float
        +apply(image) ImmutableImage
    }
    class HueAdjustFilter {
        +deltaDegrees: Float
        +apply(image) ImmutableImage
    }
    class ColorTemperatureFilter {
        +kelvin: Int
        +apply(image) ImmutableImage
    }
    class MedianBlurFilter {
        +radius: Int
        +boundary: MedianBoundaryMode
        +apply(image) ImmutableImage
    }
    class RoundedCornerFilter {
        +radius: Int
        +apply(image) ImmutableImage
    }
    class SuspendJpegWriter {
        +writeImage(image) ByteArray
    }
    class SuspendPngWriter {
        +writeImage(image) ByteArray
    }
    class SuspendWebpWriter {
        +writeImage(image) ByteArray
    }
    class SuspendGif2WebpWriter {
        +writeImage(image) ByteArray
    }

    ImmutableImage --> ImageScaler : uses
    ImmutableImage --> ImageSplitter : uses
    ImmutableImage --> WatermarkFilter : uses
    ImmutableImage --> ImageFilterChain : applyFilters
    ImmutableImage --> SuspendJpegWriter : output
    ImmutableImage --> SuspendPngWriter : output
    ImmutableImage --> SuspendWebpWriter : output
    ImmutableImage --> SuspendGif2WebpWriter : output
    ImageFilterChain --> SaturationAdjustFilter
    ImageFilterChain --> HueAdjustFilter
    ImageFilterChain --> ColorTemperatureFilter
    ImageFilterChain --> MedianBlurFilter
    ImageFilterChain --> RoundedCornerFilter
    ColorSpaceConverter <-- SaturationAdjustFilter : uses
    ColorSpaceConverter <-- HueAdjustFilter : uses
    ColorSpaceConverter <-- ColorTemperatureFilter : uses

    style ImmutableImage fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style ImageScaler fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style ImageSplitter fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style WatermarkFilter fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style ImageFilterChain fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style ColorSpaceConverter fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style SaturationAdjustFilter fill:#FFF8E1,stroke:#FFD54F,color:#E65100
    style HueAdjustFilter fill:#FFF8E1,stroke:#FFD54F,color:#E65100
    style ColorTemperatureFilter fill:#FFF8E1,stroke:#FFD54F,color:#E65100
    style MedianBlurFilter fill:#FFF8E1,stroke:#FFD54F,color:#E65100
    style RoundedCornerFilter fill:#FFF8E1,stroke:#FFD54F,color:#E65100
    style SuspendJpegWriter fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style SuspendPngWriter fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style SuspendWebpWriter fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style SuspendGif2WebpWriter fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

## 주요 기능

### 이미지 포맷 지원

| 포맷   | Writer/Reader                    | 특징                                                              |
|------|----------------------------------|------------------------------------------------------------------|
| PNG  | `SuspendPngWriter`               | 무손실, 투명도 지원                                                     |
| GIF  | `SuspendGifWriter`               | 애니메이션 지원                                                        |
| JPG  | `SuspendJpegWriter`              | 빠른 처리, 손실 압축                                                     |
| WEBP | `SuspendWebpWriter`              | 최고 압축률, 최신 포맷                                                    |
| TIFF | `SuspendTiffWriter` / `SuspendTiffMultiPageWriter` | 다중 페이지, 다양한 압축 방식 (DEFLATE/LZW/NONE/JPEG) |
| SVG  | `BatikSvgRasterizer`             | 래스터 변환; XXE/SSRF 방어 기본 적용                                        |
| AVIF | `AvifWriter` *(incubating)*      | 인터페이스만 제공, 구현체는 `bluetape4k-images-vips`                         |
| HEIC | `HeicReader` *(incubating)*      | 인터페이스만 제공, 구현체는 `bluetape4k-images-vips`                         |

- **동적 생성**: JPG가 가장 빠름 (실시간 처리용)
- **정적 파일**: WebP가 가장 효율적 (저장 공간 절약)
- **문서 이미징**: TIFF 다중 페이지로 아카이브 워크플로우 지원
- **벡터 그래픽**: Batik SVG 래스터화 (opt-in 의존성)

### 주요 파일

| 파일                                                   | 설명                            |
|------------------------------------------------------|-------------------------------|
| `ImmutableImageSupport.kt`                           | ImmutableImage 생성, 저장, 그래픽 작업 |
| `BufferedImageSupport.kt`                            | BufferedImage 생성, 저장, 그래픽 작업  |
| `ImageFormat.kt`                                     | 지원 이미지 포맷 열거형                 |
| `WriteContextExtensions.kt`                          | 쓰기 컨텍스트 확장 함수                 |
| `IIORegistryUtils.kt`                                | ImageIO 레지스트리 유틸리티            |
| `batch/ImageBatchFlow.kt`                            | Coroutine Flow 기반 배치 이미지 처리   |
| `batch/ImageProcessingDsl.kt`                        | 이름 있는 기본값을 쓰는 배치 변환 DSL   |
| `thumbnail/ThumbnailPipeline.kt`                     | 여러 크기 썸네일 생성 파이프라인        |
| `tiles/TileProcessor.kt`                             | 타일 분할/병합과 병렬 타일 처리         |
| `scaler/ImageScaler.kt`                              | 이미지 크기 조절                     |
| `splitter/ImageSplitter.kt`                          | 이미지 분할                        |
| `filters/WatermarkFilterSupport.kt`                  | 워터마크 필터                       |
| `filters/CaptionFilterSupport.kt`                    | 캡션 필터                         |
| `filters/PaddingSupport.kt`                          | 패딩 필터                         |
| `filters/WatermarkFilterType.kt`                     | 워터마크 타입 (COVER/STAMP)         |
| `analysis/DominantColor.kt`                          | 대표 색상 추출 — `dominantColor()`, `dominantColors()` |
| `analysis/BlurDetector.kt`                           | 블러 감지 — `blurScore()`, `isBlurry()` |
| `analysis/ExifData.kt`                               | EXIF 파싱 — `readExif()`, GPS PII 제거 |
| `similarity/ImageSimilarity.kt`                      | 핵심 유사도: 픽셀 Δ, MSE, PSNR, 전역 SSIM, pHash |
| `similarity/MssimSimilarity.kt`                      | MSSIM — 슬라이딩 윈도우 Gaussian SSIM            |
| `similarity/HashSimilarity.kt`                       | aHash/dHash/wHash/phashOf (64/256/1024bit), HashDistance |
| `similarity/HistogramSimilarity.kt`                  | 색상 히스토그램: ChiSquare, Bhattacharyya, EarthMover |
| `similarity/KeypointSimilarity.kt`                   | Block-Mean descriptor, bestRotationSimilarityTo  |
| `similarity/SimilarityScaleUtils.kt`                 | prepareForSimilarity — MSSIM 전 다운스케일 유틸리티  |
| `fonts/FontSupport.kt`                               | 폰트 유틸리티                       |
| `filters/dsl/ImageFilterChain.kt`                    | 필터/색보정 DSL (`applyFilters`, `suspendApplyFilters`) |
| `filters/dsl/ImageFilterChainDsl.kt`                 | DSL 멤버 함수 (40+ 필터)            |
| `filters/SaturationAdjustFilter.kt`                  | HSV 채도 조절 필터                  |
| `filters/HueAdjustFilter.kt`                         | HSV 색조 회전 필터                  |
| `filters/ColorTemperatureFilter.kt`                  | 켈빈 색온도 조절 필터                 |
| `filters/MedianBlurFilter.kt`                        | 미디언 블러 노이즈 제거 필터             |
| `filters/RoundedCornerFilter.kt`                     | 모서리 둥글게 알파 마스크 필터            |
| `filters/ColorSpaceConverter.kt`                     | RGB/HSV/YCbCr/켈빈 색 공간 변환     |
| `io/ImageInputStreamSupport.kt`                      | 이미지 입력 스트림                    |
| `io/ImageOuptputStreamSupport.kt`                    | 이미지 출력 스트림                    |
| `coroutines/SuspendImageWriter.kt`                   | 비동기 이미지 Writer 인터페이스          |
| `coroutines/SuspendMultiPageImageWriter.kt`          | 비동기 다중 페이지 Writer 인터페이스       |
| `coroutines/SuspendJpegWriter.kt`                    | 비동기 JPEG Writer               |
| `coroutines/SuspendPngWriter.kt`                     | 비동기 PNG Writer                |
| `coroutines/SuspendGifWriter.kt`                     | 비동기 GIF Writer                |
| `coroutines/SuspendWebpWriter.kt`                    | 비동기 WebP Writer               |
| `coroutines/SuspendTiffWriter.kt`                    | 비동기 TIFF Writer (단일 페이지, TwelveMonkeys) |
| `coroutines/SuspendTiffMultiPageWriter.kt`           | 비동기 TIFF 다중 페이지 Writer        |
| `coroutines/TiffCompression.kt`                      | TIFF 압축 방식 (DEFLATE/LZW/NONE/PACKBITS/JPEG) |
| `coroutines/SuspendWriteContext.kt`                  | 비동기 쓰기 컨텍스트                   |
| `coroutines/animated/SuspendAnimatedImageWriter.kt`  | 비동기 애니메이션 Writer              |
| `coroutines/animated/SuspendGif2WebpWriter.kt`       | GIF → WebP 변환 Writer          |
| `coroutines/animated/AnimatedGifExtensions.kt`       | AnimatedGif 확장 함수             |
| `coroutines/animated/SuspendAnimatedWriteContext.kt` | 애니메이션 쓰기 컨텍스트                 |
| `svg/SuspendSvgRasterizer.kt`                        | SVG 래스터라이저 인터페이스              |
| `svg/BatikSvgRasterizer.kt`                          | SVG 래스터라이저 (Apache Batik, XXE-안전) |
| `svg/SvgRasterizeOptions.kt`                         | SVG 래스터화 옵션                   |
| `avif/AvifWriter.kt`                                 | AVIF Writer 인터페이스 *(incubating)* |
| `heic/HeicReader.kt`                                 | HEIC Reader 인터페이스 *(incubating)* |
| `IncubatingImageApi.kt`                              | incubating API용 `@RequiresOptIn` 어노테이션 |

## 사용 예시

### ImmutableImage 로드

```kotlin
import io.bluetape4k.images.*

// ByteArray에서 로드
val image = immutableImageOf(byteArray)

// InputStream에서 로드
val image = immutableImageOf(inputStream)

// 파일에서 로드
val image = immutableImageOf(File("image.jpg"))

// Path에서 로드
val image = immutableImageOf(Paths.get("image.jpg"))

// Coroutines 환경에서 비동기 로드
val image = suspendImmutableImageOf(File("image.jpg"))
val image = suspendLoadImage(Paths.get("image.jpg"))
```

### BufferedImage 로드/저장

```kotlin
import io.bluetape4k.images.*

// 다양한 소스에서 로드
val image = bufferedImageOf(inputStream)
val image = bufferedImageOf(File("image.jpg"))
val image = bufferedImageOf(byteArray)

// 새 이미지 생성
val image = bufferedImageOf(200, 100)

// 저장
image.write(ImageFormat.JPG, File("output.jpg"))
image.write(ImageFormat.PNG, outputStream)

// ByteArray 변환
val bytes = image.toByteArray("png")
```

### 이미지 저장 (Coroutines)

```kotlin
import io.bluetape4k.images.*
import io.bluetape4k.images.coroutines.*

val image = immutableImageOf(File("input.png"))

// JPEG로 저장 (80% 품질)
image.suspendWrite(SuspendJpegWriter(compression = 80), Paths.get("output.jpg"))

// PNG로 저장 (최대 압축)
image.suspendWrite(SuspendPngWriter.MaxCompression, Paths.get("output.png"))

// WebP로 저장
image.suspendWrite(SuspendWebpWriter.Default, Paths.get("output.webp"))

// ByteArray로 변환
val jpegBytes = image.suspendBytes(SuspendJpegWriter.Default)
val webpBytes = image.suspendBytes(SuspendWebpWriter.Default)
```

### 배치 이미지 처리 (Issue #135)

`ImageBatchFlow`는 대량 이미지에 동일한 변환을 적용하는 코루틴 Flow 파이프라인입니다.
동시성 제어와 픽셀 단위 메모리 한도를 통해 안전하게 대규모 배치 작업을 처리합니다.

```kotlin
import io.bluetape4k.images.batch.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import java.nio.file.Path

// 배치 옵션 설정
val options = ImageProcessingOptions(
    parallelism = defaultImageBatchParallelism(),
    maxPixels = DEFAULT_MAX_PIXELS,
    maxInFlightPixels = DEFAULT_MAX_IN_FLIGHT_PIXELS,
    skipFailures = true,                        // 실패 항목 건너뜀, 전체 중단 없음
)

// 처리 후 결과 경로 수집
val writtenPaths: List<Path> = listOf(Path.of("a.jpg"), Path.of("b.jpg"))
    .asFlow()
    .processImages(options) {
        resize(width = 1280, height = 720)      // 먼저 크기 조절
        watermark("© bluetape4k")               // 워터마크 오버레이
        toJpeg(quality = 85)                    // JPEG로 인코딩
    }
    .writeImagesTo(Path.of("output"), options)  // 출력 디렉터리에 저장
    .toList()
```

`ImageProcessingDsl`의 변환은 `transformDispatcher`에서 실행되고, 저장은 호출자가 넘긴
`ioDispatcher`에서 실행됩니다. 배치 기본값은 `DEFAULT_MAX_PIXELS`,
`DEFAULT_MAX_IN_FLIGHT_PIXELS`, `JPEG_QUALITY_MIN`, `JPEG_QUALITY_MAX`,
`PERFORMANCE_SAMPLE_IMAGE_COUNT`처럼 이름 있는 상수로 제공합니다.

더 큰 이미지 세트는 `ImageProcessingOptions.largeJobs()`를 사용하거나
`maxPixels` / `maxInFlightPixels`를 명시적으로 높여 처리할 수 있습니다.

```kotlin
val largeOptions = ImageProcessingOptions.largeJobs(
    parallelism = defaultImageBatchParallelism(),
    maxPixels = LARGE_JOB_MAX_PIXELS,
    maxInFlightPixels = LARGE_JOB_MAX_IN_FLIGHT_PIXELS,
)
```

### 썸네일 파이프라인

`ThumbnailPipeline`은 소스 이미지마다 여러 크기의 썸네일을 한 번의 패스로 생성합니다.
`ThumbnailPipeline.builder()`로 크기·크롭 전략·포맷·오류 처리를 설정하고,
`process(Flow<Path>)`를 호출하면 결과가 스트리밍됩니다.

```kotlin
import io.bluetape4k.images.batch.ImageProcessingOptions
import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.thumbnail.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import java.nio.file.Path

// 이미지당 세 가지 크기의 썸네일을 생성하는 파이프라인 구성
val pipeline = ThumbnailPipeline.builder()
    .outputDirectory(Path.of("output/thumbs"))
    .size(width = 1280, height = 720, suffix = "hd")
    .size(width = 640,  height = 360, suffix = "md")
    .size(width = 320,  height = 180, suffix = "sm")
    .format(ThumbnailFormat(SuspendJpegWriter.Default.withCompression(85), "jpg"))
    .crop(ThumbnailCrop.Smart())                // 중요 영역 자동 크롭
    .options(ImageProcessingOptions(parallelism = 4, skipFailures = true))
    .onFailure { result ->
        // result.status에 실패 단계와 원인 포함
        println("썸네일 실패: ${result.source} → ${result.status}")
    }
    .build()

// 소스 이미지 스트림을 처리하고 결과 수집
pipeline
    .process(listOf(Path.of("photos/photo.jpg"), Path.of("photos/banner.png")).asFlow())
    .collect { result ->
        when (val status = result.status) {
            is ThumbnailStatus.Success ->
                println("OK  ${result.output.fileName} — ${status.bytes} bytes")
            is ThumbnailStatus.Failure ->
                println("ERR ${result.source.fileName} at ${status.stage}")
        }
    }
```

`ThumbnailCrop` 종류:
- `ThumbnailCrop.Fit` — 비율을 유지하면서 지정 크기에 맞게 축소 (기본값)
- `ThumbnailCrop.Smart()` — Sobel 엣지 기반 saliency 크롭 후 정확한 크기로 리사이즈

### 타일 처리

`TileProcessor`는 큰 이미지를 격자 타일로 분할하고, 각 타일을 병렬로 변환한 다음,
하나의 출력 이미지로 재조립합니다. 이미지 전체를 한 번에 처리하기 어려울 때 유용합니다.

```kotlin
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.GrayscaleFilter
import io.bluetape4k.images.tiles.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList

val image: ImmutableImage = ImmutableImage.loader().fromFile(File("large.jpg"))
val processor = TileProcessor(maxTileCount = 256, parallelism = 4)

// 1. 512×512 타일로 분할
val tiles: List<ImageTile> = processor.split(image, TileSize(width = 512, height = 512))

// 2. 각 타일에 병렬로 변환 적용
val processedTiles: List<ImageTile> = processor
    .processTiles(tiles.asFlow()) { tile ->
        tile.copy(image = tile.image.filter(GrayscaleFilter()))
    }
    .toList()

// 3. 원래 크기로 재조립
val result: ImmutableImage = processor.merge(processedTiles, image.width, image.height)
result.output(JpegWriter.Default, File("output.jpg"))
```

단순 분할·병합 (타일별 변환 없음):

```kotlin
val processor = TileProcessor()
val tiles = processor.split(image, TileSize(width = 512, height = 512))
val merged = processor.merge(tiles, image.width, image.height)
```

### TIFF 지원 (Issue #134)

```kotlin
import io.bluetape4k.images.coroutines.*
import java.io.ByteArrayOutputStream

val image = immutableImageOf(File("photo.jpg"))

// 단일 페이지 TIFF (기본 DEFLATE 압축)
val writer = SuspendTiffWriter.Default
val bos = ByteArrayOutputStream()
writer.suspendWrite(image, bos)

// LZW 압축
val lzwWriter = SuspendTiffWriter.Lzw
val bos2 = ByteArrayOutputStream()
lzwWriter.suspendWrite(image, bos2)

// 다중 페이지 TIFF
val pages = listOf(page1, page2, page3)
val multiWriter = SuspendTiffMultiPageWriter.Default
val bos3 = ByteArrayOutputStream()
multiWriter.suspendWrite(pages, bos3)
```

### SVG 래스터화 (Issue #134)

```kotlin
import io.bluetape4k.images.svg.*
import com.sksamuel.scrimage.nio.PngWriter

val rasterizer = BatikSvgRasterizer()

// 기본 래스터화 (외부 리소스 차단 기본값)
File("diagram.svg").inputStream().use { svg ->
    val image: ImmutableImage = rasterizer.rasterize(svg)
    image.output(PngWriter.MaxCompression, File("diagram.png"))
}

// 옵션 지정
val opts = SvgRasterizeOptions(
    width = 800,
    height = 600,
    dpi = 144,
    allowExternalResources = false,  // SSRF/XXE 방어 (기본값)
)
File("diagram.svg").inputStream().use { svg ->
    val image = rasterizer.rasterize(svg, opts)
}
```

### 이미지 크기 조절

```kotlin
import io.bluetape4k.images.scaler.*
import java.awt.image.BufferedImage

// 비율로 조절
val scaled = bufferedImage.scale(0.5)  // 50% 크기

// 절대 크기로 조절 (비율 유지)
val scaled = bufferedImage.scale(width = 200, height = 200, proportional = true)

// 절대 크기로 조절 (비율 무시)
val scaled = bufferedImage.scale(width = 200, height = 200, proportional = false)

// X, Y 축 비율로 조절
val scaled = bufferedImage.scale(xScale = 0.5, yScale = 0.5)
```

### 이미지 분할

높이가 큰 이미지(예: 상품 상세 이미지)를 지정된 높이로 분할합니다.

```kotlin
import io.bluetape4k.images.splitter.ImageSplitter
import io.bluetape4k.images.ImageFormat

val splitter = ImageSplitter(maxHeight = 2048)

// 기본 분할
val splitImages: Flow<ByteArray> = splitter.split(
    input = inputStream,
    format = ImageFormat.JPG,
    splitHeight = 1024
)

// 분할 + 압축
val compressedImages: Flow<ByteArray> = splitter.splitAndCompress(
    input = inputStream,
    format = ImageFormat.JPG,
    splitHeight = 1024,
    writer = SuspendJpegWriter(compression = 80)
)

// 결과 처리
splitImages.collect { bytes ->
    // 분할된 이미지 처리
}
```

### 워터마크 추가

```kotlin
import io.bluetape4k.images.filters.*
import com.sksamuel.scrimage.ImmutableImage

val image = ImmutableImage.loader().fromFile(File("photo.jpg"))

// 커버 워터마크 (전체 덮기)
val watermarked = image.filter(
    watermarkFilterOf(
        text = "© bluetape4k",
        type = WatermarkFilterType.COVER,
        alpha = 0.2,
        color = Color.WHITE
    )
)

// 스탬프 워터마크
val stamped = image.filter(
    watermarkFilterOf(
        text = "© bluetape4k",
        type = WatermarkFilterType.STAMP,
        alpha = 0.3
    )
)

// 특정 위치에 워터마크
val positioned = image.filter(
    watermarkFilterOf(
        text = "© bluetape4k",
        x = 100,
        y = 100,
        alpha = 0.5
    )
)
```

### 캡션 추가

```kotlin
import io.bluetape4k.images.filters.*
import com.sksamuel.scrimage.Position

val image = ImmutableImage.loader().fromFile(File("photo.jpg"))

val captioned = image.filter(
    captionFilterOf(
        text = "Powered by bluetape4k",
        position = Position.BottomLeft,
        textAlpha = 0.8,
        color = Color.WHITE
    )
)
```

### 패딩 추가

```kotlin
import io.bluetape4k.images.filters.*

// 상하좌우 동일 패딩
val padding = paddingOf(20)

// 개별 패딩 지정
val padding = paddingOf(top = 10, right = 20, bottom = 10, left = 20)
```

### 그래픽 작업

```kotlin
import io.bluetape4k.images.*
import java.awt.Color

// 새 이미지 생성
val image = bufferedImageOf(200, 100)

// 그래픽 작업
image.useGraphics { graphics ->
    graphics.color = Color.RED
    graphics.fillRect(0, 0, 100, 100)
    graphics.color = Color.BLACK
    graphics.drawString("Hello, World!", 10, 50)
}

// ImmutableImage로 그래픽 작업
val immutableImage = immutableImageOf(File("input.jpg"))
immutableImage.useGraphics { graphics ->
    graphics.color = Color.BLUE
    graphics.drawRect(10, 10, 100, 100)
}
```

### 이미지 유사도 비교

환경 독립적인 이미지 회귀 테스트, 중복 탐지, 압축 품질 평가용 지표 모음입니다.

```kotlin
import io.bluetape4k.images.*
import io.bluetape4k.images.similarity.*

val a = immutableImageOf(File("a.jpg"))
val b = immutableImageOf(File("b.jpg"))

// 픽셀 단위 비교
a.pixelAvgDeltaTo(b)   // 채널당 평균 RGB 차이 (0.0 ~ 255.0, 0이 동일)
a.pixelMaxDeltaTo(b)   // 채널당 최대 RGB 차이 (0 ~ 255)

// 통계 기반 지표
a.mseTo(b)             // Mean Squared Error
a.psnrTo(b)            // Peak SNR dB (≥ 30 양호, ≥ 40 거의 동일, 동일시 +∞)
a.ssimTo(b)            // 전역 SSIM (-1.0 ~ 1.0, ≥ 0.95 거의 구분 불가)

// 지각 해시 — 레거시 64bit API (크기 변화·JPEG 재압축에 견고)
a.phash()                      // 64bit Long
a.phashDistanceTo(b)           // Hamming distance 0 ~ 64 (≤ 5 거의 동일, ≤ 10 유사)
```

| 지표                  | 용도                            | 완전 동일 |
|---------------------|-------------------------------|-------|
| `pixelAvgDeltaTo`   | 바이트 단위 회귀 테스트 (허용 오차 비교)      | 0.0   |
| `pixelMaxDeltaTo`   | 단일 픽셀 이상치 탐지                  | 0     |
| `psnrTo`            | JPEG/WebP 압축 품질 평가            | +∞    |
| `ssimTo`            | 전역 인지적 유사도                    | 1.0   |
| `phashDistanceTo`   | 중복 이미지·크롭·리사이즈 탐지             | 0     |

### MSSIM (슬라이딩 윈도우 SSIM)

11×11 Gaussian 윈도우 기반 SSIM으로, 전역 SSIM보다 공간 구조 변화에 민감합니다.

```kotlin
import io.bluetape4k.images.similarity.*

val a = immutableImageOf(File("a.jpg"))
val b = immutableImageOf(File("b.jpg"))

// 11×11 Gaussian 윈도우 (기본), 전체 유효 위치 평균
val mssim = a.mssimTo(b)                    // 0.0 ~ 1.0
val mssim = a.mssimTo(b, windowSize = 7)    // 더 작은 윈도우
val mssim = a.mssimTo(b, sigma = 2.0)       // 더 넓은 가우시안

// 대형 이미지는 먼저 다운스케일해서 속도 향상
val prepared = a.prepareForSimilarity(maxSide = 512)
val score = prepared.mssimTo(b.prepareForSimilarity(512))
```

> 두 이미지의 크기가 동일해야 합니다. 일관된 처리를 위해 `prepareForSimilarity` 사용 권장.

### 확장 지각 해시 (aHash / dHash / wHash / pHash)

가변 비트폭(64 / 256 / 1024bit) 지각 해시.

```kotlin
import io.bluetape4k.images.similarity.*

val a = immutableImageOf(File("a.jpg"))
val b = immutableImageOf(File("b.jpg"))

// 64bit 편의 단축형
val da = HashDistance.hamming(a.ahash(), b.ahash())   // 평균 해시
val dd = HashDistance.hamming(a.dhash(), b.dhash())   // 차이 해시
val dw = HashDistance.hamming(a.whash(), b.whash())   // 웨이블릿 해시
// 참고: a.phash() == a.phashOf(PHashSize.BITS_64)[0]

// 가변 비트폭 — LongArray
val p256 = a.phashOf(PHashSize.BITS_256)              // LongArray(4)
val p1024 = b.phashOf(PHashSize.BITS_1024)            // LongArray(16)
val dist = HashDistance.hamming(a.phashOf(PHashSize.BITS_256), b.phashOf(PHashSize.BITS_256))
```

| 해시  | 알고리즘                | 특징                         |
|-------|---------------------|------------------------------|
| aHash | 평균 밝기              | 빠르고 단순                    |
| dHash | 인접 픽셀 그래디언트     | 약한 밝기 변화에 견고             |
| wHash | Haar DWT LL subband | pHash보다 빠르고 정확도 유사       |
| pHash | DCT 저주파 성분        | JPEG·리사이즈에 가장 견고           |

### 색상 히스토그램 유사도

채널별 색상 분포로 이미지를 비교합니다.

```kotlin
import io.bluetape4k.images.similarity.*

val a = immutableImageOf(File("a.jpg"))
val b = immutableImageOf(File("b.jpg"))

// ChiSquare (가장 변별력 높음, [0, 1])
val sim = a.histogramSimilarityTo(b)                                   // ChiSquare, RGB, 32 bins
val sim = a.histogramSimilarityTo(b, HistogramSimilarity.chiSquare())

// Bhattacharyya 계수 ([0, 1])
val sim = a.histogramSimilarityTo(b, HistogramSimilarity.bhattacharyya())

// Earth Mover's Distance 정규화 ([0, 1])
val sim = a.histogramSimilarityTo(b, HistogramSimilarity.earthMover())

// HSV 색공간, 채널당 64 bins
val measure = HistogramSimilarity.ChiSquare(colorSpace = ColorSpace.HSV, binsPerChannel = 64)
val sim = a.histogramSimilarityTo(b, measure)
```

### Block-Mean Descriptor (키포인트 없는 매칭)

그리드 기반 휘도 descriptor로 회전 대응 유사도를 계산합니다.

```kotlin
import io.bluetape4k.images.similarity.*

val a = immutableImageOf(File("a.jpg"))
val b = immutableImageOf(File("b.jpg"))

// 그리드 유사도 (L2 정규화, [0, 1])
val sim = a.blockMeanSimilarityTo(b)                        // 8×8 그리드
val sim = a.blockMeanSimilarityTo(b, gridRows = 16, gridCols = 16)

// 회전 대응: 0°/90°/180°/270° 중 최대 유사도 반환
val sim = a.bestRotationSimilarityTo(b)

// Raw descriptor
val desc = a.blockMeanDescriptor()   // DoubleArray(64) 정규화된 셀별 휘도
```

### 애니메이션 GIF → WebP 변환

```kotlin
import io.bluetape4k.images.coroutines.animated.*
import com.sksamuel.scrimage.nio.AnimatedGif

val gif = AnimatedGif.fromFile(File("animation.gif"))

// WebP로 변환
gif.suspendWrite(SuspendGif2WebpWriter.Default, Paths.get("animation.webp"))

// ByteArray로 변환
val webpBytes = gif.suspendBytes(SuspendGif2WebpWriter.Default)
```

## 이미지 Writer 옵션

### SuspendJpegWriter

```kotlin
// 기본 (80% 품질)
SuspendJpegWriter.Default

// 커스텀 품질
SuspendJpegWriter(compression = 90)

// 프로그레시브 JPEG
SuspendJpegWriter(compression = 80, progressive = true)

// 메타데이터에서 압축 정보 사용
SuspendJpegWriter.CompressionFromMetaData
```

### SuspendPngWriter

```kotlin
// 최대 압축 (느림)
SuspendPngWriter.MaxCompression  // level 9

// 최소 압축 (빠름)
SuspendPngWriter.MinCompression  // level 1

// 압축 없음 (가장 빠름)
SuspendPngWriter.NoCompression  // level 0
```

### SuspendWebpWriter

```kotlin
// 기본
SuspendWebpWriter.Default

// 최대 무손실 압축 (배치 작업용)
SuspendWebpWriter.MaxLosslessCompression

// 커스텀 옵션
SuspendWebpWriter(
    z = 9,           // 압축 레벨 (0-9)
    q = 75,          // 품질 (0-100)
    m = 4,           // 압축 방법 (0-6)
    lossless = false,
    noAlpha = false
)
```

## 필터 / 색보정 DSL (Issue #131)

패키지: `io.bluetape4k.images.filters.dsl`

### `ImageFilterChain` DSL

`applyFilters { ... }` 및 `suspendApplyFilters { ... }` 확장 함수는 이미지 필터를 체이닝하는 플루언트 DSL을 제공합니다. 주요 설계 특징:

- `source.copy()` 방어 복사로 원본 이미지 보호
- 인접한 scrimage 네이티브 필터는 자동으로 `PipelineFilter`로 묶어 성능 최적화
- 색상/톤, 스타일, 블러, 효과, 텍스트를 포함한 40+ DSL 멤버 함수 제공

```kotlin
import io.bluetape4k.images.filters.dsl.*

// 동기 필터 체인
val result = image.applyFilters {
    brightness(1.2f)
    contrast(1.1)
    saturation(1.15f)
    sepia()
}

// 비동기(코루틴) 필터 체인
val result2 = image.suspendApplyFilters {
    gaussianBlur(3)
    colorTemperature(3000)
    vignette()
}

// 이스케이프 해치: 커스텀 필터 직접 주입
val result3 = image.applyFilters {
    raw(MyCustomFilter())
    pixel { img -> img.flipX() }
}
```

### 신규 필터 5종

| 필터 | DSL 함수 | 설명 |
|------|----------|------|
| `SaturationAdjustFilter` | `saturation(factor)` | HSV 채도 배수 조정 (1.0=원본, 0=흑백) |
| `HueAdjustFilter` | `hue(deltaDegrees)` | HSV 색조 회전 (도 단위) |
| `ColorTemperatureFilter` | `colorTemperature(kelvin)` | 켈빈 색온도 조정 (1000–40000 K) |
| `RoundedCornerFilter` | `roundedCorners(radius)` | 모서리 둥글게 (알파 마스크) |
| `MedianBlurFilter` | `medianBlur(radius, boundary)` | 미디언 블러 노이즈 제거 (`MedianBoundaryMode`: REPLICATE/REFLECT) |

### `ColorSpaceConverter`

신규 필터들이 내부적으로 사용하는 색 공간 변환 유틸리티 객체입니다.

```kotlin
import io.bluetape4k.images.filters.ColorSpaceConverter

// RGB ↔ HSV
val (h, s, v) = ColorSpaceConverter.rgbToHsv(255, 128, 0)
val (r, g, b) = ColorSpaceConverter.hsvToRgb(30f, 1f, 1f)

// 켈빈 → RGB
val (r, g, b) = ColorSpaceConverter.kelvinToRgb(6500)

// 픽셀 배열 일괄 변환
val hsvArray = image.toHsvArray()     // FloatArray [h0,s0,v0, h1,s1,v1, ...]
val ycbcrArray = image.toYCbCrArray() // FloatArray [y0,cb0,cr0, ...]
```

## 이미지 변환 (Issue #132)

순수 JVM(Java2D) 기반 고급 이미지 변환 연산. 모든 연산은 suspend 변형을 제공합니다.

### 변환 아키텍처

```mermaid
flowchart TD
    subgraph Transforms["transforms 패키지"]
        AC["AutoCrop\nautoCrop()"]
        SC["SmartCrop\nsmartCrop(AspectRatio)"]
        RT["Rotation\nrotateDegrees / flipH / flipV"]
        PT["PerspectiveTransform\nperspectiveTransform(4pts)"]
        HE["HistogramEqualization\nclahe / globalEqualize"]
    end

    subgraph DSL["applyFilters { } DSL"]
        DO["ImageFilterChainTransformOps\nautoCrop / smartCrop / rotateDegrees\nrotateLeft / rotateRight\nflipH / flipV / perspective / clahe"]
    end

    ImmutableImage --> Transforms
    Transforms --> ImmutableImage
    DSL --> Transforms

    classDef opStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef dslStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    class AC,SC,RT,PT,HE opStyle
    class DO dslStyle
```

### AutoCrop — 자동 여백 제거

배경색을 자동으로 감지하여 불필요한 여백을 제거합니다.

```kotlin
// 4개 모서리 평균으로 배경색 자동 감지
val cropped = image.autoCrop(tolerance = 10, padding = 2)

// 배경색 명시
val cropped2 = image.autoCrop(tolerance = 5, backgroundColor = Color.WHITE)

// Suspend 변형
val cropped3 = image.suspendAutoCrop()
```

### SmartCrop — 중요 영역 자동 크롭

Sobel 엣지 에너지 기반 휴리스틱 saliency (ML 없음).

```kotlin
// 16:9 와이드스크린 비율로 가장 흥미로운 영역 크롭
val wide = image.smartCrop(AspectRatio.WIDESCREEN)

// 크롭 후 정확한 출력 크기로 리사이즈
val thumb = image.smartCropTo(400, 300)

// Suspend 변형
val wide2 = image.suspendSmartCrop(AspectRatio.SQUARE)
```

`AspectRatio` 프리셋: `SQUARE (1:1)`, `WIDESCREEN (16:9)`, `PORTRAIT (9:16)`, `STANDARD (4:3)`.

### Rotation & Flip — 회전 및 반전

```kotlin
// 임의 각도 회전 (투명 배경, 캔버스 자동 확장)
val rotated = image.rotateDegrees(45.0)
val rotatedRed = image.rotateDegrees(30.0, background = Color.RED)

// 90도 단위 (scrimage 네이티브, 무손실)
val cw90  = image.rotateRight()
val ccw90 = image.rotateLeft()

// 반전
val hFlip = image.flipHorizontal()
val vFlip = image.flipVertical()

// Suspend
val async = image.suspendRotateDegrees(45.0)
```

### Perspective Transform — 원근 변환

4점 호모그래피를 이용한 문서 왜곡 보정 및 원근 교정.

```kotlin
val src = listOf(
    ImagePoint(10.0, 10.0), ImagePoint(490.0, 0.0),
    ImagePoint(500.0, 490.0), ImagePoint(0.0, 500.0),
)
val dst = listOf(
    ImagePoint(0.0, 0.0), ImagePoint(499.0, 0.0),
    ImagePoint(499.0, 499.0), ImagePoint(0.0, 499.0),
)
val corrected = image.perspectiveTransform(src, dst, outputWidth = 500, outputHeight = 500)

// Suspend 변형
val async = image.suspendPerspectiveTransform(src, dst, 500, 500)
```

### CLAHE — 히스토그램 균일화

YCbCr 색공간(BT.601) 기반 CLAHE(Contrast Limited Adaptive Histogram Equalization).

```kotlin
// 기본 설정 (tileSize=8, clipLimit=2.0)
val enhanced = image.clahe()

// 타일/클립 직접 지정
val enhanced2 = image.clahe(tileSize = 16, clipLimit = 3.0)

// 전역(단일 타일) 균일화
val global = image.globalEqualize()

// Suspend 변형
val async = image.suspendClahe(tileSize = 8, clipLimit = 2.0)
```

### DSL 통합

모든 변환은 `applyFilters { }` / `suspendApplyFilters { }` DSL에서 사용 가능합니다.

```kotlin
val result = image.applyFilters {
    autoCrop(tolerance = 10, backgroundColor = Color.WHITE)
    rotateDegrees(15.0)
    clahe()
}

// Suspend DSL
val asyncResult = image.suspendApplyFilters {
    smartCrop(AspectRatio.WIDESCREEN)
    flipHorizontal()
}
```

### 이미지 분석

대표 색상 추출, 블러 감지, EXIF 메타데이터 파싱 — 순수 JVM, 네이티브 의존성 없음.

#### 대표 색상 추출 (Median Cut)

```kotlin
import io.bluetape4k.images.analysis.*

val image = ImmutableImage.loader().fromFile(File("photo.jpg"))

// 상위 5개 대표 색상 추출
val colors: List<DominantColor> = image.dominantColors(5)
colors.forEach { c ->
    println("${c.hex} (population=${c.population})")
}

// 단일 대표 색상 (완전 투명 이미지이면 null)
val primary: DominantColor? = image.dominantColor()

// 커스텀 추출기 — 흰색 픽셀 제외
val extractor = DominantColorExtractor.medianCut(quality = 5, ignoreWhite = true)
val filtered = image.dominantColors(3, extractor)

// Suspend (CPU-bound → Dispatchers.Default)
val asyncColors = image.suspendDominantColors(5)
```

#### 블러 감지 (Laplacian Variance)

```kotlin
import io.bluetape4k.images.analysis.*

val image = ImmutableImage.loader().fromFile(File("photo.jpg"))

// 블러 점수 — 높을수록 선명
val result: BlurScore = image.blurScore(threshold = 100.0)
println("score=${result.score}, isBlurry=${result.isBlurry}")

// Boolean 단축형
if (image.isBlurry()) {
    println("이미지가 흐림 — 처리 건너뜀")
}

// Suspend
val asyncScore: BlurScore = image.suspendBlurScore(threshold = 150.0)
```

#### EXIF 메타데이터

```kotlin
import io.bluetape4k.images.analysis.*

// File에서 읽기
val exif: ExifData = File("photo.jpg").readExif()
println("제조사=${exif.cameraMake}, 모델=${exif.cameraModel}")
println("ISO=${exif.iso}, 조리개=f/${exif.aperture}")
println("촬영시각=${exif.dateTimeOriginal}")

// GPS — PII 제거 헬퍼
if (exif.hasGps) {
    val safe = exif.withoutGps()  // lat/lon/altitude 제거
    println("${safe.cameraMake}")
}

// ByteArray에서 읽기 (최대 50MB)
val exifFromBytes: ExifData = readExif(bytes)

// Path에서 읽기 (jar/zip 내부 경로 지원)
val exifFromPath: ExifData = Paths.get("photo.jpg").readExif()

// Suspend
val asyncExif: ExifData = File("photo.jpg").suspendReadExif()
```

#### 주요 파일

| 파일                                     | 설명                                              |
|------------------------------------------|--------------------------------------------------|
| `analysis/DominantColor.kt`             | `DominantColor` data class + `DominantColorExtractor` sealed interface |
| `analysis/MedianCutQuantizer.kt`        | Median Cut quantization 엔진 (5-bit/channel)     |
| `analysis/BlurDetector.kt`              | `BlurScore` + Laplacian variance 계산             |
| `analysis/ExifData.kt`                  | `ExifData` 모델 + `readExif()` 진입점            |

## 테스트 & 품질

### 골든 이미지 테스트

[`GoldenImageAssert`](src/test/kotlin/io/bluetape4k/images/golden/GoldenImageAssert.kt)를 통한 픽셀 단위 회귀 테스트.

- **비교 모드** (기본): 저장된 골든 PNG와 허용 오차 범위 내 비교
- **갱신 모드**: `-Dbluetape4k.images.golden.update=true` 로 골든 이미지 재생성
- **CI 가드**: CI 환경에서 갱신 모드 실행 차단

### 속성 기반 테스트 (PBT)

[`ImagePropertyTest`](src/test/kotlin/io/bluetape4k/images/property/ImagePropertyTest.kt)가 6개 결정론적 입력(320×240, 640×480, 1280×720, 3840×2160 단색/그라디언트/노이즈)에 대해 10개 불변식을 검증합니다.

| # | 불변식 | 설명 |
|---|--------|------|
| 1 | scaleTo 크기 일치 | `scaleTo(w, h)` 결과가 정확히 `w×h` |
| 2 | fit 경계 내 | `fit(w, h)` 결과가 `w×h` 이내 |
| 3 | grayscale R==G==B | grayscale 후 모든 픽셀 R==G==B |
| 4 | resize 라운드트립 | decode→encode→decode 시 크기 유지 |
| 5 | PNG 바이트 > 0 | PNG 인코딩은 항상 비어 있지 않은 바이트 반환 |
| 6 | sepia ≠ grayscale | sepia와 grayscale은 서로 다른 결과 |
| 7 | scaleTo 멱등성 | 동일 타겟으로 `scaleTo` 두 번 호출 시 결과 동일 |
| 8 | resize 바이트 감소 | 다운스케일 JPEG ≤ 원본 JPEG 바이트 |
| 9 | 단색 JPEG 라운드트립 | 단색 이미지 encode→decode 가능 |
| 10 | filter 크기 보존 | `filter()` 후 원본 width/height 유지 |

```bash
# PBT + 골든 테스트 실행
./gradlew :bluetape4k-images:test

# 골든 이미지 재생성
./gradlew :bluetape4k-images:test -Dbluetape4k.images.golden.update=true
```

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-images:${version}")
}
```
