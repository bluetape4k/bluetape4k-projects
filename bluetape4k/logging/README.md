# Module bluetape4k-logging

## 개요

Sfl4j Logging Framework를 Kotlin에서 쉽게 사용하기 위해 제공되는 라이브러리입니다.

## 사용법

bluetape4k-logging 사용법은 slf4j와 같은 방식으로 logging을 수행합니다.
추가적으로 Kotlin 언어의 lambda 방식으로 로그메시지를 생성할 수 있어, 성능부하를 최소화했습니다.

### 클래스의 로깅

클래스 내부에서 로깅을 사용하기 위해서는 companion object를 `KLogging` 으로부터 상속받으면 static한 logger를 사용할 수 있습니다.

```kotlin
class SomeClass {
    companion object: KLogging()    // Define KotlinLogger

    fun someMethod(param1: String) {
        try {
            log.debug { "Some logging... param1=$param1" } // Write debug log message
        } catch (e: Exception) {
            log.error(e) { "Fail to execute method. param1=$param1" } // Write error log message
        }
    }
}
```

### package method 에서 사용하기

package method 에서 logging하기 위해서는 다음과 같이 log를 선언하시고, 사용하시면 됩니다.

```kotlin
private val log = KotlinLogging.logger {}
private val loggerWithName = KotlinLogging.logger("example")

fun method1(arg1: String, arg2: String) {
    log.debug { "method1 arg1=$arg1, arg2=$arg2" }
    // Somthing to do
}
```

### MDC Context 사용하기

Slf4j 의 MDC Context를 사용하기 위해서는 `withLoggingContext` 함수를 사용하시면 됩니다.

```kotlin
withLoggingContext("traceId" to 100, "spanId" to 200) {
    // MDC.put("traceId", "200")
    log.debug { "Inside with MDCContext" }

    withLoggingContext("traceId" to "200", "spanId" to 300) {
        // MDC.put("traceId", "200")
        log.debug { "Nested with MDCContext" }
        MDC.get("traceId") shouldBeEqualTo "200"
        MDC.get("spanId") shouldBeEqualTo "300"
    }

    MDC.get("traceId") shouldBeEqualTo "100"
    MDC.get("spanId") shouldBeEqualTo "200"
}
```

위의 MDC 항목을 Log 메시지에 포함시키기 위해서는 `traceId=%X{traceId}`, `spanId=%X{spanId}` 같이 항목을 pattern 에 추가해주셔야 합니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="Console" class="ch.qos.logback.core.ConsoleAppender">
        <immediateFlush>true</immediateFlush>
        <encoder>
            <!-- @formatter:off -->
            <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) [traceId=%X{traceId}][spanId=%X{spanId}][%.24thread] %logger{36}:%line: %msg%n%throwable</pattern>
            <!-- @formatter:on -->
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <logger name="io.bluetape4k" level="TRACE"/>

    <root level="INFO">
        <appender-ref ref="Console"/>
    </root>
</configuration>
```
