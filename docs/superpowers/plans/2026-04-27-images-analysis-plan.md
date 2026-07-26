# Implementation Plan: utils/images Image Analysis (#133)

**Date**: 2026-04-27 **Spec**: docs/superpowers/specs/2026-04-27-images-analysis-design.md **Issue**: #133
**Branch**: `feat/issue-133-images-analysis`
**Worktree**: `.worktrees/feat/issue-133-images-analysis`
**Module**: `utils/images` (`bluetape4k-images`)

## Overview

`utils/images` 모듈에 3종의 순수 JVM 기반 이미지 분석 기능을 추가한다.

1. **Dominant Color Extraction** — Median Cut quantization 자체 구현 (5-bit color cube).
2. **Blur Detection** — Laplacian variance grayscale convolution.
3. **EXIF Metadata Extraction** — drewnoakes `metadata-extractor 2.19.0` 활용.

설계 원칙: pure JVM (native dep 금지), 기존 `HistogramSimilarity` sealed interface 패턴 재사용, extension function API, suspend 변형은 CPU-bound → `Dispatchers.Default` / 파일 I/O는 `Dispatchers.IO`.

OCR / Face Detection / 분류는 본 PR scope 외 — follow-up issue 등록.

## Source Tree (생성/변경 파일)

```
buildSrc/src/main/kotlin/Libs.kt                                              # 변경 (T01)
utils/images/build.gradle.kts                                                 # 변경 (T02)

utils/images/src/main/kotlin/io/bluetape4k/images/analysis/
├── DominantColor.kt                                                          # 신규 (T03 + T06)
├── DominantColorExtractor.kt                                                 # 신규 (T05)
├── MedianCutQuantizer.kt                                                     # 신규 (T04)
├── BlurDetector.kt                                                           # 신규 (T07 + T08 + T09)
└── ExifData.kt                                                               # 신규 (T10 + T11)

utils/images/src/test/kotlin/io/bluetape4k/images/analysis/
├── DominantColorExtractorTest.kt                                             # 신규 (T12)
├── BlurDetectorTest.kt                                                       # 신규 (T13)
├── ExifDataTest.kt                                                           # 신규 (T15)
└── SuspendAnalysisTest.kt                                                    # 신규 (T16)

utils/images/src/test/resources/images/exif/
├── with-gps.jpg                                                              # 신규 fixture (T14)
├── no-exif.jpg                                                               # 신규 fixture (T14)
├── full-exif.jpg                                                             # 신규 fixture (T14)
└── README.md                                                                 # 신규 license/origin 문서 (T14)

utils/images/README.md                                                        # 변경 (T17)
utils/images/README.ko.md                                                     # 변경 (T17)
```

## Task List

---

### T01 — Libs.kt 의존성 좌표/버전 추가

- **complexity**: low
-

**파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat/issue-133-images-analysis/buildSrc/src/main/kotlin/Libs.kt`
- **내용**:
    - `Versions` object에 `const val metadata_extractor = "2.19.0"` 추가
    - `Libs` object에 `val metadata_extractor = "com.drewnoakes:metadata-extractor:${Versions.metadata_extractor}"` 추가 (scrimage 인접 위치 ~line 1511)
- **완료 기준**: `Libs.metadata_extractor` 참조가 컴파일됨. (DoD §Dependencies 1)

---

### T02 — utils/images/build.gradle.kts 의존성 추가

- **complexity**: low
-

**파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat/issue-133-images-analysis/utils/images/build.gradle.kts`
- **내용**:
    - `dependencies { ... implementation(Libs.metadata_extractor) }` 추가
    - 공개 API에 metadata-extractor 타입을 노출하지 않으므로 `implementation` 스코프 (api 아님)
- **완료 기준**: `./gradlew :bluetape4k-images:dependencies | grep metadata-extractor` 출력 확인. (DoD §Dependencies 2)

---

### T03 — DominantColor data class

