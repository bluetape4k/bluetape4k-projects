# utils/images — Image Analysis Design Spec

**Date**: 2026-04-27 **Issue**: #133 **Branch**: `feat/issue-133-images-analysis`
**Worktree**: `.worktrees/feat/issue-133-images-analysis`
**Module**: `utils/images` (`bluetape4k-images`)

## 1. 배경 / 목표

`utils/images` 모듈에 이미지 분석 기능 3종을 추가한다.

- **Dominant Color Extraction**: 대표 색상 N개 추출 (palette 생성, 썸네일 컬러, UI theming).
- **Blur Detection**: 흐림 정도 점수화 (사진 품질 검증, autofocus 검수).
- **EXIF Metadata**: GPS/카메라/촬영일시 등 EXIF 메타 추출 (사진 자산 색인, 지오태깅).

OCR / Face Detection은 **본 PR 범위에서 제외**하며 별도 이슈로 등록한다 (외부 native 의존성/모델 파일 부담 회피).

### 비목표 (Non-Goals)

- OCR (Tesseract, Google ML Kit 등 native 모델 의존).
- Face Detection (OpenCV / dlib native binding 필요).
- 이미지 분류/라벨링 (TensorFlow/ONNX 모델 필요).
- EXIF **쓰기** — 본 PR은 **읽기만** 지원.

### 설계 원칙

- **Pure JVM only** — 외부 native dependency 금지 (Scrimage가 이미 ImageIO + WebP만 사용).
- 기존 모듈의 sealed interface + data class 패턴 (`HistogramSimilarity`) 재사용.
- Extension function 형태의 공개 API (`ImmutableImage.xxx`, `File.xxx`, `Path.xxx`).
- Coroutines suspend 변형은 `withContext(Dispatchers.IO)` 래핑 — 큰 이미지 처리는 IO 풀로.
- 기능별 서브패키지: `io.bluetape4k.images.analysis`.

## 2. Brainstorming — 구현 방식 비교

### 2.1 Dominant Color Extraction

| 방식                            | 장점                                                                                  | 단점                                                                         | 평가     |
|---------------------------------|---------------------------------------------------------------------------------------|------------------------------------------------------------------------------|----------|
| **(a) Median Cut quantization** | 표준 알고리즘, color-thief-java 검증, O(n·log k), 메모리 효율, k=5 등 정확한 N개 보장 | 직접 구현 필요 (~150 LoC), edge case (단색 이미지) 처리 필요                 | **선택** |
| (b) K-means clustering          | 통계적으로 더 균일한 클러스터, 라이브러리 (commons-math) 활용 가능                    | k-means 초기 시드 random → 비결정적, 수렴 안 할 수 있음, 느림 (반복)         | 보류     |
| (c) Histogram bucket            | 가장 단순, 빠름 (O(n))                                                                | 낮은 정확도 — bin 경계 근처 색상이 분산됨, "비슷한 색"이 한 그룹으로 안 묶임 | 보류     |

**선택: (a) Median Cut**.

- color-thief-java (SvenWoltmann/color-thief-java MIT 라이선스) 알고리즘을 Kotlin으로 자체 구현.
- 핵심 단계: ① 픽셀 샘플링 → ② RGB 큐브 분할 (가장 긴 축 기준 median으로 분할) → ③ k 개 박스가 될 때까지 반복 → ④ 각 박스의 평균 색상 반환.
- 외부 라이브러리 import 불가 (color-thief-java는 jcenter 종료, 일부 fork만 maven central).

**성능 고려**:

- 큰 이미지는 픽셀 샘플링 (every Nth pixel) — 기본 quality=10 (color-thief-java 디폴트).
- Alpha=0 픽셀 제외, 거의 흰색/검정 (선택적) 제외 옵션.

### 2.2 Blur Detection

