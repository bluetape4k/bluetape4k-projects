# 이슈 #1329 모듈별 Kover Instruction Coverage 개선 설계

## 목표

Actions run [31353596034](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/31353596034)의 기준 커밋
`6457c9eb1d7515f28a09135b041aa7f4b2713bae`에서 85.02% 미만으로 측정된 12개 모듈의 실제 production path를
테스트하고, 저장소와 동일한 Kover method-level Instruction Coverage 집계 기준으로 각 모듈을 85.02% 이상으로
올린다. 전체 aggregate도 기준선 85.02% 이상을 유지한다.

## 기준선과 대상

집계는 `.github/scripts/aggregate-kover-coverage.py`를 사용한다. 이 스크립트가 중복 XML report를 method union으로
병합하므로 모듈별 covered/missed instruction 수와 aggregate 수치를 모두 같은 스크립트로 기록한다.

| 모듈 | Gradle project | 기준 coverage | 목표 |
|---|---|---:|---:|
| `bluetape4k/core` | `:bluetape4k-core` | 82.84% | >= 85.02% |
| `cache/cache-redisson` | `:bluetape4k-cache-redisson` | 84.45% | >= 85.02% |
| `data/cassandra` | `:bluetape4k-cassandra` | 69.71% | >= 85.02% |
| `data/hibernate` | `:bluetape4k-hibernate` | 76.66% | >= 85.02% |
| `data/jdbc` | `:bluetape4k-jdbc` | 79.14% | >= 85.02% |
| `data/r2dbc` | `:bluetape4k-r2dbc` | 67.54% | >= 85.02% |
| `infra/redisson` | `:bluetape4k-redisson` | 61.10% | >= 85.02% |
| `io/http` | `:bluetape4k-http` | 77.01% | >= 85.02% |
| `io/okio` | `:bluetape4k-okio` | 82.79% | >= 85.02% |
| `io/vertx` | `:bluetape4k-vertx` | 64.70% | >= 85.02% |
| `ktor/resilience4j` | `:bluetape4k-ktor-resilience4j` | 84.52% | >= 85.02% |
| `utils/idgenerators` | `:bluetape4k-idgenerators` | 71.01% | >= 85.02% |

## 측정 경계

`data/r2dbc`, `infra/redisson`, `utils/idgenerators`는 `src/benchmark` source set을 별도로 선언하고 현재 Kover
측정에 포함한다. benchmark 코드는 라이브러리 production API의 품질 지표가 아니며, 저장소 정책
`docs/governance/kover-coverage-policy.md`는 benchmark/generated/test-fixture 코드를 명시적으로 제외하도록
요구한다. 따라서 root Kover 설정에서 이름이 `benchmark`인 source set을 제외하여 기존 정책과 측정 대상을
일치시킨다.

이 경계 보정은 production class를 임의로 제외하거나 threshold를 낮추는 수치 조작이 아니다. `src/benchmark`
내부 코드를 테스트로 실행해 coverage를 인위적으로 올리지 않으며, production class·method에 대한 제외/억제
설정은 추가하지 않는다. QueryDSL generated class처럼 현재 report에 포함된 generated output은 우선 실제 report와
소스 경로를 확인하고, 테스트로 검증할 수 있는 public contract는 테스트한다. 별도 generated-class 제외는 이
이슈 범위에 추가하지 않는다.

## 테스트 설계

기존 테스트 파일과 fixture를 우선 확장하고, 책임이 명확한 미검증 API surface에만 새 테스트 파일을 만든다.

- `bluetape4k/core`, `io/http`, `io/okio`, `io/vertx`: 순수 extension/adapter, 오류·경계·취소 경로를 JUnit 5와
  MockK로 검증한다. coroutine virtual-time은 `runTest`, 실제 I/O와 `testApplication`은 `runSuspendIO` 또는
  repository helper를 사용한다.
- `data/cassandra`, `data/hibernate`, `data/jdbc`, `data/r2dbc`: existing unit/mocking, H2,
  `consumerRuntimeTest`, Testcontainers launcher를 재사용한다. DB·외부 서비스 검증은 모듈 간 순차 실행한다.
- `cache/cache-redisson`, `infra/redisson`: 기존 `RedisServers`/`RedissonServer.Launcher`와 codec/cache fixture를
  재사용하여 cache invalidation, coroutine adapter, codec 오류 경로를 검증한다. raw container를 새로 만들지 않는다.
- `ktor/resilience4j`, `utils/idgenerators`: 현재 Ktor test application과 deterministic generator fixture를
  재사용하여 route/policy overload 및 ID 경계·노드 식별자 경로를 검증한다.

모든 새/수정 Kotlin 테스트는 `$bluetape-kotlin-patterns` 계약을 따른다.

- JUnit 5, MockK, `io.bluetape4k.assertions` assertion과 descriptive backtick test name을 사용한다.
- assertion 블록은 Given/When/Then 구조로 작성하고, 새 예외 검증은 `io.bluetape4k.assertions.assertFailsWith`를
  사용한다.
- touched block의 AssertJ/Kluent/JUnit/kotlin.test assertion은 bluetape4k assertion으로 전환한다.
- production code 변경은 테스트로 드러난 실제 defect가 있을 때만 최소 범위로 허용하며, coverage 수치만을 위한
  production branching이나 no-op 호출은 추가하지 않는다.

## 검증과 완료 조건

각 모듈에 대해 다음 순서로 검증한다.

1. 대상 test task를 `--no-configuration-cache`로 실행한다.
2. 해당 모듈의 `koverXmlReport`를 생성한다.
3. 전체 artifact를 모아 `python3 .github/scripts/aggregate-kover-coverage.py <coverage-root>`로 재집계한다.
4. `git diff --check`, Kotlin diagnostics/compile, detekt 및 필요한 static check를 실행한다.
5. GitHub PR에서는 기존 CI lane, `Coverage Report`, `CI Status`를 live log로 확인한다.

완료 조건은 다음과 같다.

- 12개 대상 모듈 각각 Instruction Coverage >= 85.02%.
- aggregate TOTAL >= 85.02%이며 비대상 모듈의 coverage를 의도치 않게 낮추지 않음.
- 변경된 테스트가 실제 production path를 검증하고, 임의의 class/package exclusion·suppression을 추가하지 않음.
- PR/issue에 모듈별 전후 covered/missed instruction 수, benchmark 경계 보정 근거, 실행 명령과 결과를 기록.
- review 문서에 P0/P1 발견 사항과 Kotlin checklist KT-01..KT-FIN 결과를 기록.

## 위험과 되돌리기

- Kover DSL의 source-set exclusion이 현재 Gradle/Kover 조합에서 동작하지 않으면 설정 변경을 되돌리고
  baseline을 보존한 채 원인을 기록한다.
- Testcontainers 또는 외부 DB가 불안정하면 해당 모듈을 단독·순차 재실행하며 다른 모듈과 병렬화하지 않는다.
- 목표에 도달하지 못한 모듈은 미검증 class/method와 남은 instruction을 기록하고, 수치만을 위한 제외 설정으로
  우회하지 않는다.
- 모든 변경은 issue 전용 worktree와 branch에 한정하며, 기준 `develop` worktree와 기존 detached worktree는
  수정하지 않는다.
