# Issue 756 Fory/FastFory Buffer 후속 벤치마크

## 범위

Issue [#756](https://github.com/bluetape4k/bluetape4k-projects/issues/756)의 후속 slice는 raw
Fory/FastFory 경로에서 codec-level handoff `ByteArray`를 줄일 수 있는지를 검증한다.

- Lettuce: `serialize() -> ByteArray -> ByteBuf.writeBytes()` baseline과 caller-owned
  `ByteBuf` target으로 보내는 bounded stream candidate를 비교한다.
- Redisson decode: `ByteBufUtil.getBytes()` copied baseline과 single-NIO-component
  read-only view candidate를 비교한다.
- Redisson encode: production 변경 전에 별도 feasibility probe로 owned-output
  `ByteBuf` candidate를 평가한다.

압축 codec은 이 범위에 포함하지 않는다. Apache Fory의 내부 재사용 `MemoryBuffer`와 최종
destination write는 그대로이므로 이 결과는 zero-copy 근거가 아니다.

## 실행 권한과 환경

- Benchmark input commit: `444c382ee4d8768b05a641083dbc84d540847cf8`
- Lettuce JMH JAR SHA-256:
  `bfb80752cc5c1f6a0fe9016b3462fc7fafc39415125c7d07d76a2824b2725a37`
- Redisson JMH JAR SHA-256:
  `2b033519f0fcee4fcf7a16163c29420289e6df3a5318b1c37d9a496fc1123ddb`
- Aggregate manifest SHA-256:
  `68f81d30c406ab24770127b92c4bef2a11ebfc66169a7ccf648d02d7efd50aae`
- Host: macOS Darwin, arm64
- Java: GraalVM JDK `21.0.12`
- JVM: `-Xms1g -Xmx1g -XX:+UseG1GC`
- JMH: thread 1, fork 2, warmup 3 × 1초, measurement 5 × 1초,
  throughput `ops/ms`, `-prof gc`

두 canonical run은 module별로 하나의 고정 JAR을 재사용했다. 각 leaf의 `argv.json`,
`environment.json`, `metadata.json`, `jmh.json`, `summary.csv`, `comparison.json`,
`validation.json`이 aggregate validator를 통과했다. 기존
[`raw/issue-756/`](./raw/issue-756/) authority는 변경하지 않았다.

## 판정 규칙

`accepted`는 canonical A/B 모두에서 아래 조건을 충족한 promotable cell이다.

- candidate allocation이 baseline의 95% 이하
- candidate allocation의 `score + error`가 baseline의 `score - error`보다 작음
- throughput delta가 -20%보다 큼
- allocation metric은 finite positive `B/op`

`fallback`은 correctness만 확인하는 non-promotable cell이다. `rejected`와
`inconclusive`도 차트나 allocation 개선 주장에 포함하지 않는다.

## 결과

각 run은 `allocation 감소율, throughput delta` 순서다. Allocation 감소율은
`(1 - candidate / baseline) × 100`으로 계산했다.

| Transport | Backend | Operation/storage | Canonical A | Canonical B | Disposition |
|---|---|---|---:|---:|---|
| Lettuce | Fory | heap encode | +99.99947%, +8.70% | +99.99947%, +1.44% | accepted |
| Lettuce | Fory | direct encode | +99.99950%, +2.42% | +99.99949%, +7.03% | accepted |
| Lettuce | FastFory | heap encode | +99.99952%, +41.71% | +99.99954%, +13.80% | accepted |
| Lettuce | FastFory | direct encode | +99.99950%, -0.30% | +99.99954%, +4.91% | accepted |
| Redisson | Fory | heap decode | -30.00%, -18.49% | -20.00%, -19.94% | rejected |
| Redisson | Fory | direct decode | +28.57138%, -6.06% | +28.57138%, -9.75% | accepted |
| Redisson | Fory | composite decode | 0.00%, -2.67% | 0.00%, -4.58% | fallback |
| Redisson | FastFory | heap decode | -22.22%, -29.25% | -22.22%, -28.77% | rejected |
| Redisson | FastFory | direct decode | +26.98408%, -13.18% | +26.98408%, -10.36% | accepted |
| Redisson | FastFory | composite decode | 0.00%, -6.21% | 0.00%, -7.24% | fallback |

![Accepted Fory/FastFory allocation reductions](../images/readme-charts/issue756-fory-followup-allocation-chart-01.png)

차트에는 두 run 모두 `accepted`인 6개 cell만 표시한다. Redisson heap decode의
`rejected` 2개 cell과 composite copied `fallback` 2개 cell을 0으로 그리지 않았다.
Lettuce label은 소수점 넷째 자리로 반올림하며, `100%`나 zero-allocation을 뜻하지 않는다.
차트 source는 aggregate manifest SHA-256에 묶여 있으며
[`validate-chart-source.py`](./raw/issue-756-fory-followup/validate-chart-source.py)가
raw comparison, SVG metadata, PNG 크기, summary를 fail-closed로 대조한다.

## Redisson encode feasibility

Redisson encode candidate는 두 probe에서 모두 allocation gate를 통과하지 못했다.

| Backend | Probe A baseline → candidate | Probe B baseline → candidate | 최종 판정 |
|---|---:|---:|---|
| Fory | 232.00 → 272.00 B/op (+17.24%) | 232.00 → 272.00 B/op (+17.24%) | rejected |
| FastFory | 216.00 → 272.00 B/op (+25.93%) | 216.00 → 272.00 B/op (+25.93%) | rejected |

따라서 `encodeDisposition=rejected`로 고정했고 Redisson production encode는 변경하지
않았다. Feasibility 결과를 canonical accepted 결과로 승격하지 않는다.

## 호환성과 운영 경계

| 경계 | 결정 |
|---|---|
| Scope | 압축하지 않은 raw Fory/FastFory path만 해당 |
| Migration | 같은 codec을 유지하면 caller API/payload migration 없음; Fory ↔ FastFory 전환은 migration 또는 eviction 필요 |
| Fory internals | 내부 재사용 `MemoryBuffer`가 남으므로 zero-copy 아님 |
| Trust | registration-off decode는 배포 경계가 제어하는 trusted payload에만 사용 |
| Lettuce failure | target-taking encode에는 codec fallback이 없음; 실패 시 `writerIndex`를 commit하지 않음 |
| Redisson fallback | `FastForyCodec`은 legacy Fory bytes를 읽을 수 있지만 반대 방향은 지원하지 않음 |
| Redisson storage | direct decode만 accepted; heap은 rejected; composite는 correctness fallback |
| Redisson encode | feasibility rejected; 기존 allocating encode 유지 |

## 결정

- Lettuce raw Fory/FastFory heap/direct target-taking encode 4개 cell만 allocation
  개선으로 문서화한다.
- Redisson raw Fory/FastFory direct decode 2개 cell만 allocation 개선으로
  문서화한다.
- Redisson heap decode는 direct-view 구현을 correctness 경로로 유지하되 allocation
  개선을 주장하지 않는다.
- Redisson composite decode는 copied compatibility fallback이며 promotable하지 않다.
- Redisson encode candidate는 채택하지 않는다.

## Raw artifact

- [Aggregate manifest](raw/issue-756-fory-followup/manifest.json),
  [aggregate validation](raw/issue-756-fory-followup/validation.json)
- [Lettuce canonical A](raw/issue-756-fory-followup/lettuce/canonical-a/),
  [Lettuce canonical B](raw/issue-756-fory-followup/lettuce/canonical-b/)
- [Redisson canonical A](raw/issue-756-fory-followup/redisson/canonical-a/),
  [Redisson canonical B](raw/issue-756-fory-followup/redisson/canonical-b/)
- [Encode feasibility](raw/issue-756-fory-followup/feasibility/)
- [Release checklist](../superpowers/checklists/2026-07-23-issue-756-fory-followup-release.md),
  [cross-version compatibility](raw/issue-756-fory-followup/release/compatibility-results.json),
  [rollback smoke](raw/issue-756-fory-followup/release/rollback-smoke.json)
- [Chart source](../images/readme-charts/issue756-fory-followup-allocation-chart-01-source.json),
  [SVG](../images/readme-charts/issue756-fory-followup-allocation-chart-01.svg),
  [PNG](../images/readme-charts/issue756-fory-followup-allocation-chart-01.png)

## 한계와 재측정 조건

이 결과는 고정 payload, 기본 serializer/codec 설정, no-growth buffer state, 명시한
JVM/JMH profile에만 적용된다. 다른 payload/object graph, allocator/pooling, capacity
growth, 압축, Redis round trip, 일반 처리량, 전체 Fory 내부 allocation에는 일반화하지
않는다. Production code, benchmark matrix, payload, JAR, classpath 또는 validator가
바뀌면 clean pinned JAR로 canonical A/B를 다시 수집해야 한다.
