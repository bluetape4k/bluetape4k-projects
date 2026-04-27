# utils/images 포맷 지원 확장 구현 Plan (TIFF / SVG / AVIF·HEIC)

- **Spec**: [`docs/superpowers/specs/2026-04-27-images-format-extension-design.md`](../specs/2026-04-27-images-format-extension-design.md)
- **이슈**: #134 — utils/images 포맷 지원 확장
- **연관 이슈**: #136 — utils/images-vips (libvips JNI 기반 AVIF/HEIC 구현)
- **브랜치**: `feat/issue-134-images-format` (`.worktrees/feat/issue-134-images-format`)
- **모듈**: `bluetape4k-images` (`utils/images`)
- **작성일**: 2026-04-27

---

## 진행 원칙

- **Plan Task는 모두 필수** — 선택 없이 전수 완료 후 PR 생성, 완료 후 Plan 대비 비교 표 보고.
- **TDD**: 각 task는 RED → GREEN → REFACTOR 순서. 인터페이스/데이터 클래스만 정의되는 task도 컴파일 가드 + reflection 검증 테스트 동반.
- **테스트 작성 + 실행 검증** 필수: 작성·수정 즉시 `./gradlew :bluetape4k-images:test --tests "<class>"` 실행, pass/skip/fail 결과 보고에 포함.
- **Kluent 비교 matcher** 사용 (`shouldBeGreaterThan`, `shouldBeLessOrEqualTo`, `shouldBeInRange`). `(x > y).shouldBeTrue()` 금지.
- **단계별 commit 분리** (Korean + prefix). 각 task 종료 시 commit, T22 종료 후 push + PR.
- **편집 후 `ide_diagnostics`** 확인, 임포트 오류·`@Deprecated`는 즉시 해소.
- **공개 API KDoc 한국어** + `## 동작/계약` + `## 예시` 섹션 필수, `companion object: KLoggingChannel()` 패턴 유지.
- **Worktree 안에서 작업**: 모든 변경은 `.worktrees/feat/issue-134-images-format/` 내부에서만 발생.

---

## Task 의존 그래프

```
T1 ── T2 ─┬─ T21(픽스처) ──────────────────────────────────────────────────┐
          │                                                                │
          ├─ T3 ─┬─ T4 ── T7(enum) ── T6(Writer) ── T8(MultiPage) ── T16/T17
          │      ├─ T5(MultiPage IF)                                      │
          │      ├─ T9 ── T10 ── T11 ── T18(Batik 변환) ── T19(보안) ─────┤
          │      └─ T12 ── T13 ── T14 ── T20(Incubating) ─────────────────┤
          │                                                                │
          └── T15(parse 테스트) ──────────────────────────────────────────→ T22(README) ── T23(patterns 검증) ── T24(wiki-update)
```

- T1 → T2: Libs.kt 상수 → build.gradle.kts 의존성.
- **T21 (픽스처) 은 T2 직후 즉시 준비** — T16/T17/T18/T19 모두가 사용하는 TIFF/SVG 샘플 파일. 병렬로 진행 가능.
- T3: ImageFormat enum 변경은 가장 먼저 (parser 테스트 전 선행).
- **T7 (TiffCompression enum) 은 T6 앞에** — T6이 TiffCompression을 참조하므로 enum 먼저 정의.
- T4: IIORegistryUtils SPI init은 T6 (TIFF Writer) 정적 초기화에서 호출되므로 선행.
- T9~T11 SVG는 T2 의존성 추가 후 가능 (Batik testImplementation).
- T12~T14 incubating은 ImageFormat AVIF/HEIC enum 도입 (T3) 후 의미 부여.
- T15~T20 테스트는 모두 본문 task 종료 직후 이어 작성 (TDD 원칙).
- **T23 bluepate4k-patterns 체크 + T24 /wiki-update** 는 모든 코드/테스트/README GREEN 후 마지막.

---

### T1. Libs.kt에 TwelveMonkeys + Batik 의존성 상수 추가
- **complexity**: low
- **파일**: `buildSrc/src/main/kotlin/Libs.kt`
- **내용**:
  - 새 `// region TwelveMonkeys ImageIO` 섹션 추가:
    ```kotlin
    const val twelvemonkeys_imageio = "3.12.0"
    val twelvemonkeys_imageio_tiff = "com.twelvemonkeys.imageio:imageio-tiff:$twelvemonkeys_imageio"
    val twelvemonkeys_imageio_metadata = "com.twelvemonkeys.imageio:imageio-metadata:$twelvemonkeys_imageio"
    val twelvemonkeys_imageio_core = "com.twelvemonkeys.imageio:imageio-core:$twelvemonkeys_imageio"
    ```
  - 새 `// region Apache Batik` 섹션 추가:
    ```kotlin
    const val batik = "1.18"
    val batik_transcoder = "org.apache.xmlgraphics:batik-transcoder:$batik"
    val batik_codec = "org.apache.xmlgraphics:batik-codec:$batik"
    ```
  - 기존 `scrimage_*` 섹션 인접에 배치, 알파벳/주제 정렬 유지.
- **검증**:
  - `./gradlew :bluetape4k-images:dependencies --configuration compileClasspath` 실행 시 새 라이브러리 좌표 출력.
  - `ide_diagnostics` 0건.
  - 커밋: `chore: TwelveMonkeys 3.12.0 / Batik 1.18 의존성 상수 추가`.
- **의존**: 없음.

---

