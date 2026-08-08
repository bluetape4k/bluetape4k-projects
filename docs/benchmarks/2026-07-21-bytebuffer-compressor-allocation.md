# 호출자 소유 ByteBuffer 압축기 할당 벤치마크 - 2026-08-01

## 한눈에 보기

이 보고서는 이슈 #755에서 추가한 `compress(source, target)` 및
`decompress(source, target)`가 재사용 가능한 caller-owned target을 사용할 때 payload 크기에
비례하는 중간 할당을 줄이는지 검증한다. 동일 commit과 benchmark JAR로 canonical JMH run을 두
번 수행했으며 96개 조합을 비교했다.

- 96개 조합 중 63개가 allocation gate를 통과했다.
- LZ4와 Deflate는 압축·복원 및 heap/direct/mixed 네 storage 조합을 모두 채택할 수 있다.
- Zstd는 matched heap 및 matched direct 조합을 채택할 수 있고 mixed 조합은 fallback이라 제외한다.
- Snappy는 direct compression만 채택한다. direct decompression은 allocation은 줄였지만 안전성
  검증 비용 때문에 medium/large payload 처리량이 baseline의 약 59~63%로 낮아 채택하지 않는다.
- 이 결과는 reusable, pre-sized target에 대한 allocation 근거이며 일반적인 throughput 향상이나
  zero-allocation을 주장하지 않는다.

## 범위와 판정 기준

