# Issue 654 - Vert.x route coroutine cancellation

## Context

Vert.x route coroutine helpers caught `Throwable` and passed every failure to
`RoutingContext.fail(...)`. That converted coroutine cancellation into an HTTP
route failure instead of preserving structured cancellation.

## Decision

Catch `CancellationException` before the broad `Throwable` fallback and rethrow
it from both `suspendHandler` and `suspendFailureHandler`. Keep normal
exceptions mapped to `ctx.fail(e)`.

## Outcome

Route handlers now preserve coroutine cancellation semantics while retaining the
existing error path for ordinary exceptions.

## Verification

- `./gradlew :bluetape4k-vertx:compileKotlin :bluetape4k-vertx:compileTestKotlin :bluetape4k-vertx:test --tests io.bluetape4k.vertx.web.VertxRouteExtensionsTest`
- `./gradlew :bluetape4k-vertx:koverXmlReport`
- `git diff --check`

## Future guard

Do not catch `Throwable` around suspend route handlers without rethrowing
`CancellationException` first.
