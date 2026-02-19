# Module bluetape4k-logback-kafka

## 개요

로그 정보를 Apache Kafka로 직접 전송하는 Logback Appender를 제공합니다. 운영 환경에서 로그를 중앙 집중화하여 ELK Stack이나 다른 로그 분석 시스템으로 전송할 수 있습니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-logback-kafka:${version}")
}
```

## 주요 기능

- **KafkaAppender**: Logback 이벤트를 Kafka로 전송
- **다양한 KeyProvider**: 로그 기반 Partition Key 생성 (Hostname, ThreadName, LoggerName 등)
- **비동기 Exporter**: Kafka 전송 실패 시 대체 Appender 지원
- **Kafka 로그 무한 루프 방지**: Kafka 클라이언트 로그를 별도 처리

## Logback 설정

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Kafka Appender -->
    <appender name="Kafka" class="io.bluetape4k.logback.kafka.KafkaAppender">
        <!-- 로그 포맷 설정 -->
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [${HOSTNAME}] %level [%thread] %logger: %msg%n%throwable</pattern>
        </encoder>

        <!-- Kafka 설정 -->
        <bootstrapServers>localhost:9092</bootstrapServers>
        <topic>application-logs</topic>
        <appendTimestamp>true</appendTimestamp>
        <acks>1</acks>

        <!-- Kafka Producer 추가 설정 -->
        <producerConfig>linger.ms=100</producerConfig>
        <producerConfig>batch.size=16384</producerConfig>
        <producerConfig>compression.type=gzip</producerConfig>

        <!-- Partition Key Provider (기본: NullKafkaKeyProvider) -->
        <keyProvider class="io.bluetape4k.logback.kafka.keyprovider.HostnameKafkaKeyProvider"/>

        <!-- Exporter (기본: DefaultKafkaExporter) -->
        <exporter class="io.bluetape4k.logback.kafka.exporter.DefaultKafkaExporter"/>
    </appender>

    <!-- Console Appender (개발용) -->
    <appender name="Console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [${HOSTNAME}] %highlight(%-5level) [%blue(%.24t)] %yellow(%logger{36}):%line:
                %msg%n%throwable
            </pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 특정 로거에 Kafka Appender 적용 -->
    <logger name="com.example" level="INFO" additivity="false">
        <appender-ref ref="Kafka"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="Console"/>
    </root>
</configuration>
```

## Key Provider 옵션

| KeyProvider                   | 설명                        | 사용 예시         |
|-------------------------------|---------------------------|---------------|
| `NullKafkaKeyProvider`        | Key 없음 (기본값, 라운드로빈 분배)    | 파티션 분산        |
| `HostnameKafkaKeyProvider`    | 호스트명을 Key로 사용             | 서버별 로그 분리     |
| `ThreadNameKafkaKeyProvider`  | 스레드명을 Key로 사용             | 스레드별 로그 분리    |
| `LoggerNameKafkaKeyProvider`  | 로거명을 Key로 사용              | 로거별 로그 분리     |
| `ContextNameKafkaKeyProvider` | Logback Context명을 Key로 사용 | 애플리케이션별 로그 분리 |

```xml
<!-- 호스트명으로 Partitioning -->
<keyProvider class="io.bluetape4k.logback.kafka.keyprovider.HostnameKafkaKeyProvider"/>

        <!-- 스레드명으로 Partitioning -->
<keyProvider class="io.bluetape4k.logback.kafka.keyprovider.ThreadNameKafkaKeyProvider"/>

        <!-- 로거명으로 Partitioning -->
<keyProvider class="io.bluetape4k.logback.kafka.keyprovider.LoggerNameKafkaKeyProvider"/>
```

## Kafka Producer 설정

| 설정                 | 설명             | 기본값            |
|--------------------|----------------|----------------|
| `bootstrapServers` | Kafka 브로커 주소   | localhost:9092 |
| `topic`            | 로그 전송 토픽       | (필수)           |
| `acks`             | 메시지 확인 수준      | 1              |
| `appendTimestamp`  | 타임스탬프 추가 여부    | false          |
| `producerConfig`   | 추가 Producer 설정 | -              |

### 권장 설정

```xml
<!-- 고처리량 설정 -->
<producerConfig>linger.ms=10</producerConfig>
<producerConfig>batch.size=32768</producerConfig>
<producerConfig>compression.type=lz4</producerConfig>

        <!-- 저지연 설정 -->
<producerConfig>linger.ms=0</producerConfig>
<producerConfig>acks=0</producerConfig>

        <!-- 신뢰성 설정 -->
<producerConfig>acks=all</producerConfig>
<producerConfig>retries=3</producerConfig>
<producerConfig>enable.idempotence=true</producerConfig>
```

## Exporter 옵션

| Exporter                     | 설명                 |
|------------------------------|--------------------|
| `DefaultKafkaExporter`       | 비동기 Kafka 전송 (기본값) |
| `NoopExportExceptionHandler` | 전송 실패 시 무시         |

```xml

<exporter class="io.bluetape4k.logback.kafka.exporter.DefaultKafkaExporter"/>
```

## 주요 기능 상세

| 파일                                           | 설명                 |
|----------------------------------------------|--------------------|
| `KafkaAppender.kt`                           | Kafka 로그 Appender  |
| `AbstractKafkaAppender.kt`                   | Appender 기본 구현     |
| `keyprovider/KafkaKeyProvider.kt`            | Key Provider 인터페이스 |
| `keyprovider/NullKafkaKeyProvider.kt`        | Key 없음             |
| `keyprovider/HostnameKafkaKeyProvider.kt`    | 호스트명 Key           |
| `keyprovider/ThreadNameKafkaKeyProvider.kt`  | 스레드명 Key           |
| `keyprovider/LoggerNameKafkaKeyProvider.kt`  | 로거명 Key            |
| `keyprovider/ContextNameKafkaKeyProvider.kt` | Context명 Key       |
| `exporter/KafkaExporter.kt`                  | Exporter 인터페이스     |
| `exporter/DefaultKafkaExporter.kt`           | 기본 비동기 Exporter    |
| `exporter/ExportExceptionHandler.kt`         | 전송 실패 처리기          |

## 주의사항

- Kafka 브로커 장애 시 로그 유실 가능성이 있습니다.
- `acks=all` 설정으로 신뢰성을 높일 수 있습니다.
- 대량 로그 전송 시 `linger.ms`와 `batch.size`를 조정하세요.
- Kafka 클라이언트 자체의 로그는 무한 루프 방지를 위해 별도 처리됩니다.
