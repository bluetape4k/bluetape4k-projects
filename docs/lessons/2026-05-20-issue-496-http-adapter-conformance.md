# 이슈 496 HTTP Adapter Conformance Test

## 배경

Issue #496은 이전 Retrofit 및 Feign adapter bug 이후 shared conformance test를 요청했다. Cancellation,
timeout, tag, delayed-response behavior가 HTTP backend 사이에서 drift될 수 있기 때문이다.

## 결정

중복된 Retrofit HC5/Vert.x regression coverage를 shared `CallFactoryConformanceTest`로 승격한다.
Feign sync/async conformance suite는 public-network service 대신 local `MockWebServer` fixture를 사용해
Apache HC5와 Vert.x client에 추가한다.

## 결과

Retrofit HC5와 Vert.x는 cancel-before-enqueue, in-flight cancel, delayed response body cleanup,
timeout exposure, request tag test를 공유한다. Feign HC5와 Vert.x sync/async client는
delayed-response 및 read-timeout coverage를 공유하고, async client는 cancellable future state도
assert한다. `VertxHttpClient`는 명시적인 event-loop guard coverage를 가진다. Retrofit과 Feign README
pair는 transport contract를 문서화한다.

## 검증

- `./gradlew :bluetape4k-retrofit2:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-feign:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-retrofit2:test --tests 'io.bluetape4k.retrofit2.client.hc5.Hc5HttpClientTest' --tests 'io.bluetape4k.retrofit2.client.vertx.VertxHttpClientTest' --no-configuration-cache` (56 passing)
- `./gradlew :bluetape4k-feign:test --tests 'io.bluetape4k.feign.clients.hc5.ApacheHc5ClientConformanceTest' --tests 'io.bluetape4k.feign.clients.hc5.ApacheHc5AsyncClientConformanceTest' --tests 'io.bluetape4k.feign.clients.vertx.VertxClientConformanceTest' --tests 'io.bluetape4k.feign.clients.vertx.VertxAsyncClientConformanceTest' --tests 'io.bluetape4k.feign.clients.vertx.VertxClientTest.execute fails fast on Vertx event loop thread' --no-configuration-cache` (11 passing)

## 향후 agent 가이드

Backend-specific test를 추가하기 전에 adapter behavior guarantee를 shared abstract suite에 둔다.
Feign async cancellation은 현재 public `CompletableFuture` state만 assert한다. Adapter가 stable request
handle 또는 cancellation hook을 expose할 때만 underlying-request cancellation coverage를 추가한다.
