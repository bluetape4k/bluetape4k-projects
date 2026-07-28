# Issue 433 Infra Resilience Wrapper 분류

## 배경

JVM-only 방향은 Bucket4j/Resilience4j 질문을 native porting이 아니라 wrapper
standardization 문제로 바꾼다. `infra/bucket4j`와 `infra/resilience4j`는 이미
Kotlin facade code, coroutine wrapper, test, README pair를 포함한다.

## 결정

Umbrella decision은 issue `#433`으로 추적한다. Bucket4j와 Resilience4j는 upstream
JVM engine으로 유지하고, bluetape4k의 Kotlin coroutine facade contract를 강화한다.
internal을 port하지 않고 KMP/Kotlin Native scope도 도입하지 않는다. 실행은 Bucket4j용
`#434`와 Resilience4j용 `#435`라는 module-specific issue로 분리한다.

## 결과

WIP queue에 infra resilience facade lane이 생겼다. `#433`은 decision record로 남고,
`#434`와 `#435`는 implementation acceptance criteria를 담당한다. 이 분리는
Bucket4j token-bucket provider ergonomics와 Resilience4j coroutine resilience
policy composition을 분리해 유지한다.

## 검증

- 기존 Bucket4j/Resilience4j wrapper 항목이 있는지 open issue를 검색했고, 중복 open
  issue는 없었다.
- 현재 module footprint를 계산했다. `infra/bucket4j`는 main Kotlin file 19개와 test
  Kotlin file 26개, `infra/resilience4j`는 main Kotlin file 22개와 test Kotlin file
  40개를 가진다.
- `enhancement`, `design`, `refactor` label과 `debop` assignee를 지정해 GitHub issue
  `#433`을 만들었다.
- umbrella가 너무 넓다는 점을 확인한 뒤 implementation을 issue `#434`와 `#435`로
  분리했다.
- `gh issue list --assignee debop --state open --search 'created:>=2026-01-01'`로
  현재 open assigned issue 수를 확인했고 결과는 12개였다.
- `WIP.md` snapshot count를 active assigned issue 10개에서 12개로 갱신하고, 닫힌
  `#251`을 active queue에서 제거했다.

## 향후 지침

이 module을 수정할 때는 새 abstraction layer보다 contract hardening과 documentation을
우선한다. Cancellation propagation test가 첫 번째 implementation guardrail이어야
한다. 향후 PR이 더 넓은 Bucket4j versus Resilience4j positioning decision을 다시
열지 않고 집중된 module issue를 닫을 수 있도록 umbrella issue와 execution issue를
분리해 유지한다.
