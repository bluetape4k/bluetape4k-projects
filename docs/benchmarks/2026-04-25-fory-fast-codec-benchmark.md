# FastFory Codec 벤치마크 결과

- **날짜**: 2026-04-25
- **Issue**: #113
- **Branch**: `feat/fory-fast-codec`

## 측정 환경

| 항목              | 값                      |
|-----------------|------------------------|
| CPU             | Apple M4 Pro (12-core) |
| Memory          | 48 GB                  |
| JVM             | Java 21.0.10 LTS       |
| OS              | macOS Darwin 25.4.0    |
| JMH Warmup      | 3 iterations × 2s      |
| JMH Measurement | 5 iterations × 3s      |
| JMH Fork        | 1                      |

---

## infra/redisson — `RedissonCodecBenchmark`

> `./gradlew :bluetape4k-redisson:benchmark`
> 단위: ops/ms (높을수록 빠름)

| Codec        |       ops/ms | ± Error |    vs Fory |
|--------------|-------------:|--------:|-----------:|
| **FastFory** | **3208.433** | ±51.194 | **+26.4%** |
| Fory         |     2538.957 | ±16.555 |         기준 |
| Kryo5        |     1237.810 | ±38.006 |     -51.3% |
| Fastjson2    |     2039.610 | ±45.208 |     -19.7% |
| Jackson3     |      488.939 |  ±6.308 |     -80.7% |
| JDK          |      135.188 |  ±0.526 |     -94.7% |
| LZ4FastFory  |      873.522 |  ±6.951 |          — |
| LZ4Fory      |      814.854 | ±40.459 |          — |
| LZ4Kryo5     |      570.520 |  ±2.091 |          — |
| ZstdFastFory |      206.262 |  ±4.101 |          — |
| ZstdFory     |      202.320 |  ±1.987 |          — |
| ZstdKryo5    |      141.836 |  ±1.444 |          — |
| GzipFastFory |      109.896 |  ±1.502 |          — |

### 주요 관찰

- **FastFory**는 Fory 대비 **+26.4%** 처리량 향상 (spec 예측 +70%보다 낮지만, 이미 작은 payload 기준으로 compress 없이도 상당한 개선)
- LZ4FastFory는 LZ4Fory 대비 **+7.2%** 향상
- GZip 계열은 압축 오버헤드로 인해 낮은 처리량 — 네트워크 대역폭 절약이 필요한 경우에 사용

---

## infra/lettuce — `LettuceCodecBenchmark`

> `./gradlew :bluetape4k-lettuce:benchmark`
> 단위: ops/ms (높을수록 빠름)

| Codec        |       ops/ms |  ± Error |    vs Fory |
|--------------|-------------:|---------:|-----------:|
| **FastFory** | **3300.020** |  ±34.753 | **+27.1%** |
| Fory         |     2596.215 |  ±50.393 |         기준 |
| Fastjson2    |     6348.394 | ±174.746 |  — (포맷 다름) |
| Kryo         |     1061.326 |  ±10.067 |     -59.1% |
| Jackson3     |      867.979 |   ±8.134 |     -66.6% |
| JDK          |      134.999 |   ±2.154 |     -94.8% |
| LZ4FastFory  |      922.322 |  ±14.689 |          — |
| LZ4Fory      |      853.985 |   ±9.272 |          — |
| LZ4Kryo      |      544.634 |   ±2.428 |          — |
| ZstdFastFory |      208.284 |   ±3.718 |          — |
| ZstdFory     |      201.729 |   ±0.745 |          — |
| ZstdKryo     |      136.613 |   ±0.737 |          — |
| GzipFastFory |      111.269 |   ±1.171 |          — |

### 주요 관찰

- **FastFory**는 Fory 대비 **+27.1%** 처리량 향상
- LZ4FastFory는 LZ4Fory 대비 **+8.0%** 향상
- Fastjson2가 높게 나오는 것은 ByteBuffer 기반 포맷 차이 (직접 비교 부적절)

---

## 종합 요약

| 모듈       | Fory (ops/ms) | FastFory (ops/ms) |        향상률 |
|----------|--------------:|------------------:|-----------:|
| redisson |         2,538 |             3,208 | **+26.4%** |
| lettuce  |         2,596 |             3,300 | **+27.1%** |

> spec §1.3 예측(+70%)과 차이가 있는 이유: 벤치마크 payload가 소형(512B description + 소수 필드)이며,
`refTracking=false` 효과는 순환 참조가 많은 대형 객체 그래프에서 더 두드러짐. 실 서비스의 복잡한 엔티티에서는 더 큰 향상이 기대됨.