### T2. utils/images build.gradle.kts 의존성 업데이트
- **complexity**: low
- **파일**: `utils/images/build.gradle.kts`
- **내용**:
  - 기존 `dependencies {}` 블록에 추가:
    ```kotlin
    // TIFF — TwelveMonkeys ImageIO SPI
    api(Libs.twelvemonkeys_imageio_tiff)
    api(Libs.twelvemonkeys_imageio_metadata)

    // SVG — Apache Batik (사용자 명시 추가 필요, compileOnly로 격리)
    compileOnly(Libs.batik_transcoder)
    compileOnly(Libs.batik_codec)
    testImplementation(Libs.batik_transcoder)
    testImplementation(Libs.batik_codec)
    ```
  - `scrimage_webp` 등 기존 implementation 라인 유지.
  - 의존성 정렬은 spec §4.4 표 순서 따름.
- **검증**:
  - `./gradlew :bluetape4k-images:build -x test` 컴파일 성공.
  - `./gradlew :bluetape4k-images:dependencies` 출력에 `imageio-tiff`, `batik-transcoder` 표시.
  - 커밋: `chore: utils/images에 TwelveMonkeys TIFF / Batik SVG 의존성 추가`.
- **의존**: T1.

---

### T3. ImageFormat enum 확장 + isWritableByImageIO + requireWritable
- **complexity**: medium
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/ImageFormat.kt`
- **내용**:
  - 기존 `GIF/JPG/PNG/WEBP` 뒤에 4개 enum entry 추가:
    ```kotlin
    TIFF("tiff"),
    SVG("svg"),
    AVIF("avif"),
    HEIC("heic"),
    ```
  - `parse(formatName: String)` 함수는 enum.values()를 순회하므로 자동으로 신규 포맷을 인식 — 변경 없음 (단, 변경 없음을 KDoc에 명시).
  - `companion object`에 두 신규 멤버 함수 추가 (spec §4.3.1):
    ```kotlin
    /** SVG, AVIF, HEIC 는 [ImageIO.write] 직접 호출 불가. */
    @JvmStatic
    fun ImageFormat.isWritableByImageIO(): Boolean = this !in setOf(SVG, AVIF, HEIC)

    /** 호출 시 [SVG]/[AVIF]/[HEIC]면 IllegalArgumentException 던짐. */
    @JvmStatic
    fun ImageFormat.requireWritable() {
        require(isWritableByImageIO()) {
            "ImageFormat.$name 은 ImageIO Writer를 지원하지 않습니다. " +
            "SVG → SuspendSvgRasterizer, AVIF/HEIC → bluetape4k-images-vips 모듈을 사용하세요."
        }
    }
    ```
  - 한국어 KDoc + `## 동작/계약` 섹션 추가 (계약: SVG.ioName을 ImageIO에 넘기지 말 것).
- **검증**:
  - 컴파일 성공, T15에서 행위 검증.
  - 커밋: `feat: ImageFormat enum에 TIFF/SVG/AVIF/HEIC 추가 + writable 검사 헬퍼`.
- **의존**: T2.

---

### T4. IIORegistryUtils.registerApplicationClasspathSpis() 추가
- **complexity**: low
- **파일**: `utils/images/src/main/kotlin/io/bluetape4k/images/IIORegistryUtils.kt`
- **내용**:
  - 기존 object에 함수 추가:
    ```kotlin
    /**
     * 클래스패스 상의 ImageIO SPI를 강제로 재등록합니다.
     *
     * ## 동작/계약
     * - TwelveMonkeys 처럼 jar lazy-load 시점에 SPI가 누락되는 회귀를 방지.
     * - 멱등 — 여러 번 호출해도 동일 SPI는 1회만 등록.
     * - 호출 시점은 [SuspendTiffWriter] / `BatikSvgRasterizer` 정적 초기화 등에서.
     *
     * ## 예시
     * ```kotlin
     * IIORegistryUtils.registerApplicationClasspathSpis()
     * ```
     */
    @JvmStatic
    fun registerApplicationClasspathSpis() {
        IIORegistry.getDefaultInstance().registerApplicationClasspathSpis()
    }
    ```
  - import: `javax.imageio.spi.IIORegistry`.
  - companion (이미 object지만) `KLoggingChannel` 인스턴스 보유 — 함수 진입 시 `log.debug { ... }` 1회 emit.
- **검증**:
  - `./gradlew :bluetape4k-images:test --tests "*IIORegistryUtilsTest"` PASS (없으면 T4-T 작성).
  - T4-T: `IIORegistryUtilsTest` — `registerApplicationClasspathSpis()` 호출 후 `ImageIO.getReaderFormatNames()`에 `"tiff"` 포함 검증.
  - 커밋: `feat: IIORegistryUtils.registerApplicationClasspathSpis 추가`.
- **의존**: T2.

---

### T5. SuspendMultiPageImageWriter 인터페이스 + extension function
- **complexity**: medium
- **파일**:
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/coroutines/SuspendMultiPageImageWriter.kt`
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/coroutines/SuspendMultiPageImageWriterExtensions.kt`
- **내용**:
  - 인터페이스 (spec §4.3.2):
    ```kotlin
    interface SuspendMultiPageImageWriter {
        suspend fun suspendWrite(images: List<ImmutableImage>, out: OutputStream)
    }
    ```
  - extension functions 파일:
    ```kotlin
    /** 단일 이미지를 다중 페이지 컨테이너로 직렬화 (편의 오버로드). */
    suspend fun SuspendMultiPageImageWriter.suspendWrite(
        image: ImmutableImage,
        out: OutputStream,
    ) = suspendWrite(listOf(image), out)

    /** [Path]에 임시파일 → atomic move로 부분 파일 방지. */
    suspend fun SuspendMultiPageImageWriter.suspendWrite(
        images: List<ImmutableImage>,
        target: Path,
    ) { /* tmp 파일에 쓰고 ATOMIC_MOVE */ }
    ```
  - 한국어 KDoc + `## 동작/계약`: SuspendImageWriter와 별개 인터페이스, 0 페이지 → IllegalArgumentException, 1 페이지 → 단일 IFD 컨테이너로 정상 직렬화.
