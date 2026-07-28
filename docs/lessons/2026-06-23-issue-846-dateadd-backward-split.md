# 이슈 #846 DateAdd backward split period seek

issue #846은 backward calculation이 여러 split available period 뒤에서 시작할 때
`DateAdd.findPrevPeriod()`가 더 오래된 available period를 선택할 수 있음을 찾았다.

## 결정

previous-period distance를 `Duration.between(period.end, start)`로 비교해 가장 가까운
이전 period가 가장 작은 positive distance를 갖게 한다. 기존 forward seek logic은 그대로
두고 plain `DateAdd` include period와 `CalendarDateAdd` working-hour period 양쪽의
backward seek selection을 고친다.

## 교훈

- backward search는 candidate period end에서 start moment까지의 거리를 비교해야 한다.
  `start`에서 `period.end`까지 비교하면 negative duration이 생기고 오래된 period가 더
  작아 보일 수 있다.
- zero-offset regression은 duration arithmetic 전에 선택된 start period를 직접 드러내므로
  이 bug에 유용하다.
- bug가 `DateAdd.calculateEnd`에 있을 때는 `CalendarDateAdd`도 cover해야 한다. weekly
  working period를 다시 만들고 같은 path에 delegate하기 때문이다.

## 검증

- RED: split backward regression은 `2011-04-15T00:00Z` 대신 `2011-04-24T00:00Z`,
  `2011-04-04T12:00Z` 대신 `2011-04-04T18:00Z`로 실패했다.
- GREEN targeted: `./gradlew :bluetape4k-javatimes:test --tests "io.bluetape4k.javatimes.period.calendars.DateAddTest.backward seek after split include periods starts from nearest previous period" --tests "io.bluetape4k.javatimes.period.calendars.CalendarDateAddTest.backward seek after split working hours starts from nearest previous period" --no-build-cache`가 2 tests로 통과했다.
- module: `./gradlew :bluetape4k-javatimes:test --no-build-cache`가 690 tests, 36 pending으로 통과했다.
- build: `./gradlew :bluetape4k-javatimes:build --no-build-cache`가 통과했다.
- hygiene: `git diff --check`가 통과했다.
- static analysis: `./gradlew detekt`가 `:detekt NO-SOURCE`와 함께 통과했다.
