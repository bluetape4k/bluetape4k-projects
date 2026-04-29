한국어 | [English](./README.md)

# Module bluetape4k-images-benchmark

[scrimage](https://sksamuel.github.io/scrimage/)와 [libvips](https://www.libvips.org/) 이미지 처리 성능을 비교하는 JMH 벤치마크 모듈.

## 아키텍처

```mermaid
flowchart TD
    subgraph Benchmarks["JMH 벤치마크 클래스"]
        RESIZE["ImageResizeBenchmark\n(scrimage vs vips 리사이즈)"]
        ENCODE["ImageEncodeBenchmark\n(scrimage vs vips JPEG/PNG)"]
        FILTER["ImageFilterBenchmark\n(scrimage 필터 성능)"]
    end

    subgraph State["JMH 상태 객체"]
        VSTATE["VipsBenchmarkState\n(런타임 초기화 + 샘플 이미지\nmacOS 라이브러리 경로 자동 설정)"]
        IMGSETS["BenchmarkImageSets\n(합성 photo4k / document / thumbnail)"]
    end

    subgraph Impls["구현체"]
        SCRIMAGE["bluetape4k-images\n(Scrimage / Java 2D)"]
        JAVA21["bluetape4k-images-vips-java21\n(JVips / JNI — Linux 전용)"]
        JAVA25["bluetape4k-images-vips-java25\n(vips-ffm / Panama FFM)"]
    end

    RESIZE --> VSTATE
    ENCODE --> VSTATE
    FILTER --> IMGSETS
    VSTATE --> JAVA21
    VSTATE --> JAVA25
    IMGSETS --> SCRIMAGE

    classDef benchStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef stateStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef implStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32

    class RESIZE,ENCODE,FILTER benchStyle
    class VSTATE,IMGSETS stateStyle
    class SCRIMAGE,JAVA21,JAVA25 implStyle
```

## 벤치마크 결과

> 실행 환경: macOS Apple Silicon, GraalVM JDK 25.0.3, vips-ffm 1.9.6 (libvips 8.18.2), AverageTime ms/op

### 리사이즈 (4K 3840×2160 → 타겟)

```mermaid
xychart-beta horizontal
    title "Resize: scrimage vs vips (ms/op, 낮을수록 빠름)"
    x-axis ["scrimage 1920×1080", "vips 1920×1080", "scrimage 1280×720", "vips 1280×720"]
    y-axis "ms/op" 0 --> 80
    bar [71.16, 0.20, 47.85, 0.21]
```

| 타겟 해상도 | scrimage (ms/op) | vips (ms/op) | 속도 향상 |
|------------|-----------------|--------------|----------|
| 1920×1080  | 71.16 ± 2.02    | 0.202 ± 0.006 | **352배** |
| 1280×720   | 47.85 ± 1.72    | 0.207 ± 0.011 | **231배** |

### 인코딩 (1240×1754 document 이미지)

```mermaid
xychart-beta horizontal
    title "Encode: scrimage vs vips (ms/op, 낮을수록 빠름)"
    x-axis ["scrimage JPEG", "vips JPEG", "scrimage PNG", "vips PNG"]
    y-axis "ms/op" 0 --> 110
    bar [52.49, 15.67, 94.87, 49.88]
```

| 포맷 | scrimage (ms/op) | vips (ms/op) | 속도 향상 |
|------|-----------------|--------------|----------|
| JPEG | 52.49 ± 0.44    | 15.67 ± 0.27  | **3.3배** |
| PNG  | 94.87 ± 4.65    | 49.88 ± 1.02  | **1.9배** |

### 필터 (scrimage 전용, 1240×1754)

```mermaid
xychart-beta horizontal
    title "scrimage 필터 (ms/op, 낮을수록 빠름)"
    x-axis ["Sepia", "Grayscale", "Blur"]
    y-axis "ms/op" 0 --> 35
    bar [13.19, 22.51, 29.80]
```

| 필터      | scrimage (ms/op) |
|-----------|-----------------|
| Sepia     | 13.19 ± 0.49    |
| Grayscale | 22.51 ± 9.19    |
| Blur      | 29.80 ± 1.23    |

전체 원본 데이터: [`docs/benchmark-results-2026-04-29.md`](docs/benchmark-results-2026-04-29.md)

---

## 벤치마크 실행

```bash
# Java 25 — scrimage + vips-ffm (Panama FFM, macOS/Linux)
./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java25

# Java 21 — scrimage + JVips JNI (Linux 전용)
./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java21
```

**macOS 사전 요구사항**: `brew install vips`

`VipsBenchmarkState`가 macOS를 자동 감지하고 Homebrew 라이브러리 경로를 등록합니다
(`vipsffm.libpath.*.override`). macOS SIP가 `DYLD_LIBRARY_PATH`를 제거하므로 필수.

---

## 벤치마크 클래스

### `ImageResizeBenchmark`

합성 4K 사진(3840×2160)을 여러 해상도로 리사이즈합니다.

| 파라미터     | 값 |
|-------------|-----|
| `resolution` | `1920x1080`, `1280x720` |

```kotlin
@Benchmark
fun scrimage_scaleTo(bh: Blackhole) {
    val resized = BenchmarkImageSets.photo4k.scaleTo(targetWidth, targetHeight)
    bh.consume(resized)
}

@Benchmark
fun vips_resize(state: VipsBenchmarkState, bh: Blackhole) {
    if (!state.vipsAvailable) { bh.consume(null); return }
    state.createVipsImage(state.photo4kJpegBytes).use { img ->
        bh.consume(img.resize(targetWidth, targetHeight))
    }
}
```

### `ImageEncodeBenchmark`

합성 1240×1754 document 이미지를 JPEG/PNG로 인코딩합니다.

```kotlin
@Benchmark
fun scrimage_encodeJpeg(bh: Blackhole) {
    bh.consume(BenchmarkImageSets.document.bytes(JpegWriter(85, false)))
}

@Benchmark
fun vips_encodeJpeg(state: VipsBenchmarkState, bh: Blackhole) {
    if (!state.vipsAvailable) { bh.consume(null); return }
    state.createVipsImage(state.photo4kJpegBytes).use { img ->
        bh.consume(img.toJpegBytes(85))
    }
}
```

### `ImageFilterBenchmark`

1240×1754 document 이미지에 scrimage 필터를 적용합니다.

| 벤치마크             | 필터              |
|----------------------|-----------------|
| `scrimage_blur`      | `BlurFilter`    |
| `scrimage_grayscale` | `GrayscaleFilter` |
| `scrimage_sepia`     | `SepiaFilter`   |

### `VipsBenchmarkState`

JMH `@State(Scope.Thread)` — 리플렉션으로 vips 런타임을 Trial당 1회 초기화합니다
(`FfmVipsRuntime` Java 25 또는 `JVipsRuntime` Java 21 자동 탐색).

```kotlin
@Setup(Level.Trial)
fun setup() {
    photo4kJpegBytes = BenchmarkImageSets.photo4k.bytes(JpegWriter(80, false))
    applyMacOsVipsLibraryPaths()   // macOS에서 vipsffm.libpath.*.override 설정
    vipsAvailable = tryInitVipsRuntime()
}
```
