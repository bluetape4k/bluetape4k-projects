package io.bluetape4k.logging

import io.bluetape4k.logging.internal.KLoggerFactory
import org.slf4j.Logger
import kotlin.reflect.KClass

/**
 * SLF4J [Logger]를 생성하는 진입점 유틸리티입니다.
 *
 * ## 동작/계약
 * - 이름 기반/람다 호출 위치 기반/KClass 기반 로거 생성을 제공합니다.
 * - `logger(name)`은 blank 이름을 허용하지 않으며 위반 시 `IllegalArgumentException`이 발생합니다.
 * - 로거 생성은 내부적으로 `KLoggerFactory`에 위임합니다.
 *
 * ```kotlin
 * val logA = KotlinLogging.logger("my.logger")
 * val logB = KotlinLogging.logger { }
 * // logA.name == "my.logger"
 * ```
 */
object KotlinLogging {

    /**
     * 지정한 이름으로 로거를 생성합니다.
     *
     * ## 동작/계약
     * - `name.isNotBlank()`를 검증합니다.
     * - 이름이 blank면 `IllegalArgumentException`이 발생합니다.
     *
     * ```kotlin
     * val log = KotlinLogging.logger("app.main")
     * // log.name == "app.main"
     * ```
     *
     * @param name 생성할 로거 이름입니다. blank면 예외가 발생합니다.
     */
    fun logger(name: String): Logger {
        require(name.isNotBlank()) { "Logger name must not be blank" }
        return KLoggerFactory.logger(name)
    }

    /**
     * 람다 호출 위치를 기준으로 로거를 생성합니다.
     *
     * ## 동작/계약
     * - 내부적으로 람다의 클래스명을 해석해 로거 이름을 만듭니다.
     * - 파일/컴패니언/내부 클래스 이름은 해석 규칙에 따라 정규화됩니다.
     *
     * ```kotlin
     * val log = KotlinLogging.logger { }
     * // 호출 위치 기준 이름으로 로거가 생성된다.
     * ```
     *
     * @param action 로거 이름 해석에 사용할 람다입니다.
     */
    fun logger(action: () -> Unit = {}): Logger =
        KLoggerFactory.logger(action)

    /**
     * [kclass]의 이름을 기준으로 로거를 생성합니다.
     *
     * ## 동작/계약
     * - `qualifiedName`이 있으면 우선 사용하고, 없으면 `java.name`을 사용합니다.
     * - 최종 생성은 `logger(String)`에 위임합니다.
     *
     * ```kotlin
     * val log = KotlinLogging.logger(String::class)
     * // log.name == "kotlin.String" 또는 JVM 이름
     * ```
     *
     * @param kclass 로거 이름의 기준이 되는 클래스입니다.
     */
    fun logger(kclass: KClass<*>): Logger {
        val loggerName = kclass.qualifiedName ?: kclass.java.name
        return logger(loggerName)
    }
}