| 방식                           | 장점                                                                                   | 단점                                                                                 | 평가     |
|--------------------------------|----------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|----------|
| **(a) Laplacian variance**     | 표준 (Pech-Pacheco 2000), 직관적 — variance 낮을수록 blur, OpenCV 표준 방법, 단일 패스 | threshold가 이미지 도메인에 따라 다름 (절대값 아님) — 사용자에게 threshold 명시 필요 | **선택** |
| (b) FFT 주파수 분석            | 더 정확, 노이즈에 강건                                                                 | FFT 라이브러리 의존 (commons-math3.transform), 구현 복잡                             | 보류     |
| (c) Tenengrad gradient (Sobel) | Laplacian 변형, 조명 변화에 강건                                                       | Laplacian 대비 이점 미미, 두 패스 필요                                               | 보류     |

**선택: (a) Laplacian variance**.

- Laplacian 커널 `[[0,1,0],[1,-4,1],[0,1,0]]` 적용 → grayscale 픽셀에 convolution → variance 계산.
- `threshold` 디폴트 `100.0` (OpenCV/PyImageSearch 권장값) — 이미지 사이즈에 따라 사용자가 조정.
- grayscale 변환은 luminance 공식 `0.299*R + 0.587*G + 0.114*B` (Rec. 601).

**성능 고려**:

- 이미지 크기 dependent. 큰 이미지는 사전 다운샘플링 옵션 (예: max 1024px) — 첫 PR에서는 다운샘플링 옵션 보류, 사용자가 직접 `scale()` 호출.

### 2.3 EXIF Metadata

| 방식                                        | 장점                                                                                 | 단점                                                                                                                            | 평가     |
|---------------------------------------------|--------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|----------|
| **(a) metadata-extractor 2.x (drewnoakes)** | EXIF/IPTC/XMP/GPS 광범위, 80개+ 카메라 maker note, 활발한 유지보수, pure Java, 800KB | 외부 의존 (Apache 2.0)                                                                                                          | **선택** |
| (b) scrimage 내장 EXIF                      | 추가 의존 없음                                                                       | scrimage는 EXIF metadata write/preserve만 지원, **파싱 API 없음** (확인됨: `ImageMetadata` 는 directory 단위 raw tag 노출 정도) | 보류     |
| (c) javax.imageio EXIF                      | JDK 내장                                                                             | 매우 제한적 — JPEG/TIFF만, 일부 tag 누락, GPS 파싱 직접 해야 함                                                                 | 보류     |

**선택: (a) metadata-extractor 2.x**.

- `com.drewnoakes:metadata-extractor:2.19.0` (current latest).
- JPEG/TIFF/PNG/HEIF/WebP/RAW 등 광범위 포맷 지원.
- GPS 좌표는 자동 decimal degree 변환 제공 (`GpsDirectory.geoLocation`).
- `LocalDateTime` 변환은 `ExifSubIFDDirectory.getDateOriginal(timezone)` 사용.

## 3. API 설계

### 3.1 패키지 / 파일 구조

```
utils/images/src/main/kotlin/io/bluetape4k/images/analysis/
├── DominantColor.kt                  // data class + ImmutableImage extensions
├── DominantColorExtractor.kt         // sealed interface (Strategy)
├── MedianCutQuantizer.kt             // internal Median Cut implementation
├── BlurDetector.kt                   // BlurScore + extensions + internal Laplacian
└── ExifData.kt                       // ExifData + ImmutableImage/File/Path extensions
```

### 3.2 Dominant Color API