- **검증**:
  - 컴파일 성공, T17에서 행위 검증.
  - 커밋: `feat: SuspendMultiPageImageWriter 인터페이스 + extension function 추가`.
- **의존**: T2.

---

### T6. SuspendTiffWriter 구현
- **complexity**: high
- **파일**:
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/coroutines/SuspendTiffWriter.kt`
- **내용**:
  - scrimage `ImageWriter` 직접 구현 + `SuspendImageWriter` 인터페이스도 구현 (spec §4.3.3 채택 설계).
    ```kotlin
    class SuspendTiffWriter(
        val compression: TiffCompression = TiffCompression.DEFLATE,
        val quality: Float = 0.9f,
    ) : ImageWriter, SuspendImageWriter {
        init {
            require(quality in 0.0f..1.0f) { "quality must be in 0.0..1.0: $quality" }
        }
        override fun write(image: AwtImage, metadata: ImageMetadata, out: OutputStream) {
            // ImageIO.getImageWritersByFormatName("TIFF")
            // TIFFImageWriteParam.compressionType = compression.ioName
            // JPEG 시에만 compressionQuality = quality
        }
        override suspend fun suspendWrite(image: ImmutableImage, out: OutputStream) {
            runInterruptible(Dispatchers.IO) {
                write(image.awt(), ImageMetadata.empty, out)
            }
        }

        companion object : KLoggingChannel() {
            init {
                IIORegistryUtils.registerApplicationClasspathSpis()
            }
            @JvmStatic val Default = SuspendTiffWriter()
            @JvmStatic val Lzw = SuspendTiffWriter(TiffCompression.LZW)
            @JvmStatic val Uncompressed = SuspendTiffWriter(TiffCompression.NONE)
            @JvmStatic val JpegCompression = SuspendTiffWriter(TiffCompression.JPEG, quality = 0.9f)
        }
    }
    ```
  - 한국어 KDoc + `## 동작/계약` + `## 예시` (TwelveMonkeys SPI 사용, 압축 모드별 프리셋).
  - `try/finally` 로 `ImageWriter.dispose()` 보장.
  - `runInterruptible` 사용 (코루틴 취소 → Thread.interrupt 전파).
- **검증**:
  - T16 테스트로 검증.
  - 커밋: `feat: SuspendTiffWriter 구현 (TwelveMonkeys SPI + 4개 압축 프리셋)`.
- **의존**: T3, T4, T7 (TiffCompression enum 먼저).

---

### T7. TiffCompression enum
- **complexity**: low
- **파일**: 신규 (T6 전에 생성) → `utils/images/src/main/kotlin/io/bluetape4k/images/coroutines/TiffCompression.kt`
- **내용**:
  ```kotlin
  /**
   * TIFF 압축 모드.
   *
   * ## 동작/계약
   * - [ioName] 은 javax.imageio.plugins.tiff.TIFFImageWriteParam#setCompressionType 에 그대로 전달.
   * - [JPEG] 사용 시 quality 파라미터를 함께 지정해야 함.
   */
  enum class TiffCompression(val ioName: String) {
      NONE("None"),
      LZW("LZW"),
      DEFLATE("Deflate"),
      JPEG("JPEG"),
      PACKBITS("PackBits"),
  }
  ```
  - companion `KLoggingChannel()` 불필요 (값 객체).
- **검증**:
  - `T6` 컴파일 통과 시 함께 검증, T16 라운드트립에서 ioName 적용 확인.
  - 커밋: `feat: TiffCompression enum 추가`.
- **의존**: T4 (없음 — enum은 leaf dependency, T6 전에 작성).

---

### T8. SuspendTiffMultiPageWriter 구현 (maxPages / maxPixelsPerPage 가드 포함)
- **complexity**: high
- **파일**: 신규 — `utils/images/src/main/kotlin/io/bluetape4k/images/coroutines/SuspendTiffMultiPageWriter.kt`
- **내용**:
  ```kotlin
  class SuspendTiffMultiPageWriter(
      val compression: TiffCompression = TiffCompression.DEFLATE,
      val quality: Float = 0.9f,
      val maxPages: Int = 1024,
      val maxPixelsPerPage: Long = 100_000_000L,
  ) : SuspendMultiPageImageWriter {
      override suspend fun suspendWrite(images: List<ImmutableImage>, out: OutputStream) {
          require(images.isNotEmpty()) { "images must not be empty" }
          require(images.size <= maxPages) {
              "TIFF page count (${images.size}) exceeds limit ($maxPages)"
          }
          images.forEachIndexed { idx, img ->
              val pixels = img.width.toLong() * img.height.toLong()
              require(pixels <= maxPixelsPerPage) {
                  "TIFF page[$idx] pixel count ($pixels) exceeds limit ($maxPixelsPerPage)"
              }
          }
          runInterruptible(Dispatchers.IO) {
              val writer = ImageIO.getImageWritersByFormatName("TIFF").next()
              try {
                  ImageIO.createImageOutputStream(out).use { ios ->
                      writer.output = ios
                      val param = (writer.defaultWriteParam as TIFFImageWriteParam).apply {
                          compressionMode = ImageWriteParam.MODE_EXPLICIT
                          compressionType = compression.ioName
                          if (compression == TiffCompression.JPEG) {
                              compressionQuality = quality
                          }
                      }
                      writer.prepareWriteSequence(null)
                      images.forEach { img ->
                          writer.writeToSequence(IIOImage(img.awt(), null, null), param)
                      }
                      writer.endWriteSequence()
                  }
              } finally {
                  writer.dispose()
              }
          }
      }

      companion object : KLoggingChannel() {
          init {
              IIORegistryUtils.registerApplicationClasspathSpis()
          }
          @JvmStatic val Default = SuspendTiffMultiPageWriter()
          @JvmStatic val Lzw = SuspendTiffMultiPageWriter(TiffCompression.LZW)
      }
  }
  ```
  - 한국어 KDoc + `## 동작/계약` (원자성/취소: `runInterruptible`로 cancel propagation, `dispose()` finally 보장, OutputStream 부분 쓰기 책임은 caller).