- **complexity**: low
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/DominantColor.kt`
- **내용**:
    - `data class DominantColor(r: Int, g: Int, b: Int, population: Int)` 정의
    - `init` 블록: `r/g/b in 0..255`, `population >= 0` require 검증
    - `val hex: String = "#%02x%02x%02x".format(r, g, b)` 계산 프로퍼티
    - `fun toAwtColor(): java.awt.Color`
    - `companion object { fun fromRgb(rgb: Int, population: Int = 1): DominantColor }`
    - `companion object: KLogging()` 패턴 적용
- **완료 기준**: 단위 테스트 (T12)에서 `DominantColor(200, 100, 50, 1234).hex shouldBeEqualTo "#c86432"` 통과. (DoD §Code 1)

---

### T04 — MedianCutQuantizer internal 알고리즘

- **complexity**: high
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/MedianCutQuantizer.kt`
- **내용**:
    - `internal object MedianCutQuantizer` 또는 `internal class`
    - 5-bit color cube (32 levels/channel) — RGB → bin index `((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)`
    - 픽셀 샘플링: `quality` 간격으로 `image.pixels()` 순회, alpha=0 제외, ignoreWhite 옵션
    - `VBox` 내부 데이터 구조: rmin/rmax/gmin/gmax/bmin/bmax + histogram bin 카운트
    - `vbox.volume()`, `vbox.count()`, `vbox.average()` 헬퍼
    - 가장 긴 축 기반 median split — pixel histogram median으로 분할점 결정
    - PriorityQueue (population 큰 박스 우선) 또는 list + sort 방식으로 박스 분할 반복
    - 종료 조건: `count == k` 또는 `box.count <= 1 || box.volume == 0` (단색 edge case)
    - 결과: `List<DominantColor>` — 각 박스의 평균 RGB + population, population 내림차순
    - 빈 픽셀 시 `emptyList()`
- **완료 기준**:
    - 단색 이미지 → size=1 List 반환
    - 투명 이미지 → emptyList ()
    - 5색 그라데이션 이미지 → count=5 호출 시 5개 색상 반환, population 내림차순
    - (DoD §Code 3)

---

### T05 — DominantColorExtractor sealed interface

- **complexity**: medium
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/DominantColorExtractor.kt`
- **내용**:
    - `sealed interface DominantColorExtractor { fun extract(image: ImmutableImage, count: Int): List<DominantColor> }`
    - `data class MedianCut(val quality: Int = 10, val ignoreWhite: Boolean = false): DominantColorExtractor`
        - `init { require(quality in 1..30) { "quality 1..30, 입력: $quality" } }`
        - `extract` → `MedianCutQuantizer` 위임
    - `companion object { fun medianCut(quality: Int = 10, ignoreWhite: Boolean = false): DominantColorExtractor = MedianCut(quality, ignoreWhite) }`
- **완료 기준**:
    - `DominantColorExtractor.medianCut().extract(image, 5)` 호출 가능
    - `quality=0` 시 `IllegalArgumentException`
    - (DoD §Code 2)

---

### T06 — ImmutableImage extensions (DominantColor)

- **complexity**: low
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/DominantColor.kt` (T03 동일 파일에 extension 추가)
- **내용**:
    - `fun ImmutableImage.dominantColors(count: Int = 5, extractor: DominantColorExtractor = DominantColorExtractor.medianCut()): List<DominantColor>`
        - `require(count >= 1)` 검증
    - `fun ImmutableImage.dominantColor(extractor: DominantColorExtractor = DominantColorExtractor.medianCut()): DominantColor?`
        - `dominantColors(1, extractor).firstOrNull()`
    - `suspend fun ImmutableImage.suspendDominantColors(...)` → `withContext(Dispatchers.Default) { dominantColors(...) }`
    - KDoc 한국어 작성
- **완료 기준**: 4-5 종 fixture 이미지에서 `dominantColors(5).size == 5` 또는 단색 이미지 size==1 검증 (T12). (DoD §Code 4-6)

---

### T07 — BlurScore data class

- **complexity**: low
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/BlurDetector.kt`
- **내용**:
    - `data class BlurScore(val score: Double, val threshold: Double)`
    - `val isBlurry: Boolean get() = score < threshold`
    - `companion object { const val DEFAULT_THRESHOLD: Double = 100.0 }` (Pech-Pacheco/PyImageSearch 권장)
- **완료 기준**: `BlurScore(50.0, 100.0).isBlurry shouldBeEqualTo true` (T13). (DoD §Code 7)

---

### T08 — BlurDetector internal Laplacian variance

- **complexity**: medium
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/BlurDetector.kt` (T07 동일 파일)
- **내용**:
    - `internal object BlurDetector { fun computeVariance(image: ImmutableImage): Double }`
    - require: `image.width >= 3 && image.height >= 3`
    - 1픽셀 boundary 제외하고 모든 내부 픽셀에 3x3 Laplacian 커널 `[[0,1,0],[1,-4,1],[0,1,0]]` 적용
    - grayscale 변환: `0.299*R + 0.587*G + 0.114*B` (Rec. 601)
    - convolution 결과 픽셀 값들의 분산 (variance) 계산: `mean = sum / n`, `variance = sumSq/n - mean*mean`
    - 효율성: 단일 패스, double 누적
