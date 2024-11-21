# Module bluetape4k-logback-kafka

로그 정보를 Kafka에 바로 전송하는 Logback Appender를 제공합니다.

## Logback 설정

일반적으로 개발 시에는 `ConsoleAppender`, Production 환경에서는 `RollingFileAppender`를 사용하는데, 이를 `KafkaAppender`를 사용하도록 설정합니다.

```xml
[src/main/resources/logback-sample.xml]
        <?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Appender to log to Kafka -->
    <appender name="Kafka" class="io.bluetape4k.logback.kafka.KafkaAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [${HOSTNAME}] %level [%thread] %logger: %msg\n%throwable</pattern>
        </encoder>

        <!-- Kafka Producer 용 환경설정 -->
        <bootstrapServers>localhost:9093</bootstrapServers>
        <topic>logs</topic>
        <appendTimestamp>true</appendTimestamp>

        <producerConfig>acks=1</producerConfig>
        <producerConfig>linger.ms=100</producerConfig>

        <!-- 로그 정보를 기준으로 Key를 생성하여 Partitioning 을 할 수 있다. 기본값은 NullKafkaKeyProvider 를 사용하세요. -->
        <keyProvider class="io.bluetape4k.logback.kafka.keyprovider.HostnameKafkaKeyProvider"/>
        <!-- 로그를 비동기 방식으로 Kafka 로 Export 하는 KafkaExporter 입니다. -->
        <exporter class="io.bluetape4k.logback.kafka.exporter.DefaultKafkaExporter"/>
    </appender>

    <!-- Appender to log to console -->
    <appender name="Console" class="ch.qos.logback.core.ConsoleAppender">
        <immediateFlush>true</immediateFlush>
        <encoder>
            <!-- @formatter:off -->
            <pattern>%d{HH:mm:ss.SSS} [${HOSTNAME}] %highlight(%-5level) [%blue(%.24t)] %yellow(%logger{36}):%line: %msg%n%throwable</pattern>
            <!-- @formatter:on -->
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <logger name="LogbackIntegrationTest" level="INFO" additivity="false">
        <appender-ref ref="Kafka"/>
    </logger>

    <logger name="io.bluetape4k" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="Console"/>
    </root>

</configuration>
```
