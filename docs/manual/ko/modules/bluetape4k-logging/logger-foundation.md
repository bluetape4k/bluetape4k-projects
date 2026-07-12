---
title: Logger foundation
description: KLogging과 KotlinLogging의 이름 결정, 초기화, SLF4J provider 경계를 선택합니다.
manualId: bluetape4k-logging
chapterId: logger-foundation
---

# Logger foundation

Logger 선언 방식은 호출 위치, 이름 안정성, 테스트에서의 식별 가능성을 결정합니다.

![KLogging, KotlinLogging, KLoggerFactory와 SLF4J의 책임 경계](../../../assets/logging/logger-api-map.svg)

## 두 진입점

`KLogging`은 class나 companion object가 상속하고 `log` property를 공유할 때 적합합니다. `log`는 Kotlin `lazy`로 첫 접근에 한 번 생성됩니다.

```kotlin
class InvoiceService {
    companion object : KLogging()

    fun issue() {
        log.info { "Issuing invoice" }
    }
}
```

`KotlinLogging`은 top-level 코드, 명시적 category, `KClass` 기반 이름에 적합합니다.

```kotlin
private val auditLog = KotlinLogging.logger("billing.audit")
private val fileLog = KotlinLogging.logger { }
val serviceLog = KotlinLogging.logger(InvoiceService::class)
```

## 이름 계약

`logger(name)`은 blank 이름을 `IllegalArgumentException`으로 거부합니다. lambda entrypoint와 `KLogging`은 내부 `KLoggerNameResolver`가 synthetic/companion 이름을 정규화합니다. 이름이 dashboard와 routing rule의 public key라면 추론 이름보다 명시적 category를 선택합니다.

## Provider 경계

모듈은 SLF4J `Logger`를 반환할 뿐 backend를 설치하지 않습니다. 애플리케이션은 Logback 등 provider 하나와 설정을 소유합니다. library가 appender나 root level을 몰래 바꾸면 host application 정책을 침범합니다.

## 선택 기준

| 상황 | 선택 |
| --- | --- |
| class의 모든 instance가 같은 category 사용 | companion `KLogging()` |
| file/top-level function | `KotlinLogging.logger {}` |
| 안정적인 운영 category 필요 | `logger("stable.name")` |
| type을 명시적으로 반영 | `logger(Type::class)` |

## Source와 tests

- [`KLogging.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/KLogging.kt)
- [`KotlinLogging.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/KotlinLogging.kt)
- [`KLoggerNameResolver.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/internal/KLoggerNameResolver.kt)
- [`KLoggingTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/KLoggingTest.kt)
- [`KotlinLoggingTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/KotlinLoggingTest.kt)

다음: message 계산 비용과 실패를 [Lazy messages](./lazy-messages.md)에서 다룹니다.
