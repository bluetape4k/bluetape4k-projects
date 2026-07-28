# 이슈 #610 Ktor module family design

## 배경

milestone 1.10.0은 `bluetape4k-projects`의 reusable Ktor module family를 시작한다.
첫 단계는 scaffold나 implementation 전에 #610의 design-only work를 완료하는 것이었다.

## 결정

첫 Ktor module family는 `bluetape4k-projects` 안에 두고, server foundation을
`ktor/core`, `ktor/observability`, `ktor/testing`으로 나눈다. server-side extension
point가 검증될 때까지 `ktor/client`, `ktor/resilience4j`, `ktor/openapi`,
`ktor/auth`는 backlog milestone에 남겨 둔다.

## 결과

design과 plan 문서는 #611부터 #616까지의 module boundary, dependency rule, API
direction, PR sequence를 정의한다.

## 검증

- GNO로 기존 #609-#616 issue context를 확인했다.
- sibling bluetape4k repository의 기존 Ktor example을 조사했다.
- official Ktor docs에서 plugin installation, `StatusPages`, `ContentNegotiation`,
  `CallLogging`, `MicrometerMetrics`, `testApplication` API를 확인했다.

## 향후 가드

#610 design이 review를 통과하기 전에는 Ktor implementation을 시작하지 않는다. plugin
installation은 명시적으로 유지하고, design boundary를 다시 열지 않은 채 backlog
module을 첫 slice로 승격하지 않는다.
