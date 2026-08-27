# `src/main` 호출 경로만으로 production consumer를 증명하지 않는다

## 배경

Issue #767의 G1은 “서로 독립된 production consumer 두 곳”을 요구한다. 후속
평가에서는 Workshop #800이 지목한 billing ledger와 order audit가 별도 runnable
application이고, 두 모듈의 `src/main`에서 금액·통화·산술·저장 경계를 실제로
호출한다는 사실을 확인했다.

초기 판단은 이 근거를 production-shaped domain 두 곳으로 보아 G1을 `PASS`로
높이려 했다. 독립 검토는 두 모듈의 README가 각각 example과 workshop임을 명시하며,
canonical gate가 요구하는 production consumer 자격은 별도 근거가 없다는 점을
P1으로 지적했다.

## 잘못된 가정

“서로 의존하지 않는 runnable `src/main` application 두 곳이면 production consumer
두 곳이다”라는 가정은 다음 범주를 섞었다.

- `src/main` 호출 경로: test fixture가 아닌 application code에서 계약이 필요한지
  증명한다.
- consumer 독립성: 같은 example series나 동일 consumer를 두 번 세지 않았는지
  증명한다.
- production 자격: 실제 production 사용인지, 또는 gate 소유자가 production-shaped
  Workshop application을 증거로 인정했는지 증명한다.
- library 채택: 현재 공통 library type을 이미 사용하는지 증명한다.

앞의 두 조건은 source와 dependency graph로 확인할 수 있다. 그러나 그것만으로
세 번째 조건이 자동으로 성립하지 않는다. 네 번째 조건을 G1에 요구하면 공통 API
도입 전에 공통 API 채택을 증명해야 하는 순환 조건이 되므로 별도로 배제해야 한다.

## 드러난 증거

- [Epic #1423](https://github.com/bluetape4k/bluetape4k-projects/issues/1423)의
  canonical G1은 “서로 독립된 production consumer 두 곳”이다.
- [billing README](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/commerce/usage-metering-billing-ledger/README.md#L1-L7)는
  모듈을 Spring Boot example로 설명한다.
- [order audit README](https://github.com/bluetape4k/bluetape4k-workshop/blob/f95ea45c1c053f3901d91d29bca58f4e18fb3bdf/spring-modulith/ddd-order-audit/README.md#L1-L7)는
  모듈을 workshop example로 설명한다.
- 독립 검토 초회 결과는 G1 `PASS` 판정을 P1으로 분류했고, 판정을 `PENDING`으로
  낮춘 뒤 재검토에서 P0 0건, P1 0건을 확인했다.

## 결정

G1은 consumer 후보의 호출 경로, 독립성, production 자격, library 채택을 각각
분리해 판정한다.

1. 고정 SHA의 caller부터 산술·저장 경계까지 `src/main` 호출 경로를 확인한다.
2. module dependency와 application entry point로 consumer 독립성을 확인한다.
3. production 자격은 실제 production 사용 근거 또는 #767/#1423의 명시적 인정
   규칙으로만 통과시킨다.
4. 현재 공통 library 채택 여부는 G1의 production 자격 대신 사용하지 않는다.
5. 1-2만 충족하고 3이 없으면 G1은 `PENDING`이며 production 구현을 시작하지 않는다.

## 재발 방지 절차

- evidence gate를 높이기 전에 canonical 문구의 각 명사를 그대로 옮긴 admission
  matrix를 작성한다. `production`, `independent`, `consumer`, `actual call path`를
  하나의 source 존재 검사로 축약하지 않는다.
- `src/main`, README의 audience 선언, dependency graph, live issue의 승인된 판정
  규칙을 서로 다른 증거 열로 유지한다.
- example/workshop/demo를 production 근거로 사용할 때는 gate 소유 issue에 인정
  규칙이 이미 있는지 먼저 확인한다. 없으면 `PASS`가 아니라 `PENDING`으로 둔다.
- 독립 리뷰에는 “증거가 존재하는가”뿐 아니라 “그 증거가 canonical gate의 자격을
  증명하는가”를 별도 질문으로 포함한다.
- G1이 `PENDING`인 동안에는 candidate API manifest, compatibility layer,
  deprecation, benchmark를 구현 목적으로 확장하지 않는다.

## 결과

후속 평가 문서는 G1을 `PENDING`으로 낮추고 두 runnable consumer를 후보로만
기록했다. G1-G5가 모두 `PASS`하고 별도 Type A 승인을 받기 전까지 owned Money API
구현 차단도 유지했다.

## 검증

- 한국어 용어 감사와 `git diff --check`를 연구 문서와 이 lesson에 다시 적용한다.
- 독립 검토로 G1 판정, canonical gate 보존, source citation과 후속 차단을 다시
  확인한다.
- 새 commit SHA를 remote와 PR head에서 읽은 뒤 exact-head CI를 다시 실행한다.

## DoD Status

- [x] 잘못된 가정과 이를 반박한 source·review 증거를 기록했다.
- [x] 호출 경로, 독립성, production 자격, library 채택을 별도 판정 축으로 분리했다.
- [x] 같은 오판을 막을 admission matrix와 독립 리뷰 질문을 정의했다.
- [x] G1 `PENDING` 및 G1-G5·Type A 구현 차단을 유지했다.
