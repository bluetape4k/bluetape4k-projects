package io.bluetape4k.examples.idgenerator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * bluetape4k idgenerator용 Spring Boot demo application입니다.
 *
 * ## Behavior
 * - Registers `IdGenerator` implementations as Spring beans and injects them into the REST controller.
 * - Provides both explicit `/ids/...` endpoints and generic `/idgen/{type}` endpoints.
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
