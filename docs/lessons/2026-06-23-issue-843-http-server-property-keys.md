# 이슈 843 - BluetapeHttpServer property key

## 배경

`PropertyExportingServer`는 exported system property key를
`testcontainers.{propertyNamespace}.{key}` 아래의 lowercase kebab-case로 정의한다.

`BluetapeWebfluxServer`는 이미 `httpbin-url`, `jsonplaceholder-url`, `web-url`로 이
contract를 따랐지만, `BluetapeHttpServer`는 같은 값을 `httpbinUrl`,
`jsonplaceholderUrl`, `webUrl`로 노출했다.

## 결정

`BluetapeHttpServer.propertyKeys()`는 shared export contract가 쓰는 canonical
kebab-case key만 반환하게 한다.

`properties()`는 canonical kebab-case entry를 쓰고, 기존 downstream test를 위해
`withCompatKeys`로 이전 camelCase key를 compatibility alias로 유지한다.

## 후속 가드

public README placeholder는 `PropertyExportingServer`와 맞춘다.
`PropertyExportingServerContractTest`는 Docker를 시작하지 않고도
`BluetapeHttpServer`와 `BluetapeWebfluxServer` divergence를 잡도록 mock HTTP server
wrapper 양쪽을 포함한다.
