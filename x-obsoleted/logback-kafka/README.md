# Module bluetape4k-logback-kafka

English | [한국어](./README.ko.md)

## Overview

Provides a Logback Appender that ships log events directly to Apache Kafka. Centralizes logs in production environments for forwarding to the ELK Stack or other log analysis systems.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-logback-kafka:${version}")
}
```

## Key Features

- **KafkaAppender**: Sends Logback events to Kafka
- **Multiple KeyProviders**: Partition key generation based on hostname, thread name, logger name, and more
- **Async Exporter**: Fallback appender support for Kafka delivery failures
- **Infinite loop prevention**: Kafka client logs are handled separately to avoid log feedback loops

## Logback Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Kafka Appender -->
    <appender name="Kafka" class="io.bluetape4k.logback.kafka.KafkaAppender">
        <!-- Log format -->
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [${HOSTNAME}] %level [%thread] %logger: %msg%n%throwable</pattern>
        </encoder>

        <!-- Kafka settings -->
        <bootstrapServers>localhost:9092</bootstrapServers>
        <topic>application-logs</topic>
        <appendTimestamp>true</appendTimestamp>
        <acks>1</acks>

        <!-- Additional Kafka Producer settings -->
        <producerConfig>linger.ms=100</producerConfig>
        <producerConfig>batch.size=16384</producerConfig>
        <producerConfig>compression.type=gzip</producerConfig>

        <!-- Partition Key Provider (default: NullKafkaKeyProvider) -->
        <keyProvider class="io.bluetape4k.logback.kafka.keyprovider.HostnameKafkaKeyProvider"/>

        <!-- Exporter (default: DefaultKafkaExporter) -->
        <exporter class="io.bluetape4k.logback.kafka.exporter.DefaultKafkaExporter"/>
    </appender>

    <!-- Console Appender (for development) -->
    <appender name="Console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [${HOSTNAME}] %highlight(%-5level) [%blue(%.24t)] %yellow(%logger{36}):%line:
                %msg%n%throwable
            </pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- Apply Kafka Appender to specific loggers -->
    <logger name="com.example" level="INFO" additivity="false">
        <appender-ref ref="Kafka"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="Console"/>
    </root>
</configuration>
```

## Key Provider Options

| KeyProvider                   | Description                                   | Use Case                        |
|-------------------------------|-----------------------------------------------|---------------------------------|
| `NullKafkaKeyProvider`        | No key (default, round-robin distribution)    | Even partition spread           |
| `HostnameKafkaKeyProvider`    | Uses hostname as key                          | Separate logs by server         |
| `ThreadNameKafkaKeyProvider`  | Uses thread name as key                       | Separate logs by thread         |
| `LoggerNameKafkaKeyProvider`  | Uses logger name as key                       | Separate logs by logger         |
| `ContextNameKafkaKeyProvider` | Uses Logback context name as key              | Separate logs by application    |

```xml
<!-- Partition by hostname -->
<keyProvider class="io.bluetape4k.logback.kafka.keyprovider.HostnameKafkaKeyProvider"/>

<!-- Partition by thread name -->
<keyProvider class="io.bluetape4k.logback.kafka.keyprovider.ThreadNameKafkaKeyProvider"/>

<!-- Partition by logger name -->
<keyProvider class="io.bluetape4k.logback.kafka.keyprovider.LoggerNameKafkaKeyProvider"/>
```

## Kafka Producer Settings

| Setting              | Description              | Default        |
|--------------------|--------------------------|----------------|
| `bootstrapServers` | Kafka broker address     | localhost:9092 |
| `topic`            | Log destination topic    | (required)     |
| `acks`             | Message acknowledgement  | 1              |
| `appendTimestamp`  | Append timestamp to logs | false          |
| `producerConfig`   | Additional producer config | -             |

### Recommended Configurations

```xml
<!-- High-throughput -->
<producerConfig>linger.ms=10</producerConfig>
<producerConfig>batch.size=32768</producerConfig>
<producerConfig>compression.type=lz4</producerConfig>

<!-- Low-latency -->
<producerConfig>linger.ms=0</producerConfig>
<producerConfig>acks=0</producerConfig>

<!-- High-reliability -->
<producerConfig>acks=all</producerConfig>
<producerConfig>retries=3</producerConfig>
<producerConfig>enable.idempotence=true</producerConfig>
```

## Exporter Options

| Exporter                     | Description                          |
|------------------------------|--------------------------------------|
| `DefaultKafkaExporter`       | Async Kafka delivery (default)       |
| `NoopExportExceptionHandler` | Silently ignores delivery failures   |

```xml
<exporter class="io.bluetape4k.logback.kafka.exporter.DefaultKafkaExporter"/>
```

## Key Files

| File                                           | Description                    |
|----------------------------------------------|--------------------------------|
| `KafkaAppender.kt`                           | Kafka log appender             |
| `AbstractKafkaAppender.kt`                   | Base appender implementation   |
| `keyprovider/KafkaKeyProvider.kt`            | Key provider interface         |
| `keyprovider/NullKafkaKeyProvider.kt`        | No key                         |
| `keyprovider/HostnameKafkaKeyProvider.kt`    | Hostname-based key             |
| `keyprovider/ThreadNameKafkaKeyProvider.kt`  | Thread name-based key          |
| `keyprovider/LoggerNameKafkaKeyProvider.kt`  | Logger name-based key          |
| `keyprovider/ContextNameKafkaKeyProvider.kt` | Context name-based key         |
| `exporter/KafkaExporter.kt`                  | Exporter interface             |
| `exporter/DefaultKafkaExporter.kt`           | Default async exporter         |
| `exporter/ExportExceptionHandler.kt`         | Delivery failure handler       |

## Notes

- Log loss is possible if the Kafka broker is unavailable.
- Use `acks=all` to improve reliability.
- Tune `linger.ms` and `batch.size` for high-volume log shipping.
- Kafka client logs are handled separately to prevent infinite feedback loops.
