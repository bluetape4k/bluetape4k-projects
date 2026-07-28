# 이슈 612 - Ktor core baseline helper

## 배경

issue #612는 #611에서 Ktor module family를 scaffold한 뒤 첫 reusable Ktor runtime
API를 구현한다. idgenerator Ktor example에는 수동 JSON 설정, `StatusPages`, health
route, query parameter validation code가 반복되어 있었다.

## 결정

작고 명시적인 Ktor-native core surface를 제공한다.

- opt-in baseline installation을 위한 `installBluetape4kKtorCore()`.
- 공유 JSON default를 위한 `Bluetape4kKtorJson.defaultJson()`.
- `ApiErrorResponse`, `HealthResponse`, health/readiness route.
- cancellation을 rethrow하는 `StatusPagesConfig.bluetape4kErrorResponses()`.
- default status pages가 caller input failure를 HTTP 400으로 매핑하도록
  `IllegalArgumentException`을 던지는 route parameter helper.

public API가 `Json`, `StatusPagesConfig`, Ktor application type을 언급하므로 module은
Ktor와 kotlinx serialization dependency를 `api`로 노출한다.

## 결과

첫 core API는 의도적으로 framework-light하게 유지한다. Spring Boot
auto-configuration은 없고, health/readiness 외의 숨겨진 application route도 없으며,
client helper도 없다. example migration은 shared API가 안정된 뒤 #615에 남긴다.

## 검증

- `./gradlew :bluetape4k-ktor-core:compileKotlin :bluetape4k-ktor-core:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-core:test :bluetape4k-ktor-core:koverXmlReport`
- Kover XML: line coverage 90/98 (91.8%).

## 향후 가드

Ktor shared helper를 더 추가할 때는 installer를 명시적으로 유지한다. application이
자주 customize하는 Ktor plugin은 config에서 개별 switch가 가능하지 않다면 설치하지
않는다.
