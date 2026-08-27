# Issue #767 Money API G1 후속 평가

## 결정

**G1을 `PENDING`으로 유지하고 G2부터 G5까지는 기존 판정을 유지한다.**

서로 다른 두 production-shaped domain의 `src/main` 호출 경로가 금액과 통화,
산술, 저장 경계를 요구하므로 G1 consumer 후보 두 곳은 확보됐다.

- `commerce/usage-metering-billing-ledger`: 사용량과 단가를 곱하고 통화별 금액을
  ledger에 저장하며, reconciliation에서 금액과 통화 일치를 다시 검증한다.
- `spring-modulith/ddd-order-audit`: 주문 항목이 단가와 통화를 보유하고 수량을
  곱해 주문 합계를 계산하며, JPA와 JaVers 경계에서 금액과 통화를 보존한다.

그러나 두 모듈은 Workshop 저장소의 example이다. [billing README](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/commerce/usage-metering-billing-ledger/README.md#L1-L7)와
[order audit README](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/spring-modulith/ddd-order-audit/README.md#L1-L7)는
각각 example과 workshop임을 명시한다. Epic #1423의 canonical G1은 “서로 독립된
production consumer 두 곳”이므로, production-shaped runnable application을
production consumer로 인정한다는 합의나 실제 production 사용 근거 없이 `PASS`로
높이지 않는다.

두 consumer가 이미 같은 library type을 사용해야 G1을 통과하는 것은 아니다. 그런
조건은 공통 API 도입 전에 공통 API 채택을 증명해야 하는 순환 조건이 된다. 다만
library 채택과 consumer의 production 자격은 별개다. 전자는 G1 조건으로 요구하지
않고, 후자는 별도 근거로 확인한다.

두 consumer는 별도 Spring Boot application과 dependency graph를 가진 Gradle
module이다. [billing module](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/commerce/usage-metering-billing-ledger/build.gradle.kts#L27-L79)과
[order audit module](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/spring-modulith/ddd-order-audit/build.gradle.kts#L15-L40)은
서로를 production dependency로 참조하지 않는다. 하나의 example series나 동일한
consumer를 두 번 센 것이 아니다.

G1의 consumer 후보 확보는 production 구현 승인이 아니다. 두 domain의 rounding,
currency validation, 음수·0, 합산, serialization과 persistence 계약이 아직
일치하지 않으므로 G2는 `FAIL`이다. G2부터 G5까지 모두 통과하고 별도 Type A
설계·계획을 승인받기 전에는 owned Money API, compatibility layer, deprecation을
구현하지 않는다.

## 확인 범위

- 확인일: `2026-08-27` (Asia/Seoul)
- projects 기준: `origin/develop@5d0c22dab9169821fdaa75c321c2b1d627b2eb41`
- Workshop 기준: `origin/develop@f95ea45c1c053f3901d91d29bca58f4e18fb3bdf`
- 대상: [projects #767](https://github.com/bluetape4k/bluetape4k-projects/issues/767)
- 상위 Epic: [projects #1423](https://github.com/bluetape4k/bluetape4k-projects/issues/1423)
- consumer 후보: [Workshop #800](https://github.com/bluetape4k/bluetape4k-workshop/issues/800)
- 기존 평가: [PR #1532](https://github.com/bluetape4k/bluetape4k-projects/pull/1532)

Workshop #800이 기록한 기준 commit
`79fd71d9a55f6b43ac78ee9bab55f9808931c8b8`부터 현재 Workshop 기준까지 두 대상
모듈의 source diff는 없다. #800은 `OPEN`이고 연결된 구현 PR은 없다. 따라서 이
평가는 consumer demand를 판정할 뿐 `bluetape4k-money` 채택이나 이행 완료를
주장하지 않는다.

## Consumer 1: usage metering과 billing ledger

### 실제 호출 경로

[`BillingCloseService.kt:129-144`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/commerce/usage-metering-billing-ledger/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/application/BillingCloseService.kt#L129-L144)는
사용량과 단가를 곱하고 `MONEY_SCALE`, `RoundingMode.HALF_UP`을 적용한 뒤 amount와
currency를 ledger에 함께 저장한다.

[`ReconciliationService.kt:193-217`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/commerce/usage-metering-billing-ledger/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/application/ReconciliationService.kt#L193-L217)는
같은 계산을 재현하고 price, ledger entry, posting period, service period 사이의
currency 일치를 검증한다. 금액과 통화는 billing correctness와 reconciliation
결과를 결정하는 application contract다.

### 주의할 경계

[`PricingModels.kt:30-51`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/commerce/usage-metering-billing-ledger/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/domain/PricingModels.kt#L30-L51)의
application-local `Money`는 amount 비음수, currency fraction digit 기반
`HALF_UP`, currency mismatch 거부를 정의한다. 그러나 현재 `src/main` source는
이 wrapper를 호출하지 않고 서비스에서 `BigDecimal` 계산을 직접 수행한다.
[`MeteringTypesTest.kt:39-60`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/commerce/usage-metering-billing-ledger/src/test/kotlin/io/bluetape4k/workshop/commerce/metering/domain/MeteringTypesTest.kt#L39-L60)만
wrapper의 USD/KRW rounding과 currency mismatch를 직접 검증한다.

[`BillingCloseService.kt:64-70`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/commerce/usage-metering-billing-ledger/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/application/BillingCloseService.kt#L64-L70)는
batch 처리에서 이 계산·저장 함수를 호출한다. 따라서 이 후보의 근거는 wrapper
채택이 아니라 runnable application service의 금액·통화 호출 경로다. wrapper와
실제 서비스의 scale 정책 차이는 G2에서 해결해야 한다.

## Consumer 2: Spring Modulith order audit

[`OrderDomain.kt:53-92`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/spring-modulith/ddd-order-audit/src/main/kotlin/io/bluetape4k/workshop/spring/modulith/ddd/audit/orders/OrderDomain.kt#L53-L92)는
amount가 0 이상이고 currency가 비어 있지 않은 serializable `Money`를 주문 항목의
단가로 사용한다. command 경계에서는 단가가 양수여야 한다.

[`OrderDomain.kt:267-271`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/spring-modulith/ddd-order-audit/src/main/kotlin/io/bluetape4k/workshop/spring/modulith/ddd/audit/orders/OrderDomain.kt#L267-L271)는
단가에 수량을 곱해 주문 합계를 계산한다.
[`OrderEntity.kt:91-117`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/spring-modulith/ddd-order-audit/src/main/kotlin/io/bluetape4k/workshop/spring/modulith/ddd/audit/orders/OrderEntity.kt#L91-L117)는
amount와 3자리 currency를 별도 JPA column으로 저장하고 domain value로 왕복한다.
[`OrderCommandService.kt:27-38`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/spring-modulith/ddd-order-audit/src/main/kotlin/io/bluetape4k/workshop/spring/modulith/ddd/audit/orders/OrderCommandService.kt#L27-L38)는
aggregate를 JPA에 저장하고 audit 등록을 요청한다.
[`OrderAuditService.kt:61-78`](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/spring-modulith/ddd-order-audit/src/main/kotlin/io/bluetape4k/workshop/spring/modulith/ddd/audit/orders/OrderAuditService.kt#L61-L78)는
commit 뒤 JaVers 이력을 기록한다. Money는 aggregate, persistence와 audit
경계를 통과하는 runnable application contract다.

이 consumer는 currency별 rounding이나 서로 다른 currency 합산 거부를 정의하지
않는다. billing consumer와 공통 요구는 확인했지만 세부 의미는 아직 같지 않다.

## 공통 요구와 의미 차이

| 계약 | billing ledger | order audit | G1/G2 판단 |
| --- | --- | --- | --- |
| decimal amount와 currency code/value | ledger column과 reconciliation에서 함께 사용 | aggregate와 JPA column에서 함께 사용 | G1 후보 공통 요구 |
| amount validation | 금액 비음수, 단가 양수 | 금액 비음수, command 단가 양수 | G1 후보 공통 요구 |
| 산술 | 사용량 × 단가, ledger 합산 | 수량 × 단가, 주문 합산 | G1 후보 공통 요구 |
| currency validation | `java.util.Currency`와 일치 검사 | nonblank `String`만 검사 | G2 미결정 |
| rounding | production은 고정 scale `HALF_UP`, wrapper는 currency fraction digit | 명시적 규칙 없음 | G2 미결정 |
| currency mismatch | reconciliation과 wrapper에서 거부 | 주문 합산에서 검증하지 않음 | G2 미결정 |
| persistence | Exposed amount/currency column과 digest | JPA amount/currency column, Java serialization, JaVers | G4 미완료 |

두 consumer는 동일한 최소 value boundary를 요구한다. 반면 rounding, currency
mismatch, serialization과 exact persistence 의미는 다르다. G1과 G2를 분리해야
consumer 후보의 공통 수요를 인정하면서도 불완전한 의미 계약을 public API로
고정하지 않는다.

## 현재 bluetape4k-money와의 차이

[`MoneySupport.kt:18-57`](https://github.com/bluetape4k/bluetape4k-projects/blob/5d0c22dab9169821fdaa75c321c2b1d627b2eb41/utils/money/src/main/kotlin/io/bluetape4k/money/MoneySupport.kt#L18-L57)는
`moneyOf`와 `toMoney`에서 Moneta `Money`를 반환한다.
[`CurrencySupport.kt:80-106`](https://github.com/bluetape4k/bluetape4k-projects/blob/5d0c22dab9169821fdaa75c321c2b1d627b2eb41/utils/money/src/main/kotlin/io/bluetape4k/money/CurrencySupport.kt#L80-L106)는
`currencyUnitOf`를 제공한다.
[`MoneyAmountSupport.kt:234-251`](https://github.com/bluetape4k/bluetape4k-projects/blob/5d0c22dab9169821fdaa75c321c2b1d627b2eb41/utils/money/src/main/kotlin/io/bluetape4k/money/MoneyAmountSupport.kt#L234-L251)는
currency rounding과 default rounding을 별도 helper로 제공한다.

현재 API가 두 consumer의 공통 생성·통화 lookup·산술 일부를 제공할 수는 있다.
그러나 다음 항목은 G2 이후에만 결정한다.

- nonnegative amount와 positive unit price를 owned value invariant로 둘지 domain
  policy로 둘지;
- billing의 고정 scale과 currency fraction digit 중 어느 규칙을 기본값으로 둘지;
- order 합산에서 currency mismatch를 언제 거부할지;
- JPA, Exposed, JSON, Java serialization과 JaVers에서 old/new shape를 어떻게
  왕복하고 rollback할지.

## G1-G5 재판정

| Gate | 판정 | 근거와 다음 조건 |
| --- | --- | --- |
| G1 — 독립 consumer | **PENDING** | 서로 독립된 runnable `src/main` consumer 후보 두 곳은 확인했으나 둘 다 Workshop example이다. production consumer 인정 규칙 또는 실제 production 사용 근거가 필요하다. |
| G2 — semantic contract | **FAIL** | scale, rounding, currency mismatch, serialization과 domain policy 경계가 일치하지 않는다. |
| G3 — compatibility·migration | **FAIL** | source/binary/dependency compatibility와 두 consumer checkpoint·rollback fixture가 없다. |
| G4 — adapter·persistence | **PARTIAL** | 기존 JPA/Exposed/Protobuf 경계는 있으나 owned type 기준 exact round-trip과 malformed/overflow fixture가 없다. |
| G5 — 성능·안정성 | **FAIL** | 후보 API benchmark와 provider lifecycle·failure 계약이 없다. |

다음 게이트는 여전히 G1이다. 실제 production 사용 근거를 확보하거나, 서로 독립된
runnable Workshop `src/main` application을 production consumer로 인정하는 판정 규칙을
#767/#1423에 명시적으로 승인해야 한다. G1 통과 뒤에는 기존 설계 계약에 따라
candidate API manifest를 작성하되 구현하거나 public signature로 확정하지 않는다.
이 manifest와 공통 fixture는 currency validation, deterministic construction,
arithmetic·comparison, 음수·0, default/no-currency, scale·rounding, mismatch, 합산,
overflow, stable JSON/text, exchange failure와 bounded input/error를 함께 고정해야
G2·G3 증거가 된다.

## 대안과 선택

| 대안 | 장점 | 위험 | 결정 |
| --- | --- | --- | --- |
| G1 PENDING, production 자격 근거 확보 | runnable consumer 후보의 domain demand를 보존하면서 canonical gate를 임의로 낮추지 않는다. | G1 판정 근거가 추가로 필요하다. | **선택** |
| library 채택 전까지 G1 FAIL 유지 | 이미 구현된 library consumer만 센다. | G1을 adoption gate로 바꿔 순환 조건을 만든다. | 기각 |
| owned API 즉시 구현 | 두 consumer migration을 바로 시작할 수 있다. | G2-G5와 별도 Type A 승인 없이 public API를 고정한다. | 기각 |

## 검증 증거

- live GitHub: #1423, #767, Workshop #800은 `OPEN`; PR #1532는 `MERGED`;
  #800을 연결한 구현 PR은 0개다.
- native sub-issue: #767은 #1423의 child다.
- source inventory: 두 Workshop module의 `src/main` Money 관련 호출과 저장 경로를
  `git grep`과 `git show`로 current `origin/develop`에서 확인했다.
- source stability: Workshop #800 기준 commit부터 현재 commit까지 두 대상 모듈
  source diff가 없다.
- production boundary: Kotlin 운영 source, dependency, public API와 build 설정을
  변경하지 않는다.

## DoD Status

- [x] 두 production-shaped domain과 실제 `src/main` 금액 호출 경로를 확인했다.
- [x] test-only wrapper와 `src/main` service 계산을 구분했다.
- [x] G1 consumer 후보 두 곳의 source 근거와 production 자격의 미확정 경계를 기록했다.
- [x] G2의 rounding, mismatch, validation, serialization 미결정 범위를 기록했다.
- [x] G3-G5와 별도 Type A 승인 전 production 구현 차단을 유지했다.
- [ ] G1 production consumer 인정 규칙 또는 실제 production 사용 근거 — 후속 작업.
- [ ] G1 통과 뒤 candidate API manifest와 G2 공통 semantic fixture·bounded input/error contract — 후속 작업.
- [ ] G3 source/binary/dependency compatibility와 migration/rollback — 후속 작업.
- [ ] G4 Protobuf/JPA/Exposed exact round-trip — 후속 작업.
- [ ] G5 benchmark와 provider lifecycle·failure 계약 — 후속 작업.

최종 판정은 **G1 PENDING, production 구현 보류**다. #1423과 #767은 G1의
production 자격, G2-G5와 별도 Type A 승인 조건을 추적하기 위해 `OPEN`으로 유지한다.
