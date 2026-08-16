# Issue #1351 NearJCache 통계 비용 benchmark

## 목적

`NearJCache` management/statistics 계측을 추가하기 전에 disabled 경로의 처리량과 allocation을
고정한다. Task 9의 candidate는 동일한 committed harness, JDK, JMH profile로 다시 측정한다.

이 결과는 단일 로컬 머신에서 얻은 비교용 snapshot이다. production ranking이나 서로 다른
환경의 절대 성능 비교에 사용하지 않는다.

## 재현 정보

- Harness commit: `136979bff4702be0d23fbc1f92335f3bbb81e952`
- Source SHA-256: `910dd0f209af0f34fa2479f60a4ab15f89bb5bb6c349d2de265de438cd4c9fb3`
- Baseline JAR SHA-256: `c2949db3a166ce008f4bce64fe6fdb68cf4fb4ee5e6c9eab722172c34adcf5d8`
- Candidate JAR SHA-256: `7c94c3d226fc855fbfb8bbceb888a3266b321a98865bf69edd72a96202211118`
- CPU/RAM: Apple M4 Pro / 48 GiB
- OS: Darwin 25.6.0 arm64
- Java: Oracle GraalVM 25.2.4, JDK 25.0.4
- JMH: 1.37, throughput, `ops/ms`, GC profiler allocation `B/op`
- Profile: 3 forks, 5 × 1초 warmup, 10 × 1초 measurement
- Parameter: `statisticsEnabled=false`
- Docker/Testcontainers: 사용하지 않음

처음 생성된 fat JAR은 dependency의 `META-INF/*.SF`/`*.DSA` 때문에 main class 로드 전에
`SecurityException`이 발생했다. 저장소의 기존 benchmark 패턴과 동일하게 signature metadata를
제외하고 JAR을 `--rerun-tasks`로 재생성한 뒤 측정을 시작했다. 실패한 실행은 원시 결과를 만들지
않았으며 아래 baseline에 포함되지 않는다.

## Baseline 결과

`rawData` 3개 fork의 모든 measurement를 평탄화한 median과 JMH score/error를 함께 기록한다.

| Method | Raw median (ops/ms) | JMH score ± error (ops/ms) | Allocation ± error (B/op) |
|---|---:|---:|---:|
| `bulkHit` | 6,643.274 | 6,635.132 ± 57.676 | 1,040.005 ± 0.008 |
| `frontHit` | 159,069.854 | 159,130.955 ± 1,285.648 | 0.000207 ± 0.000332 |
| `getAndPut` | 136.274 | 134.065 ± 8.344 | 1,408.268 ± 13.309 |
| `put` | 137.806 | 133.138 ± 8.864 | 1,844.251 ± 3.975 |
| `putAll` | 132.771 | 132.428 ± 4.924 | 2,285.395 ± 14.364 |

### Disabled concurrency 관찰

`frontHit`만 같은 profile에서 thread 수를 바꿔 기록했다.

| Threads | Raw median (ops/ms) | JMH score ± error (ops/ms) | Allocation ± error (B/op) |
|---:|---:|---:|---:|
| 1 | 160,810.888 | 160,720.095 ± 1,239.766 | 0.000204 ± 0.000328 |
| 2 | 26,457.490 | 25,400.888 ± 3,393.650 | 4.302160 ± 0.870679 |
| 4 | 101,735.712 | 100,166.161 ± 3,172.115 | 0.059037 ± 0.005445 |
| 8 | 96,579.659 | 97,077.353 ± 2,867.536 | 0.053055 ± 0.004081 |
| 16 | 99,203.699 | 99,920.465 ± 1,823.571 | 0.072812 ± 0.005661 |

2-thread snapshot의 allocation과 throughput은 다른 thread 수보다 변동이 크다. 원시값을
재선별하지 않으며 candidate도 같은 profile로 측정한다. 환경/profile identity가 다르거나
candidate 비교가 실패하면 candidate만 반복하지 않고 baseline/candidate pair 전체를 폐기하고
같은 환경에서 다시 실행한다.

## 판정 정책

- 모든 method 및 disabled concurrency key에서 candidate raw median throughput은 baseline의 95% 이상이어야 한다.
- allocation은 `candidate <= baseline + max(0.001, baselineError + candidateError)`여야 한다.
- `0 B/op`은 exact zero 단정이 아니다. 위 profiler uncertainty 안에서 새 allocation이 관찰되지 않았다는 뜻이다.
- active statistics concurrency는 contention 관찰 자료로만 사용하고 release hard gate로 사용하지 않는다.

## 원시 증거