```kotlin
package io.bluetape4k.images.analysis

/**
 * 추출된 대표 색상 단위.
 *
 * @property r 0..255 red
 * @property g 0..255 green
 * @property b 0..255 blue
 * @property hex `"#rrggbb"` 형식
 * @property population 이 색상이 속한 박스의 픽셀 수 (가중치)
 */
data class DominantColor(
    val r: Int,
    val g: Int,
    val b: Int,
    val population: Int,
) {
    val hex: String = "#%02x%02x%02x".format(r, g, b)
    fun toAwtColor(): java.awt.Color = java.awt.Color(r, g, b)

    companion object {
        fun fromRgb(rgb: Int, population: Int = 1): DominantColor =
            DominantColor((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, population)
    }
}

/**
 * Dominant color 추출 전략 (sealed interface — `HistogramSimilarity` 패턴 재사용).
 */
sealed interface DominantColorExtractor {

    fun extract(image: ImmutableImage, count: Int): List<DominantColor>

    /**
     * Median Cut quantization (color-thief-java 알고리즘).
     *
     * @property quality 픽셀 샘플링 간격 (1=모든 픽셀, 10=10픽셀당 1개). 1..30
     * @property ignoreWhite `true`면 거의 흰색 픽셀 제외
     */
    data class MedianCut(
        val quality: Int = 10,
        val ignoreWhite: Boolean = false,
    ): DominantColorExtractor {
        init { require(quality in 1..30) { "quality 1..30, 입력: $quality" } }
        override fun extract(image: ImmutableImage, count: Int): List<DominantColor> { /* ... */ }
    }

    companion object {
        fun medianCut(quality: Int = 10, ignoreWhite: Boolean = false): DominantColorExtractor =
            MedianCut(quality, ignoreWhite)
    }
}

// Extension functions (블로킹)
fun ImmutableImage.dominantColors(
    count: Int = 5,
    extractor: DominantColorExtractor = DominantColorExtractor.medianCut(),
): List<DominantColor> = extractor.extract(this, count)

// 빈 이미지(alpha=0 전체)에서 null 반환 — emptyList() 계약과 일관
fun ImmutableImage.dominantColor(
    extractor: DominantColorExtractor = DominantColorExtractor.medianCut(),
): DominantColor? = dominantColors(1, extractor).firstOrNull()

// Coroutines suspend — CPU-bound이므로 Dispatchers.Default 사용
suspend fun ImmutableImage.suspendDominantColors(
    count: Int = 5,
    extractor: DominantColorExtractor = DominantColorExtractor.medianCut(),
): List<DominantColor> = withContext(Dispatchers.Default) { dominantColors(count, extractor) }
```

**검증 규칙**:

- `count >= 1` (require).
- `count` 가 추출 가능한 색상 수보다 크면 가능한 만큼만 반환 (단색 이미지 → size=1).
- 픽셀 0개 (alpha=0 전부) → `emptyList()`.
- 결과는 `population` 내림차순 정렬.

### 3.3 Blur Detection API

```kotlin
/**
 * 블러 검사 결과.
 *
 * @property score Laplacian variance — 클수록 sharp, 작을수록 blur
 * @property threshold 비교 기준값
 * @property isBlurry `score < threshold`이면 true
 */
data class BlurScore(
    val score: Double,
    val threshold: Double,
) {
    val isBlurry: Boolean get() = score < threshold
}

/**
 * Laplacian variance 기반 블러 점수.
 *
 * @param threshold 디폴트 100.0 (OpenCV/PyImageSearch 권장값)
 * @return BlurScore
 */
fun ImmutableImage.blurScore(threshold: Double = 100.0): BlurScore

fun ImmutableImage.isBlurry(threshold: Double = 100.0): Boolean = blurScore(threshold).isBlurry

// CPU-bound이므로 Dispatchers.Default 사용
suspend fun ImmutableImage.suspendBlurScore(threshold: Double = 100.0): BlurScore =
    withContext(Dispatchers.Default) { blurScore(threshold) }
```

**구현 노트**:

- Laplacian kernel `[[0,1,0],[1,-4,1],[0,1,0]]` 3x3 single-pass convolution.
- 이미지 boundary 픽셀은 분석에서 제외 (1px border).
- ImmutableImage → grayscale variance — luminance Rec. 601.
- 너무 작은 이미지 (`width < 3 || height < 3`) → `IllegalArgumentException`.

### 3.4 EXIF Metadata API

