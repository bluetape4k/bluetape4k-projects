# Issue #135 사전 조사 — utils/images 배치 이미지 처리

- Issue: #135 `[utils/images] 성능 / 배치 처리 (Flow DSL / 썸네일 파이프라인 / Tile 처리)`
- Branch: `feat/issue-135-images-batch-flow`
- Worktree: `.worktrees/feat/issue-135-images-batch-flow`
- Module: `utils/images` (`bluetape4k-images`)
- Date: 2026-04-28

## 1. 요구사항 요약

Issue #135는 `utils/images`에 다음 세 기능을 추가하는 작업이다.

1. `Flow<File>.processImages { ... }` 형태의 배치 처리 Flow DSL
2. 다중 사이즈 썸네일을 생성하는 `ThumbnailPipeline`
3. 대용량 이미지를 타일 단위로 분할/처리/재조합하는 `TileProcessor`

핵심 제약은 다음과 같다.

- Kotlin Coroutines Flow 기반이어야 한다.
- blocking 이미지 I/O는 `Dispatchers.IO`에서 실행한다.
- scrimage 기반 기존 API를 재사용한다.
- 에러 이미지가 섞여도 정상 이미지는 계속 처리할 수 있어야 한다.
- 타일 분할 후 무변환 병합 결과는 원본과 픽셀 동일해야 한다.

## 2. 공식 문서 조사

### Kotlinx Coroutines Flow

Context7 `/kotlin/kotlinx.coroutines` 조사 결과:

- `flatMapMerge(concurrency)`는 각 입력을 내부 Flow로 변환한 뒤 지정 동시성으로 병합하는 표준 패턴이다.
- `flowOn(context)`는 upstream 실행 컨텍스트를 바꾸는 권장 방식이다.
- blocking file/network I/O는 `Dispatchers.IO` 사용이 권장된다.
- `buffer()`는 생산/소비 단계를 동시에 실행해 파이프라인 처리량을 높일 수 있다.
- `catch`는 upstream 예외 처리용이며 fallback emit에도 사용할 수 있다.

적용 결정:

- `Flow<Path>.processImages(...)`는 기존 `mapParallel(parallelism, Dispatchers.IO)`을 재사용해 동시성을 제한한다.
- `processImages` 내부의 per-image 실패는 전체 Flow 실패가 아니라 `ImageBatchResult.Failure`로 방출할 수 있게 한다.
- 호출자가 기존 Flow 연산자 `.filterIsInstance`, `.catch`, `.buffer`를 조합할 수 있게 결과 타입을 명시한다.

### Scrimage

Context7 `/sksamuel/scrimage` 조사 결과:

- 로딩: `ImmutableImage.loader().fromFile(...)`, `fromPath(...)`, `fromBytes(...)`
- 변환: `scaleTo`, `scaleToWidth`, `scaleToHeight`, `cover`, `filter(...)`
- 필터: `GaussianBlurFilter`, `BlurFilter`, `SharpenFilter` 등
- 저장: `JpegWriter().withCompression(...)`, `PngWriter.Default/MaxCompression`, `image.bytes(writer)`, `image.output(writer, file)`

적용 결정:

- 기존 `immutableImageOf(path)` / `suspendImmutableImageOf(path)`를 재사용한다.
- 기존 `SuspendImageWriter`와 `SuspendJpegWriter`를 출력 포맷 추상화로 재사용한다.
- DSL은 기존 `ImageFilterChain`의 native filter/pixel transform 모델을 직접 활용하되, batch 입력/출력 메타데이터만 새 타입으로 감싼다.

## 3. 기존 코드 조사

### `utils/images`

핵심 파일:

- `utils/images/build.gradle.kts`
    - 이미 `project(":bluetape4k-coroutines")`, `kotlinx_coroutines_core`, `kotlinx_coroutines_test`, `scrimage_core`, `scrimage_filters` 의존성이 있다.
    - 새 의존성 없이 구현 가능하다.
