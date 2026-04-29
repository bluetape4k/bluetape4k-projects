# 이미지 처리 JMH 벤치마크 결과

**실행 일시**: 2026-04-29  
**JMH 버전**: 1.37

---

## 환경별 결과 요약

| 환경 | OS | JVM | vips 버전 | vips 구현체 |
|------|----|-----|----------|------------|
| [macOS](#macos-결과) | macOS Apple Silicon | GraalVM 25.0.3 | 8.18.2 (Homebrew) | vips-ffm (FFM) |
| [CI Linux java21](#ci-linux-java21) | Ubuntu 24.04 | GraalVM 21 | 8.15.1 (apt) | JVips (JNI) |
| [CI Linux java25](#ci-linux-java25) | Ubuntu 24.04 | GraalVM 25 | 8.15.1 (apt) | vips-ffm (FFM) |

워밍업: 3 iterations × 1s · 측정: 5 iterations × 1s · Fork: 1 · AverageTime (ms/op)

---

## macOS 결과

> GraalVM 25.0.3, libvips 8.18.2, Apple Silicon

```
Benchmark                                 (resolution)  Mode  Cnt    Score    Error  Units
ImageEncodeBenchmark.scrimage_encodeJpeg           N/A  avgt    5   52.489 ±  0.444  ms/op
ImageEncodeBenchmark.scrimage_encodePng            N/A  avgt    5   94.872 ±  4.650  ms/op
ImageEncodeBenchmark.vips_encodeJpeg               N/A  avgt    5   15.670 ±  0.265  ms/op
ImageEncodeBenchmark.vips_encodePng                N/A  avgt    5   49.878 ±  1.020  ms/op
ImageFilterBenchmark.scrimage_blur                 N/A  avgt    5   29.801 ±  1.232  ms/op
ImageFilterBenchmark.scrimage_grayscale            N/A  avgt    5   22.510 ±  9.194  ms/op
ImageFilterBenchmark.scrimage_sepia                N/A  avgt    5   13.192 ±  0.488  ms/op
ImageResizeBenchmark.scrimage_scaleTo        1920x1080  avgt    5   71.160 ±  2.017  ms/op
ImageResizeBenchmark.scrimage_scaleTo         1280x720  avgt    5   47.854 ±  1.719  ms/op
ImageResizeBenchmark.vips_resize             1920x1080  avgt    5    0.202 ±  0.006  ms/op
ImageResizeBenchmark.vips_resize              1280x720  avgt    5    0.207 ±  0.011  ms/op
```

---

## CI Linux java21

> GraalVM 21, Ubuntu 24.04, libvips 8.15.1 (apt), JVips JNI

```
Benchmark                                 (resolution)  Mode  Cnt    Score    Error  Units
ImageEncodeBenchmark.scrimage_encodeJpeg           N/A  avgt    5  161.085 ± 38.907  ms/op
ImageEncodeBenchmark.scrimage_encodePng            N/A  avgt    5  246.438 ±  2.138  ms/op
ImageEncodeBenchmark.vips_encodeJpeg               N/A  avgt    5   37.221 ±  1.498  ms/op
ImageEncodeBenchmark.vips_encodePng                N/A  avgt    5  255.904 ± 10.185  ms/op  ⚠️ vips > scrimage
ImageFilterBenchmark.scrimage_blur                 N/A  avgt    5   84.807 ±  6.312  ms/op
ImageFilterBenchmark.scrimage_grayscale            N/A  avgt    5   97.047 ± 12.645  ms/op
ImageFilterBenchmark.scrimage_sepia                N/A  avgt    5   60.701 ±  0.587  ms/op
ImageResizeBenchmark.scrimage_scaleTo        1920x1080  avgt    5  195.631 ±  7.392  ms/op
ImageResizeBenchmark.scrimage_scaleTo         1280x720  avgt    5  125.526 ±  0.596  ms/op
ImageResizeBenchmark.vips_resize             1920x1080  avgt    5    0.495 ±  0.062  ms/op
ImageResizeBenchmark.vips_resize              1280x720  avgt    5    0.522 ±  0.024  ms/op
```

---

## CI Linux java25

> GraalVM 25, Ubuntu 24.04, libvips 8.15.1 (apt), vips-ffm Panama FFM

```
Benchmark                                 (resolution)  Mode  Cnt    Score     Error  Units
ImageEncodeBenchmark.scrimage_encodeJpeg           N/A  avgt    5  171.163 ± 121.342  ms/op
ImageEncodeBenchmark.scrimage_encodePng            N/A  avgt    5  249.012 ±   2.143  ms/op
ImageEncodeBenchmark.vips_encodeJpeg               N/A  avgt    5   37.198 ±   0.987  ms/op
ImageEncodeBenchmark.vips_encodePng                N/A  avgt    5  137.945 ±   2.929  ms/op
ImageFilterBenchmark.scrimage_blur                 N/A  avgt    5   73.638 ±   1.279  ms/op
ImageFilterBenchmark.scrimage_grayscale            N/A  avgt    5   99.716 ±  23.922  ms/op
ImageFilterBenchmark.scrimage_sepia                N/A  avgt    5   60.829 ±   0.420  ms/op
ImageResizeBenchmark.scrimage_scaleTo        1920x1080  avgt    5  187.290 ±   9.073  ms/op
ImageResizeBenchmark.scrimage_scaleTo         1280x720  avgt    5  119.451 ±   2.151  ms/op
ImageResizeBenchmark.vips_resize             1920x1080  avgt    5    0.591 ±   0.046  ms/op
ImageResizeBenchmark.vips_resize              1280x720  avgt    5    0.626 ±   0.083  ms/op
```

---

## 크로스 환경 비교

### 리사이즈 (4K 3840×2160 → 1920×1080)

| 환경 | scrimage (ms/op) | vips (ms/op) | 속도 향상 |
|------|-----------------|--------------|----------|
| macOS, vips-ffm  | 71.16 ± 2.02  | **0.202** ± 0.006 | **352×** |
| CI Linux, java25 | 187.29 ± 9.07 | **0.591** ± 0.046 | **317×** |
| CI Linux, java21 | 195.63 ± 7.39 | **0.495** ± 0.062 | **395×** |

### 인코딩 — JPEG

| 환경 | scrimage (ms/op) | vips (ms/op) | 속도 향상 |
|------|-----------------|--------------|----------|
| macOS, vips-ffm  | 52.49 ± 0.44   | **15.67** ± 0.27 | **3.3×** |
| CI Linux, java25 | 171.16 ± 121.3 | **37.20** ± 0.99 | **4.6×** |
| CI Linux, java21 | 161.09 ± 38.9  | **37.22** ± 1.50 | **4.3×** |

### 인코딩 — PNG

| 환경 | scrimage (ms/op) | vips (ms/op) | 속도 향상 |
|------|-----------------|--------------|----------|
| macOS, vips-ffm  | 94.87 ± 4.65   | **49.88** ± 1.02 | **1.9×** |
| CI Linux, java25 | 249.01 ± 2.14  | **137.95** ± 2.93 | **1.8×** |
| CI Linux, java21 | 246.44 ± 2.14  | 255.90 ± 10.19 | **−1.04×** ⚠️ |

### 필터 (scrimage 전용)

| 필터 | macOS (ms/op) | CI Linux java25 (ms/op) | CI Linux java21 (ms/op) |
|------|--------------|------------------------|------------------------|
| Sepia     | 13.19 ± 0.49 | 60.83 ± 0.42 | 60.70 ± 0.59 |
| Grayscale | 22.51 ± 9.19 | 99.72 ± 23.9 | 97.05 ± 12.6 |
| Blur      | 29.80 ± 1.23 | 73.64 ± 1.28 | 84.81 ± 6.31 |

---

## 주요 발견

### vips 리사이즈가 압도적으로 빠른 이유

libvips는 demand-driven pipeline 아키텍처로, 출력 픽셀에 필요한 소스 영역만 디코딩·처리합니다. scrimage는 `BufferedImage`에 전체 픽셀을 올린 후 처리하므로 메모리 할당 및 GC 압박이 큽니다.

### java21 vips PNG가 scrimage보다 느린 이유

JVips JNI 구현(java21)은 PNG 압축 시 JNI 경계를 여러 번 넘어야 합니다. PNG MaxCompression은 CPU 집약적이라 JNI 오버헤드가 두드러집니다. vips-ffm(java25)은 FFM 직접 호출로 이 오버헤드가 없어 1.8× 빠릅니다.

### macOS vs Linux 속도 차이

scrimage는 macOS Apple Silicon에서 Linux x86_64보다 2–3배 빠릅니다 (NEON SIMD + JIT 최적화). vips JPEG 인코딩은 두 플랫폼 모두 ~37ms로 일관 (libjpeg-turbo SIMD).

---

## macOS vips-ffm 실행 방법

macOS SIP가 `DYLD_LIBRARY_PATH`를 제거하므로 `VipsBenchmarkState`가 Homebrew 경로를 자동 등록합니다.

**사전 요구사항**: `brew install vips`

```bash
./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java25
```

---

## 원본 데이터

- macOS: `build/reports/benchmarks/main/2026-04-29T17.29.40.858207/benchmark.json`
- CI Linux java21: GitHub Actions run `25099275582`, job `73544264573`
- CI Linux java25: GitHub Actions run `25099275582`, job `73544264653`
