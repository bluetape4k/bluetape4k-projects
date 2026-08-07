---
manualId: bluetape4k-nats
title: "NATS Client Extensions"
description: "Use jNATS core messaging, JetStream, KeyValue, ObjectStore, and Service APIs through Kotlin DSLs and coroutine adapters."
kind: library
group: messaging
learningOrder: 730
---

# NATS Client Extensions

## Provided capabilities {#problem}

`bluetape4k-nats` adds Kotlin-friendly builders and extensions to the official jNATS Java client. It shortens string payload publishing, request-reply, suspending waits for `CompletableFuture`, JetStream stream and consumer management, KeyValue and ObjectStore configuration, and NATS Service construction.

The module is not a broker or a separate messaging framework. jNATS and the NATS server still define connection behavior, reconnects, dispatcher threads, subject routing, JetStream persistence, and acknowledgments. This manual separates the code removed by the wrappers from the operational decisions still owned by the application.

## Decide before adoption {#when-to-use}

- Decide whether messages only need current subscribers through core NATS or need storage, redelivery, and acknowledgments through JetStream.
- Assign the component that creates and closes each `Connection`; this module provides neither a connection singleton nor a Spring bean.
- Define whether request timeouts and no-responder results are expected business outcomes or failures.
- Choose whether applications reconcile streams and consumers at startup or a separate deployment step owns them.
- Use KeyValue for small revisioned and watchable state, not as a general database replacement.
- For Spring Boot, declare `nats-spring` in the application and own lifecycle, properties, and health integration there.

## Coordinates {#coordinates}

Consumers manage only the central BOM version, not the individual jNATS version.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-nats")
}
```

Gradle project path: `:bluetape4k-nats`. Source directory: `infra/nats`. Coroutines and `nats-spring` are `compileOnly`; applications using those APIs must provide the runtime dependencies.

## First publish and subscribe {#quick-start}

The smallest core NATS flow opens and closes its connection explicitly.

```kotlin
val options = natsOptions {
    server("nats://localhost:4222")
    maxReconnects(10)
}