- `docs/benchmarks/raw/issue-1351/baseline/jmh.json`
- `docs/benchmarks/raw/issue-1351/baseline/concurrency.json`
- `docs/benchmarks/raw/issue-1351/baseline/manifest.json`

- `docs/benchmarks/raw/issue-1351/candidate/jmh.json`
- `docs/benchmarks/raw/issue-1351/candidate/concurrency-disabled.json`
- `docs/benchmarks/raw/issue-1351/candidate/concurrency-active.json`
- `docs/benchmarks/issue-1351-compare.jq`

## Candidate 비교

두 번의 선행 canonical pair는 고정 comparator가 8-thread allocation budget을 각각
`0.000288 B/op`, `0.000083 B/op` 초과해 pair 전체를 폐기했다. candidate만 반복하거나
threshold를 완화하지 않았다. disabled `get`이 새 recorder field를 읽지 않고 immutable
configuration snapshot의 boolean으로 active 경로를 선택하도록 조정한 뒤 baseline과 candidate
전체 pair를 같은 세션에서 다시 측정했다. 아래 표는 최종 통과 pair만 요약한다.

| Method | Baseline median | Candidate median | Ratio | Baseline → candidate allocation (B/op) | 판정 |
|---|---:|---:|---:|---:|---|
| `bulkHit` | 6,643.274 | 6,779.785 | 102.05% | 1,040.004995 → 1,040.005510 | PASS |
| `frontHit` | 159,069.854 | 158,150.474 | 99.42% | 0.000207 → 0.000237 | PASS |
| `getAndPut` | 136.274 | 143.136 | 105.04% | 1,408.268392 → 1,373.723454 | PASS |
| `put` | 137.806 | 139.119 | 100.95% | 1,844.251164 → 1,842.620533 | PASS |
| `putAll` | 132.771 | 134.947 | 101.64% | 2,285.395406 → 2,294.893606 | PASS |

### Disabled concurrency 비교

| Threads | Baseline median | Candidate median | Ratio | Baseline → candidate allocation (B/op) | Allocation max | 판정 |
|---:|---:|---:|---:|---:|---:|---|
| 1 | 160,810.888 | 155,550.742 | 96.73% | 0.000204 → 0.000236 | 0.001204 | PASS |
| 2 | 26,457.490 | 32,920.855 | 124.43% | 4.302160 → 3.022181 | 5.818477 | PASS |
| 4 | 101,735.712 | 97,897.518 | 96.23% | 0.059037 → 0.058141 | 0.067977 | PASS |
| 8 | 96,579.659 | 95,647.941 | 99.04% | 0.053055 → 0.059547 | 0.063184 | PASS |
| 16 | 99,203.699 | 96,012.357 | 96.78% | 0.072812 → 0.059125 | 0.084452 | PASS |

모든 method와 thread key가 median throughput 95% guard 및 allocation uncertainty budget을
통과했다. 가장 낮은 throughput ratio는 4-thread의 96.23%다.

### Active statistics 관찰

| Threads | Raw median (ops/ms) | JMH score ± error (ops/ms) | Allocation ± error (B/op) |
|---:|---:|---:|---:|
| 1 | 17,842.472 | 17,881.655 ± 244.720 | 0.002070 ± 0.003426 |
| 2 | 4,354.226 | 4,907.794 ± 1,029.496 | 14.396216 ± 3.095516 |
| 4 | 10,239.733 | 10,235.260 ± 87.678 | 0.430349 ± 0.039800 |
| 8 | 10,377.917 | 10,305.332 ± 151.312 | 0.361692 ± 0.043356 |
| 16 | 10,285.216 | 10,249.269 ± 90.748 | 0.406377 ± 0.040770 |

active 수치는 recorder contention과 계측 비용의 관찰값이다. disabled hard gate와 달리 release
판정, production sizing, provider 간 ranking으로 일반화하지 않는다.

## 재현 및 판정 명령

```bash
jq -e -n --slurpfile b docs/benchmarks/raw/issue-1351/baseline/jmh.json \
  --slurpfile c docs/benchmarks/raw/issue-1351/candidate/jmh.json \
  -f docs/benchmarks/issue-1351-compare.jq
jq -e -n --slurpfile b docs/benchmarks/raw/issue-1351/baseline/concurrency.json \
  --slurpfile c docs/benchmarks/raw/issue-1351/candidate/concurrency-disabled.json \
  -f docs/benchmarks/issue-1351-compare.jq
```

두 명령은 benchmark key/cardinality 불일치, 5% 초과 median throughput 회귀, allocation
uncertainty budget 초과 중 하나라도 있으면 non-zero로 종료한다.