- **검증**:
  - T17 테스트.
  - 커밋: `feat: SuspendTiffMultiPageWriter 구현 (maxPages/maxPixelsPerPage 가드)`.
- **의존**: T5, T6, T7.

---

### T9. SvgRasterizeOptions data class
- **complexity**: low
- **파일**: 신규 — `utils/images/src/main/kotlin/io/bluetape4k/images/svg/SvgRasterizeOptions.kt`
- **내용** (spec §4.3.4):
  ```kotlin
  data class SvgRasterizeOptions(
      val width: Int? = null,
      val height: Int? = null,
      val dpi: Int = 96,
      val backgroundColor: java.awt.Color? = null,
      val allowExternalResources: Boolean = false,
      val allowedSchemes: Set<String> = setOf("data"),
      val timeoutMillis: Long = 10_000L,
      val maxWidthPx: Int = 8192,
      val maxHeightPx: Int = 8192,
  ) {
      init {
          require(dpi > 0) { "dpi must be positive: $dpi" }
          require(timeoutMillis > 0) { "timeoutMillis must be positive: $timeoutMillis" }
          require(maxWidthPx > 0 && maxHeightPx > 0) {
              "maxWidthPx/maxHeightPx must be positive"
          }
      }

      companion object {
          @JvmStatic val Default = SvgRasterizeOptions()
      }
  }
  ```
  - 한국어 KDoc + `## 동작/계약`: secure default (allowExternalResources=false), data: URI는 임베디드 이미지 허용, 96 DPI 기본.
- **검증**:
  - `SvgRasterizeOptionsTest` (T18 일부): init require 검증 (dpi=0/음수 → IllegalArgumentException).
  - 커밋: `feat: SvgRasterizeOptions data class 추가`.
- **의존**: T2.

---

### T10. SuspendSvgRasterizer 인터페이스
- **complexity**: low
- **파일**: 신규 — `utils/images/src/main/kotlin/io/bluetape4k/images/svg/SuspendSvgRasterizer.kt`
- **내용**:
  ```kotlin
  interface SuspendSvgRasterizer {
      suspend fun suspendRasterize(
          input: InputStream,
          options: SvgRasterizeOptions = SvgRasterizeOptions.Default,
      ): ImmutableImage

      suspend fun suspendRasterize(
          svg: String,
          options: SvgRasterizeOptions = SvgRasterizeOptions.Default,
      ): ImmutableImage = suspendRasterize(svg.byteInputStream(Charsets.UTF_8), options)

      suspend fun suspendRasterize(
          svgBytes: ByteArray,
          options: SvgRasterizeOptions = SvgRasterizeOptions.Default,
      ): ImmutableImage = suspendRasterize(ByteArrayInputStream(svgBytes), options)
  }
  ```
  - 한국어 KDoc + `## 동작/계약`: 외부 URL/DTD 로딩 금지 default, viewBox 기준 사이즈, 출력은 PNG 래스터.
- **검증**:
  - 컴파일 성공, T18에서 행위 검증.
  - 커밋: `feat: SuspendSvgRasterizer 인터페이스 추가`.
- **의존**: T9.

---

### T11. BatikSvgRasterizer 구현 (보안 설정 포함)
- **complexity**: high
- **파일**: 신규 — `utils/images/src/main/kotlin/io/bluetape4k/images/svg/BatikSvgRasterizer.kt`
- **내용**:
  - `class BatikSvgRasterizer : SuspendSvgRasterizer`.
  - `suspendRasterize` 본체:
    1. `withTimeout(options.timeoutMillis)` 로 감싸고 내부에서 `runInterruptible(Dispatchers.IO)`.
    2. `PNGTranscoder` 인스턴스 생성.
    3. `TranscodingHints` 적용:
       - `SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES = options.allowExternalResources`
       - `XMLAbstractTranscoder.KEY_XML_PARSER_VALIDATING = false`
       - `KEY_WIDTH` / `KEY_HEIGHT` (options.width/height 지정 시)
       - `KEY_BACKGROUND_COLOR` (옵션 지정 시)
       - `KEY_PIXEL_UNIT_TO_MILLIMETER = 25.4f / options.dpi`
       - `KEY_MAX_WIDTH = options.maxWidthPx`, `KEY_MAX_HEIGHT = options.maxHeightPx`
    4. `SAXSVGDocumentFactory` 또는 `SecurityManagerUserAgent` 를 통해 SAX 4종 보안 옵션 강제 (spec §2.2):
       - `disallow-doctype-decl = true`
       - `external-general-entities = false`
       - `external-parameter-entities = false`
       - `nonvalidating/load-external-dtd = false`
    5. `allowExternalResources=false` 시 `SecurityManagerUserAgent : UserAgentAdapter` 인스턴스를 PNGTranscoder에 주입 → `loadExternalDocument()`에서 `SecurityException` throw, `data:` 스킴은 `allowedSchemes`로 통과.
    6. `TranscoderInput(input)` → `ByteArrayOutputStream` → `TranscoderOutput`. 결과 byte array를 `ImmutableImage.loader().fromBytes(bytes)` 로 디코드.
    7. `try/finally`로 transcoder 정리 (필요한 경우 ByteArrayOutputStream close).
  - `companion object: KLoggingChannel()` + `@JvmStatic val Default`.
  - 한국어 KDoc + `## 동작/계약`: secure default, timeout, 외부 리소스 차단 동작 명시.
  - 내부 `class SecurityManagerUserAgent(private val allowedSchemes: Set<String>) : UserAgentAdapter()` 정의 (top-level private 또는 BatikSvgRasterizer 내부 nested class).
