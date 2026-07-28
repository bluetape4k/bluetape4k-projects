# 이슈 #611 Ktor module scaffold

## 배경

#610에서 boundary를 정의한 뒤, issue #611은 1.10.0 Ktor module family를 시작한다.
이 단계는 module registration만 수행하고 production API는 #612부터 #614까지로 남겨야
한다.

## 결정

기존 `includeModules("ktor", withBaseDir = true)` pattern 아래에 `ktor/core`,
`ktor/observability`, `ktor/testing`을 추가해 Gradle project name이
`bluetape4k-ktor-core`, `bluetape4k-ktor-observability`, `bluetape4k-ktor-testing`이
되게 한다.

## 결과

scaffold는 build file, README locale stub, test resource, root module-list update를
추가한다. CI/Nightly workflow 변경은 계속 #616에 배정한다.

## 검증

- `./gradlew projects`가 세 새 module을 나열해야 한다.
- targeted compile task로 empty/minimal source set이 build를 깨지 않음을 증명해야 한다.

## 향후 가드

scaffold-only work에서는 production Ktor helper API를 추가하지 않는다. module
registration이 증명된 뒤 #612, #613, #614에서 behavior를 시작한다.