```kotlin
/**
 * 추출된 EXIF 메타데이터 (모든 필드 nullable — 카메라/포맷별 누락 정상).
 */
data class ExifData(
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAltitude: Double? = null,
    val dateTimeOriginal: LocalDateTime? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val iso: Int? = null,
    val shutterSpeed: String? = null,        // e.g. "1/250"
    val aperture: Double? = null,            // f-number
    val focalLength: Double? = null,         // mm
    val focalLength35mm: Int? = null,        // 35mm equiv.
    val orientation: Int? = null,            // EXIF 1..8
    val width: Int? = null,
    val height: Int? = null,
    val flashFired: Boolean? = null,
    val whiteBalance: String? = null,
) {
    val hasGps: Boolean get() = gpsLatitude != null && gpsLongitude != null
    companion object {
        val EMPTY = ExifData()
    }
}

// Extensions
fun readExif(bytes: ByteArray): ExifData
fun File.readExif(): ExifData
fun Path.readExif(): ExifData
fun InputStream.readExif(): ExifData

// suspend
suspend fun File.suspendReadExif(): ExifData = withContext(Dispatchers.IO) { readExif() }
suspend fun Path.suspendReadExif(): ExifData = withContext(Dispatchers.IO) { readExif() }
```

**중요한 결정 — `ImmutableImage.readExif()`는 제공하지 않는다**:

- Scrimage `ImmutableImage` 는 디코딩된 픽셀만 보존하며 EXIF는 `ImageMetadata` 객체로 분리되어 있음.
- EXIF 추출에는 **원본 byte stream** 이 필요 → `ImmutableImage` 단계에서는 ByteArray 손실.
- 따라서 EXIF API는 `File`/`Path`/`ByteArray`/`InputStream` 진입점만 제공.

**구현 노트**:

- `metadata-extractor` `Metadata` 객체에서 `ExifIFD0Directory`, `ExifSubIFDDirectory`, `GpsDirectory`, `JpegDirectory` 추출.
- 누락된 tag는 모두 `null` 처리 (예외 X) — `try` 가드 후 `null`.
- 손상된 / EXIF 없는 이미지 → `ExifData.EMPTY` 반환 (예외 X).

## 4. 의존성 추가

### 4.1 `buildSrc/src/main/kotlin/Libs.kt`

```kotlin
object Versions {
    // ...existing...
    const val metadata_extractor = "2.19.0"  // https://mvnrepository.com/artifact/com.drewnoakes/metadata-extractor
}

object Libs {
    // ...existing... (after scrimage entries ~line 1511)
    val metadata_extractor = "com.drewnoakes:metadata-extractor:${Versions.metadata_extractor}"
}
```

### 4.2 `utils/images/build.gradle.kts`

```kotlin
dependencies {
    // ...existing...

    // EXIF metadata — 공개 API에서 metadata-extractor 타입 미노출, implementation 스코프
    implementation(Libs.metadata_extractor)
}
```

> **결정: `implementation`** — 본 모듈의 공개 API (`ExifData`)는 metadata-extractor 타입을 노출하지 않는다.
> metadata-extractor는 **필수 런타임 의존성**이다 (optional/compileOnly 아님).
> 다운스트림이 추가 EXIF 처리를 원하면 별도로 의존성을 선언해야 한다.

## 5. 설계 리스크 & 실패 모드

### Risk 1 — Median Cut: 단색/저색상 이미지 edge case

- **시나리오**: 단색 이미지 (모두 흰색) 에서 `dominantColors(5)` 호출 → 원본 알고리즘은 box 1개에서 멈춤.
- **완화**: 박스 분할 시 `box.count <= 1 || box.volume == 0` 이면 분할 중단. 사용자에게는 size < count 인 List 반환 (계약 명시).
- **테스트**: 단색/2색/투명 이미지 fixture 추가.

### Risk 2 — Median Cut: 픽셀 5x5x5 색공간 vs 8x8x8 색공간 결정

- color-thief-java는 5-bit/channel (32 levels) — RGB cube 32x32x32 = 32K bin → 메모리/속도 균형.
- **결정**: 5-bit (32 levels)/channel 채택. 박스 분할 시 longest-axis variance 기반.
- 8-bit (256 levels) 사용 시 메모리 16MB+ 폭발. 5-bit가 표준.

### Risk 3 — Blur Detection: 이미지 사이즈 의존성

