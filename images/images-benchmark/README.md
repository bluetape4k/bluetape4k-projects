[한국어](./README.ko.md) | English

# Module bluetape4k-images-benchmark

JMH benchmarks comparing scrimage and libvips image processing performance.

## Architecture

```mermaid
flowchart TD
    subgraph Benchmarks["JMH Benchmark Classes"]
        RESIZE["ImageResizeBenchmark\n(scrimage vs vips resize)"]
        ENCODE["ImageEncodeBenchmark\n(scrimage vs vips JPEG/PNG)"]
        FILTER["ImageFilterBenchmark\n(scrimage filter performance)"]
    end

    subgraph State["JMH State Objects"]
        VSTATE["VipsBenchmarkState\n(runtime init + sample images)"]
        ISTATE["ImageBenchmarkState\n(scrimage ImmutableImage samples)"]
    end

    subgraph Impls["Implementations"]
        SCRIMAGE["bluetape4k-images\n(Scrimage)"]
        JAVA21["bluetape4k-images-vips-java21\n(JVips / JNI)"]
        JAVA25["bluetape4k-images-vips-java25\n(vips-ffm / FFM)"]
    end

    RESIZE --> VSTATE
    ENCODE --> VSTATE
    FILTER --> ISTATE
    VSTATE --> JAVA21
    VSTATE --> JAVA25
    ISTATE --> SCRIMAGE

    classDef benchStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef stateStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef implStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32

    class RESIZE,ENCODE,FILTER benchStyle
    class VSTATE,ISTATE stateStyle
    class SCRIMAGE,JAVA21,JAVA25 implStyle
```

## Overview

This module provides JMH benchmarks for comparing:

- **Resize**: scrimage `scaleTo` vs vips `resize` at multiple output sizes
- **Encode**: scrimage JPEG/PNG writer vs vips JPEG/PNG encoder
- **Filter**: scrimage `GrayscaleFilter`, `BlurFilter`, `SepiaFilter` pipeline throughput

## Running Benchmarks

```bash
# Run all benchmarks (default JMH settings)
./gradlew :bluetape4k-images-benchmark:benchmark

# Run with vips java21 binding
./gradlew :bluetape4k-images-benchmark:benchmark -Pvips.impl=java21

# Run with vips java25 binding (requires Java 25 + --enable-native-access)
./gradlew :bluetape4k-images-benchmark:benchmark -Pvips.impl=java25

# Run a specific benchmark class
./gradlew :bluetape4k-images-benchmark:benchmark \
    -Pbenchmark.includes=".*ImageResizeBenchmark.*"

# Custom JMH options (forks, warmup, measurement iterations)
./gradlew :bluetape4k-images-benchmark:benchmark \
    -Pbenchmark.forks=2 \
    -Pbenchmark.warmupIterations=3 \
    -Pbenchmark.measurementIterations=5
```

## Benchmark Classes

### `ImageResizeBenchmark`

Measures resize throughput at multiple output dimensions.

| Parameter | Values |
|-----------|--------|
| `width`   | 320, 640, 1280 |
| `height`  | 240, 480, 720  |
| `impl`    | `scrimage`, `vips` |

```kotlin
@Benchmark
fun resizeScrimage(state: ImageBenchmarkState): ImmutableImage =
    state.image.scaleTo(state.width, state.height)

@Benchmark
fun resizeVips(state: VipsBenchmarkState): VipsImage =
    state.image.resize(state.width, state.height)
```

### `ImageEncodeBenchmark`

Measures JPEG and PNG encoding throughput at different quality levels.

| Parameter | Values |
|-----------|--------|
| `format`  | `JPEG`, `PNG` |
| `quality` | 75, 85, 95 |
| `impl`    | `scrimage`, `vips` |

### `ImageFilterBenchmark`

Measures scrimage filter pipeline throughput.

| Benchmark | Filter |
|-----------|--------|
| `grayscale` | `GrayscaleFilter` |
| `blur`      | `BlurFilter` |
| `sepia`     | `SepiaFilter` |
| `chain`     | `GrayscaleFilter + BlurFilter + SepiaFilter` |

### `VipsBenchmarkState`

JMH `@State` that initializes the vips runtime and loads sample images once per fork.

```kotlin
@State(Scope.Benchmark)
class VipsBenchmarkState {
    lateinit var runtime: VipsRuntime
    lateinit var imageBytes: ByteArray

    @Setup(Level.Trial)
    fun setUp() {
        runtime = vipsRuntimeOf()
        runtime.init(concurrency = 4, maxPixels = 150_000_000L)
        imageBytes = loadSampleImageBytes()  // 1920x1080 JPEG
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        runtime.shutdown()
    }
}
```

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-images-benchmark:${version}")
}
```
