# 이미지 처리 JMH 벤치마크 결과

**실행 일시**: 2026-04-29  
**JMH 버전**: 1.37  
**실행 명령**: `./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java21`

---

## 실행 환경

| 항목 | 값 |
|------|----|
| OS | macOS (Apple Silicon) |
| JVM | GraalVM JDK 21.0.11 (Java HotSpot 64-Bit Server VM) |
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
| 문서 이미지 (필터 소스) | 1240×1754 (합성) |
| 썸네일 소스 | 256×256 (합성) |

---

## 결과 요약

```
Benchmark                                 (resolution)  Mode  Cnt    Score    Error  Units
ImageEncodeBenchmark.scrimage_encodeJpeg           N/A  avgt    5   68.691 ±  1.074  ms/op
ImageEncodeBenchmark.scrimage_encodePng            N/A  avgt    5  110.956 ±  1.179  ms/op
ImageEncodeBenchmark.vips_encodeJpeg               N/A  avgt    5   ≈ 10⁻⁶           ms/op  ⚠️ skip
ImageEncodeBenchmark.vips_encodePng                N/A  avgt    5   ≈ 10⁻⁶           ms/op  ⚠️ skip
ImageFilterBenchmark.scrimage_blur                 N/A  avgt    5   29.759 ±  0.190  ms/op
ImageFilterBenchmark.scrimage_grayscale            N/A  avgt    5   20.269 ±  9.732  ms/op
ImageFilterBenchmark.scrimage_sepia                N/A  avgt    5   12.131 ±  0.242  ms/op
ImageResizeBenchmark.scrimage_scaleTo        1920x1080  avgt    5   65.870 ±  5.554  ms/op
ImageResizeBenchmark.scrimage_scaleTo         1280x720  avgt    5   44.562 ±  0.419  ms/op
ImageResizeBenchmark.vips_resize             1920x1080  avgt    5   ≈ 10⁻⁶           ms/op  ⚠️ skip
ImageResizeBenchmark.vips_resize              1280x720  avgt    5   ≈ 10⁻⁶           ms/op  ⚠️ skip
```

---

## scrimage 상세 결과

### 리사이즈 (4K 3840×2160 → 타겟)

| 타겟 해상도 | 평균 (ms/op) | 오차 | min | max |
|------------|-------------|------|-----|-----|
| 1920×1080 | **65.87** | ±5.55 | 64.74 | 68.36 |
| 1280×720  | **44.56** | ±0.42 | 44.49 | 44.74 |

- 1920×1080은 오차(±5.55)가 크다 — 단일 이상치 iteration (68.36ms)이 분산을 키웠음
- 1280×720은 매우 안정적 (±0.42)

### 인코딩 (합성 1240×1754 document 이미지)

| 포맷 | 평균 (ms/op) | 오차 |
|------|-------------|------|
| JPEG (quality=85) | **68.69** | ±1.07 |
| PNG (MaxCompression) | **110.96** | ±1.18 |

- PNG MaxCompression이 JPEG보다 **61% 느림** — 압축 연산 비용
- 두 포맷 모두 오차가 작아 안정적 측정

### 필터 (합성 1240×1754 document 이미지)

| 필터 | 평균 (ms/op) | 오차 | 비고 |
|------|-------------|------|------|
| Sepia | **12.13** | ±0.24 | 가장 빠름 |
| Grayscale | **20.27** | ±9.73 | 오차 큼 |
| Blur | **29.76** | ±0.19 | 안정적 |

- Grayscale 오차(±9.73)가 이상하게 크다 — 첫 번째 iteration(21.1ms)과 두 번째(24.3ms) 이후 수렴(~18.5ms)하는 패턴으로, JIT 최적화 지연 또는 GC 영향으로 보임

---

## vips 결과: macOS에서 skip

JVips(java21 JNI) 런타임이 macOS에서 로드에 실패했습니다.

```
INFO: Trying to load JVips
WARN: VipsBenchmarkState: vips 런타임 초기화 실패 — vips 벤치마크를 skip합니다
```

**원인**: JVips 라이브러리는 Linux x86_64/ARM64 native binary를 jar에 포함합니다. macOS native binary는 미포함.

**vips 비교 결과 측정 방법**:
- CI 환경 (GitHub Actions, Ubuntu) 에서 `benchmark-images` job이 자동 실행됨
- 또는 Linux Docker 컨테이너에서 수동 실행:
  ```bash
  ./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java21
  # or java25 (FFM, Linux Java 25 필요)
  ./gradlew :bluetape4k-images-benchmark:benchmarkBenchmark -Pvips.impl=java25
  ```

---

## 예상 vips 비교 (CI 환경)

vips는 native C 라이브러리로 JVM 기반 scrimage 대비 대폭 빠른 성능이 기대됩니다.  
Linux CI에서 실행 후 이 섹션을 업데이트할 예정입니다.

| 연산 | scrimage (ms/op) | vips 예상 |
|------|-----------------|----------|
| resize 1920×1080 | 65.87 | ~2–5 |
| resize 1280×720 | 44.56 | ~1–3 |
| encode JPEG | 68.69 | ~5–15 |
| encode PNG | 110.96 | ~10–30 |

---

## 결론 (scrimage 단독)

- **리사이즈**: 4K → FHD는 약 **66ms**, 4K → HD는 약 **45ms**
- **인코딩**: JPEG가 PNG보다 61% 빠름 — 성능 민감한 파이프라인에서는 JPEG 권장
- **필터**: Sepia(12ms) < Grayscale(20ms) < Blur(30ms)
- **vips 비교**: Linux CI `benchmark-images` job 결과 참조 필요

---

## 원본 데이터

`images/images-benchmark/build/reports/benchmarks/main/2026-04-29T17.01.16.464564/benchmark.json`
