package io.bluetape4k.logging

import io.bluetape4k.logging.internal.KLoggerFactory
import org.slf4j.Logger
import kotlin.reflect.KClass

/**
 * sfl4j 용 Logger 생성을 도와주는 함수들을 제공합니다.
 *
 * ```
 * // 이름이 mylogger인 Logger를 생성합니다.
 * val log = KotlinLogging.logger("mylogger")
 *
 * // action이 속한 package name이 Logger name이 됩니다.
 * val log = KotlinLoggging.logger {}
 * ```
 */
object KotlinLogging {

    /**
     * 이름이 [name]인 Logger ([org.slf4j.Logger]) 를 생성합니다.
     *
     * ```
     * val log = KotlinLogging.logger("mylogger")
     * ```
     *
     * @param name Logger name
     * @return Logger instance
     */
    fun logger(name: String): Logger {
        require(name.isNotBlank()) { "Logger name must not be blank" }
        return KLoggerFactory.logger(name)
    }

    /**
     * [action]이 속한 package name이 Logger name이 됩니다.
     *
     * ```
     * val log = KotlinLogging.logger {}
     * ```
     *
     * @param action [action]이 속한 package name이 Logger name이 됩니다.
     * @return Logger instance
     */
    fun logger(action: () -> Unit = {}): Logger =
        KLoggerFactory.logger(action)

    /**
     * [kclass]의 name을 Logger name으로하는 Logger 를 생성합니다.
     *
     * @param kclass Logger가 될 수형
     * @return Logger instance
     */
    fun logger(kclass: KClass<*>): Logger {
        val loggerName = kclass.qualifiedName ?: kclass.java.name
        return logger(loggerName)
    }
}