- `utils/images/src/main/kotlin/io/bluetape4k/images/ImmutableImageSupport.kt`
    - `immutableImageOf(File|Path|ByteArray|InputStream)`와 `suspendImmutableImageOf(File|Path)`가 있다.
    - `ImmutableImage.suspendBytes(writer)`와 `suspendWrite(writer, destPath)`가 있다.
- `utils/images/src/main/kotlin/io/bluetape4k/images/coroutines/SuspendImageWriter.kt`
    - `suspendWrite`가 `Dispatchers.IO`에서 blocking writer 호출을 감싼다.
- `utils/images/src/main/kotlin/io/bluetape4k/images/coroutines/SuspendJpegWriter.kt`
    - scrimage `JpegWriter`를 상속하며 `withCompression`, `withProgressive`를 반환 타입 유지 방식으로 제공한다.
- `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChain.kt`
    - `ImageFilterChain`이 native `Filter`와 pixel transform을 누적하고 `PipelineFilter`로 인접 native 필터를 compact 한다.
- `utils/images/src/main/kotlin/io/bluetape4k/images/filters/dsl/ImageFilterChainBlurOps.kt`
    - `gaussianBlur(radius)`가 이미 존재하며 입력 검증을 수행한다.
- `utils/images/src/main/kotlin/io/bluetape4k/images/transforms/dsl/ImageFilterChainTransformOps.kt`
    - `smartCrop`, `autoCrop`, `rotate`, `flip`, `perspectiveTransform`, `clahe`가 기존 filter chain에 통합되어 있다.
- `utils/images/src/main/kotlin/io/bluetape4k/images/transforms/SmartCrop.kt`
    - `smartCropTo(width, height)`와 `suspendSmartCrop(...)`가 존재한다.
- `utils/images/src/main/kotlin/io/bluetape4k/images/splitter/ImageSplitter.kt`
    - 긴 이미지를 높이 기준으로 분할해 `Flow<ByteArray>`로 내보내며 `async`/`buffer` 패턴을 사용한다.

테스트/문서:

- `utils/images/src/test/kotlin/io/bluetape4k/images/AbstractImageTest.kt`
    - 공통 fixture 경로와 `writeToFileAsync(Flow<ByteArray>, ...)` 헬퍼가 있다.
- `utils/images/src/test/kotlin/io/bluetape4k/images/transforms/SmartCropTest.kt`
    - smart crop 동작과 suspend variant를 검증한다.
- `utils/images/src/test/kotlin/io/bluetape4k/images/filters/WatermarkFilterTest.kt`
    - watermark 결과를 fixture와 비교한다.
- `utils/images/README.md`, `README.ko.md`
    - split/compress Flow 사용 예제가 이미 있다.

### `bluetape4k-coroutines`

핵심 파일:

- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/mapParallel.kt`
    - `Flow<T>.mapParallel(parallelism, context, transform)`가 이미 있다.
    - parallelism은 최소 1로 보정하며, 1이면 일반 `map`, 그 외는 `flatMapMerge(concurrency)` 경로를 사용한다.
    - 결과 순서는 보장하지 않는다고 KDoc에 명시되어 있다.
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/AsyncFlow.kt`
    - 순서 보장 async Flow 래퍼가 있다.

적용 결정:

- Issue #135의 기본 배치 처리 결과 순서는 요구사항에 명시되지 않았고 처리량이 목적이므로 `mapParallel` 재사용이 적합하다.
- 순서 보장 옵션이 필요해지면 `ordered = true` 옵션으로 `AsyncFlow` 경로를 추가할 수 있으나, 1차 범위에서는 과설계로 본다.

### 관련 이미지 이슈 worktree

현재 `develop`에는 최근 이미지 관련 결과가 이미 반영되어 있다.

- Issue #131: filter/color DSL
- Issue #132: transforms/smart crop
- Issue #133: analysis/exif/blur/dominant color
- Issue #134: format extension

