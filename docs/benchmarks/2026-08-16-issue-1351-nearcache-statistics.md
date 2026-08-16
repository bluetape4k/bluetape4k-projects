# Issue #1351 NearJCache 통계 비용 benchmark

## 목적

`NearJCache` management/statistics 계측을 추가하기 전에 disabled 경로의 처리량과 allocation을
고정한다. Task 9의 candidate는 동일한 committed harness, JDK, JMH profile로 다시 측정한다.

이 결과는 단일 로컬 머신에서 얻은 비교용 snapshot이다. production ranking이나 서로 다른
환경의 절대 성능 비교에 사용하지 않는다.

## 재현 정보

- Harness commit: `136979bff4702be0d23fbc1f92335f3bbb81e952`
- Source SHA-256: `910dd0f209af0f34fa2479f60a4ab15f89bb5bb6c349d2de265de438cd4c9fb3`
- Baseline JAR SHA-256: `532a858dc3c9b6389e126e518566dfd04b0235a36b8890799411bc248612ca5b`
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
| `bulkHit` | 6,577.058 | 6,096.416 ± 849.007 | 1,040.005 ± 0.008 |
| `frontHit` | 160,245.624 | 151,526.050 ± 19,498.268 | 0.000211 ± 0.000327 |
| `getAndPut` | 138.386 | 122.202 ± 24.950 | 1,400.287 ± 3.540 |
| `put` | 114.199 | 110.130 ± 17.499 | 1,843.029 ± 5.675 |
| `putAll` | 97.048 | 76.848 ± 29.110 | 2,287.535 ± 14.898 |

### Disabled concurrency 관찰

`frontHit`만 같은 profile에서 thread 수를 바꿔 기록했다.

| Threads | Raw median (ops/ms) | JMH score ± error (ops/ms) | Allocation ± error (B/op) |
|---:|---:|---:|---:|
| 1 | 161,193.558 | 150,767.657 ± 19,473.644 | 0.000298 ± 0.000564 |
| 2 | 33,010.639 | 35,749.306 ± 7,376.846 | 2.140363 ± 0.747464 |
| 4 | 97,327.822 | 93,465.052 ± 8,966.005 | 0.051959 ± 0.015082 |
| 8 | 98,728.917 | 98,074.382 ± 2,694.602 | 0.056633 ± 0.007511 |
| 16 | 98,820.938 | 97,839.641 ± 4,493.835 | 0.056507 ± 0.011999 |

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

Candidate 결과와 최종 비교는 Task 9에서 이 문서에 추가한다.
