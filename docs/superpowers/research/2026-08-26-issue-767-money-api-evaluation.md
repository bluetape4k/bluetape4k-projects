# Issue #767 Money API 평가 기록

## 결정

**owned Money API 구현을 보류하고, 현재 JSR-354/Moneta public API를 유지한다.**

현재 저장소에는 서로 독립된 production consumer가 없고, semantic contract,
source/binary compatibility, persistence/serialization, 성능·provider 운영 근거가
모두 충족되지 않았다. 따라서 이번 변경은 production code, dependency, public API,
deprecation을 추가하거나 변경하지 않는다.

- 대상 이슈: [#767 bluetape 소유 money API 도입 및 Moneta를 호환성 계층으로 전환](https://github.com/bluetape4k/bluetape4k-projects/issues/767)
- 상위 Epic: [#1423 재사용 가능한 플랫폼 API 평가](https://github.com/bluetape4k/bluetape4k-projects/issues/1423)
- source 기준: `origin/develop@552d9921520492033ad650743e8696e6352402c2`
- 분류: Type A 공개 API·아키텍처 평가
- production 구현 재개 조건: G1부터 G5까지 모두 `PASS`하고, 별도 Type A 설계·계획을 승인받아 새 train을 시작한다.

## 현재 공개 경계

| 근거 | 현재 상태 | 판단 |
| --- | --- | --- |
| `utils/money/build.gradle.kts` | `javax.money`와 Moneta를 모두 `api` dependency로 선언한다. | dependency scope 자체가 consumer compile classpath와 호환성 계약이다. |
| `utils/money/src/main/kotlin/io/bluetape4k/money/MoneySupport.kt` | `moneyOf`, `toMoney`, 통화별 factory가 Moneta `Money`를 반환한다. | owned type 전환은 source·binary breaking change가 될 수 있다. |
| `utils/money/src/main/kotlin/io/bluetape4k/money/FastMoneySupport.kt` | Moneta `FastMoney`와 minor-unit factory를 공개한다. | precision, scale, overflow 의미를 후보 API가 다시 증명해야 한다. |
| `utils/money/src/main/kotlin/io/bluetape4k/money/MoneyAmountSupport.kt` | JSR-354 `MonetaryAmount`, `CurrencyUnit`, rounding과 conversion을 공개 signature로 사용한다. | 새 계층을 병행하면 산술·반올림·변환 의미가 중복된다. |
| `utils/money/src/main/kotlin/io/bluetape4k/money/CurrencyConverter.kt` | `MonetaryConversions`와 `ECB`, `IMF` provider를 직접 사용한다. | 순수 value 계약과 provider IO·cache·failure 계약을 분리할 근거가 필요하다. |
| `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MoneySupport.kt` | Protobuf `Money`와 Moneta `Money`를 직접 변환한다. | first-party adapter 경계지만 독립 business consumer는 아니다. |
| `io/protobuf/build.gradle.kts` | `bluetape4k-money`를 `compileOnly`로 사용한다. | production bridge는 존재하지만 Money module의 독립 production 도입 증거는 아니다. |

현재 공개 API는 편의 함수 이름만 bluetape가 소유하고 핵심 타입과 동작은
JSR-354/Moneta에 의존한다. 이 상태에서 additive facade를 먼저 만들면 해결할 실제
consumer 문제 없이 두 arithmetic·rounding·serialization 계층을 장기간 유지하게
된다. 즉시 전환하면 반환 타입, compile classpath, Protobuf bridge를 한꺼번에 바꾸게
된다. 두 경로 모두 현재 근거로는 정당화되지 않는다.

## Consumer inventory

엄격한 독립 production consumer 수는 **0개**다.

| 후보 | source 분류 | 판정 |
| --- | --- | --- |
| `io/protobuf` | production source의 first-party serialization bridge | integration proof이지만 독립 business consumer로 계산하지 않는다. |
| `exposed-workshop/06-advanced/05-exposed-money/src/test/kotlin/exposed/examples/money/Ex02_Money.kt` | JDBC workshop test source | persistence-shaped example이며 production adoption proof가 아니다. |
| `exposed-workshop/06-advanced/05-exposed-money/src/test/kotlin/exposed/examples/money/MoneyData.kt` | JDBC workshop test fixture | 위 예제의 같은 teaching series다. |
| `exposed-r2dbc-workshop/06-advanced/05-exposed-r2dbc-money/src/test/kotlin/exposed/r2dbc/examples/money/Ex01_MoneyDefaults.kt` | R2DBC workshop test source | default와 null 의미의 참고 자료이며 독립 consumer가 아니다. |
| `exposed-r2dbc-workshop/06-advanced/05-exposed-r2dbc-money/src/test/kotlin/exposed/r2dbc/examples/money/Ex02_Money.kt` | R2DBC workshop test source | JDBC 예제와 같은 teaching series다. |
| `exposed-r2dbc-workshop/06-advanced/05-exposed-r2dbc-money/src/test/kotlin/exposed/r2dbc/examples/money/MoneyData.kt` | R2DBC workshop test fixture | production 호출 경로가 아니다. |

G1이 실패한 동안에는 구체 owned type 이름과 signature를 확정하지 않는다. 실제
consumer 요구보다 API를 먼저 고정하는 것을 막기 위한 의도적인 stop condition이다.

## 대안 평가

| 대안 | 장점 | 비용·위험 | 결정 |
| --- | --- | --- | --- |
| 현재 API 유지 및 evidence gate 운영 | public surface와 runtime 동작을 보존하면서 실제 consumer 요구를 먼저 수집한다. | API 소유권 전환은 뒤로 미뤄진다. | **선택** |
| additive owned facade | 기존 API를 즉시 제거하지 않고 후보 API를 노출할 수 있다. | 검증할 caller 없이 두 의미 계층과 adapter를 함께 유지한다. | 기각 |
| owned type 즉시 전환 | 목표 구조에 가장 빨리 도달한다. | ABI, dependency, serialization, migration 근거 없이 breaking change를 만든다. | 기각 |

## 재평가 게이트

다섯 게이트가 모두 통과하기 전에는 production Money API를 구현하지 않는다.

| Gate | PASS 조건 | 현재 판정 |
| --- | --- | --- |
| G1 — 독립 consumer | 서로 다른 두 production domain이 같은 Money contract를 실제 호출 경로에서 요구한다. | **FAIL** — 독립 production consumer 0개 |
| G2 — semantic contract | currency validation, constructor, arithmetic, comparison, rounding, overflow, stable JSON/text shape, default/no-currency와 exchange failure가 공통 fixture로 고정된다. | **FAIL** — 의미가 upstream 구현과 예제에 분산됨 |
| G3 — compatibility·migration | old/new source compile, binary surface, dependency direction, staged deprecation, checkpoint와 rollback이 이전 artifact까지 재현된다. | **FAIL** — 전용 fixture 없음 |
| G4 — adapter·persistence | Protobuf와 JDBC/R2DBC amount·currency·null·scale 왕복이 exact decimal로 보존되고 malformed/overflow 입력이 안정적으로 실패한다. | **PARTIAL** — Moneta 기준 예제와 제한된 Protobuf 왕복만 있음 |
| G5 — 성능·안정성 | 동일 workload에서 현재 `Money`/`FastMoney`와 후보를 비교하고 provider failure·lifecycle·운영 계약을 fixture로 검증한다. | **FAIL** — benchmark와 provider contract 없음 |

재평가는 G1부터 시작한다. G1이 `FAIL`이면 compatibility layer, benchmark harness,
candidate API manifest를 구현 목적으로 확장하지 않는다. G1부터 G5까지 모두
`PASS`한 뒤에도 이 문서만으로 구현 권한이 생기지 않는다. 별도 Type A 설계·계획,
범위, 테스트, migration·rollback 계약을 승인받아 production train을 새로 시작한다.

## 향후 계약이 증명해야 할 항목

### Semantic과 adapter

- 유효·무효 currency, decimal·major·minor unit, 음수·0·경계·overflow를 포함한다.
- 다른 currency의 암묵 산술을 금지하고 conversion을 명시적 경계로 둔다.
- JSON/text, Protobuf `units`/`nanos`, JDBC/R2DBC amount·currency·null·scale를 exact decimal로 비교한다.
- pure value 연산과 provider-backed conversion을 분리한다.
- bounded input, typed error, cancellation/deadline, stale fallback 또는 fail-closed 정책을 고정한다.

### Compatibility와 migration

- 기존과 후보 API의 Kotlin·Java source compile 및 binary surface를 비교한다.
- owned core의 public signature와 `api` dependency에는 JSR-354, Moneta, Protobuf가 없어야 한다.
- compatibility와 Protobuf adapter는 owned core를 향해 의존하며 core는 adapter를 역참조하지 않는다.
- 첫 번째·두 번째 consumer checkpoint, abort signal, 이전 artifact 복귀와 persisted data read-back을 재현한다.

### 성능과 운영

- 같은 JDK, warmup, fork, iteration, heap에서 creation, arithmetic, rounding, aggregate, serialization, Protobuf conversion을 비교한다.
- throughput 또는 average time과 `gc.alloc.rate.norm`을 기록하고 허용 회귀율을 사전에 승인한다.
- provider 경로는 fake clock/provider로 cache freshness, timeout, cancellation, retry, max-stale와 secret-free observability를 검증한다.
- 실제 network 성공은 deterministic acceptance나 capacity 증거로 사용하지 않는다.

## 검증 증거

Fresh baseline은 다음 명령으로 전체 task를 재실행했다.

```bash
./gradlew :bluetape4k-money:test :bluetape4k-protobuf:test \
  --no-configuration-cache --console=plain --rerun-tasks
```

- Gradle: `BUILD SUCCESSFUL in 2m 39s`, 58 actionable tasks 모두 실행
- `repo-test-summary`: 16.8초 동안 실행된 테스트 257개, 실패 0
- JUnit XML 교차 확인: 19 suites, 선언된 test case 303개, skipped 1개, failures 0, errors 0
- 여섯 specification lens: Performance, Stability, Security, Operator/Ops, Developer/API, User/caller 모두 `P0=0`, `P1=0`
- Korean terminology audit: PASS
- `git diff --check`: PASS

`repo-test-summary`의 실행 이벤트 수와 JUnit XML의 선언 test case 수는 parameterized·
dynamic test 보고 단위가 달라 동일한 총계가 아니다. 두 결과 모두 실패 0을 가리키며,
skipped 1개는 XML 기준으로 별도 공개한다.

다음 항목은 이번 문서 train에서 검증하지 않았다.

- 이전 artifact까지 포함한 ABI/source compatibility fixture
- 실제 downstream consumer migration과 rollback
- 실제 환율 provider network, freshness와 장애 운영
- 후보 owned API 구현 및 JMH benchmark

## DoD

### 이번 train에서 완료

- [x] live #1423/#767 관계와 현재 source 공개 경계를 확인했다.
- [x] production bridge와 test/example consumer를 독립 production consumer와 구분했다.
- [x] 독립 production consumer가 0개임을 기준으로 구현 보류 결정을 기록했다.
- [x] 유지, additive facade, 즉시 전환 대안을 비교했다.
- [x] G1부터 G5까지의 PASS 조건과 현재 `FAIL/FAIL/FAIL/PARTIAL/FAIL` 판정을 고정했다.
- [x] fresh Money/Protobuf baseline과 독립 review 결과를 기록했다.
- [x] production code, dependency, public API와 deprecation을 변경하지 않았다.

### 후속 train 전까지 차단

- [ ] 서로 독립된 production consumer 두 곳의 호출 경로와 공통 semantic 요구 확보
- [ ] source·binary·dependency·serialization·persistence compatibility suite
- [ ] 두 consumer migration checkpoint와 rollback 재현
- [ ] 후보 owned API와 현재 `Money`/`FastMoney`의 승인된 JMH 기준 비교
- [ ] provider lifecycle, failure, security, observability와 운영 계약 검증
- [ ] G1부터 G5까지 모두 PASS한 뒤 별도 Type A 설계·계획 승인

최종 상태는 **구현 보류**다. #767은 재평가 계약을 보존하기 위해 OPEN으로 유지한다.
