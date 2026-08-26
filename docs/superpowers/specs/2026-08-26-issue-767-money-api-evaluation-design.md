# Issue #767 bluetape 소유 Money API 평가 설계

## 1. 문서 상태와 결정

- 대상 이슈: [#767 bluetape 소유 money API 도입 및 Moneta를 호환성 계층으로 전환](https://github.com/bluetape4k/bluetape4k-projects/issues/767)
- 상위 Epic: [#1423 재사용 가능한 플랫폼 API 평가](https://github.com/bluetape4k/bluetape4k-projects/issues/1423)
- 기준 commit: `origin/develop@552d9921520492033ad650743e8696e6352402c2`
- 분류: Type-A 공개 API·아키텍처 평가
- 승인된 결정: **구현을 보류하고 현재 JSR-354/Moneta 공개 API를 유지한다.**

현재 근거에는 bluetape 소유 Money 타입을 요구하는 독립 production consumer가 없다.
`io/protobuf`는 first-party 변환 경계이지만 독립 금융 도메인 소비자가 아니며,
Exposed JDBC/R2DBC 예제는 모두 test source다. 공개 타입을 추가하거나 교체하면 두
API 계층을 장기간 함께 유지하면서도 어떤 consumer 문제를 해결하는지 검증할 수 없다.

따라서 이번 train은 production code, dependency scope, 기존 public signature를
변경하지 않는다. #767에는 아래 다섯 도입 게이트와 재평가 절차를 기록하고 OPEN
상태를 유지한다. 다섯 게이트를 모두 통과하면 별도의 Type-A 구현 설계와 계획을
승인받는다.

## 2. 문제와 목표

`bluetape4k-money`는 편의 함수의 이름은 소유하지만 핵심 타입과 동작은 JSR-354와
Moneta에 맡긴다.

- `javax.money`와 Moneta가 `api` dependency라서 consumer compile classpath에
  노출된다.
- `moneyOf`, `toMoney`, `fastMoneyOf`가 Moneta concrete type을 반환한다.
- 산술, 반올림, 통화 조회와 환전 extension이 `MonetaryAmount`, `CurrencyUnit`,
  `MonetaryConversions`를 공개 계약으로 사용한다.
- 환전 편의 객체는 `ECB`, `IMF` provider 이름과 예외를 호출자에게 그대로 전달한다.
- Protobuf 변환은 Moneta `Money`에 직접 결합한다.

#767의 장기 목표인 API 소유권은 타당한 후보지만, 소유권 자체가 새 타입 도입의
충분한 근거는 아니다. 이번 설계의 목표는 구현 전에 다음 질문을 검증 가능한
게이트로 바꾸는 것이다.

1. 어떤 독립 consumer가 동일한 Money 의미를 요구하는가?
2. bluetape가 amount, currency, rounding, serialization, provider failure 중 어디까지
   소유해야 하는가?
3. 기존 JSR-354/Moneta consumer를 source·binary·dependency 측면에서 어떻게
   이동시키는가?
4. wrapper가 추가하는 비용과 오용 방지 효과를 무엇으로 비교하는가?

## 3. 현재 근거

### 3.1 프로젝트 공개 경계

| 경계 | 현재 상태 | 설계 영향 |
| --- | --- | --- |
| `utils/money/build.gradle.kts` | `javax.money`와 Moneta를 모두 `api`로 선언 | dependency scope 변경도 public compatibility 작업이다. |
| `MoneySupport.kt` | Moneta `Money`를 생성·반환 | 반환 타입 교체는 source·binary breaking change다. |
| `FastMoneySupport.kt` | Moneta `FastMoney`와 minor-unit factory를 공개 | owned type이 precision·scale 의미를 다시 정의해야 한다. |
| `MoneyAmountSupport.kt` | JSR-354 타입의 산술·rounding·conversion extension 공개 | 새 API와 기존 extension의 중복 및 변환 규칙 불일치가 생길 수 있다. |
| `CurrencyConverter.kt` | `ECB`, `IMF` provider를 직접 조회 | 순수 value 연산과 provider IO·cache·failure를 분리하지 못한다. |
| `io/protobuf/MoneySupport.kt` | Protobuf `Money`와 Moneta `Money`를 직접 변환 | future owned type adapter의 first-party migration 경계다. |

현재 baseline에서 다음 명령은 257개 테스트와 함께 통과했다.

```bash
./gradlew :bluetape4k-money:test :bluetape4k-protobuf:test --no-configuration-cache
```

### 3.2 consumer 근거

| 후보 consumer | 분류 | 판정 |
| --- | --- | --- |
| `io/protobuf` | production source의 first-party serialization bridge | integration proof 1개지만 독립 business consumer로 계산하지 않는다. |
| `exposed-workshop` JDBC Money 예제 | `testImplementation`, test source의 persistence-shaped example | amount/currency/scale 저장 계약의 참고 자료이며 production adoption proof가 아니다. |
| `exposed-r2dbc-workshop` Money 예제 | test source의 R2DBC persistence-shaped example | JDBC와 같은 teaching series이므로 두 번째 독립 consumer가 아니다. |
| `bluetape4k-workshop` | catalog alias와 locale 기본값만 존재 | 실제 Money API 호출이 없어 consumer가 아니다. |
| `bluetape-go/money` | 다른 언어 생태계의 owned wrapper | API 소유권과 순수/provider 분리 원칙은 참고하되 JVM compatibility proof로 사용하지 않는다. |

엄격한 독립 production consumer 수는 `0`이다. `bluetape-go`는 decimal 문자열,
minor unit, invalid sentinel, half-even rounding, stable serialization, 순수 환율과
provider-backed 환율의 분리를 보여 준다. 그러나 Go의 error·zero-value·dependency
모델을 Kotlin/JVM API에 그대로 이식하지 않는다.

### 3.3 빠진 증거

- 기존 공개 signature와 새 후보 API를 함께 compile하는 compatibility fixture
- source·binary migration diff와 downstream dependency graph 비교
- Protobuf 및 JDBC/R2DBC persistence의 owned type 왕복 contract
- 현재 `Money`, `FastMoney`, 후보 wrapper의 동일 workload benchmark
- 환율 provider의 freshness, stale fallback, cancellation, deadline, retry 의미
- 두 독립 production domain이 공유하는 최소 Money semantic contract
- Protobuf `units`/`nanos`의 음수·carry·overflow·rounding을 exact decimal로
  보존하는 fixture
- 기본 locale currency와 no-currency sentinel을 포함한 기존 overload compatibility
  fixture

## 4. 설계 제약과 비범위

### 4.1 제약

- 이번 slice는 문서와 설계 근거만 변경한다.
- 현재 JSR-354/Moneta API, dependency scope, serialization, provider 동작을 보존한다.
- Moneta는 보안상 긴급 제거 대상으로 취급하지 않는다.
- `bluetape-go` 구현은 용어와 경계의 참고 자료이며 JVM API 명세가 아니다.
- release, merge, branch 삭제, downstream consumer migration은 별도 승인 대상이다.

### 4.2 비범위

- `Money`, `Currency`, `ExchangeRate`, provider interface의 production 구현
- 기존 API의 deprecation 또는 `api` dependency 축소
- 회계, 원장, 세금, 거래 환율, 결제 달력, 관할권별 rounding policy
- ECB/IMF client, cache, retry 또는 network stack 구현
- 새로운 module이나 외부 dependency 추가

## 5. 대안과 선택

### 대안 A — evidence gate를 유지하고 구현 보류 (선택)

현재 API를 유지하고 도입 조건, compatibility 범위, benchmark와 consumer proof를
먼저 정의한다. 검증되지 않은 중복 API를 만들지 않고 public surface를 안정적으로
유지한다. 독립 consumer가 생기기 전까지 #767을 OPEN 상태로 둔다.

### 대안 B — additive owned facade 추가

기존 JSR-354/Moneta API 옆에 bluetape 소유 타입을 추가한다. 즉시 호환성 파괴는
피하지만 두 arithmetic·rounding·serialization·conversion 계층을 동시에 유지해야
한다. 어떤 API를 기본값으로 삼을지 검증할 production caller가 없으므로 선택하지
않는다.

### 대안 C — owned type으로 즉시 전환

owned type과 adapter를 만들고 기존 반환 API를 deprecate하며 dependency scope를
축소한다. 목표 구조에는 가장 빨리 도달하지만 현재 반환 타입, compile classpath,
Protobuf bridge를 한꺼번에 바꾼다. ABI fixture와 migration proof가 없으므로
선택하지 않는다.

## 6. 선택한 재평가 계약

다음 다섯 게이트가 모두 PASS일 때만 production 구현 설계를 시작한다.

| Gate | PASS 조건 | 현재 판정 |
| --- | --- | --- |
| G1 — 독립 consumer | 서로 다른 두 production domain이 같은 Money contract를 실제 호출 경로에서 요구 | **FAIL** — 독립 consumer 0개 |
| G2 — semantic contract | currency validation, deterministic constructor, arithmetic, comparison, rounding, overflow, stable JSON/text shape, default currency/no-currency 동작과 exchange failure가 공통 fixture로 고정됨 | **FAIL** — 현재 의미는 upstream 구현과 예제에 분산됨 |
| G3 — compatibility·migration | 기존과 후보 API의 source compile, binary surface, target dependency direction, staged deprecation, migration checkpoint와 rollback이 이전 artifact까지 재현됨 | **FAIL** — dedicated fixture 없음 |
| G4 — adapter·persistence | Protobuf와 JDBC/R2DBC amount·currency·null·scale 왕복이 owned type 후보에서도 exact decimal로 보존되고 malformed/overflow 입력이 안정적으로 실패함 | **PARTIAL** — Moneta 기준 예제와 제한된 Protobuf 왕복만 있음 |
| G5 — 성능·안정성 | 고정 value/adapter workload에서 현재 `Money`/`FastMoney`와 후보 wrapper를 비교하고, 별도 provider fixture가 failure/lifecycle/운영 계약을 검증 | **FAIL** — 전용 benchmark와 provider contract 없음 |

재평가 시에는 G1을 먼저 검증한다. G1이 FAIL이면 다른 게이트를 구현 목적으로
확장하지 않는다. consumer proof 없이 compatibility layer와 benchmark harness부터
만들면 실제 caller 요구가 아닌 추상 API를 최적화하게 된다.

### 6.1 G2·G4 compatibility fixture

공통 fixture는 다음 입력과 결과를 기존 API와 후보 API에 같은 방식으로 적용한다.

- `KRW`, `USD`, `EUR`, `CNY`, `JPY`, invalid code, 빈 code, no-currency
  sentinel과 default locale currency
- decimal string, major unit, minor unit, 음수, 0, 최대/최소 경계, overflow와
  floating-point 특수값
- 같은 currency 산술·비교·합계와 다른 currency 거부
- currency scale 및 explicit scale rounding 결과
- JSON/text shape와 Protobuf `units`/`nanos`의 양방향 exact amount
- Protobuf의 음수, `±1` nanos, carry/borrow, 9자리 scale, overflow, invalid nanos와
  unknown/empty currency
- JDBC/R2DBC의 amount·currency 동시 null, `DECIMAL` scale, 검색 및 왕복

G2는 외부 입력이 cache key나 provider query가 되기 전에 검증하는 contract를
포함한다. Currency code의 허용 형식, decimal 최대 길이·scale·지수, serialized
message와 provider response 크기 상한을 승인된 수치로 고정하고 빈 값, no-currency,
NaN/Infinity, 초과 입력을 deterministic typed error로 거부한다. Fuzz와 bounded
adversarial-load fixture가 이 순서를 검증해야 G2가 PASS다.

Protobuf assertion은 currency와 units만 비교하지 않는다. 원본과 복원된 exact decimal,
`units`, `nanos`를 직접 비교한다. 현재 구현의 `nanos.toDouble()`, `toLong()`,
`HALF_UP` 동작은 compatibility 기준으로 먼저 기록한다. 다만 범위를 벗어난 amount,
`units`, `nanos`의 silent truncation/wrap은 보존 대상이 아니다. 후보 adapter는
BigDecimal·`units`·`nanos` 경계를 exact arithmetic으로 검사하고 typed overflow
error로 fail-closed 해야 G4가 PASS다. 정상 범위의 rounding 차이만 preserve, fix,
intentional break 중 하나를 별도 설계에서 선택한다.

### 6.2 G5 benchmark contract

Benchmark는 pure value와 adapter 경로만 JMH로 비교한다. 생성(decimal/major/minor),
동일 currency add/subtract/compare, multiply/divide, currency-scale rounding,
same-currency sum/aggregate, JSON/text serialization, Protobuf conversion을 고정
workload로 둔다. Same-currency aggregate는 provider 호출이 0회여야 한다. 교차 통화
aggregate와 provider conversion은 별도 fixture에서 provider 호출 횟수, latency와
allocation을 측정한다. 입력은 통화,
금액 크기, 소수 자릿수, 정상/overflow 비율을 고정한 fixture에서 가져온다.

각 후보는 같은 JDK, warmup, fork, measurement iteration, heap 조건에서
throughput 또는 average time과 JMH GC profiler의 `gc.alloc.rate.norm`을 기록한다.
첫 구현 계획은 baseline과 허용
회귀율을 수치로 승인받아야 하며, 수치가 없는 “충분히 빠름” 판정은 G5 PASS가
아니다. 현재 문서는 consumer와 후보 구현이 없으므로 허용 회귀율을 임의로 정하지
않고 G5를 FAIL로 유지한다.

Provider 경로는 pure benchmark에 섞지 않는다. Warm cached lookup, cold provider
resolution, refresh failure, cancellation/deadline을 controllable fake clock과 fake
provider로 분리한다. 실제 network 결과는 기능 acceptance나 capacity 보장으로
사용하지 않는다.

### 6.3 G3·G5 migration 및 운영 contract

G3 증거에는 old/new source compile fixture, published API surface, resolved dependency
graph, 첫 번째·두 번째 consumer migration checkpoint가 포함된다. Rollback owner와
trigger, 이전 artifact/default로 복귀하는 명령, Protobuf/DB persisted data 처리,
rollback 뒤 compatibility test를 함께 기록한다.

Target graph의 invariant는 미리 고정한다. Owned core의 public signature와 `api`
dependency에는 JSR-354, Moneta, Protobuf가 없어야 한다. 별도 compatibility adapter만
owned core와 JSR-354/Moneta에 의존할 수 있고, Protobuf adapter는 owned core를 향해
의존한다. Core가 compatibility/Protobuf module을 역참조하거나 adapter 사이에 cycle이
생기면 G3는 FAIL이다. Compile classpath dependency 목록과 module-cycle 검사가 이를
증명한다.

각 checkpoint는 적용 artifact/version, 대상 consumer, 승인자, canary 또는 hold
조건, abort signal과 read-back 결과를 남긴다. 일부 consumer만 이동한 상태에서는
old/new wire·storage shape를 동시에 읽을 수 있는지 증명하거나 migration을 중단한다.
부분 migration 복구와 이전 artifact 재배포가 검증되지 않으면 G3는 FAIL이다.

Provider 설계는 owner와 close/invalidation 여부, caller deadline과 cancellation 전파,
retry 대상과 idempotency, stale fallback과 fail-closed 선택, typed result/error,
low-cardinality metrics와 secret-free logs를 fixture로 고정한다. 이 항목이 정해지지
않으면 G5는 FAIL이다.

첫 provider plan은 component owner, refresh cadence/TTL/max-stale, provider fallback,
retry/backoff, cache invalidation, caller deadline 정책을 명시한다. Metrics는 provider,
result class, freshness bucket처럼 제한된 차원만 사용하고 raw amount, currency pair의
무제한 조합, endpoint, credential과 response body를 기록하지 않는다. Freshness/stale
alert threshold, health/readiness 영향, SLO와 incident/runbook 조치를 함께 승인받는다.
정책별 fake-clock test, metric/log assertion과 release evidence가 없으면 G5는 FAIL이다.

외부 provider adapter는 허용 endpoint와 scheme, TLS verification, redirect 정책,
response body 크기, currency/rate/precision 범위와 clock skew를 명시한다. Malformed,
oversized, 범위 밖 rate, timeout, max-stale 초과 응답은 fail-closed 하며 credential,
endpoint query와 raw response를 log에 남기지 않는다. 이 transport·parser 부정
fixture가 없으면 G5는 FAIL이다.

### 6.4 G1 통과 뒤 작성할 candidate API manifest

G1이 FAIL인 동안에는 구체 타입명과 signature를 확정하지 않는다. G1이 PASS하면
구현 계획 전에 candidate API manifest를 작성하고 G2·G3 증거로 검토한다. Manifest는
Kotlin과 Java 호출 양쪽에서 다음 항목을 고정한다.

- value type, factory, nullability, default argument와 Java-visible overload
- arithmetic/operator 반환 타입, equality/hash, rounding, overflow와 serialization
- legacy `Number`/`CurrencyUnit`/default currency/`fractionDigits` overload 대응표
- owned core, JSR-354/Moneta compatibility adapter와 Protobuf adapter의 양방향 변환
- invalid input, currency mismatch, overflow, provider unavailable/stale/cancelled/deadline의
  caller-visible result/error taxonomy, stable code/field와 retryability
- 기존 Moneta exception을 compatibility adapter의 stable result/error로 mapping하는
  fixture

Construction, arithmetic, rounding, JSON/text/Protobuf, unsupported input와 provider
failure의 old/new caller example을 Kotlin과 Java로 제공한다. G1 전에 예시 signature를
작성하면 실제 consumer 요구보다 API를 먼저 고정하므로 이번 문서에는 넣지 않는다.

## 7. 향후 API 경계 원칙

이 절은 현재 구현 약속이 아니라 다섯 게이트 통과 후 설계가 지켜야 할 최소
경계다.

1. **순수 value 경로와 provider 경로를 분리한다.** Caller-supplied 환율 변환은
   network·cache IO를 수행하지 않는다. Provider-backed 변환은 source, 관측 시각,
   freshness, stale 여부와 refresh failure를 반환한다.
2. **통화 불일치를 암묵 변환하지 않는다.** Add/subtract/compare/aggregate는 같은
   currency를 요구하고, conversion은 명시적인 별도 연산이다.
3. **입력과 rounding을 명시한다.** Decimal string과 major/minor unit 경로를
   구분하며 floating-point 입력과 통화 scale rounding의 손실을 숨기지 않는다.
   미래 owned API는 currency를 명시적으로 요구한다. JVM default locale convenience는
   기존 compatibility API에만 남기고 새 core constructor의 기본값으로 사용하지 않는다.
4. **serialization을 안정된 contract로 다룬다.** JSON, text, Protobuf, DB
   amount/currency mapping은 각각 compatibility fixture와 migration note를 갖는다.
5. **upstream interop은 adapter 경계에 둔다.** JSR-354/Moneta 변환은 core value
   계약과 분리하며 초기 migration에서는 양방향 호환성을 제공한다.
6. **dependency는 core를 향하는 단방향으로 유지한다.** Compatibility와 Protobuf
   adapter는 owned core에 의존하고, core는 adapter나 upstream Money API를 공개하지
   않는다.

## 8. 실패 모드와 대응

| 실패 모드 | 탐지 | 대응 |
| --- | --- | --- |
| consumer 없이 facade를 추가해 두 API 의미가 불일치 | G1 consumer inventory와 API fixture 부재 | 구현을 보류하고 실제 consumer 요구부터 수집 |
| 반환 타입·dependency scope 변경으로 downstream source/binary가 깨짐 | G3 compile fixture, API surface와 dependency graph 비교 | additive migration, deprecation 기간과 rollback을 별도 설계 |
| Protobuf 또는 DB 왕복에서 scale·rounding·null 의미가 달라짐 | G4 golden/round-trip fixture | adapter별 wire/storage contract를 먼저 고정 |
| value 연산이 provider IO·cache·stale fallback을 숨김 | 순수 변환 test와 provider failure test 분리 | value와 provider API를 별도 경계로 유지 |
| locale 기본 통화나 no-currency 값이 암묵적으로 허용됨 | locale/invalid code matrix | 명시 currency를 기본으로 하고 convenience 동작을 별도 계약화 |
| wrapper가 allocation·latency를 늘리거나 FastMoney 정밀도 문제를 재현 | G5 동일 workload benchmark와 precision fixture | 측정 결과에 따라 wrapper shape를 수정하거나 도입을 기각 |

## 9. 호환성·마이그레이션

이번 결정은 production code를 변경하지 않으므로 source, binary, runtime,
serialization compatibility에 변화가 없다. 향후 구현 제안은 다음 순서를 기본으로
하되 별도 승인을 받아야 한다.

1. owned type 후보와 compatibility fixture를 application-local 또는 test-support에
   둔다.
2. 기존 JSR-354/Moneta API를 유지한 채 양방향 adapter와 first consumer migration을
   검증한다.
3. 두 번째 독립 consumer와 Protobuf/persistence migration을 검증한다.
4. API surface·dependency graph·benchmark를 비교한 뒤에만 기본 API와 deprecation을
   결정한다.
5. 각 checkpoint에서 old/new API·dependency 기준 기록을 보존한다. Migration owner는
   source/binary/serialization 검증 실패나 승인된 성능 회귀율 초과를 rollback
   trigger로 사용한다. Provider가 포함된 slice는 SLO 위반, max-stale 초과,
   timeout/error-rate 임계치 초과와 readiness 저하에서도 rollout을 hold하고
   rollback한다.
6. 이전 artifact/default로 복귀하고 Protobuf/DB persisted data를 읽은 뒤 기존
   G2/G4/G5 fixture를 재실행한다. 이 복구가 재현되기 전에는 deprecation을 진행하지
   않는다.
7. rollback 명령, 지원 기간과 deprecation 일정을 release note에 기록한다.

## 10. 수용 기준과 DoD

- [x] live #767/#1423, 현재 source, tests, first-party integration을 확인했다.
- [x] 독립 production consumer와 test/example consumer를 구분했다.
- [x] 구현 보류, additive facade, 즉시 전환의 세 대안을 비교했다.
- [x] 구현 재개를 위한 다섯 adoption gate와 현재 판정을 기록했다.
- [x] compatibility, serialization, provider, persistence, performance 실패 모드를
  기록했다.
- [x] baseline Money·Protobuf 테스트 257개가 통과했다.
- [x] production code, dependency scope, public API와 GitHub metadata를 변경하지 않았다.
- [x] future owned core와 compatibility/Protobuf adapter의 target dependency 방향을
  기록했다.
- [x] 여섯 독립 review perspective와 main-session integration에서 P0=0/P1=0이다.
- [ ] Candidate API 선택 뒤 KDoc/README/API docs의 `Long`/`Number`, overflow,
  conversion 설명을 source signature와 일치시킨다 — G1 PASS 뒤 G3에서 완료한다.
- [ ] #1423/#767 progress metadata와 평가 PR — 승인된 implementation/evaluation plan
  이후에만 변경한다.

## 11. Step 2-R review 통합

여섯 perspective는 최신 문서를 대상으로 독립 prompt lane에서 검토했다. 초기
finding은 문서에 반영하고 영향받은 lane만 다시 실행했다.

| Lens | 통합한 핵심 보완 | 최신 P0/P1/P2/P3 | 판정 |
| --- | --- | --- | --- |
| Performance | 고정 JMH workload, GC allocation, exact Protobuf, pure/provider 및 aggregate 분리 | `0/0/0/0` | PASS |
| Stability | provider lifecycle, exact persistence, rollback과 default-currency compatibility | `0/0/0/0` | PASS |
| Security | fail-closed overflow, bounded input, provider transport/response trust와 log redaction | `0/0/0/0` | PASS |
| Operator/Ops | owner, metrics/SLO/runbook, rollout checkpoint, partial migration과 rollback trigger | `0/0/0/0` | PASS |
| Developer/API | core→adapter dependency 방향, Kotlin/Java manifest, module cycle과 docs parity | `0/0/0/0` | PASS |
| User/caller | stable error/result, legacy overload matrix, migration example과 명확한 G1 stop condition | `0/0/0/0` | PASS |

Main-session integration은 중복 finding을 G2–G5 fixture, migration/operation contract,
candidate manifest로 합쳤다. 구체 API signature와 수치 threshold는 consumer evidence가
없는 상태에서 임의로 정하지 않고 G1 PASS 뒤 별도 승인 대상으로 남겼다. 이는
누락이 아니라 premature API fixation을 막는 의도적인 defer다. 최신 통합 결과는
P0=0, P1=0, unresolved blocker=0이다.

## 12. 승인 및 다음 게이트

사용자는 2026-08-26에 대안 A를 승인했다. 이 승인은 설계 기록과 독립 검토를
허가하지만 production 구현, PR merge, issue closure 또는 cleanup 권한은 포함하지
않는다.

Step 2-R review가 수렴하면 이 결정을 실행 가능한 문서·GitHub progress plan으로
변환한다. Plan 승인 전에는 #1423 본문, #767 milestone, remote branch와 PR을
변경하지 않는다.
