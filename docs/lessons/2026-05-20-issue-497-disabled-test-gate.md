# 이슈 #497 Disabled Test Gate 교훈

## 배경

Disabled test가 example, infrastructure test, conditional environment case 전반에 흩어져 있었다.
Runtime test behavior를 바꾸지 않고 이를 드러내는 release gate가 필요했다.

## 결정

`buildSrc` 기반 root Gradle task로 test source를 scan하고 markdown report를 생성한다. GitHub issue
reference가 없는 `known-bug` disabled test만 실패 처리한다. Unsupported, environment, slow,
conditional, intentional example skip은 보이게 두되 blocking하지 않는다.

## 결과

Release checklist는 이제 `./gradlew checkDisabledTests`와 generated report
`build/reports/disabled-tests/disabled-tests.md`를 가리킨다.

## 검증

- `./gradlew :buildSrc:test --no-configuration-cache` 통과.
- `./gradlew checkDisabledTests --no-configuration-cache` 통과, disabled annotation 37개와 known-bug
  issue-reference violation 0개 보고.
- `./gradlew help --task checkDisabledTests --no-configuration-cache`로 root verification task registration 확인.
- Task input을 whole project directory에서 explicit `src/test` source file로 좁힌 뒤
  `./gradlew build -x test --parallel --no-configuration-cache` 통과.
- `git diff --check` 통과.

## 향후 가이드

실제 defect를 숨기기 위해 `@Disabled`를 추가할 때는 annotation reason에 tracking issue를 포함한다.
Bug가 아닌 skip이면 scanner가 unsupported capability, manual environment, slow optional,
conditional environment, intentional example behavior로 분류할 수 있게 이유를 적는다.