- **완료 기준**:
    - 강하게 블러 처리한 이미지 (`image.blur(20)`) variance ≪ 100
    - sharp 원본 이미지 variance ≫ 100
    - `width=2` 이미지에서 `IllegalArgumentException`
    - (DoD §Code 8)

---

### T09 — ImmutableImage extensions (Blur)

- **complexity**: low
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/BlurDetector.kt` (T07 동일 파일에 extension)
- **내용**:
    - `fun ImmutableImage.blurScore(threshold: Double = BlurScore.DEFAULT_THRESHOLD): BlurScore`
        - `BlurScore(BlurDetector.computeVariance(this), threshold)`
    - `fun ImmutableImage.isBlurry(threshold: Double = BlurScore.DEFAULT_THRESHOLD): Boolean = blurScore(threshold).isBlurry`
    - `suspend fun ImmutableImage.suspendBlurScore(threshold: Double = BlurScore.DEFAULT_THRESHOLD): BlurScore = withContext(Dispatchers.Default) { blurScore(threshold) }`
    - KDoc 한국어 작성
- **완료 기준**: T13 테스트 통과. (DoD §Code 9-11)

---

### T10 — ExifData data class

- **complexity**: low
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/ExifData.kt`
- **내용**:
    - 17개 nullable 필드: `gpsLatitude`, `gpsLongitude`, `gpsAltitude`, `dateTimeOriginal: LocalDateTime?`, `cameraMake`, `cameraModel`, `lensModel`, `iso: Int?`, `shutterSpeed: String?` (e.g. "1/250"), `aperture: Double?`, `focalLength: Double?`, `focalLength35mm: Int?`, `orientation: Int?`, `width: Int?`, `height: Int?`, `flashFired: Boolean?`, `whiteBalance: String?`
    - `val hasGps: Boolean get() = gpsLatitude != null && gpsLongitude != null`
    - `companion object { val EMPTY: ExifData = ExifData() }`
    - 모든 필드 default = null
- **완료 기준**: `ExifData.EMPTY.hasGps shouldBeEqualTo false` (T15). (DoD §Code 12)

---

### T11 — ExifData metadata-extractor 변환 + 진입점 4종