- threshold=100 은 ~640x480 기준. 4K 이미지에서는 variance 자체가 더 큼 → blur 판정 어려움.
- **완화 (이번 PR)**: KDoc에 명시 — "동일 도메인 이미지에서 상대 비교용으로 사용. 절대 threshold는 캘리브레이션 필요". 사용자가 직접 다운샘플링.
- **완화 (후속 옵션)**: `BlurDetector.LaplacianVariance(maxSize = 1024)` — 자동 다운스케일 옵션. 본 PR에서는 보류 (YAGNI).

### Risk 4 — EXIF: `LocalDateTime` 타임존 없음

- EXIF `DateTimeOriginal` 태그는 **timezone 정보를 포함하지 않는다** (ISO 8601과 달리).
- **결정**: `ExifData.dateTimeOriginal: LocalDateTime?` — EXIF wall-clock 값을 그대로 보존. "UTC 가정"이 아니라 단순히 timezone-naive.
- KDoc에 명시: "EXIF DateTimeOriginal은 timezone 정보 없음. 이 필드는 카메라 현지 시각 그대로이며 UTC 변환은 호출자 책임 (GPS 좌표 또는 카메라 설정 기반 보정 권장)."
- 구현에서 `getDateOriginal(TimeZone.UTC)` 호출 시 metadata-extractor는 UTC 해석이 아닌 날짜/시간 문자열만 파싱하므로 결과 값 동일. KDoc 경고로 충분.

### Risk 5 — metadata-extractor: 의존성 크기 증가

- 약 800KB jar. webp + scrimage-core 합산이 이미 크므로 영향 미미. 그러나 모든 utils/images 사용자에게 부과.
- **완화**: `implementation` 스코프 — transitive 노출 차단. **필수 런타임 의존** (optional 처리 안 함).
- 추가 완화 검토: `DominantColor`/`Blur` 만 쓰는 사용자를 위한 별도 모듈 분리는 **불필요** (단일 모듈 유지).

### Risk 6 — Coroutines: CPU-bound 작업 Dispatcher

- Median Cut / Laplacian convolution은 **CPU-bound** 연산.
-

**결정: `Dispatchers.Default`** — CPU-bound 작업 본질에 맞는 선택. 기존 `suspendBytes`/`suspendWrite`는 실제 I/O이므로 IO Dispatcher가 맞음 — 본 분석 함수는 다름.
- `suspend` 변형 전체 (`suspendDominantColors`, `suspendBlurScore`, `suspendReadExif`) 에 `Dispatchers.Default` 적용.
- (단, `suspendReadExif`는 파일 읽기 포함이므로 `Dispatchers.IO` 유지)

### Risk 7 — 입력 이미지 크기 / malformed EXIF

- 제한 없는 이미지 입력 → OOM 위험. 100MP RAW 이미지 (12000×8000px)에서 Laplacian 전체 픽셀 순회 → 수십 MB 힙 점유.
- malformed/조작된 JPEG EXIF → metadata-extractor 내부 무한루프 또는 메모리 폭발 가능성 (버전별 패치 이력 있음).
- **완화 (이번 PR)**:
    - `readExif(bytes: ByteArray)`: `require(bytes.size <= 50 * 1024 * 1024)` 가드 (50MB 상한).
    - metadata-extractor가 던지는 모든 예외 (`ImageProcessingException`, `IOException`, `Exception`) → catch → `ExifData.EMPTY` 반환.
    - Blur/DominantColor: 사용자 책임 명시 (KDoc에 "큰 이미지는 호출 전 다운샘플링 권장"). 자동 상한은 YAGNI.
- **완화 (후속)**: `maxPixels` 파라미터 추가 옵션.

## 6. DoD (Definition of Done)