Nats.connect(options).use { connection ->
    val subscription = connection.subscribe("orders.created")

    connection.publish("orders.created", "{\"orderId\":\"O-100\"}")
    connection.flush(2.seconds)

    val message = subscription.nextMessage(2.seconds)
    println(message?.data?.toUtf8String())
}
```

`flush` confirms that the server processed prior protocol commands. It is not a JetStream storage acknowledgment. Use a stream and consumer when messages must survive a period without subscribers.

## API selection map {#api-by-task}

| Task | Start with | Boundary to retain |
| --- | --- | --- |
| Build connection options | `natsOptions`, `natsOptionsOf` | Builders do not create or close a connection. |
| Core publish and request | `Connection.publish`, `requestAsync`, `requestSuspending` | Core publish has no persistence acknowledgment. |
| Messages and subscriptions | `natsMessageOf`, `Subscription.nextMessage` | The next message is `null` on timeout. |
| Callback dispatch | jNATS `createDispatcher` | The caller owns callback threads, unsubscribe, and drain ordering. |
| JetStream publish | `JetStream.publishSuspending` | A successful `PublishAck` confirms stream storage. |
| Stream and consumer management | `JetStreamManagement` extensions, `consumerContextOf` | Replace, purge, and delete mutate operational state. |
| KeyValue and ObjectStore | Configuration DSLs and management extensions | Both use JetStream and inherit server constraints. |
| Service endpoints | `endpointOf`, `serviceEndpointOf`, `natsServiceOf` | The caller owns service start, stop, and handler failure policy. |

## Learning path {#concepts}

These six chapters follow the 1.12.1 release source and tests from connection ownership through core messaging, JetStream, and operations. Each chapter includes a runnable flow, failure conditions, and source anchors for deeper study.

1. [Connection and options ownership](./bluetape4k-nats/connection-options-ownership.md) — assign option, connection, reconnect, and shutdown responsibilities.
2. [Core pub-sub and request-reply](./bluetape4k-nats/core-pubsub-request-reply.md) — distinguish ephemeral delivery, flush, timeouts, and no responders.
3. [Messages, subscriptions, and dispatchers](./bluetape4k-nats/messages-subscriptions-dispatchers.md) — build messages and choose blocking pulls or callback dispatch.
4. [JetStream streams and consumers](./bluetape4k-nats/jetstream-streams-consumers.md) — use publish acknowledgments, stream reconciliation, durable consumers, and fetch limits.
5. [KeyValue, ObjectStore, and Service APIs](./bluetape4k-nats/keyvalue-objectstore-services.md) — separate revisioned state, chunked objects, and request handlers.
6. [Failures, testing, operations, and ecosystem boundaries](./bluetape4k-nats/failures-testing-operations-ecosystem.md) — handle not-found, Testcontainers, telemetry, and Spring integration ownership.

For a first integration, follow chapters 1→2→3 with core NATS. Add chapter 4 when storage and reprocessing are required, then use chapter 5 only for JetStream-backed state, files, or services.

## Recommended patterns {#patterns}

Tie one connection to an application lifecycle instead of calling `Nats.connect` per operation. `use` is convenient for tests and short batch jobs; long-running services should create the connection during startup and define graceful drain and close ordering.

Do not collapse core NATS and JetStream into a vague delivery guarantee. Core subjects fit immediate notifications where loss is acceptable. Restart-safe processing, durable consumers, replay, and acknowledgment require a stream whose retention, storage, and replicas are managed as operational configuration.

## Integrations {#integrations}

The module exposes `jnats` as an API dependency. Coroutine bridges and `nats-spring` are `compileOnly`. Applications using `requestSuspending`, `publishSuspending`, or `drainSuspending` must add the coroutine runtime.

This artifact does not auto-configure Spring Boot. Its main source has no `@AutoConfiguration`, bean factory, properties type, health indicator, or `src/main/resources`. A Spring application must declare `nats-spring`, configure the integration, and ensure that only one component creates and closes the shared `Connection`.

## Configuration {#configuration}

There is no module-specific property namespace. Configure server URLs, authentication, TLS, reconnects, buffers, pings, listeners, and error handling through jNATS `Options.Builder`.

```kotlin
val options = natsOptions {
    servers(arrayOf(primaryUrl, secondaryUrl))
    maxReconnects(20)
    connectionListener { connection, event ->
        logger.info { "NATS event=$event servers=${connection.servers}" }
    }
}
```

Do not log URLs containing credentials or tokens. Treat stream and consumer configuration as deployment and operations data separate from connection options.

## Failure behavior {#failures}

The wrappers throw `IllegalArgumentException` for blank required subjects, stream names, and bucket names. Other connection, timeout, protocol, and JetStream failures remain jNATS exceptions or failed `CompletableFuture` results.

`forcedDelete*`, `get*OrNull`, and `exists*` translate only the JetStream not-found code into normal control flow. Network `IOException`, authorization errors, and other API errors still propagate. A `forced` prefix does not mean unconditional success.

## Operations {#operations}

Observe connection state, reconnect attempts, slow consumers, dropped messages, request timeouts, and dispatcher backlog. For JetStream, add publish acknowledgment latency, consumer pending and redelivery counts, acknowledgment floor, stream bytes and messages, and storage status.

Stream replacement, purge, consumer deletion, and KeyValue or ObjectStore bucket deletion are destructive control operations. Keep them out of ordinary request handling and place them in restricted startup reconciliation or a separate operations tool.

## Testing {#testing}

MockK unit tests cover builder and delegation contracts. Actual publish, request, JetStream, KeyValue, and ObjectStore examples use the `NatsServer` Testcontainer.

```bash
./gradlew :bluetape4k-nats:test --no-configuration-cache
```

`AbstractNatsTest` is a test-source fixture, not a published API. Run the Testcontainers suite sequentially with other infrastructure tests. Include deliberate server interruption, reconnect, request timeout, and consumer redelivery cases in application tests.

## Workshops and runnable examples {#workshops}

The module test source acts as a compact workshop. `PubSubExample` and `RequestReplyExample` cover core messaging, `KeyValueIntroExamples` covers revisions and watchers, and `ObjectStoreExample` covers chunked upload with digest checks. The JetStream `simple` examples progress from `ConsumerContext` to `next`, `fetch`, and iterable consumption.

Do not copy test settings directly into production. The examples favor memory storage and short timeouts; choose retention, replicas, file storage, and shutdown thresholds for the actual service.

## 1.12.1 scope {#limitations}

This manual targets release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e` for version 1.12.1. The module contains jNATS builder DSLs and small extensions. It does not provide a broker, schema registry, serialization contract, retry framework, outbox, or tracing instrumentation.

Spring Boot auto-configuration and a Spring Cloud Stream binder are also outside the module. `nats-spring` is only a compile-time API edge; the application supplies the dependency and lifecycle configuration.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### nats Class Structure diagram

[![nats Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-nats-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-nats-diagram-01.svg)

_Release README: [`infra/nats/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/nats/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`build.gradle.kts`](../../../../infra/nats/build.gradle.kts)
- [`Options.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/Options.kt)
- [`ConnectionExtensions.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ConnectionExtensions.kt)
- [`SubscriptionExtensions.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/SubscriptionExtensions.kt)
- [`JetStream.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStream.kt)
- [`JetStreamManagement.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStreamManagement.kt)
- [`KeyValueManagement.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/KeyValueManagement.kt)
- [`ObjectStreamManagement.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/client/ObjectStreamManagement.kt)
- [`Service.kt`](../../../../infra/nats/src/main/kotlin/io/bluetape4k/nats/service/Service.kt)
- [`SimplePublishExample.kt`](../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/SimplePublishExample.kt)
- [`NatsManagementExtensionsTest.kt`](../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsManagementExtensionsTest.kt)
- [`ServiceExtensionsTest.kt`](../../../../infra/nats/src/test/kotlin/io/bluetape4k/nats/service/ServiceExtensionsTest.kt)