- **complexity**: medium
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/analysis/ExifData.kt` (T10 동일 파일)
- **내용**:
    - `private fun Metadata.toExifData(width: Int? = null, height: Int? = null): ExifData`
        - `ExifIFD0Directory` → make/model/orientation
        - `ExifSubIFDDirectory` → dateTimeOriginal/iso/shutterSpeed/aperture/focalLength/focalLength35mm/lensModel/flashFired/whiteBalance
        - `GpsDirectory.geoLocation` → latitude/longitude (decimal), altitude
        - `JpegDirectory` → width/height (없으면 width/height 파라미터 사용)
        - 모든 필드 read는 `try { dir.getXxx(tag) } catch { null }` 가드
        - `LocalDateTime` 변환: EXIF 문자열 직접 파싱 (tz-independent)
          ```kotlin
          private val EXIF_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
          subIfd.getString(TAG_DATETIME_ORIGINAL)?.let {
              runCatching { LocalDateTime.parse(it, EXIF_DATE_FORMATTER) }.getOrNull()
          }
          ```
          (이유: `TimeZone.getDefault()` 의존 방식은 CI 환경 tz에 따라 비결정적)
        - `shutterSpeed`: `Rational` → `"${numer}/${denom}"` 문자열 변환 (e.g. "1/250")
          ```kotlin
          subIfd.getRational(TAG_SHUTTER_SPEED_VALUE)?.let { "${it.numerator}/${it.denominator}" }
              ?: subIfd.getString(TAG_EXPOSURE_TIME)
          ```
    - 진입점 4종:
        - `fun readExif(bytes: ByteArray): ExifData`
            - `require(bytes.size <= 50 * 1024 * 1024) { "이미지 byte 50MB 초과: ${bytes.size}" }`
            - `try { ImageMetadataReader.readMetadata(ByteArrayInputStream(bytes)) } catch (e: Exception) { return ExifData.EMPTY }`
        - `fun File.readExif(): ExifData` — `try { ImageMetadataReader.readMetadata(this) } catch { ExifData.EMPTY }`
        - `fun Path.readExif(): ExifData` — `Files.newInputStream(this).use { it.readExif() }`
          (이유: `toFile()` 방식은 jar/zip 내 Path 접근 시 실패)
        - `fun InputStream.readExif(): ExifData` — `try { ImageMetadataReader.readMetadata(this) } catch { ExifData.EMPTY }`
    - suspend:
        - `suspend fun File.suspendReadExif(): ExifData = withContext(Dispatchers.IO) { readExif() }`
        - `suspend fun Path.suspendReadExif(): ExifData = withContext(Dispatchers.IO) { readExif() }`
    - 모든 catch 블록은 `companion object: KLogging()` 통한 debug 로그 + EMPTY 반환
- **완료 기준**:
    - GPS 포함 이미지에서 `readExif().hasGps == true` (T15)
    - 손상/EXIF 미포함 이미지에서 `ExifData.EMPTY` 반환 (T15)
    - 50MB 초과 ByteArray → `IllegalArgumentException`
    - (DoD §Code 13-15)

---

### T12 — DominantColorExtractorTest

- **complexity**: medium
- **파일**: `utils/images/src/test/kotlin/io/bluetape4k/images/analysis/DominantColorExtractorTest.kt`
- **내용**:
    - `class DominantColorExtractorTest: AbstractImageTest()`
    - `companion object: KLoggingChannel()` 패턴
    - 테스트 케이스:
        - `homer.jpg`/`landscape.jpg`/`cafe.jpg`(기존 fixture 가용시) 또는 동적 생성 이미지에서 `dominantColors(5)` 추출 → size <= 5, 모든 hex 형식 검증
        - count=1 → size=1
        - 단색 이미지 (전체 white) → size=1, hex == "#ffffff"
        - 투명 이미지 (alpha=0 전부) → emptyList ()
        - `quality=0` IllegalArgumentException
        - `count=0` IllegalArgumentException
        - `ignoreWhite=true` 동작 검증 (대부분 흰색 + 빨간 점 → 빨간색이 1순위)
    - `population 내림차순` 검증
- **완료 기준**: 8개 이상 테스트 케이스 모두 PASS. (DoD §Tests 1)

---

### T13 — BlurDetectorTest

- **complexity**: medium
- **파일**: `utils/images/src/test/kotlin/io/bluetape4k/images/analysis/BlurDetectorTest.kt`
- **내용**:
    - `class BlurDetectorTest: AbstractImageTest()`
    - `companion object: KLoggingChannel()` 패턴 적용
    - sharp fixture: 기존 `src/test/resources/images/` 의 `homer.jpg` 사용 (사진 이미지, 선명)
    - blur fixture: `homer.jpg`에 Scrimage `image.blur(20)` 적용 (동적 생성)
    - 테스트 케이스:
        - `sharp.blurScore().score shouldBeGreaterThan blurred.blurScore().score`
        - `blurred.isBlurry(100.0) shouldBeEqualTo true` (강한 블러)
        - `sharp.isBlurry(100.0) shouldBeEqualTo false`
        - `BlurScore(50.0, 100.0).isBlurry shouldBeEqualTo true`
        - 너무 작은 이미지 (`width=2`) → IllegalArgumentException
        - threshold 변경 시 isBlurry 결과 변화
    - **bluetape4k-assertions matcher 규칙**: `(x > y).shouldBeTrue()` 금지 → `shouldBeGreaterThan` 사용
- **완료 기준**: 6개 이상 테스트 케이스 모두 PASS. (DoD §Tests 2)

---

### T14 — EXIF test fixtures

- **complexity**: medium
- **파일**:
    - `utils/images/src/test/resources/images/exif/with-gps.jpg`
    - `utils/images/src/test/resources/images/exif/no-exif.jpg`
    - `utils/images/src/test/resources/images/exif/full-exif.jpg`
    - `utils/images/src/test/resources/images/exif/README.md`
- **내용**:
    - `with-gps.jpg`: GPS EXIF 포함 샘플 (Wikimedia Commons / CC0 출처). 작은 사이즈 (~100KB 이하) 권장
    - `no-exif.jpg`: EXIF 없는 이미지 (Scrimage 등으로 새로 인코딩하면 EXIF 제거됨)
    - `full-exif.jpg`: 카메라 make/model/lens/iso/aperture 풍부한 EXIF (DPReview/Wikimedia 샘플)
    - `README.md`: 각 fixture의 origin URL + license + 추출된 EXIF 요약 (테스트 기대값 documentation)
- **완료 기준**: 3개 jpg + README 모두 존재, fixture를 T15에서 로드 가능. license 명시 완료. (DoD §Test Resources)

---

### T15 — ExifDataTest

- **complexity**: medium
- **파일**: `utils/images/src/test/kotlin/io/bluetape4k/images/analysis/ExifDataTest.kt`
- **내용**:
    - `class ExifDataTest: AbstractImageTest()`
    - `companion object: KLoggingChannel()` 패턴 적용
    - 테스트 케이스:
        - `with-gps.jpg`.readExif () → `hasGps == true`, latitude/longitude 범위 검증 (`shouldBeInRange`)
        - `no-exif.jpg`.readExif () → `ExifData.EMPTY` 또는 모든 필드 null
        - `full-exif.jpg`.readExif () → cameraMake, cameraModel, iso, aperture 등 not null
        - `dateTimeOriginal` LocalDateTime 검증
        - `readExif(ByteArray)` 진입점 검증
        - `Path.readExif()` 진입점 검증
        - `InputStream.readExif()` 진입점 검증
        - `ByteArray(50 * 1024 * 1024 + 1)` → IllegalArgumentException
        - 손상 byte (`byteArrayOf(0x00, 0x01, 0x02)`) → ExifData.EMPTY (예외 X)
    - `ExifData.EMPTY.hasGps == false` 검증
- **완료 기준**: 9개 이상 테스트 케이스 모두 PASS. (DoD §Tests 3)

---

### T16 — SuspendAnalysisTest

- **complexity**: low
- **파일**: `utils/images/src/test/kotlin/io/bluetape4k/images/analysis/SuspendAnalysisTest.kt`
- **내용**:
    - `class SuspendAnalysisTest: AbstractImageTest()`
    - `companion object: KLoggingChannel()` 패턴 적용
    - `kotlinx.coroutines.test.runTest(timeout = 30.seconds)` 사용
    - 테스트:
        - `suspendDominantColors(5)` — 결과가 blocking `dominantColors(5)`와 동일
        - `suspendBlurScore()` — 결과가 blocking `blurScore()`와 동일 (score 동일)
        - `File.suspendReadExif()` — 결과가 blocking `readExif()`와 동일
- **완료 기준**: 3개 테스트 PASS. (DoD §Tests 4)

---

### T17 — README + README.ko 업데이트

- **complexity**: medium
- **파일**:
    - `utils/images/README.md`
    - `utils/images/README.ko.md`
- **내용**:
    - 새 섹션 "Image Analysis" 추가 (영어/한국어 둘 다)
    - 하위: Dominant Color / Blur Detection / EXIF Metadata 각 코드 예제 포함
    - Mermaid 다이어그램: `Image → Extractor → Result` 파이프라인
      ```mermaid
      graph LR
        Image[ImmutableImage / File / ByteArray] --> Analyzer
        Analyzer{Analyzer}
        Analyzer -->|MedianCut| DC[List<DominantColor>]
        Analyzer -->|Laplacian| BS[BlurScore]
        Analyzer -->|metadata-extractor| EX[ExifData]
      ```
    - Architecture / Features / Examples 표준 구조 따르기
    - 양쪽 README 동기화 (구조 동일, 언어만 다름)
- **완료 기준**: 두 README 모두 새 섹션 포함, Mermaid 다이어그램 렌더 가능. (DoD §Documentation 1-3)

---

### T18 — KDoc 한국어 작성

- **complexity**: low
- **파일**: T03~T11에서 작성한 모든 .kt 파일
- **내용**:
    - 모든 public class/data class/sealed interface/extension function에 KDoc
    - `@property` / `@param` / `@return` / `@throws` 명시
    - 한국어 OK (CLAUDE.md 가이드)
    - 핵심 KDoc 주의사항:
        - `BlurScore` — "동일 도메인 이미지에서 상대 비교용. 절대 threshold는 캘리브레이션 필요"
        - `ExifData.dateTimeOriginal` — "EXIF DateTimeOriginal은 timezone 정보 없음. 카메라 wall-clock 그대로"
        - `readExif(ByteArray)` — "50MB 상한, 파싱 실패 시 EMPTY 반환"
        - `dominantColor()` — "빈 이미지 (alpha=0 전부) → null"
- **완료 기준**: 모든 public symbol이 KDoc 보유. (DoD §Documentation 4)

---

### T19 — Follow-up issues 등록

- **complexity**: low
- **파일**: 없음 (GitHub Issue)
- **내용**: `gh issue create` 비대화형 모드로 3건 등록
    1. **OCR support (Tesseract)** — Tess4J/Tesseract via JNI 평가
    2. **Face Detection** — OpenCV / openimaj / native binding 평가
    3. **Image Classification** — ONNX Runtime / TFLite Java 평가

    - 각 이슈에 본 PR (#133) 링크
    - 라벨: `enhancement`, `images`
- **완료 기준**: 3개 이슈 URL을 PR 본문에 첨부. (DoD §Follow-up)

---

### T20 — :bluetape4k-images:test 전수 통과 확인

- **complexity**: low
- **파일**: 없음 (CI/local verification)
- **내용**:
    - `./gradlew :bluetape4k-images:test` 실행
    - 결과 (통과 수 + duration) 보고
    - `./gradlew :bluetape4k-images:detekt` 실행 — 0 issue 확인
    - `oh-my-claudecode:code-reviewer` 실행 — HIGH/CRITICAL 0건 확인
- **완료 기준**: 모든 신규 테스트 PASS, detekt clean, code-reviewer clean. (DoD §Verification)

---

## 의존성 순서 / 병렬화 가능성

```
Wave 1: T01 → T02 (의존성 추가 sequential)
Wave 2: T03 / T07 / T10 (data class 3종 병렬 — 알고리즘 미의존)
Wave 3: T04 / T08 / T11 (internal 알고리즘 3종 병렬)
Wave 4: T05 → T06 / T09 (T04 완료 후 sealed interface, T08 완료 후 blur extensions)
         ※ T05는 T04 의존, T06은 T05 의존, T09는 T08 의존
