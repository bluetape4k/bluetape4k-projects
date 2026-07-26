# utils/images 배치 처리 Flow DSL 구현 계획 — Issue #135

- **Issue**: #135 `[utils/images] 성능 / 배치 처리 (Flow DSL / 썸네일 파이프라인 / Tile 처리)`
- **Spec**: `docs/superpowers/specs/2026-04-28-images-batch-flow-design.md`
- **Research**: `docs/superpowers/research/2026-04-28-issue-135-images-batch-research.md`
- **모듈**: `utils/images` (`bluetape4k-images`)
- **브랜치**: `feat/issue-135-images-batch-flow`
- **작성일**: 2026-04-28
- **상태**: Draft

---

## 1. 구현 원칙

- 새 외부 의존성을 추가하지 않는다.
- public API와 public type에는 한국어 KDoc을 작성한다.
- 기본값과 검증 기준값은 magic number로 직접 쓰지 않는다. `const val` 또는 named helper로 정의한다.
- blocking 이미지 I/O와 writer 호출은 caller가 지정한 `ioDispatcher`에서 실행한다.
- CPU-bound transform은 caller가 지정한 `transformDispatcher`에서 실행한다.
- `CancellationException`은 실패 결과로 감싸지 않고 그대로 전파한다.
- 실패를 skip하는 기본 계약은 result + warn log + callback으로 관측 가능해야 한다.

---

## 2. 파일 계획

### Main

- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/ImageBatchDefaults.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/ImageBatchFailureStage.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/ImageBatchException.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/ImageBatchResult.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/ImageProcessingOptions.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/ImageProcessingDsl.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/ImageBatchFlow.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/ImageDimensionProbe.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/batch/PixelPermitLimiter.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/thumbnail/ThumbnailFormat.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/thumbnail/ThumbnailOutputName.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/thumbnail/ThumbnailPipeline.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/thumbnail/ThumbnailResult.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/thumbnail/ThumbnailSize.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/tiles/ImageTile.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/tiles/TileProcessor.kt`
- `utils/images/src/main/kotlin/io/bluetape4k/images/tiles/TileSize.kt`

### Tests

- `utils/images/src/test/kotlin/io/bluetape4k/images/batch/ImageBatchFlowTest.kt`
- `utils/images/src/test/kotlin/io/bluetape4k/images/batch/ImageProcessingDslTest.kt`
- `utils/images/src/test/kotlin/io/bluetape4k/images/batch/PixelPermitLimiterTest.kt`
- `utils/images/src/test/kotlin/io/bluetape4k/images/thumbnail/ThumbnailPipelineTest.kt`
- `utils/images/src/test/kotlin/io/bluetape4k/images/tiles/TileProcessorTest.kt`

### Docs

- `utils/images/README.md`
- `utils/images/README.ko.md`
- `docs/testlogs/2026-04.md`
- `docs/superpowers/INDEX.md`
- `docs/superpowers/index/2026-04.md`

---

## 3. Task Plan

### T01 — 기본 모델과 상수 추가

**Complexity**: medium

작업:

- `ImageBatchDefaults.kt`에 다음을 정의한다.
    - `const val DEFAULT_MAX_PIXELS = 16_777_216L`
    - `const val DEFAULT_MAX_IN_FLIGHT_PIXELS = DEFAULT_MAX_PIXELS * 2`
    - `const val DEFAULT_MAX_TILE_COUNT = 65_536`
    - `const val JPEG_QUALITY_MIN = 0`
    - `const val JPEG_QUALITY_MAX = 100`
    - `const val PERFORMANCE_SAMPLE_IMAGE_COUNT = 100`
    - tile 테스트용 상수는 test source set에 정의한다.
    - `fun defaultImageBatchParallelism(): Int`
- `ImageBatchFailureStage`, `ImageBatchException`, `ImageBatchResult`, `ImageProcessingOptions`를 추가한다.
- validation은 기존 `requirePositiveNumber`, `requireInRange` 계열을 우선 재사용한다.

검증:

- `:bluetape4k-images:compileKotlin`

### T02 — Batch Flow DSL 구현

**Complexity**: high

작업:

- `Flow<Path>.processImages(options, block)`와 `Flow<File>.processImages(...)`를 구현한다.
- per-image 처리 순서:
    1. decode 전 dimension probe
    2. pixel permit 획득
    3. `ioDispatcher`에서 load
    4. `transformDispatcher`에서 DSL transform 실행
    5. `Image` 또는 `WritableImage` result 방출
    6. `finally`에서 permit 해제
- `skipFailures = true`는 `Failure` result + warn log + `onFailure` callback을 수행한다.
- `skipFailures = false`는 `ImageBatchException`을 전파한다.
- `CancellationException`은 항상 전파한다.

검증:

- 정상 이미지 N개 처리 success count
- 깨진 이미지 skip
- `skipFailures = false` 예외 전파
- dispatcher 주입 테스트
- max pixel guard 테스트

### T03 — ImageProcessingDsl 구현

**Complexity**: high

작업:

- `resize`, `fit`, `gaussianBlur`, `smartCrop`, `filters` bridge를 제공한다.
- `watermark(text, ...)`는 기존 text watermark DSL을 위임한다.
- `watermark(logo, position, alpha)`는 logo image overlay를 구현한다.
- `toJpeg(quality, progressive)`와 `writer(writer)`는 writer 선택만 수행한다.
- writer 중복 지정은 `IllegalArgumentException`으로 거부한다.
- `quality`는 `JPEG_QUALITY_MIN..JPEG_QUALITY_MAX`로 검증한다.

검증:

- DSL 선언 순서 유지
- logo watermark output dimension 유지 및 픽셀 변화 확인
- writer 중복 지정 실패
- quality range 검증

### T04 — Processed image write convenience 구현

**Complexity**: medium

작업:

- `WritableImage.writeTo(path, ioDispatcher)` 구현
- `Image.writeTo(path, writer, ioDispatcher)` 구현
- 기존 `SuspendImageWriter.suspendWrite`의 고정 dispatcher를 우회하고 지정 dispatcher에서 `writer.write(...)`를 직접 호출한다.

검증:

- JPEG/PNG 쓰기 결과 파일 존재 및 bytes > 0
- dispatcher 주입 테스트

### T05 — ThumbnailPipeline 구현

**Complexity**: high

작업:

- `ThumbnailSize`, `ThumbnailCrop`, `ThumbnailFormat`, `ThumbnailOutputName`, `ThumbnailStatus`, `ThumbnailResult`를 추가한다.
- builder에 `sizes`, `format`, `outputDir`, `outputName`, `parallelism`, `ioDispatcher`, `transformDispatcher`, `maxPixels`, `maxInFlightPixels`, `skipFailures`, `onFailure`를 제공한다.
- output path containment와 duplicate output path를 검증한다.
- default output name은 `{sourceHash}_{sourceStem}_{width}x{height}.{extension}`로 생성한다.
- batch DSL과 동일한 pixel permit 계약을 사용한다.

검증:

- 3개 size 생성
- SmartCrop size 검증
- path traversal 거부
- duplicate output path 거부
- failure skip + warn/callback

### T06 — TileProcessor 구현

**Complexity**: high

작업:

- `TileSize`, `ImageTile`, `TileProcessor`를 추가한다.
- split은 마지막 row/column 크기를 원본 경계에 맞춘다.
- process는 `Semaphore(parallelism)`으로 동시 실행 tile 수를 제한한다.
- merge는 duplicate, missing, out-of-bounds, image dimension mismatch를 검증한다.
- `maxPixels`, `maxTileCount`를 상수 기본값으로 사용한다.

검증:

- split geometry
- 무변환 merge pixel identity
- tile transform 반영
- geometry invalid cases
- max guard

### T07 — README와 예제 갱신

**Complexity**: low

작업:

- `utils/images/README.md`, `README.ko.md`에 batch, thumbnail, tile 섹션을 추가한다.
- error handling, ordering, dispatcher, memory guard, timeout/cancellation, output path containment를 명시한다.
- 예제는 writer 지정 후 `writeTo(...)`까지 보여준다.

검증:

- README 링크와 코드 조각 명칭이 실제 API와 일치하는지 `rg`로 확인한다.

### T08 — 100-image non-gating 성능 로그

**Complexity**: low

작업:

- test fixture 복제 또는 synthetic 이미지로 `PERFORMANCE_SAMPLE_IMAGE_COUNT` 입력을 구성한다.
- `parallelism = 1`과 `parallelism = min(4, availableProcessors)`의 처리 시간을 기록한다.
- 결과는 assertion이 아니라 `docs/testlogs/2026-04.md`에 환경/입력/시간으로 남긴다.

검증:

- 로그에 입력 수, 이미지 크기, dispatcher, parallelism, 처리 시간이 포함된다.

### T09 — Superpowers index 갱신

**Complexity**: low

작업:

- `docs/superpowers/INDEX.md`
- `docs/superpowers/index/2026-04.md`

검증:

- spec, plan, research 링크가 모두 상대 경로로 연결된다.

### T10 — 최종 검증

**Complexity**: medium

명령:

```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-images:compileKotlin :bluetape4k-images:compileTestKotlin
./bin/repo-test-summary -- ./gradlew :bluetape4k-images:test
```

검증 기준:

- compile/test 통과
- public API KDoc 누락 없음
- spec DoD 항목과 plan task가 모두 충족됨
- root `develop` worktree의 다른 에이전트 변경은 건드리지 않음

---

## 4. 구현 순서

1. T01 모델/상수
2. T02/T03/T04 batch DSL + write convenience
3. T05 thumbnail
4. T06 tile
5. T07 문서
6. T08/T09 로그와 index
7. T10 검증

---

## 5. 리스크와 대응

| Risk                                      | 대응                                                                    |
|-------------------------------------------|-------------------------------------------------------------------------|
| Dispatcher 옵션이 실제 실행 경로와 어긋남 | `suspendApplyFilters`/`suspendWrite` 대신 지정 dispatcher에서 직접 실행 |
| image watermark가 과도한 API가 됨         | 위치/alpha만 1차 지원, resize는 caller 책임                             |
| pixel permit 누수                         | permit 획득/해제 테스트와 cancellation 테스트 추가                      |
| thumbnail path traversal                  | 파일명 전략 결과를 normalize 후 outputDir containment 검증              |
| 성능 테스트 flaky                         | CI pass/fail 기준이 아닌 testlog 기록으로 제한                          |