- 이슈: [#755](https://github.com/bluetape4k/bluetape4k-projects/issues/755),
  [#1260](https://github.com/bluetape4k/bluetape4k-projects/issues/1260)
- 모듈: `:bluetape4k-io`
- 코덱: LZ4, Deflate, Snappy, Zstd
- 연산: compression, decompression
- 저장소: heap, direct, heap-to-direct, direct-to-heap
- 페이로드: small 1,147 B, medium 65,718 B, large 524,349 B
- 할당 게이트: 두 run 모두에서 candidate `gc.alloc.rate.norm`이 baseline보다 5% 이상
  낮고 error interval이 겹치지 않으며, small에서 large로 갈수록 절감량이 커져야 한다.
- 처리량 가드: 두 run 모두에서 candidate throughput이 baseline보다 20% 이상 낮고 error
  interval이 겹치지 않으면 자동 채택하지 않고 design review 대상으로 분리한다.

## 실행 조건과 불변 identity

| 항목 | 값 |
|---|---|
| 커밋 | `c0ba3dc5e72851370257584ddb99c9785111199b` |
| 트리 | `3b88638ac2bf4f09e7a261b6f46a6c87191d1cbe` |
| 벤치마크 JAR SHA-256 | `540a36a7011bd1a9205b1933123edb5482cbb80a752c09ff823e09451d6f24c3` |
| 실행 1 | `run-20260731T235242Z-300ece58` · 288 records · PASS |
| 실행 2 | `run-20260801T011556Z-956940c2` · 288 records · PASS |
| JDK | GraalVM JDK `21.0.12` |
| JMH | `1.37`, 1 thread, 2 forks, 3 warmups, 5 measurements, 1 second each |
| 힙 / GC | `-Xms1g -Xmx1g -XX:+UseG1GC` |
| 호스트 | Apple M4 Pro, macOS 26.5.2 arm64 |

실행기는 commit, tree, JAR hash, dependency hash, 실제 JVM args와 결과 수를 검증한 뒤에만
staging directory를 immutable run directory로 publish한다. 두 run의 identity가 다르거나
필요한 JMH secondary metric이 누락되면 비교 파일을 만들지 않는다.

## 채택 matrix

`accepted`는 두 canonical run에서 allocation gate와 throughput guard를 모두 통과했다는 뜻이다.
`ineligible`은 correctness path로는 유효하지만 allocation 채택 대상으로 사용하지 않는다는
뜻이다.

| 코덱 | 연산 | heap | direct | heap → direct | direct → heap |
|---|---|---|---|---|---|
| LZ4 | compress | accepted | accepted | accepted | accepted |
| LZ4 | decompress | accepted | accepted | accepted | accepted |
| Deflate | compress | accepted | accepted | accepted | accepted |
| Deflate | decompress | accepted | accepted | accepted | accepted |
| Snappy | compress | ineligible | accepted | ineligible | ineligible |
| Snappy | decompress | ineligible | ineligible | ineligible | ineligible |
| Zstd | compress | accepted | accepted | ineligible | ineligible |
| Zstd | decompress | accepted | accepted | ineligible | ineligible |

최종 96개 cell의 verdict는 `accepted` 63개, `ineligible` 33개이며
`design-review-required`와 `not-demonstrated`는 0개다. codec별 accepted cell 수는 LZ4 24개,
Deflate 24개, Snappy 3개, Zstd 12개다.

## Allocation 결과

아래 값은 small부터 large까지 두 run에서 관찰한 baseline 대비 allocation 절감 범위다. B/op
절감량이 payload보다 클 수 있는 이유는 baseline이 입력·출력 외에도 payload 크기에 비례하는
임시 객체를 만들기 때문이다.

| 코덱 / 연산 | 저장소 | 절감 B/op 범위 | 후보 처리량 / 기준선 |
|---|---|---:|---:|
| LZ4 compress | heap | 1,640–597,504 | 103.0–117.2% |
| LZ4 compress | direct | 2,680–1,121,744 | 109.0–118.7% |
| LZ4 decompress | heap | 1,224–524,424 | 96.7–114.8% |
| LZ4 decompress | direct | 1,592–595,273 | 187.2–297.8% |
| Deflate compress | heap | 1,960–557,864 | 100.0–103.8% |
| Deflate compress | direct | 3,120–1,082,224 | 99.5–105.2% |
| Deflate decompress | heap | 18,144–2,622,169 | 122.2–133.4% |
| Deflate decompress | direct | 18,400–2,655,113 | 127.0–135.6% |
| Snappy compress | direct | 2,856–1,209,088 | 111.5–129.6% |
| Zstd compress | heap | 1,584–548,824 | 101.9–104.6% |
| Zstd compress | direct | 2,624–1,073,064 | 103.4–108.1% |
| Zstd decompress | heap | 1,224–524,424 | 101.5–107.8% |
| Zstd decompress | direct | 1,360–546,618 | 103.7–126.1% |

LZ4와 Deflate의 mixed storage 경로도 gate를 통과했다. LZ4 mixed 경로는 operation과 방향에
따라 1,224–1,121,808 B/op, Deflate mixed 경로는 1,960–2,655,113 B/op을 절감했다. Zstd와
Snappy의 mixed 경로는 compatibility fallback이므로 채택 대상이 아니다.

## Snappy decompression을 채택하지 않은 이유

Snappy direct decompression은 1,336–597,168 B/op을 줄였지만 candidate throughput이
baseline의 58.4–95.2%였다. 특히 medium/large payload는 두 run 모두 약 37–41% 느렸다. 운영
계약은 native decode 전에 complete payload와 decompressed size를 검증해야 하며, allocation
수치를 높이기 위해 이 안전성 검사를 제거하지 않는다. 따라서 이 경로의 native capability는
유지하되 allocation-sensitive adoption 대상으로 추천하지 않는다.

Snappy heap compression/decompression은 array API가 caller-visible target limit을 안전하게
강제하지 못해 compatibility fallback을 유지한다. 이 경로들은 payload 크기에 비례하는 추가
할당을 만들었으므로 allocation claim에서 제외했다.

## 재현

```bash
./gradlew :bluetape4k-io:testBenchmarkJar --no-configuration-cache
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py prepare \
  --jar io/io/build/benchmarks/test/jars/bluetape4k-io-test-jmh-1.12.0-JMH.jar \
  --expected-head c0ba3dc5e72851370257584ddb99c9785111199b \
  --output-root io/io/build/issue-755-evidence \
  --receipt io/io/build/issue-755-evidence/input.json
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py run \
  --profile canonical \
  --input-receipt io/io/build/issue-755-evidence/input.json \
  --output-root docs/benchmarks/raw/issue-755 \
  --include '.*CallerOwnedByteBufferCompressorBenchmark.*'
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py run \
  --profile canonical \
  --input-receipt io/io/build/issue-755-evidence/input.json \
  --output-root docs/benchmarks/raw/issue-755 \
  --include '.*CallerOwnedByteBufferCompressorBenchmark.*'
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py compare \
  --run docs/benchmarks/raw/issue-755/run-20260731T235242Z-300ece58 \
  --run docs/benchmarks/raw/issue-755/run-20260801T011556Z-956940c2 \
  --output docs/benchmarks/raw/issue-755/comparison.csv
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py validate-delivery \
  --run docs/benchmarks/raw/issue-755/run-20260731T235242Z-300ece58 \
  --run docs/benchmarks/raw/issue-755/run-20260801T011556Z-956940c2 \
  --comparison docs/benchmarks/raw/issue-755/comparison.csv \
  --expected-head c0ba3dc5e72851370257584ddb99c9785111199b
```

원시 결과와 실행 metadata는 [`raw/issue-755/`](./raw/issue-755/)에 있으며, 96개 cell의 두
run 비교값은 [`comparison.csv`](./raw/issue-755/comparison.csv)에 있다. 표가 숫자의 source of
truth이므로 별도 chart는 만들지 않았다.

## 채택 및 rollback 지침

- target을 호출 간 재사용할 수 있고 matrix가 `accepted`인 조합에서만 two-argument API를
  allocation 최적화로 채택한다.
- target capacity를 codec의 documented bound에 맞게 미리 확보한다. overflow 뒤에는 source
  상태와 target position이 복원되므로 더 큰 target으로 전체 작업을 재시도한다.
- mutable source/target은 호출이 끝날 때까지 한 thread에 한정하고 storage가 겹치지 않게 한다.
- 회귀가 발견되면 public default와 wire format은 유지하고 해당 codec override만 compatibility
  fallback으로 되돌린다. patch 전에는 기존 allocating API 또는 matrix의 fallback 조합을 쓴다.

## 주장하지 않는 것

- 일반적인 throughput 향상, zero-copy, zero-allocation을 주장하지 않는다.
- 다른 JDK, GC, CPU, payload 분포, codec option에서도 같은 수치를 보장하지 않는다.
- caller-owned buffer가 동시 접근을 안전하게 만들거나 untrusted input의 decompressed-size
  resource bound를 제공한다고 주장하지 않는다.
- fallback 경로의 correctness PASS를 allocation 개선 근거로 사용하지 않는다.