Wave 5: T14 (EXIF fixtures — 독립, T11 병렬 가능하나 T15보다 먼저)
Wave 6: T12 / T13 / T15 / T16 (테스트 4종 병렬 — 각 기능 구현 완료 후)
Wave 7: T17 / T18 (docs 병렬)
Wave 8: T19 → T20 (follow-up + verification sequential)
```

병렬 처리 권장 그룹:

- **Wave 1**: T01 → T02 (sequential)
- **Wave 2 (parallel)**: T03 / T07 / T10 — 단순 data class, 알고리즘 의존 없음
- **Wave 3 (parallel)**: T04 / T08 / T11 — 각각 독립적 internal 구현
- **Wave 4**: T05 (T04 완료 후) → T06 / T09 (T05, T08 각 완료 후)
- **Wave 5**: T14 (EXIF fixtures, 독립)
- **Wave 6 (parallel)**: T12 / T13 / T15 / T16 — 각 기능 완료 후 테스트
- **Wave 7 (parallel)**: T17 / T18 — docs
- **Wave 8**: T19 → T20

## 주의사항 / 구현 가이드

### Median Cut 알고리즘 핵심 포인트

1. **5-bit 색공간 필수** — 8-bit는 메모리 16MB 폭발 (32^3=32K bin이 256^3=16M로 폭증)
2. **bin index 공식**: `((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)` — 각 채널 5bit
3. **Median split**: 가장 긴 축 (`max(rrange, grange, brange)`)을 선택 → 해당 축 histogram cumulative sum의 median에서 분할
4. **종료 조건**: `box.count <= 1 || box.volume == 0` 시 더 이상 분할 안 함 → 사용자 요청 count보다 적은 결과 가능
5. **빈 이미지** (alpha=0 모두 또는 ignoreWhite로 모두 제외) → emptyList () 반환 — `dominantColor()` 가 null 반환

### Laplacian Variance 핵심

- 3x3 커널 sum=0 → DC 컴포넌트 제거됨, edge 강도만 측정
- variance가 dispersion 측도로 동작 — sharp 이미지는 강한 edge 다수 → high variance
- 1픽셀 boundary 제외 (커널이 image bound 밖으로 나감 방지)
- 단일 패스: `sum`, `sumSq`, `n` 누적 → `variance = sumSq/n - (sum/n)^2`

### EXIF 변환 edge case

- `ExifSubIFDDirectory.getDate(TAG_DATETIME_ORIGINAL)` deprecated일 수 있음 — `getDate(tag, timeZone)` 사용
- `Rational` 타입 (shutterSpeed) → `numerator/denominator` 형식 직접 포매팅 ("1/250")
- `getDouble(tag)` 호출 시 tag 없으면 `MetadataException` → try/catch 필수
- GPS는 `GpsDirectory.geoLocation: GeoLocation?` (decimal degree) 권장 — DMS 변환 자동
- `flashFired` — `getInt(TAG_FLASH) and 0x01 != 0`

### Coroutines Dispatcher 결정 (spec §5 Risk 6)

- CPU-bound: `suspendDominantColors`, `suspendBlurScore` → `Dispatchers.Default`
- I/O 포함: `suspendReadExif (File/Path)` → `Dispatchers.IO`
- 직관에 어긋나지만 **CPU-bound 작업에 IO Dispatcher는 부적절** (스레드 풀 크기/특성 다름)

### 입력 검증 (Risk 7)

- `readExif(ByteArray)`: 50MB 상한 (`require`)
- 모든 metadata-extractor 예외 → catch → EMPTY (사용자에게 throw 안 함)
- `dominantColors`: `count >= 1`
- `blurScore`: `width >= 3 && height >= 3`

### bluetape4k 패턴 준수

- `companion object: KLogging()` (또는 KLoggingChannel for tests)
- `Serializable` — data class 직렬화 가능 (`serialVersionUID = 1L`)는 model이 아니므로 선택. `ExifData`는 직렬화 가능하면 좋음
- bluetape4k-assertions matcher: `shouldBeEqualTo`, `shouldBeGreaterThan`, `shouldBeInRange` — `.shouldBeTrue()` 비교 보다 우선
- Test base: `AbstractImageTest`(이미 모듈에 존재 추정)

### Library Source 추출 위치

- 필요시 `.claude/lib-sources/metadata-extractor/` 사용 — `/tmp` 또는 프로젝트 src 금지

### 알려진 라이브러리 사양

- `metadata-extractor 2.19.0` — Apache 2.0, ~800KB
- `ImageMetadataReader.readMetadata(File|InputStream)` 진입점
- 주요 directory class: `ExifIFD0Directory`, `ExifSubIFDDirectory`, `GpsDirectory`, `JpegDirectory`
- Tag 상수: `ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL`, `TAG_ISO_EQUIVALENT`, `TAG_APERTURE`, `TAG_FOCAL_LENGTH`, etc.

## 검증 체크리스트 (PR 직전)

- [ ] T01~T20 전체 완료
- [ ] `./gradlew :bluetape4k-images:test` PASS (테스트 수/duration 보고)
- [ ] `./gradlew :bluetape4k-images:detekt` 0 issue
- [ ] `oh-my-claudecode:code-reviewer` HIGH/CRITICAL 0건
- [ ] README.md / README.ko.md 동기화
- [ ] KDoc 100% public API 커버리지
- [ ] Follow-up issue 3건 URL 확보
- [ ] worktree (`.worktrees/feat/issue-133-images-analysis/`) 내부 작업 확인

## 참고

- **Spec**: `docs/superpowers/specs/2026-04-27-images-analysis-design.md`
- **color-thief-java**: https://github.com/SvenWoltmann/color-thief-java (MIT)
- **metadata-extractor**: https://github.com/drewnoakes/metadata-extractor (Apache 2.0)
- **Pech-Pacheco Laplacian**: "Diatom autofocusing in brightfield microscopy" (2000)
- **기존 패턴**: `utils/images/src/main/kotlin/io/bluetape4k/images/similarity/HistogramSimilarity.kt`