- **검증**:
  - T18 변환 테스트, T19 보안 테스트.
  - 커밋: `feat: BatikSvgRasterizer 구현 (XXE/SSRF 보안 + timeout)`.
- **의존**: T9, T10.

---

### T12. IncubatingImageApi annotation
- **complexity**: low
- **파일**: 신규 — `utils/images/src/main/kotlin/io/bluetape4k/images/incubating/IncubatingImageApi.kt`
- **내용** (spec §4.3.5):
  ```kotlin
  @RequiresOptIn(
      level = RequiresOptIn.Level.WARNING,
      message = "이 API는 incubating 상태입니다. " +
          "1.7.x 시리즈 동안 시그니처가 변경될 수 있으며 바이너리 호환성이 보장되지 않습니다.",
  )
  @MustBeDocumented
  @Retention(AnnotationRetention.BINARY)
  @Target(
      AnnotationTarget.CLASS,
      AnnotationTarget.FUNCTION,
      AnnotationTarget.PROPERTY,
  )
  annotation class IncubatingImageApi
  ```
  - 한국어 KDoc + `## 동작/계약`: WARNING 레벨, opt-in 필수, 1.7.x 시리즈 동안 시그니처 변경 허용.
- **검증**:
  - 컴파일 성공, T20에서 reflection 검증.
  - 커밋: `feat: IncubatingImageApi 마커 어노테이션 추가`.
- **의존**: T2.

---

