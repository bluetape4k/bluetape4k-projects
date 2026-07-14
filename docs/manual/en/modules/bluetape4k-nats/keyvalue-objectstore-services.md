---
title: KeyValue, ObjectStore, and Service APIs
description: Separate JetStream-backed KeyValue revisions, ObjectStore chunks and digests, and NATS Service endpoint construction.
manualId: bluetape4k-nats
chapterId: keyvalue-objectstore-services
---

# KeyValue, ObjectStore, and Service APIs

## Three different jobs

KeyValue and ObjectStore both use JetStream but serve different needs. KeyValue stores small values with revisions and watchers. ObjectStore stores larger byte streams in chunks. The NATS Service API describes request-reply endpoints and service metadata.

These APIs do not replace database transactions, S3-compatible storage, or an HTTP framework. They are options for matching requirements within an existing NATS connection and subject model.

## KeyValue buckets

```kotlin
val config = keyValueConfiguration("profiles") {
    storageType(StorageType.File)
    history(10)
    replicas(3)
}

val management = connection.keyValueManagement()
management.createOrUpdate(config)
val profiles = connection.keyValue("profiles")

val revision = profiles.put("user-42.color", "blue")
profiles.update("user-42.color", "green", revision)
```

`createOrUpdate` tries update first and falls back to create only for a JetStream not-found error. Authorization and network failures remain visible.

Revisions support optimistic concurrency. They come from the bucket's underlying stream sequence, so each key does not have an independent sequence starting at one. A watcher reports changes but does not define slow-consumer or handler-failure policy.

## ObjectStore

```kotlin
val objectConfig = objectStoreConfiguration("reports") {
    storageType(StorageType.File)
}
connection.objectStoreManagement().create(objectConfig)

val store = connection.objectStore("reports")
Files.newInputStream(reportPath).use { input ->
    val meta = objectMeta("daily.csv") {
        description("daily settlement report")
        chunkSize(128 * 1024)
    }
    store.put(meta, input)
}
```

ObjectStore splits objects into chunks and tracks metadata and a digest. It does not automatically provide file-system atomic rename or S3 lifecycle, replication, and IAM. Include maximum object size, stream storage, and disk capacity in NATS server operations.

`ObjectStoreManagement.tryDelete` ignores only an already missing bucket. Bucket deletion affects every stored object, so keep it in test cleanup or an approved operations command.

## Object links and metadata

`objectLinkOf(bucket)` creates a bucket link, and `objectLinkOf(bucket, objectName)` creates an object link. The `objectMeta` and `objectMetaOptions` DSLs wrap jNATS builders. Beyond blank-name checks, the application owns metadata and content-type contracts.

## NATS Service

The service DSL combines a connection, service name and version, and endpoints.

```kotlin
val echoEndpoint = serviceEndpointOf {
    endpointName("echo")
    endpointSubject("tools.echo")
    handler { message ->
        message.respond(connection, message.data)
    }
}

val service = natsServiceOf(
    connection,
    name = "tool-service",
    version = "1.0.0",
    echoEndpoint,
) {
    description("Small internal tools")
}

service.startService()
```

`natsServiceOf` adds endpoints in argument order. It leaves final name and version validation to the jNATS builder. The caller defines handler concurrency, timeout, error response schema, and service shutdown.

## Selection guide

- Consider KeyValue for revisioned values and wildcard watches.
- Consider ObjectStore for binary objects kept inside NATS operations.
- Use the Service API when existing NATS request-reply endpoints need discoverable service metadata.
- Prefer dedicated systems when relational queries, large object lifecycle, or an HTTP gateway is central to the workload.

## Sources and examples

- [`KeyValueManagement.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/KeyValueManagement.kt)
- [`KeyValueConfiguration.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/api/KeyValueConfiguration.kt)
- [`ObjectStreamManagement.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ObjectStreamManagement.kt)
- [`ObjectStoreConfiguration.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/api/ObjectStoreConfiguration.kt)
- [`Service.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/service/Service.kt)
- [`ServiceEndpoint.kt`](../../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/service/ServiceEndpoint.kt)
- [`KeyValueIntroExamples.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/KeyValueIntroExamples.kt)
- [`ObjectStoreExample.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/ObjectStoreExample.kt)
- [`ServiceExtensionsTest.kt`](../../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/service/ServiceExtensionsTest.kt)
