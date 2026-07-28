# 이슈 609 - Ktor module 채택과 metadata

## 배경

Ktor module family는 작은 slice로 추가됐지만, idgenerator Ktor example과 CI
metadata는 아직 module이 실제 consumer에서 사용할 수 있음을 증명해야 했다.

## 결정

Ktor surface를 확장하기 전에 `idgenerator-ktor-demo`를 공유 module로 먼저
migration한다.

- JSON, standard API error, health/readiness에는 `bluetape4k-ktor-core`를 사용한다.
- request ID propagation과 call logging에는 `bluetape4k-ktor-observability`를 사용한다.
- example이 consumer reference로 계속 유용하도록 application route는 명시적으로 유지한다.
- example workflow trigger에 `ktor/**`를 추가하고 CI와 Nightly에서 Ktor module test를 실행한다.

publication과 BOM constraint는 examples와 demo module을 제외한 root subproject에서
파생되므로, release metadata에는 수동으로 편집한 module list가 필요하지 않았다.

## 결과

example은 이제 core, observability, testing helper를 함께 사용한다. CI와 Nightly에는
명시적인 Ktor test와 coverage artifact가 있고, shared Ktor module이 바뀌면 examples
workflow가 다시 실행된다.

## 검증

- `actionlint`
- `./gradlew :idgenerator-ktor-demo:compileKotlin :idgenerator-ktor-demo:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-core:test :bluetape4k-ktor-observability:test :bluetape4k-ktor-testing:test :idgenerator-ktor-demo:test`
- `./gradlew :bluetape4k-ktor-core:koverXmlReport :bluetape4k-ktor-observability:koverXmlReport :bluetape4k-ktor-testing:koverXmlReport`
- `./gradlew -q projects | rg "bluetape4k-ktor-(core|observability|testing)|idgenerator-ktor-demo"`
- `git diff --check`

## 향후 가드

shared module을 example이 채택할 때는 양방향을 함께 업데이트한다. 즉 example
dependency와, shared module 변경 시 example을 다시 실행해야 하는 workflow trigger를
같이 갱신한다.
