package io.bluetape4k.junit5.output

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.bluetape4k.logging.KLogging
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * 지정한 logger의 Logback 이벤트를 메모리에 수집하는 테스트용 Appender입니다.
 *
 * ## 동작/계약
 * - 생성 시 즉시 `start()` 후 대상 logger에 attach됩니다.
 * - 이벤트 목록은 `CopyOnWriteArrayList`로 저장되어 읽기/추가 동시 접근에 안전합니다.
 * - [stop] 호출 시 logger에서 detach되고 내부 이벤트를 비웁니다.
 * - SLF4J substitute logger 상태에서는 logger 획득 대기를 반복할 수 있습니다.
 *
 * ```kotlin
 * val appender = InMemoryLogbackAppender("root")
 * // 로그 발생 후 appender.messages.isNotEmpty() == true
 * appender.stop()
 * // appender.size == 0
 * ```
 */
class InMemoryLogbackAppender private constructor(name: String): AppenderBase<ILoggingEvent>() {

    companion object: KLogging() {
        /**
         * 이름으로 appender를 생성해 연결합니다.
         *
         * ## 동작/계약
         * - 생성 즉시 대상 logger에 attach됩니다.
         * - 기본 이름은 `"root"`입니다.
         */
        @JvmStatic
        operator fun invoke(name: String = "root"): InMemoryLogbackAppender = InMemoryLogbackAppender(name)

        /**
         * 클래스 이름 기반으로 appender를 생성합니다.
         *
         * ## 동작/계약
         * - `canonicalName`이 없으면 `name`을 사용합니다.
         * - 내부적으로 [invoke]를 재사용합니다.
         */
        @JvmStatic
        operator fun invoke(clazz: Class<*>): InMemoryLogbackAppender = invoke(clazz.canonicalName ?: clazz.name)

        /**
         * Kotlin 클래스 기반으로 appender를 생성합니다.
         *
         * ## 동작/계약
         * - `qualifiedName`이 없으면 Java 클래스 이름을 사용합니다.
         * - 내부적으로 [invoke]를 재사용합니다.
         */
        @JvmStatic
        operator fun invoke(kclazz: KClass<*>): InMemoryLogbackAppender =
            invoke(kclazz.qualifiedName ?: kclazz.java.name)
    }

    private val logger: ch.qos.logback.classic.Logger by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val maxAttempts = 1000
        var attempts = 0
        var logger: org.slf4j.Logger = LoggerFactory.getLogger(name)
        while (logger !is ch.qos.logback.classic.Logger) {
            if (++attempts >= maxAttempts) {
                throw IllegalStateException(
                    "SLF4J 바인딩이 Logback이 아닙니다. InMemoryLogbackAppender는 Logback 환경에서만 사용할 수 있습니다. " +
                        "현재 바인딩: ${logger.javaClass.name}"
                )
            }
            Thread.sleep(1)
            logger = LoggerFactory.getLogger(name)
        }
        LoggerFactory.getLogger(name) as ch.qos.logback.classic.Logger
    }

    private val events = CopyOnWriteArrayList<ILoggingEvent>()

    /** 수집된 이벤트 개수입니다. */
    val size: Int get() = events.size

    /** 마지막 이벤트의 메시지 문자열입니다. */
    val lastMessage: String? get() = events.lastOrNull()?.message

    /** 수집된 모든 메시지 문자열 목록입니다. */
    val messages: List<String> get() = events.map { it.message }

    init {
        start()
        logger.addAppender(this)
    }

    /**
     * Logback 이벤트를 수집 목록에 추가합니다.
     *
     * ## 동작/계약
     * - null 이벤트는 무시합니다.
     * - 이벤트 객체는 복사하지 않고 참조를 그대로 저장합니다.
     */
    override fun append(event: ILoggingEvent?) {
        event?.let { events.add(it) }
    }

    /**
     * appender를 중지하고 logger 연결 및 수집 데이터를 정리합니다.
     *
     * ## 동작/계약
     * - 대상 logger에서 현재 appender를 detach합니다.
     * - 내부 이벤트를 비운 뒤 부모 [AppenderBase.stop]을 호출합니다.
     */
    override fun stop() {
        logger.detachAppender(this)
        clear()
        super.stop()
    }

    /**
     * 수집된 이벤트를 모두 제거합니다.
     *
     * ## 동작/계약
     * - 내부 목록만 비우며 logger 연결 상태는 변경하지 않습니다.
     */
    fun clear() {
        events.clear()
    }
}
