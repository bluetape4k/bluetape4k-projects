package io.bluetape4k.logging.internal

import io.bluetape4k.logging.KLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal object KLoggerFactory {

    /**
     * logger name이 [name]인 [org.slf4j.Logger]를 생성합니다.
     *
     * @param name logger name
     * @return slf4j Logger instance
     */
    fun logger(name: String): Logger {
        require(name.isNotBlank()) { "Logger name must not be blank" }
        return LoggerFactory.getLogger(name)
    }

    /**
     * 해당 함수가 호출되는 package name을 logger의 name으로 하는 [org.slf4j.Logger]를 생성합니다.
     */
    fun logger(action: () -> Unit): Logger {
        return logger(KLoggerNameResolver.name(action))
    }

    /**
     * [clazz]의 qualified name을 logger name으로 하는 [org.slf4j.Logger]를 생성합니다.
     *
     * @param clazz clazz의 qualified name이 logger name이 됩니다.
     * @return slf4j Logger instance
     */
    fun logger(clazz: Class<*>): Logger = logger(KLoggerNameResolver.name(clazz))

    /**
     * Logger
     *
     * @param klogging [KLogging] instance
     * @return slf4j Logger instance
     */
    fun logger(klogging: KLogging): Logger = logger(KLoggerNameResolver.name(klogging.javaClass))
}
