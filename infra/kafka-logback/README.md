# bluetape4k-kafka-logback

[한국어](README.ko.md)

A Logback appender that delivers log events to Apache Kafka topics. It uses asynchronous Kafka delivery by default, while keeping key selection, export behavior, and exception fallback pluggable.

## Architecture

![Kafka Logback Class Structure](../../docs/images/readme-diagrams/infra-kafka-logback-diagram-01.png)

### Append Sequence

![Kafka Logback Append Flow](../../docs/images/readme-diagrams/infra-kafka-logback-sequence-01.png)

## Features

- **KafkaAppender** — Logback `AppenderBase` implementation that sends log events to Kafka
- **Key Providers** — Pluggable strategies: hostname, logger name, thread name, context name, or null
- **KafkaExporter** — Decoupled send logic with configurable exception handling
- **Async by default** — Fire-and-forget via `KafkaProducer.send()`

## Usage

### logback.xml

```xml
<appender name="KAFKA" class="io.bluetape4k.kafka.logback.KafkaAppender">
    <topic>application-logs</topic>
    <bootstrapServers>localhost:9092</bootstrapServers>
    <keyProvider class="io.bluetape4k.kafka.logback.keyprovider.HostnameKafkaKeyProvider"/>
    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
        <pattern>%d{ISO8601} %-5level [%thread] %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<root level="INFO">
    <appender-ref ref="KAFKA"/>
</root>
```

### Custom Key Provider

```kotlin
class ServiceNameKeyProvider : AbstractKafkaKeyProvider<ILoggingEvent>() {
    override fun getKey(event: ILoggingEvent): ByteArray? =
        System.getenv("SERVICE_NAME")?.toByteArray()
}
```

## Dependencies

- `logback-classic` — Appender base classes and `ILoggingEvent`
- `kafka-clients` — `KafkaProducer` for message delivery
- `bluetape4k-core` — Kotlin utilities
