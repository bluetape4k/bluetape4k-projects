# Module bluetape4k-images

## 개요

JPG, PNG, GIF, WebP 등의 이미지를 로드, 변환, 크기 조절, 분할, 필터 적용 등의 조작을 지원하는 라이브러리입니다.
[Scrimage](https://github.com/sksamuel/scrimage) 라이브러리를 기반으로 하며, Coroutines를 활용한 비동기 이미지 처리를 제공합니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-images:${version}")
}
```

## 주요 기능

### 이미지 포맷 지원

| 포맷   | 파일 사이즈 (예시) | 처리 시간 (예시) | 특징            |
|------|-------------|------------|---------------|
| PNG  | 6.45 MB     | 569 ms     | 무손실, 투명도 지원   |
| GIF  | 1.21 MB     | 2,888 ms   | 애니메이션 지원      |
| JPG  | 417 kB      | 157 ms     | 빠른 처리, 손실 압축  |
| WEBP | 181 kB      | 913 ms     | 최고 압축률, 최신 포맷 |

- **동적 생성**: JPG가 가장 빠름 (실시간 처리용)
- **정적 파일**: WebP가 가장 효율적 (저장 공간 절약)

## 사용 예시

### 이미지 로드

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
SuspendPngWriter.NoComppression  // level 0
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

## 주요 기능 상세

| 파일                                                   | 설명                            |
|------------------------------------------------------|-------------------------------|
| `ImmutableImageSupport.kt`                           | ImmutableImage 생성, 저장, 그래픽 작업 |
| `BufferedImageSupport.kt`                            | BufferedImage 생성, 저장, 그래픽 작업  |
| `ImageFormat.kt`                                     | 지원 이미지 포맷 열거형                 |
| `WriteContextExtensions.kt`                          | 쓰기 컨텍스트 확장 함수                 |
| `IIORegistryUtils.kt`                                | ImageIO 레지스트리 유틸리티            |
| `scaler/ImageScaler.kt`                              | 이미지 크기 조절                     |
| `splitter/ImageSplitter.kt`                          | 이미지 분할                        |
| `filters/WatermarkFilterSupport.kt`                  | 워터마크 필터                       |
| `filters/CaptionFilterSupport.kt`                    | 캡션 필터                         |
| `filters/PaddingSupport.kt`                          | 패딩 필터                         |
| `filters/WatermarkFilterType.kt`                     | 워터마크 타입 (COVER/STAMP)         |
| `fonts/FontSupport.kt`                               | 폰트 유틸리티                       |
| `io/ImageInputStreamSupport.kt`                      | 이미지 입력 스트림                    |
| `io/ImageOuptputStreamSupport.kt`                    | 이미지 출력 스트림                    |
| `coroutines/SuspendImageWriter.kt`                   | 비동기 이미지 Writer 인터페이스          |
| `coroutines/SuspendJpegWriter.kt`                    | 비동기 JPEG Writer               |
| `coroutines/SuspendPngWriter.kt`                     | 비동기 PNG Writer                |
| `coroutines/SuspendGifWriter.kt`                     | 비동기 GIF Writer                |
| `coroutines/SuspendWebpWriter.kt`                    | 비동기 WebP Writer               |
| `coroutines/SuspendWriteContext.kt`                  | 비동기 쓰기 컨텍스트                   |
| `coroutines/animated/SuspendAnimatedImageWriter.kt`  | 비동기 애니메이션 Writer              |
| `coroutines/animated/SuspendGif2WebpWriter.kt`       | GIF → WebP 변환 Writer          |
| `coroutines/animated/AnimatedGifExtensions.kt`       | AnimatedGif 확장 함수             |
| `coroutines/animated/SuspendAnimatedWriteContext.kt` | 애니메이션 쓰기 컨텍스트                 |
