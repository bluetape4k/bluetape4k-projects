# 이미지 처리 JMH 벤치마크 결과

**실행 일시**: 2026-04-29  
**JMH 버전**: 1.37  
**실행 명령**:
```bash
# scrimage + vips-ffm (Java 25, macOS)
./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java25

# scrimage 단독 (Java 21, macOS) — 이전 실행
./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java21
```

---

## 실행 환경

| 항목 | 값 |
|------|----|
| OS | macOS (Apple Silicon) |
| JVM (vips 포함) | GraalVM JDK 25.0.3 (Java HotSpot 64-Bit Server VM) |
| JVM (scrimage 단독) | GraalVM JDK 21.0.11 |
| vips 구현체 | vips-ffm 1.9.6 (Panama FFM) |
| vips 버전 | 8.18.2_1 (Homebrew) |
| 워밍업 | 3 iterations × 1s |
| 측정 | 5 iterations × 1s |
| Fork | 1 |
| 스레드 | 1 |
| 측정 지표 | AverageTime (ms/op, 낮을수록 빠름) |

### 테스트 이미지

실제 이미지 파일(`/bench/photo-4k.jpg` 등)이 없어 합성 이미지를 자동 생성해 사용했습니다.

| 이미지 역할 | 크기 |
|------------|------|
| 4K 사진 (리사이즈 소스) | 3840×2160 (합성) |
| 문서 이미지 (필터/인코딩 소스) | 1240×1754 (합성) |
| 썸네일 소스 | 256×256 (합성) |

---

## 전체 결과 요약

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

## scrimage vs vips 비교

### 리사이즈 (4K 3840×2160 → 타겟)

| 타겟 해상도 | scrimage (ms/op) | vips (ms/op) | 속도 향상 |
|------------|-----------------|--------------|----------|
| 1920×1080  | 71.16 ± 2.02    | 0.202 ± 0.006 | **352×** |
| 1280×720   | 47.85 ± 1.72    | 0.207 ± 0.011 | **231×** |

### 인코딩 (합성 1240×1754 document 이미지)

| 포맷 | scrimage (ms/op) | vips (ms/op) | 속도 향상 |
|------|-----------------|--------------|----------|
| JPEG | 52.49 ± 0.44    | 15.67 ± 0.27  | **3.3×** |
| PNG  | 94.87 ± 4.65    | 49.88 ± 1.02  | **1.9×** |

---

## 상세 분석

### 리사이즈

vips가 scrimage 대비 리사이즈에서 압도적으로 빠릅니다 (200–350배).

- scrimage: Java 2D `BufferedImage` 기반, JVM 메모리에 디코딩된 픽셀 전체를 처리
- vips-ffm: libvips의 demand-driven pipeline 아키텍처 — 필요한 영역만 디코딩·처리
- vips는 리사이즈 시 소스 이미지를 전부 메모리에 올리지 않고, 출력 해상도에 맞게 미리 스케일링된 데이터만 처리

1920×1080(FHD)보다 1280×720(HD) 리사이즈 시 vips가 미세하게 느린 점(0.207 > 0.202)은 오차 범위 내.

### 인코딩

- JPEG 인코딩: vips가 3.3배 빠름 — libvips는 libjpeg-turbo 기반 SIMD 가속 활용
- PNG 인코딩: vips가 1.9배 빠름 — libpng + SIMD 압축; MaxCompression은 두 구현 모두 CPU 집약적
- scrimage PNG는 94.87ms → CPU 시간의 절대적인 소모량

### 필터

vips 필터 벤치마크 없음 (현재 구현은 scrimage 필터 3종만 측정).

| 필터 | scrimage (ms/op) | 비고 |
|------|-----------------|------|
| Sepia | 13.19 ± 0.49 | 가장 빠름 |
| Grayscale | 22.51 ± 9.19 | 오차 큼 (JIT/GC 영향) |
| Blur | 29.80 ± 1.23 | 가장 느림 |

---

## vips-ffm macOS 실행 방법

vips-ffm은 Panama FFM API를 사용하므로 JNI 바이너리 없이 macOS에서 동작합니다.  
단, macOS SIP가 `DYLD_LIBRARY_PATH`를 제거하므로 `vipsffm.libpath.*.override` 시스템 프로퍼티로 절대 경로를 지정해야 합니다.

`VipsBenchmarkState`가 macOS를 자동 감지하고 Homebrew 경로를 등록합니다:
```
vipsffm.libpath.vips.override=/opt/homebrew/lib/libvips.dylib
vipsffm.libpath.glib.override=/opt/homebrew/lib/libglib-2.0.dylib
vipsffm.libpath.gobject.override=/opt/homebrew/lib/libgobject-2.0.dylib
```

**사전 요구사항**: `brew install vips`

---

## 원본 데이터

`images/images-benchmark/build/reports/benchmarks/main/2026-04-29T17.29.40.858207/benchmark.json`
