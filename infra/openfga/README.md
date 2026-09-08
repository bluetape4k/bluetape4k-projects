# Module bluetape4k-openfga

[한국어](./README.ko.md) | English

Kotlin coroutine extensions for the official OpenFGA Java SDK. The module keeps
the SDK request and response models, adds cancellation-aware suspend calls, and
streams tuple pages as a cold `Flow`.

## Features

- Explicit immutable `OpenFgaScope` for store and authorization model IDs
- `checkSuspending`, `batchCheckSuspending`, `readSuspending`, and
  `writeSuspending` wrappers around the SDK's low-level `OpenFgaApi`
- `readTuplesFlow` with sequential pagination, backpressure, a page-size range
  of 1..100, and a default 10,000 page guard
- `openFgaTuple` validation and conversion to the SDK tuple-key types
- Pre-flight validation of the SDK/server defaults: 1..50 batch checks and at
  most 100 combined writes and deletes
- Caller-owned SDK configuration and client lifecycle; no hidden retry, cache,
  or close behavior
- Future cancellation propagation through `kotlinx.coroutines.future.await`

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-openfga:$version")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.bluetape4k</groupId>
    <artifactId>bluetape4k-openfga</artifactId>
    <version>${version}</version>
</dependency>
```

The module uses `dev.openfga:openfga-sdk:0.10.0`,
`bluetape4k-coroutines`, and `bluetape4k-logging`.

## Usage

Create the official low-level API with the configuration and authentication
owned by the application. Pass an immutable scope to every operation:

```kotlin
import dev.openfga.sdk.api.OpenFgaApi
import dev.openfga.sdk.api.configuration.Configuration
import dev.openfga.sdk.api.model.CheckRequest
import io.bluetape4k.openfga.OpenFgaScope
import io.bluetape4k.openfga.checkSuspending
import io.bluetape4k.openfga.openFgaTuple
import io.bluetape4k.openfga.toCheckRequestTupleKey

val api = OpenFgaApi(Configuration().apiUrl("http://localhost:8080"))
val scope = OpenFgaScope(
    storeId = "store-id",
    authorizationModelId = "model-id",
)
val tuple = openFgaTuple("user:anne", "reader", "document:budget")

val response = api.checkSuspending(
    scope,
    CheckRequest().tupleKey(tuple.toCheckRequestTupleKey()),
)
check(response.data.allowed == true)
```

The wrapper copies the mutable SDK request body before applying the scope, so a
shared request object keeps its original model ID and options. Use the official
`ConfigurationOverride` when a request needs a different timeout, headers, or
retry setting:

```kotlin
import dev.openfga.sdk.api.configuration.ConfigurationOverride
import java.time.Duration

val response = api.checkSuspending(
    scope,
    request,
    ConfigurationOverride().readTimeout(Duration.ofSeconds(2)),
)
```

Read tuples lazily and consume one page at a time:

```kotlin
import dev.openfga.sdk.api.model.ReadRequest
import io.bluetape4k.openfga.readTuplesFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

val tuples = api.readTuplesFlow(
    scope = OpenFgaScope("store-id"),
    request = ReadRequest(),
    pageSize = 50,
    maxPages = 10_000,
).take(100).toList()
```

`readTuplesFlow` sends no request before collection, stops when the server
returns an empty continuation token, rejects a cursor that repeats immediately,
and fails when a non-empty cursor remains after `maxPages`. Collector
cancellation prevents the next page request. Each suspend wrapper uses
`Future.await`, so cancellation stops local awaiting and requests cancellation
of the returned `CompletableFuture`. The SDK may continue its underlying
transport or retry work; use the SDK request timeout/deadline through
`ConfigurationOverride` when a network bound is required. A write that already
reached the server is not rolled back.

The SDK has no close API. This module therefore does not close the injected
`OpenFgaApi`; the caller owns the SDK configuration and HTTP resources.

## Testing

Unit tests use mocked SDK futures. The real-server test is tagged `integration`
and starts one SDK-neutral Testcontainers OpenFGA endpoint, creates a temporary
store and model, writes tuples, checks allow/deny, reads them through the Flow,
and deletes the store in `finally`.

```bash
./gradlew :bluetape4k-openfga:test :bluetape4k-openfga:detekt --max-workers=1
./gradlew :bluetape4k-openfga:test -PexcludeIntegrationTests=true --max-workers=1
```
