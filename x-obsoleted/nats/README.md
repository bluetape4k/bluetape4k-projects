# Module bluetape4k-nats

English | [한국어](./README.ko.md)

[NATS.io](https://nats.io/) is a simple, secure, and high-performance open-source messaging system for cloud-native applications, IoT messaging, and microservices architectures.

This module provides extension functions and DSLs to make working with NATS more idiomatic in Kotlin.

## Features

- **Kotlin extension functions**: Use the NATS Java client in an idiomatic Kotlin style
- **Coroutines support**: Handle async operations via `suspend` functions
- **JetStream support**: Stream creation, message publish/subscribe, and consumer management
- **NATS Service**: Build microservice endpoints
- **DSL support**: Fluent DSLs for configuring Streams, Consumers, Key-Value stores, and Object Stores

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-nats:${bluetape4kVersion}")
}
```

## Key Features

### 1. Connection Extension Functions

```kotlin
import io.bluetape4k.nats.client.*
import io.nats.client.Nats
import kotlin.time.Duration.Companion.seconds

// Create a NATS connection
val connection = Nats.connect("nats://localhost:4222")

// Publish a message
connection.publish("subject", "Hello, NATS!")

// Request-Reply pattern
val response = connection.request("subject", "request body", timeout = 5.seconds)

// Async request
val future = connection.requestAsync("subject", "body")

// Coroutines support
suspend fun coroutineExample() {
    val response = connection.requestSuspending("subject", "body".toUtf8Bytes())
}

// Drain and close connection
connection.drainSuspending(10.seconds)
```

### 2. JetStream Support

```kotlin
import io.bluetape4k.nats.client.*
import io.nats.client.api.StorageType

// Get JetStream context
val jetStream = connection.jetStream()

// Publish a message
val ack = jetStream.publish("stream.subject", "message body")

// Async publish
val future = jetStream.publishAsync("stream.subject", "message body")

// Coroutines support
suspend fun publishAsync() {
    val ack = jetStream.publishSuspending("stream.subject", "message body")
}

// Create a stream
val streamInfo = connection.createStream(
    streamName = "my-stream",
    storageType = StorageType.Memory,
    subjects = arrayOf("events.*")
)
```

### 3. JetStreamManagement

```kotlin
import io.bluetape4k.nats.client.*

val management = connection.jetStreamManagement()

// Stream management
management.createStream("my-stream", subjects = arrayOf("orders.*"))
management.createOrReplaceStream("my-stream", subjects = arrayOf("orders.*"))
management.createStreamOrUpdateSubjects("my-stream", subjects = arrayOf("orders.*", "payments.*"))

// Query streams
val exists = management.streamExists("my-stream")
val info = management.getStreamInfoOrNull("my-stream")

// Delete streams
management.forcedDeleteStream("my-stream")
management.forcedPurgeStream("my-stream")

// Consumer management
val consumerExists = management.consumerExists("my-stream", "my-consumer")
management.forcedDeleteConsumer("my-stream", "my-consumer")
```

The `forcedDelete*`, `forcedPurgeStream`, and `tryDelete` variants treat "target not found" as the normal case and propagate all other JetStream exceptions. This ensures that permission errors or server failures during operation are never silently swallowed.

### 4. Subscription Extensions

```kotlin
import io.bluetape4k.nats.client.nextMessage
import kotlin.time.Duration.Companion.seconds

val subscription = connection.subscribe("subject")
val message = subscription.nextMessage(5.seconds)
```

### 5. NATS Service

```kotlin
import io.bluetape4k.nats.service.*
import io.nats.service.ServiceEndpoint

// Create a service endpoint
val endpoint = ServiceEndpoint.builder()
    .endpoint(Endpoint.builder().name("echo").subject("service.echo").build())
    .handler { msg -> msg.respond(connection, msg.data) }
    .build()

// Create a service (factory function)
val service = natsServiceOf(
    nc = connection,
    name = "my-service",
    version = "1.0.0",
    endpoint
)

// DSL-style creation
val service = natsService {
    connection(connection)
    name("my-service")
    version("1.0.0")
    addServiceEndpoint(endpoint)
}
```

### 6. Stream Configuration DSL

```kotlin
import io.bluetape4k.nats.client.api.*
import io.nats.client.api.*

val config = streamConfiguration {
    name("my-stream")
    subjects("events.*", "logs.*")
    storageType(StorageType.File)
    retentionPolicy(RetentionPolicy.Limits)
    maxMessages(100000)
    maxBytes(1024 * 1024 * 100)  // 100MB
    maxAge(Duration.ofDays(7))
}
```

### 7. Consumer Configuration DSL

```kotlin
import io.bluetape4k.nats.client.api.*

val config = consumerConfiguration {
    name("my-consumer")
    durable("my-consumer-durable")
    deliverPolicy(DeliverPolicy.All)
    ackPolicy(AckPolicy.Explicit)
    maxDeliver(3)
    maxAckPending(1000)
}
```

### 8. Key-Value Store

```kotlin
import io.bluetape4k.nats.client.*

val kvManagement = connection.keyValueManagement()

// Create a bucket
val config = keyValueConfiguration {
    name("my-bucket")
    maxHistoryPerKey(5)
    ttl(3600)  // 1 hour
}
kvManagement.create(config)

// Key-Value operations
val kv = connection.keyValue("my-bucket")
kv.put("key", "value")
val value = kv.get("key")
kv.delete("key")
```

To update an existing bucket's configuration or create it if it doesn't exist:

```kotlin
val config = keyValueConfiguration("my-bucket") {
    maxHistoryPerKey(10)
}
kvManagement.createOrUpdate(config)
```

### 9. Object Store

```kotlin
import io.bluetape4k.nats.client.*
import io.bluetape4k.nats.client.api.*

val objManagement = connection.objectStoreManagement()

// Create a bucket
val config = objectStoreConfiguration {
    name("my-objects")
    maxBytes(1024 * 1024 * 1000)  // 1GB
}
objManagement.create(config)

// Object store operations
val store = connection.objectStore("my-objects")
store.put("file.txt", inputStream)
val obj = store.get("file.txt")
store.delete("file.txt")
```

## Test Support

Extend `AbstractNatsTest` to write tests:

```kotlin
class MyNatsTest: AbstractNatsTest() {

    @Test
    fun `publish and receive a message`() {
        val subject = "test.subject"
        val message = "Hello, NATS!"

        val subscription = connection.subscribe(subject)
        connection.publish(subject, message)

        val received = subscription.nextMessage(5.seconds)
        received.data.toUtf8String() shouldBeEqualTo message
    }
}
```

For rapid regression validation, management API contracts (not-found tolerance, exception propagation, idempotent subject updates) can be verified directly using MockK-based unit tests.

## Examples

More examples are available in the `src/test/kotlin/io/nats/examples` and `src/test/kotlin/io/bluetape4k/nats` packages.

### Key Examples

- `PubSubExample.kt`: Basic publish/subscribe
- `RequestReplyExample.kt`: Request-Reply pattern
- `jetstream/`: JetStream examples
- `service/`: NATS Service examples
- `KeyValueIntroExamples.kt`: Key-Value store examples
- `ObjectStoreExample.kt`: Object Store examples

## References

- [NATS Official Documentation](https://docs.nats.io/)
- [NATS Java Client](https://github.com/nats-io/nats.java)
- [JetStream Documentation](https://docs.nats.io/nats-concepts/jetstream)
- [NATS Service API](https://docs.nats.io/nats-concepts/service)

## License

Apache License 2.0
