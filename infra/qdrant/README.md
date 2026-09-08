# bluetape4k-qdrant

Suspending calls and cold flows that preserve the official Qdrant Java client's protobuf requests.

## Usage

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-qdrant")
}
```

Versions are managed by the bluetape4k BOM. The caller configures and closes the client and gRPC channel, including authentication, TLS, and maximum inbound message size.

```kotlin
val request = QueryPoints.newBuilder()
    .setCollectionName("documents")
    .setQuery(QueryFactory.nearest(0.1f, 0.2f, 0.3f))
    .setLimit(10)
    .build()
val matches = client.querySuspending(request, Duration.ofSeconds(3))

client.scrollAsFlow(
    ScrollPoints.newBuilder().setCollectionName("documents").setLimit(100).build()
).take(200).collect { point -> process(point) }

val template = UpsertPoints.newBuilder().setCollectionName("documents").setWait(true).build()
client.upsertBatches(pointsFlow, template, maxBatchItems = 256, maxBatchBytes = 4 * 1024 * 1024)
    .collect { result -> record(result) }
```

Reuse SDK builders such as `QueryFactory`, `ConditionFactory`, `ValueFactory`, and `VectorsFactory`. `deleteSuspending` accepts `DeletePoints`, preserving ID/filter selection and options.

Use `PointIdFactory.id(1L)` or `PointIdFactory.id(uuid)` for numeric and UUID IDs. Vector length must match the collection dimension. Store named vectors with `VectorsFactory.namedVectors(mapOf("embedding" to VectorFactory.vector(1f, 0f, 0f)))` and select them using query `setUsing("embedding")`. Dimension and name mismatches propagate as server errors. Set consistency, write ordering, and wait options on the original protobuf request.

## Execution contract

| API | Behavior |
| --- | --- |
| `querySuspending`, `upsertSuspending`, `deleteSuspending` | Await future callbacks and propagate the original failure |
| `scrollAsFlow` | Independent collection, page limit 1..1000, consume a page before requesting the next |
| `upsertBatches` | Bound item count and the entire serialized protobuf request; one request at a time |

Each RPC receives the supplied `Duration`. Use `withTimeout` to bound the entire collection. Cancellation cancels the pending future but cannot undo writes already applied by the server. Clients and channels are never closed automatically.

The default scroll `maxPages` is 10000. An immediately repeated cursor or a remaining next page at the page cap causes failure. Historical cursors are not accumulated. Configure the channel's inbound limit to bound payload size within a page.

The batch template must contain no points. A single oversized point is rejected before its request. Successful batch responses have already reached the collector when a later batch fails, so callers must handle partial success. There is no automatic retry or rollback. Input failure or cancellation discards the unsent batch. Slow collectors delay the next batch request.

Callers supply tenant filters required for authorization. Logs contain only operation and failure status, never payloads, vectors, tokens, or raw SDK error messages.

## Verification

```bash
./gradlew :bluetape4k-qdrant:test -PexcludeIntegrationTests=true
./gradlew :bluetape4k-qdrant:test
```

The second command runs Qdrant 1.19.0 in Docker and verifies upsert, tenant-filtered search, pagination, and deletion. Each test cleans up its collection and client. Until the central catalog changes are merged, development builds require `-Pbluetape4kDependenciesCatalogPath=<dependencies-worktree>/gradle/libs.versions.toml`.