재사용 가능한 요소는 대부분 현재 worktree의 `utils/images`에 이미 존재한다. sibling worktree는 구조 확인용으로만 보며, 복사는 하지 않는다.

## 4. 기술 제약

- Kotlin 2.3 / Java toolchain 기준.
- `utils/images`는 이미 `bluetape4k-coroutines`에 의존하므로 Flow helper 재사용 가능.
- 새 외부 의존성은 불필요하다.
- scrimage `ImmutableImage`는 in-memory 이미지이므로 tile 처리라도 최종 merge는 메모리 사용량이 원본 이상 필요하다.
- tile 병렬 처리는 CPU-bound transform이면 `Dispatchers.Default`, file I/O/encoding이면 `Dispatchers.IO`가 적합하다. API에는 dispatcher를 주입 가능하게 해야 한다.
- `ImageSplitter`는 높이 방향 분할 전용이므로 gigapixel tile split/merge 계약에는 직접 재사용하기 어렵다.
- `ImageFilterChain.compactAndApply`는 현재 `internal`이라 다른 패키지에서 직접 호출할 수 없다. batch DSL이 같은 `filters.dsl` 패키지에 위치하지 않는다면 public apply 함수 또는 별도 execution adapter가 필요하다.

## 5. 채택/부분 채택/스킵 결정

| 대상                                       | 결정      | 근거                                                                                        |
|--------------------------------------------|-----------|---------------------------------------------------------------------------------------------|
| `suspendImmutableImageOf`                  | 채택      | Path/File 로딩의 기존 코루틴 API이며 byte 기반 비동기 로딩 계약 보유                        |
| `SuspendImageWriter` / `SuspendJpegWriter` | 채택      | 출력 포맷 추상화가 이미 있고 blocking write를 `Dispatchers.IO`로 감쌈                       |
| `ImageFilterChain` + filter/transform DSL  | 채택      | Issue #135 예시의 `gaussianBlur`, `smartCrop`, transform chain을 기존 DSL로 표현 가능       |
| `mapParallel`                              | 채택      | `flatMapMerge(concurrency)` 공식 패턴을 이미 모듈화함                                       |
| `AsyncFlow`                                | 부분 채택 | 결과 순서 보장이 별도 요구되면 사용. 기본 batch 처리에는 처리량 우선으로 `mapParallel` 사용 |
| `ImageSplitter`                            | 부분 채택 | Flow/async 패턴과 테스트 참고용. 2D tile split/merge에는 새 `TileProcessor` 필요            |
| 새 외부 라이브러리                         | 스킵      | 기존 scrimage/coroutines 의존성으로 구현 가능                                               |

## 6. 실패 신호와 재현 조건

예상 실패 신호:

- 깨진 이미지 입력에서 전체 Flow가 중단된다.
- `parallelism <= 0` 입력이 조용히 보정되어 호출자 실수를 숨긴다.
- thumbnail output path 충돌로 여러 source/size 결과가 덮어써진다.
- tile split 후 merge 결과의 경계 픽셀이 어긋난다.
- JPEG 품질 기반 테스트가 픽셀 동일성을 요구해 flaky해진다.

재현/검증 조건:

- 정상 이미지 여러 장 + 깨진 파일 1개를 섞어 `processImages`가 success/failure를 모두 방출하는지 확인한다.
- 100개 synthetic 이미지 또는 fixture 복제 입력으로 동시성 상한을 검증한다.
- tile split/merge는 PNG 또는 in-memory `ImmutableImage` 기준으로 무손실 픽셀 동일성을 확인한다.
- thumbnail은 size별 output file 존재, 크기, failure skip/continue를 확인한다.

## 7. 권장 검증 명령

```bash
./bin/repo-test-summary -- ./gradlew :bluetape4k-images:test --tests "io.bluetape4k.images.batch.*"
./bin/repo-test-summary -- ./gradlew :bluetape4k-images:compileKotlin :bluetape4k-images:compileTestKotlin
./bin/repo-test-summary -- ./gradlew :bluetape4k-images:test
```
