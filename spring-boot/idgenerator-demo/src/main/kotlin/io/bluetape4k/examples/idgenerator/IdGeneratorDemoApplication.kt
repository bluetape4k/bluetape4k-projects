package io.bluetape4k.examples.idgenerator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * bluetape4k idgenerator Spring Boot 예제 애플리케이션입니다.
 *
 * ## 동작/계약
 * - `IdGenerator` 구현체를 Spring Bean으로 등록하고 REST controller에서 주입받아 사용합니다.
 * - `/ids/...` 명시적 endpoint와 `/idgen/{type}` generic endpoint를 모두 제공합니다.
 *
 * ```kotlin
 * fun main(args: Array<String>) {
 *     runApplication<IdGeneratorDemoApplication>(*args)
 * }
 * ```
 */
@ConfigurationPropertiesScan
@SpringBootApplication(proxyBeanMethods = false)
class IdGeneratorDemoApplication

fun main(args: Array<String>) {
    runApplication<IdGeneratorDemoApplication>(*args)
}
