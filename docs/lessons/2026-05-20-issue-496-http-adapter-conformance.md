# Issue 496 HTTP Adapter Conformance Tests

## Context

Issue #496 asked for shared conformance tests after prior Retrofit and Feign
adapter bugs showed that cancellation, timeout, tag, and delayed-response
behavior can drift between HTTP backends.

## Decision

Promote duplicated Retrofit HC5 and Vert.x regression coverage into a shared
`CallFactoryConformanceTest`. Add Feign sync and async conformance suites for
Apache HC5 and Vert.x clients using local `MockWebServer` fixtures instead of
public-network services.

## Outcome

Retrofit HC5 and Vert.x now share tests for cancel-before-enqueue, in-flight
cancel, delayed response body cleanup, timeout exposure, and request tags. Feign
HC5 and Vert.x sync/async clients share delayed-response and read-timeout
coverage; async clients also assert cancellable future state. `VertxHttpClient`
has explicit event-loop guard coverage. Retrofit and Feign README pairs document
the transport contracts.

## Verification

- `./gradlew :bluetape4k-retrofit2:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-feign:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-retrofit2:test --tests 'io.bluetape4k.retrofit2.client.hc5.Hc5HttpClientTest' --tests 'io.bluetape4k.retrofit2.client.vertx.VertxHttpClientTest' --no-configuration-cache` (56 passing)
- `./gradlew :bluetape4k-feign:test --tests 'io.bluetape4k.feign.clients.hc5.ApacheHc5ClientConformanceTest' --tests 'io.bluetape4k.feign.clients.hc5.ApacheHc5AsyncClientConformanceTest' --tests 'io.bluetape4k.feign.clients.vertx.VertxClientConformanceTest' --tests 'io.bluetape4k.feign.clients.vertx.VertxAsyncClientConformanceTest' --tests 'io.bluetape4k.feign.clients.vertx.VertxClientTest.execute fails fast on Vertx event loop thread' --no-configuration-cache` (11 passing)

## Future Agents

Keep adapter behavior guarantees in shared abstract suites before adding
backend-specific tests. Feign async cancellation currently asserts the public
`CompletableFuture` state only; add underlying-request cancellation coverage
only if the adapter exposes a stable request handle or cancellation hook.
