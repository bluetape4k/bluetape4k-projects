# Issue 756 Lettuce Serializer Target Handoff Allocation Benchmark

## 범위

Issue [#756](https://github.com/bluetape4k/bluetape4k-projects/issues/756)은 Lettuce codec이 caller-owned `ByteBuf`에 값을 기록할 때 발생하는 serializer target handoff allocation만 측정한다. JDK, Kryo, Jackson 2, Jackson 3의 `heap`/`direct` target에서 기존 `serialize -> ByteArray -> writeBytes` 경로와 codec의 direct target overload를 각각 비교했다. 값 생성, pooled target 할당, target reset, capacity 검증은 timed method 밖에 둔다.

이 결과는 decode, 압축 codec, 다른 payload/configuration, 다른 allocator, capacity growth가 필요한 target에 적용되지 않는다.

## 입력 권한과 실행 환경

- Benchmark input commit: `bc97831a13e16ddd3055e8f2c7c92146108e16f5`
- Benchmark input tree: `2aba15b188100e6327d38fa7eeb8b93cba1f095a`
- Source JMH JAR SHA-256: `440c540f43b00ce6bf5c3d2097bf705a92c06958331b94083d34af17e70a2515`
- Executable normalized JAR SHA-256: `102ddce1a22bf5bb2055ca7d0c845f0d3d07057f6ce6a3c94cf1191dcd81d5b3`
- Normalization policy: `strip-meta-inf-signatures-v1`
- Host: macOS Darwin `25.5.0`, Apple M4 Pro, arm64, logical core 12개
- Java: `/Users/debop/.jenv/versions/21`
- Protocol: thread 1, fork 2, warmup 3, measurement 5, throughput `ops/ms`, GC profiler
- Canonical A: `2026-07-22T08:52:44Z` ~ `2026-07-22T09:35:37Z`
- Canonical B: `2026-07-22T09:35:37Z` ~ `2026-07-22T10:18:31Z`

두 run은 동일한 clean HEAD, tree, normalized JAR, Jackson 2 project JAR, ordered runtime classpath를 사용했다. 실행 전 preflight는 exact 16-cell matrix, backend/config/payload hash, baseline/candidate dispatch 분리, wire/count/prefix parity, target reset, retained backend의 read-only exception/state parity를 검증했다.

Fixture는 `PooledByteBufAllocator`, capacity/maxCapacity 512, `readerIndex=3`, `writerIndex=7`, headroom 505를 사용했다. Payload SHA-256은 `5d8a934f362eee59b74c0a63d605130c0dacadea95ef5693e945772b6d43d856`이다.

## 명령

```bash
python3 infra/lettuce/scripts/run-issue756-evidence.py \
  --output docs/benchmarks/raw/issue-756 \
  --expected-head "$(git rev-parse HEAD)" \
  --runs canonical-a canonical-b

python3 infra/lettuce/scripts/validate-issue756-jmh.py \
  --root docs/benchmarks/raw/issue-756 \
  --benchmark-input-sha "$(git rev-parse HEAD)"
```

runner가 기록한 전체 Gradle/JMH argv, runtime classpath 경로·순서·SHA-256은 각 run의 `environment.json`과 `argv.json`에 있다.

## 판정 규칙

- Allocation 감소율은 `(baseline - candidate) / baseline * 100`으로 계산하며 두 run 모두 5% 이상이어야 `accepted`가 될 수 있다.
- 어느 run에서든 throughput delta가 -20% 이하이면 `ineligible`이다.
- Allocation 기준을 두 run 모두 통과하지 못했지만 wire/security parity와 throughput 차단 조건을 통과한 declared-direct path는 `inconclusive`이다.
- `inconclusive`는 ergonomic direct path 유지만 허용하며 allocation 개선 주장을 허용하지 않는다.

## 결과

각 run 열은 `baseline B/op -> candidate B/op (allocation 감소율), throughput delta` 형식이다. Jackson 2 candidate의 측정 allocation은 모든 셀/run에서 `0.001 B/op` 미만이다.

| Backend | Target | Canonical A | Canonical B | Verdict |
|---|---|---:|---:|---|
| JDK | heap | 10824.0 -> 2384.0 (+77.97%), +95.62% | 10824.0 -> 2384.0 (+77.97%), +93.75% | accepted |
| JDK | direct | 10824.0 -> 2384.0 (+77.97%), +70.29% | 10824.0 -> 2384.0 (+77.97%), +97.18% | accepted |
| Kryo | heap | 272.0 -> 120.0 (+55.88%), -6.64% | 272.0 -> 120.0 (+55.88%), -14.48% | accepted |
| Kryo | direct | 272.0 -> 120.0 (+55.88%), -14.57% | 272.0 -> 120.0 (+55.88%), -13.02% | accepted |
| Jackson 2 | heap | 176.0 -> <0.001 (+100.00%), +11.28% | 176.0 -> <0.001 (+100.00%), +10.21% | accepted |
| Jackson 2 | direct | 176.0 -> <0.001 (+100.00%), +12.44% | 176.0 -> <0.001 (+100.00%), +26.43% | accepted |
| Jackson 3 | heap | 224.0 -> 568.0 (-153.57%), -16.78% | 224.0 -> 568.0 (-153.57%), -14.73% | inconclusive |
| Jackson 3 | direct | 224.0 -> 568.0 (-153.57%), -15.19% | 224.0 -> 568.0 (-153.57%), -15.73% | inconclusive |

## 결정

- JDK heap/direct, Kryo heap/direct, Jackson 2 heap/direct만 이 exact fixture에서 allocation 개선으로 문서화한다.
- Jackson 3 heap/direct는 direct override를 유지하되 ergonomic direct path로만 취급한다. Allocation 개선 주장을 하지 않는다.
- 어느 backend도 `ineligible`이 아니므로 serializer direct override를 제거하지 않는다.
- Throughput은 promotion 차단 진단에만 사용하며 일반적인 처리량 개선을 주장하지 않는다.

## Raw artifact

- [Canonical A metadata](raw/issue-756/canonical-a/metadata.json), [environment](raw/issue-756/canonical-a/environment.json), [argv](raw/issue-756/canonical-a/argv.json), [JMH JSON](raw/issue-756/canonical-a/jmh.json), [summary](raw/issue-756/canonical-a/summary.csv), [validation](raw/issue-756/canonical-a/validation.json)
- [Canonical B metadata](raw/issue-756/canonical-b/metadata.json), [environment](raw/issue-756/canonical-b/environment.json), [argv](raw/issue-756/canonical-b/argv.json), [JMH JSON](raw/issue-756/canonical-b/jmh.json), [summary](raw/issue-756/canonical-b/summary.csv), [validation](raw/issue-756/canonical-b/validation.json)
- [Two-run comparison](raw/issue-756/comparison.csv), [delivery manifest](raw/issue-756/delivery-manifest.json), [root validation](raw/issue-756/validation.json)

표와 committed raw artifact가 수치의 source of truth이다. 별도 chart는 만들지 않았다.

## 한계와 재측정 조건

이 측정은 고정 payload, 기본 serializer configuration, pooled 512-byte no-growth target, non-zero writer index, exact four backends의 encode path에만 적용된다. Zero-copy, decode allocation, compressed codec, Redis round trip, 다른 object graph, 다른 Jackson mapper/Kryo registration, target capacity growth, 일반 처리량을 보장하지 않는다.

Benchmark source, runner, validator, Gradle, Kotlin production/test code, payload/configuration, dispatch, classpath가 바뀌면 이 evidence는 무효다. 그 경우 clean pinned JAR에서 canonical A/B를 모두 다시 실행해야 한다.