### T13. AvifEncodeOptions + AvifWriter 인터페이스
- **complexity**: low
- **파일**:
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/incubating/AvifEncodeOptions.kt`
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/incubating/AvifWriter.kt`
- **내용**:
  ```kotlin
  data class AvifEncodeOptions(
      val quality: Float = 0.85f,
      val lossless: Boolean = false,
  ) {
      init {
          require(quality in 0.0f..1.0f) { "quality must be in 0.0..1.0: $quality" }
      }

      companion object { @JvmStatic val Default = AvifEncodeOptions() }
  }

  @IncubatingImageApi
  interface AvifWriter {
      suspend fun suspendWrite(
          image: ImmutableImage,
          out: OutputStream,
          options: AvifEncodeOptions = AvifEncodeOptions.Default,
      )
  }
  ```
  - 한국어 KDoc + `## 동작/계약`: 본 모듈에 구현체 없음, `bluetape4k-images-vips` (Issue #136) 사용 안내, NoClassDefFoundError fail-fast.
- **검증**:
  - 컴파일 성공, T20에서 어노테이션 검증.
  - 커밋: `feat: AvifWriter incubating 인터페이스 추가`.
- **의존**: T12.

---

### T14. HeicReadOptions + HeicReader 인터페이스
- **complexity**: low
- **파일**:
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/incubating/HeicReadOptions.kt`
  - 신규: `utils/images/src/main/kotlin/io/bluetape4k/images/incubating/HeicReader.kt`
- **내용**:
  ```kotlin
  data class HeicReadOptions(
      val pageIndex: Int = 0,
      val applyOrientation: Boolean = true,
  ) {
      init {
          require(pageIndex >= 0) { "pageIndex must be non-negative: $pageIndex" }
      }

      companion object { @JvmStatic val Default = HeicReadOptions() }
  }

  @IncubatingImageApi
  interface HeicReader {
      suspend fun suspendRead(
          input: InputStream,
          options: HeicReadOptions = HeicReadOptions.Default,
      ): ImmutableImage
  }
  ```
  - 한국어 KDoc + `## 동작/계약` (HEIC 다중 이미지 컨테이너, pageIndex 지정, EXIF orientation 적용).
- **검증**:
  - 컴파일 성공, T20에서 검증.
  - 커밋: `feat: HeicReader incubating 인터페이스 추가`.
- **의존**: T12.

---

### T15. ImageFormatTest 업데이트 (4 신규 포맷 parse 검증)
- **complexity**: low
- **파일**: `utils/images/src/test/kotlin/io/bluetape4k/images/ImageFormatTest.kt` (기존)
- **내용**:
  - 신규 케이스 (parametrized 또는 개별):
    - `parse("tiff")?.shouldBeEqualTo(ImageFormat.TIFF)`
    - `parse("TIFF")?.shouldBeEqualTo(ImageFormat.TIFF)` (대소문자 무시)
    - `parse("svg") == ImageFormat.SVG`
    - `parse("avif") == ImageFormat.AVIF`
    - `parse("heic") == ImageFormat.HEIC`
    - `ImageFormat.SVG.isWritableByImageIO().shouldBeFalse()`
    - `ImageFormat.AVIF.isWritableByImageIO().shouldBeFalse()`
    - `ImageFormat.HEIC.isWritableByImageIO().shouldBeFalse()`
    - `ImageFormat.TIFF.isWritableByImageIO().shouldBeTrue()`
    - `assertFailsWith<IllegalArgumentException> { ImageFormat.SVG.requireWritable() }` — 메시지에 모듈 안내 포함.
  - 기존 GIF/JPG/PNG/WEBP 테스트 그대로 유지.
- **검증**:
  - `./gradlew :bluetape4k-images:test --tests "*ImageFormatTest"` PASS.
  - 커밋: `test: ImageFormat 신규 4개 포맷 + writable 검사 테스트`.
- **의존**: T3.

---

### T16. SuspendTiffWriterTest (4 압축 모드 라운드트립)
- **complexity**: medium
- **파일**: 신규 — `utils/images/src/test/kotlin/io/bluetape4k/images/coroutines/SuspendTiffWriterTest.kt`
- **내용**:
  - `class SuspendTiffWriterTest : AbstractImageTest()`.
  - `companion object: KLoggingChannel()`.
  - 픽스처: `src/test/resources/images/sample.png` 로딩 (기존 AbstractImageTest 헬퍼 사용).
  - 4개 압축 모드 (`Default`/Deflate, `Lzw`, `Uncompressed`, `JpegCompression`) parametrized 테스트:
    - given: `SuspendTiffWriter` 인스턴스 + 입력 PNG.
    - when: `runTest { writer.suspendWrite(image, baos) }`.
    - then:
      - `baos.size().shouldBeGreaterThan(0)`.
      - 출력 첫 4 bytes가 TIFF magic (`0x49 0x49 0x2A 0x00` 또는 `0x4D 0x4D 0x00 0x2A`).
      - 라운드트립: `ImageIO.read(ByteArrayInputStream(baos.toByteArray()))` 가 null 아님, width/height가 원본과 동일.
  - 추가 케이스: JPEG 압축 시 `quality=0.5f`로 조정 → 파일 크기가 `Uncompressed`보다 작음 (`shouldBeLessThan`).
  - SPI 회귀 테스트: `@BeforeAll`에서 `IIORegistryUtils.registerApplicationClasspathSpis()` 호출, `ImageIO.getReaderFormatNames().toList().shouldContain("tiff")`.
- **검증**:
  - `./gradlew :bluetape4k-images:test --tests "*SuspendTiffWriterTest"` PASS.
  - 커밋: `test: SuspendTiffWriter 4개 압축 모드 라운드트립 테스트`.
- **의존**: T6, T7, T21.

---

### T17. SuspendTiffMultiPageWriterTest (3페이지 IFD + 가드 예외)
- **complexity**: medium
- **파일**: 신규 — `utils/images/src/test/kotlin/io/bluetape4k/images/coroutines/SuspendTiffMultiPageWriterTest.kt`
- **내용**:
  - 3페이지 라운드트립:
    - given: `listOf(img1, img2, img3)` (기존 PNG 픽스처 3장).
    - when: `runTest { writer.suspendWrite(images, baos) }`.
    - then:
      - `ImageIO.createImageInputStream(...)` 으로 reader 생성.
      - `reader.getNumImages(true).shouldBeEqualTo(3)`.
      - 각 페이지 `reader.read(idx)` 가 width/height 매칭.
  - 가드 케이스:
    - 빈 리스트 → `assertFailsWith<IllegalArgumentException>`.
    - `maxPages=2` 로 설정 후 3페이지 입력 → `IllegalArgumentException` (메시지에 "exceeds limit" 포함).
    - `maxPixelsPerPage=100L` (작게) + 큰 이미지 → `IllegalArgumentException` (메시지에 "page[idx]" 포함).
  - 단일 페이지 호환:
    - `writer.suspendWrite(img1, baos)` (extension function) → IFD 1개로 정상 직렬화.
  - `@BeforeAll`에서 `IIORegistryUtils.registerApplicationClasspathSpis()` 호출 강제 (SPI 등록 회귀 검증).
- **검증**:
  - `./gradlew :bluetape4k-images:test --tests "*SuspendTiffMultiPageWriterTest"` PASS.
  - 커밋: `test: SuspendTiffMultiPageWriter 다중 IFD + 페이지/픽셀 가드 테스트`.
- **의존**: T8, T21.

---

### T18. BatikSvgRasterizerTest (단순 SVG 변환, 크기 옵션)
- **complexity**: medium
- **파일**: 신규 — `utils/images/src/test/kotlin/io/bluetape4k/images/svg/BatikSvgRasterizerTest.kt`
- **내용**:
  - 단순 SVG 변환:
    - `val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="100" height="80"><rect width="100" height="80" fill="red"/></svg>"""`.
    - `runTest { rasterizer.suspendRasterize(svg) }` → `ImmutableImage`.
    - `image.width.shouldBeEqualTo(100)`, `image.height.shouldBeEqualTo(80)`.
    - 중심 픽셀이 빨강에 가까움 (`pixel.red().shouldBeGreaterThan(200)`).
  - 크기 옵션:
    - `SvgRasterizeOptions(width=200, height=160)` 적용 → 출력 `image.width == 200`.
  - 입력 형식:
    - String, ByteArray, InputStream 3가지 진입점이 모두 동일 결과.
  - 옵션 init 검증:
    - `SvgRasterizeOptions(dpi = 0)` → `IllegalArgumentException`.
    - `SvgRasterizeOptions(timeoutMillis = -1)` → `IllegalArgumentException`.
- **검증**:
  - `./gradlew :bluetape4k-images:test --tests "*BatikSvgRasterizerTest"` PASS.
  - 커밋: `test: BatikSvgRasterizer 변환 + 크기 옵션 + Options 검증`.
- **의존**: T9, T10, T11, T21.

---

### T19. BatikSvgRasterizerSecurityTest (XXE/외부URL/DTD/billion laughs 4종)
- **complexity**: high
- **파일**: 신규 — `utils/images/src/test/kotlin/io/bluetape4k/images/svg/BatikSvgRasterizerSecurityTest.kt`
- **내용**:
  - **케이스 A — XXE (external general entity)**:
    - SVG에 `<!DOCTYPE svg [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>` 포함, `&xxe;` 참조.
    - `allowExternalResources=false` (default) → 변환 시 `/etc/passwd` 내용 미포함, 또는 `SecurityException`/`IllegalArgumentException` 발생.
    - 검증: 출력 PNG 디코드 후 텍스트 픽셀 분석 또는 예외 매처. **mock URL handler hit count 0 검증**.
  - **케이스 B — 외부 URL 참조 (SSRF)**:
    - `<image xlink:href="http://attacker.example.com/leak"/>` 포함 SVG.
    - `URL.setURLStreamHandlerFactory(mockHandler)` 로 hit count 카운팅.
    - `allowExternalResources=false` → `mockHandler.openConnectionCount.shouldBeEqualTo(0)`.
  - **케이스 C — DTD 로딩 (load-external-dtd)**:
    - SVG `<!DOCTYPE svg PUBLIC ... SYSTEM "http://attacker/dtd">`.
    - `allowExternalResources=false` → 외부 fetch 시도 없음.
  - **케이스 D — billion laughs (XML bomb)**:
    - 중첩 entity expansion 100k+ → 적절한 시간 안에 timeout 또는 정상 거부 (timeoutMillis=2000 적용).
    - `assertFailsWith<TimeoutCancellationException>` 또는 `SecurityException`/`IllegalArgumentException`.
  - 모든 케이스에서 `BatikSvgRasterizer` 의 secure default가 동작함을 검증.
  - mock URL handler 등록 위치는 `@BeforeAll` / `@AfterAll`로 정리 (다른 테스트 격리).
- **검증**:
  - `./gradlew :bluetape4k-images:test --tests "*BatikSvgRasterizerSecurityTest"` PASS, 4 케이스 모두 GREEN.
  - 커밋: `test: BatikSvgRasterizer 보안 4종 (XXE/SSRF/DTD/billion laughs) 검증`.
- **의존**: T11.

---

### T20. IncubatingApiTest (reflection으로 어노테이션 검증)
- **complexity**: low
- **파일**: 신규 — `utils/images/src/test/kotlin/io/bluetape4k/images/incubating/IncubatingApiTest.kt`
- **내용**:
  - `AvifWriter::class.annotations.any { it is IncubatingImageApi }.shouldBeTrue()`.
  - `HeicReader::class.annotations.any { it is IncubatingImageApi }.shouldBeTrue()`.
  - `IncubatingImageApi::class.annotations.filterIsInstance<RequiresOptIn>().shouldNotBeEmpty()`.
  - `RequiresOptIn` level == `Level.WARNING`.
  - `IncubatingImageApi::class.annotations.any { it is MustBeDocumented }.shouldBeTrue()`.
  - `AvifEncodeOptions(quality=1.5f)` → IllegalArgumentException, `HeicReadOptions(pageIndex=-1)` → IllegalArgumentException.
- **검증**:
  - `./gradlew :bluetape4k-images:test --tests "*IncubatingApiTest"` PASS.
  - 커밋: `test: IncubatingImageApi reflection + Options 검증`.
- **의존**: T12, T13, T14.

---

### T21. 테스트용 리소스 파일 준비 (TIFF 샘플, SVG 샘플)
- **complexity**: low
- **파일**:
  - 신규: `utils/images/src/test/resources/images/sample.tiff` (단일 페이지 TIFF, ~1024×768 또는 작은 fixture).
  - 신규: `utils/images/src/test/resources/images/sample-multipage.tiff` (3페이지, 옵션 — T17이 직접 생성하므로 미포함 가능).
  - 신규: `utils/images/src/test/resources/images/sample.svg` (간단 SVG: `<svg ... rect/>`).
  - 신규: `utils/images/src/test/resources/images/svg/with-external-image.svg` (보안 테스트 픽스처, `xlink:href="http://..."` 포함).
  - 신규: `utils/images/src/test/resources/images/svg/with-xxe.svg` (XXE entity 포함).
  - 신규: `utils/images/src/test/resources/images/svg/with-external-dtd.svg` (DOCTYPE SYSTEM).
  - 신규: `utils/images/src/test/resources/images/svg/billion-laughs.svg` (entity expansion bomb).
- **내용**:
  - `sample.tiff`: 기존 `sample.png` 를 ImageIO TIFF writer로 한 번 변환해 커밋 (검증 픽스처). 크기 ≤ 200KB 권장.
  - SVG 픽스처: 직접 손으로 작성 (모두 < 5KB).
  - 픽스처 모두 텍스트/소형 바이너리 → git에 직접 커밋.
- **검증**:
  - `git status` 에 신규 파일 표시.
  - 테스트가 `AbstractImageTest`/`javaClass.classLoader.getResourceAsStream(...)` 으로 로딩 성공.
  - 커밋: `test: TIFF/SVG 테스트 픽스처 추가`.
- **의존**: T2 (모듈 컴파일 가능 상태).

---

### T22. README.md + README.ko.md 업데이트
- **complexity**: low
- **파일**:
  - `utils/images/README.md`
  - `utils/images/README.ko.md`
- **내용**:
  - 지원 포맷 표 갱신: TIFF, SVG, AVIF (incubating), HEIC (incubating) 행 추가.
    - 컬럼: 포맷 / Reader / Writer / 비고.
    - TIFF: TwelveMonkeys SPI / `SuspendTiffWriter` + `SuspendTiffMultiPageWriter`.
    - SVG: `SuspendSvgRasterizer` / 출력 없음 (래스터화 후 다른 포맷) / **Batik 의존성 필요**.
    - AVIF/HEIC: 인터페이스만 / 구현체는 `bluetape4k-images-vips` (#136).
  - SVG 사용 시 사용자 build.gradle에 추가할 의존성 코드 블록 안내:
    ```kotlin
    implementation("org.apache.xmlgraphics:batik-transcoder:1.18")
    implementation("org.apache.xmlgraphics:batik-codec:1.18")
    ```
  - AVIF/HEIC: `bluetape4k-images-vips` 모듈 (#136) 미리 안내. 본 모듈에 구현체 없음, `NoClassDefFoundError` fail-fast 명시.
  - Mermaid 클래스 다이어그램 갱신: `SuspendMultiPageImageWriter`, `SuspendSvgRasterizer`, `BatikSvgRasterizer`, `AvifWriter`, `HeicReader`, `IncubatingImageApi` 반영.
  - `## 보안` 섹션 추가 (영문/한국어 모두): SVG secure default (외부 리소스/DTD/SAX 4종 차단), `allowExternalResources=true` 사용 시 위험성.
  - `## TIFF 다중 페이지 예시` 코드 스니펫.
  - 한국어/영문 동기 — 표 행 수, 다이어그램 노드 수 일치.
- **검증**:
  - `bat utils/images/README.md` 미리보기, 표 형식 OK.
  - 두 파일 동기성 (행 수 / 다이어그램 동일) `diff -y` 수동 확인.
  - 커밋: `docs: utils/images README 신규 포맷 + Batik 의존성 안내`.
- **의존**: T1~T21 모두.

---

## 완료 후 보고 양식 (Plan 대비 비교 표)

| Task | 상태 | 변경 파일 | 테스트 | 비고 |
|------|------|-----------|--------|------|
| T1   | ⏳   | ...       | —      |      |
| T2   | ⏳   | ...       | build  |      |
| ...  | ...  | ...       | ...    |      |
| T22  | ⏳   | README*2  | —      |      |

각 task 완료 시 `⏳` → `✅` (또는 `⚠️` + 사유). PR 본문에도 동일 표 첨부.

---

## DoD 매핑 (Spec §5)

| Spec DoD 항목 | 대응 Task |
|---------------|-----------|
| ImageFormat 신규 4개 | T3 + T15 |
| SuspendMultiPageImageWriter | T5 |
| SuspendTiffWriter + 4 프리셋 | T6 + T7 + T16 |
| SuspendTiffMultiPageWriter + 빈 리스트 가드 | T8 + T17 |
| SuspendSvgRasterizer + Options | T9 + T10 |
| BatikSvgRasterizer + secure default | T11 + T18 + T19 |
| IncubatingImageApi annotation | T12 + T20 |
| AvifWriter / HeicReader 인터페이스 | T13 + T14 + T20 |
| IIORegistryUtils SPI init | T4 + T16 (회귀 가드) |
| ImageFormatTest 신규 4개 parse | T15 |
| SuspendTiffWriterTest 4 압축 라운드트립 | T16 |
| SuspendTiffMultiPageWriterTest 다중 IFD | T17 |
| BatikSvgRasterizerTest 변환 + 크기 | T18 |
| BatikSvgRasterizerSecurityTest 4종 | T19 |
| IncubatingApiTest reflection | T20 |
| 테스트 픽스처 (TIFF/SVG) | T21 |
| README 양 언어 갱신 | T22 |
| bluetape4k-patterns 체크 | T23 |
| /wiki-update | T24 |
| detekt / IDEA format / no ktlint | T1~T22 각 commit 직전 |
| code-reviewer 1회 통과 | T22 직후, PR 생성 직전 |

---

## PR 생성 전 체크 (CLAUDE.md 규칙)

- [ ] `./gradlew :bluetape4k-images:test` 전수 PASS — 통과 개수/소요시간 PR 본문에 기록.
- [ ] `./gradlew :bluetape4k-images:detekt` PASS.
- [ ] `oh-my-claudecode:code-reviewer` (또는 `pr-review-toolkit:code-reviewer`) 1회 실행 → HIGH/CRITICAL 0건.
- [ ] README.md + README.ko.md 동기 (행 수, Mermaid 노드 수 일치).
- [ ] 모든 신규 public API에 한국어 KDoc + `## 동작/계약` + `## 예시`.
- [ ] Worktree (`.worktrees/feat/issue-134-images-format/`) 안에서 모든 변경 발생.
- [ ] `/wiki-update` 스킬 실행 — Obsidian wiki 인덱스 갱신.
- [ ] commit 메시지 모두 한국어 + prefix.
- [ ] PR 본문에 spec/plan 링크 + DoD 매핑 표 + 테스트 결과 포함.

---

---

### T23. bluetape4k-patterns 체크리스트 적용 검증
- **complexity**: low
- **내용**:
  - `bluetape4k-patterns` 스킬 로드 후 신규 파일 전체 점검:
    - 모든 public class/object에 `companion object : KLoggingChannel()` 부착 확인.
    - `requireNotBlank` / `requireNotNull` 가드 적절 사용 확인.
    - magic literal 없음 (상수화 확인).
    - `@Deprecated` 미해소 없음 (ide_diagnostics 재확인).
  - 위반 발견 시 즉시 수정 + commit.
- **검증**:
  - `./gradlew :bluetape4k-images:compileKotlin :bluetape4k-images:compileTestKotlin` PASS.
  - 커밋 (수정 발생 시): `refactor: bluetape4k-patterns 체크 결과 수정`.
- **의존**: T22.

---

### T24. /wiki-update 실행
- **complexity**: low
- **내용**:
  - `/wiki-update` 스킬 실행 → spec/plan 기반 Obsidian wiki 인덱스 갱신.
  - `docs/superpowers/index/2026-04.md` 에 Evolution Event 항목 추가 (Issue #134 완료).
  - `docs/superpowers/INDEX.md` ✅ 완료 카운트 업데이트.
- **검증**:
  - wiki-update 실행 성공 확인.
  - 커밋: `docs: superpowers index 업데이트 — Issue #134 포맷 확장 완료`.
- **의존**: T23.

---

## 변경 이력

| 일자 | 내용 |
|------|------|
| 2026-04-27 | v1.0 초안 작성 — Spec §3.4 옵션 B 채택, 22개 task 분해 + 의존 그래프 + DoD 매핑 |