```markdown
## DoD

### Code
- [ ] `io.bluetape4k.images.analysis.DominantColor` data class 정의 (r/g/b/hex/population)
- [ ] `DominantColorExtractor` sealed interface + `MedianCut` data class 구현
- [ ] `MedianCutQuantizer` internal 객체 — 픽셀 샘플링 + 5-bit color cube + 박스 분할
- [ ] `ImmutableImage.dominantColors(count, extractor)` extension
- [ ] `ImmutableImage.dominantColor(extractor): DominantColor?` extension (빈 이미지 → null)
- [ ] `ImmutableImage.suspendDominantColors(count, extractor)` extension (Dispatchers.Default)

- [ ] `BlurScore` data class (score / threshold / isBlurry)
- [ ] `ImmutableImage.blurScore(threshold)` — Laplacian variance
- [ ] `ImmutableImage.isBlurry(threshold)` 단축
- [ ] `ImmutableImage.suspendBlurScore(threshold)` (Dispatchers.Default)

- [ ] `ExifData` data class (모든 필드 nullable + `hasGps` + `EMPTY`)
- [ ] `readExif(ByteArray)` — 50MB 상한 가드, 모든 예외 → EMPTY 반환
- [ ] `File.readExif()` / `Path.readExif()` / `InputStream.readExif()` extensions
- [ ] `File.suspendReadExif()` / `Path.suspendReadExif()` extensions (Dispatchers.IO — 파일 I/O 포함)

### Dependencies
- [ ] `buildSrc/Libs.kt`에 `metadata_extractor` 버전/좌표 추가
- [ ] `utils/images/build.gradle.kts`에 `implementation(Libs.metadata_extractor)` 추가

### Tests (`utils/images/src/test/kotlin/io/bluetape4k/images/analysis/`)
- [ ] `DominantColorExtractorTest` — homer.jpg/landscape.jpg/cafe.jpg 에서 색상 추출, count=1/3/5, edge case (단색)
- [ ] `BlurDetectorTest` — sharp 이미지 vs blur 이미지 (Scrimage `ImmutableImage.blur()` 사용해 흐림 fixture 생성), threshold 검증
- [ ] `ExifDataTest` — `src/test/resources/images/exif/` 에 GPS 포함 / 미포함 / 손상 이미지 3종 fixture, GPS decimal 변환, dateTimeOriginal 검증
- [ ] `SuspendAnalysisTest` — `runTest`로 suspend 변형 호출 검증
- [ ] 모든 테스트는 `AbstractImageTest` 상속, bluetape4k-assertions `shouldBe*` 사용
- [ ] `companion object: KLoggingChannel()` 패턴 준수

### Test Resources
- [ ] `src/test/resources/images/exif/with-gps.jpg` — GPS EXIF 포함 샘플 (Wikimedia/CC0 출처 명시)
- [ ] `src/test/resources/images/exif/no-exif.jpg` — EXIF 없는 이미지
- [ ] `src/test/resources/images/exif/full-exif.jpg` — 카메라/렌즈 등 풍부한 EXIF
- [ ] 각 fixture의 origin/license는 `src/test/resources/images/exif/README.md`에 명시

### Documentation
- [ ] `utils/images/README.md` — Image Analysis 섹션 추가 (영어)
- [ ] `utils/images/README.ko.md` — 동일 섹션 (한국어)
- [ ] Mermaid 다이어그램: 분석 파이프라인 (Image → Extractor → Result)
- [ ] 모든 public API에 KDoc (Korean OK)

### Follow-up Issues
- [ ] OCR (Tesseract) — 별도 이슈 등록
- [ ] Face Detection — 별도 이슈 등록
- [ ] Image Classification (ONNX/TFLite) — 별도 이슈 등록 (장기)

### Verification
- [ ] `./gradlew :bluetape4k-images:test` 통과 (테스트 수 + duration 보고)
- [ ] `./gradlew :bluetape4k-images:detekt` 통과
- [ ] `oh-my-claudecode:code-reviewer` 실행 — HIGH/CRITICAL 0건
```

## 7. 초안 Task 목록 (복잡도 라벨)

