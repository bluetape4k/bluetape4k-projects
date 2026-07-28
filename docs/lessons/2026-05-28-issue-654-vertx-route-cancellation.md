# 이슈 654 - Vert.x route coroutine cancellation 보존

## 배경

Vert.x route coroutine helper는 `Throwable`을 catch하고 모든 failure를
`RoutingContext.fail(...)`로 넘겼다. 이 동작은 structured cancellation을 보존하지
않고 coroutine cancellation을 HTTP route failure로 바꾸었다.

## 결정

넓은 `Throwable` fallback 전에 `CancellationException`을 catch하고,
`suspendHandler`와 `suspendFailureHandler` 양쪽에서 다시 던진다. 일반 exception은
기존처럼 `ctx.fail(e)`로 매핑한다.

## 결과

route handler는 이제 일반 exception의 기존 error path를 유지하면서 coroutine
cancellation semantic을 보존한다.

## 검증

- `./gradlew :bluetape4k-vertx:compileKotlin :bluetape4k-vertx:compileTestKotlin :bluetape4k-vertx:test --tests io.bluetape4k.vertx.web.VertxRouteExtensionsTest`
- `./gradlew :bluetape4k-vertx:koverXmlReport`
- `git diff --check`

## 향후 가드

suspend route handler 주변에서 `Throwable`을 catch할 때는 먼저 `CancellationException`을
rethrow해야 한다.
