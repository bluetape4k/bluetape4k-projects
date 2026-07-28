# 이슈 614 - Ktor testing helper

## 배경

`bluetape4k-ktor-core`와 `bluetape4k-ktor-observability`가 reusable Ktor server
helper를 도입했지만, test에는 JSON decoding, status assertion, standard API error
payload check가 여전히 반복됐다.

## 결정

작은 `bluetape4k-ktor-testing` API surface를 추가한다.

- `installBluetape4kKtorCoreForTest`는 Ktor `testApplication` ownership을 test에
  남기면서 core setup boilerplate를 줄인다.
- `bluetape4kJsonClient`는 `bluetape4k-ktor-core`와 같은 JSON default를 사용한다.
- `decodeJsonBody`, `shouldHaveStatus`, `shouldHaveJsonBody`,
  `shouldHaveApiError`는 response assertion을 중앙화한다.
- `bluetape4kJsonMockEngine`은 더 큰 mocking abstraction을 추가하지 않고
  one-response JSON client test를 커버한다.

## 결과

idgenerator Ktor example은 이제 local `Json` instance로 모든 response를 직접 decode하지
않고 shared response helper를 사용한다.

## 검증

- `./gradlew :bluetape4k-ktor-testing:compileKotlin :bluetape4k-ktor-testing:compileTestKotlin :idgenerator-ktor-demo:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-testing:test :bluetape4k-ktor-testing:koverXmlReport :idgenerator-ktor-demo:test`
- `git diff --check`

## 향후 가드

이 module은 test-scoped helper에 집중한다. 반복되는 consumer test가 특정 lifecycle
abstraction이 명시적 setup 손실을 감수할 만큼 가치 있음을 증명하기 전에는 full Ktor
test lifecycle을 감싸지 않는다.