| #   | Task                                                                                      | 복잡도     | 비고                                                                                    |
|-----|-------------------------------------------------------------------------------------------|------------|-----------------------------------------------------------------------------------------|
| T01 | `Libs.kt` + `build.gradle.kts` — metadata-extractor 추가                                  | **low**    | 좌표/버전 추가                                                                          |
| T02 | `DominantColor` data class + companion (`fromRgb`)                                        | **low**    | 단순 value class                                                                        |
| T03 | `MedianCutQuantizer` internal — 5-bit color cube + 박스 분할 알고리즘                     | **high**   | core algorithm — color-thief-java 참조 자체 구현 (~150 LoC). edge case (단색/투명) 핵심 |
| T04 | `DominantColorExtractor` sealed interface + `MedianCut` data class                        | **medium** | T03 호출 wrapping                                                                       |
| T05 | `ImmutableImage.dominantColors/dominantColor/suspendDominantColors` extensions            | **low**    | T04 사용                                                                                |
| T06 | `DominantColorExtractorTest` — fixtures, count 변화, edge case                            | **medium** | 테스트 데이터 검증 까다로움                                                             |
| T07 | `BlurScore` data class                                                                    | **low**    |                                                                                         |
| T08 | `BlurDetector` internal — Laplacian variance grayscale 계산                               | **medium** | 3x3 convolution + variance, boundary 처리                                               |
| T09 | `ImmutableImage.blurScore/isBlurry/suspendBlurScore` extensions                           | **low**    |                                                                                         |
| T10 | `BlurDetectorTest` — sharp vs `image.blur()` 흐림 비교                                    | **medium** | fixture 동적 생성 가능                                                                  |
| T11 | `ExifData` data class (17 필드 + `hasGps` + `EMPTY`)                                      | **low**    |                                                                                         |
| T12 | `ExifData.kt` — metadata-extractor → `ExifData` 변환 (`readExif` 4 진입점)                | **medium** | tag null 가드, GPS decimal 변환, LocalDateTime 변환                                     |
| T13 | EXIF test fixtures 추가 (`with-gps.jpg`, `no-exif.jpg`, `full-exif.jpg`) + license README | **medium** | 외부 샘플 출처/라이선스 검증 필요                                                       |
| T14 | `ExifDataTest` — 3 fixture × 다양한 진입점 + `EMPTY` 케이스                               | **medium** |                                                                                         |
| T15 | `SuspendAnalysisTest` — runTest로 suspend 호출 검증                                       | **low**    |                                                                                         |
| T16 | `README.md` + `README.ko.md` — Image Analysis 섹션 + Mermaid                              | **medium** | 이중 언어 동기                                                                          |
| T17 | KDoc 보완 (Korean) — 모든 public API                                                      | **low**    |                                                                                         |
| T18 | Follow-up issue 3건 등록 (OCR / Face / Classification)                                    | **low**    | `gh issue create`                                                                       |
| T19 | `code-reviewer` 실행 → HIGH/CRITICAL 해소                                                 | **medium** |                                                                                         |
| T20 | `:bluetape4k-images:test` 전수 통과 + 결과 보고                                           | **low**    | CI 검증                                                                                 |

**총 20 tasks** — high 1, medium 9, low 10.

## 8. 의문점 / 후속 결정

1. **CPU-bound dispatcher**: **결정 완료** — `Dispatchers.Default` 사용 (분석 연산). `suspendReadExif`만 `Dispatchers.IO` (파일 I/O).
2. **Blur 다운스케일 옵션**: 본 PR에서는 보류. 사용자 피드백 후 옵션 추가 검토.
3. **DominantColor palette extraction with
   weighting**: 현재 `population` 단순 픽셀 수. HSV-aware weighting (saturation 보정) 은 v2.
4. **EXIF 쓰기 (write/preserve)**: 본 PR scope 외. Scrimage가 자체 metadata preserve 지원하므로 별도 이슈로 분리.

## 9. 참조

- color-thief-java: https://github.com/SvenWoltmann/color-thief-java (MIT)
- metadata-extractor: https://github.com/drewnoakes/metadata-extractor (Apache 2.0)
- Pech-Pacheco "Diatom autofocusing in brightfield microscopy" (Laplacian variance)
- 기존 패턴 참조: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HistogramSimilarity.kt`
