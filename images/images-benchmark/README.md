[한국어](./README.ko.md) | English

# Module bluetape4k-images-benchmark

JMH benchmarks comparing [scrimage](https://sksamuel.github.io/scrimage/) and [libvips](https://www.libvips.org/) image processing performance.

## Architecture

```mermaid
flowchart TD
    subgraph Benchmarks["JMH Benchmark Classes"]
        RESIZE["ImageResizeBenchmark\n(scrimage vs vips resize)"]
        ENCODE["ImageEncodeBenchmark\n(scrimage vs vips JPEG/PNG)"]
        FILTER["ImageFilterBenchmark\n(scrimage filter performance)"]
    end

    subgraph State["JMH State Objects"]
        VSTATE["VipsBenchmarkState\n(runtime init + sample images\nmacOS library path auto-config)"]
        IMGSETS["BenchmarkImageSets\n(synthetic photo4k / document / thumbnail)"]
    end

    subgraph Impls["Implementations"]
        SCRIMAGE["bluetape4k-images\n(Scrimage / Java 2D)"]
        JAVA21["bluetape4k-images-vips-java21\n(JVips / JNI — Linux only)"]
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

## Benchmark Results

> Environment: macOS Apple Silicon, GraalVM JDK 25.0.3, vips-ffm 1.9.6 (libvips 8.18.2), AverageTime ms/op

### Resize (4K 3840×2160 → target)

```mermaid
xychart-beta horizontal
    title "Resize: scrimage vs vips (ms/op, lower is better)"
    x-axis ["scrimage 1920×1080", "vips 1920×1080", "scrimage 1280×720", "vips 1280×720"]
    y-axis "ms/op" 0 --> 80
    bar [71.16, 0.20, 47.85, 0.21]
```

| Target | scrimage (ms/op) | vips (ms/op) | Speedup |
|--------|-----------------|--------------|---------|
| 1920×1080 | 71.16 ± 2.02 | 0.202 ± 0.006 | **352×** |
| 1280×720  | 47.85 ± 1.72 | 0.207 ± 0.011 | **231×** |

### Encode (1240×1754 document image)

```mermaid
xychart-beta horizontal
    title "Encode: scrimage vs vips (ms/op, lower is better)"
    x-axis ["scrimage JPEG", "vips JPEG", "scrimage PNG", "vips PNG"]
    y-axis "ms/op" 0 --> 110
    bar [52.49, 15.67, 94.87, 49.88]
```

| Format | scrimage (ms/op) | vips (ms/op) | Speedup |
|--------|-----------------|--------------|---------|
| JPEG   | 52.49 ± 0.44    | 15.67 ± 0.27  | **3.3×** |
| PNG    | 94.87 ± 4.65    | 49.88 ± 1.02  | **1.9×** |

### Filter (scrimage only, 1240×1754)

```mermaid
xychart-beta horizontal
    title "scrimage Filters (ms/op, lower is better)"
    x-axis ["Sepia", "Grayscale", "Blur"]
    y-axis "ms/op" 0 --> 35
    bar [13.19, 22.51, 29.80]
```

| Filter    | scrimage (ms/op) |
|-----------|-----------------|
| Sepia     | 13.19 ± 0.49    |
| Grayscale | 22.51 ± 9.19    |
| Blur      | 29.80 ± 1.23    |

Full raw data: [`docs/benchmark-results-2026-04-29.md`](docs/benchmark-results-2026-04-29.md)

---

## Running Benchmarks

```bash
# Java 25 — scrimage + vips-ffm (Panama FFM, macOS/Linux)
./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java25

# Java 21 — scrimage + JVips JNI (Linux only)
./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java21
```

**macOS prerequisites**: `brew install vips`

`VipsBenchmarkState` auto-detects macOS and sets Homebrew library paths
(`vipsffm.libpath.*.override`) so libvips is found even with SIP stripping `DYLD_LIBRARY_PATH`.

---

## Benchmark Classes

### `ImageResizeBenchmark`

Resizes a synthetic 4K (3840×2160) photo to multiple target resolutions.

| Parameter    | Values |
|--------------|--------|
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

Encodes a synthetic 1240×1754 document image to JPEG and PNG.

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

Applies scrimage filters to a 1240×1754 document image.

| Benchmark          | Filter          |
|--------------------|-----------------|
| `scrimage_blur`    | `BlurFilter`    |
| `scrimage_grayscale` | `GrayscaleFilter` |
| `scrimage_sepia`   | `SepiaFilter`   |

### `VipsBenchmarkState`

JMH `@State(Scope.Thread)` — initializes the vips runtime once per trial via reflection
(supports both `FfmVipsRuntime` Java 25 and `JVipsRuntime` Java 21).

```kotlin
@Setup(Level.Trial)
fun setup() {
    photo4kJpegBytes = BenchmarkImageSets.photo4k.bytes(JpegWriter(80, false))
    applyMacOsVipsLibraryPaths()   // sets vipsffm.libpath.*.override on macOS
    vipsAvailable = tryInitVipsRuntime()
}
```
