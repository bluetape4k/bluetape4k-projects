package io.bluetape4k.junit5.output

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.bluetape4k.logging.KLogging
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * 로그 메시지를 메모리에 캡쳐하기 위한 Logback Appender입니다.
 *
 * ```
 * private lateinit var appender: InMemoryLogbackAppender
 *
 * @BeforeEach
 * fun beforeEach() {
 *     appender = InMemoryLogbackAppender(InMemoryLogbackAppenderTest::class)
 * }
 * @AfterEach
 * fun aferEach() {
 *     if (this::appender.isInitialized) {
 *         appender.stop()
 *     }
 * }
 * @RepeatedTest(REPEAT_SIZE)
 * fun `capture logback log messages`() {
 *     val firstMessage = "First message - ${System.currentTimeMillis()}"
 *     log.debug { firstMessage }
 *     appender.lastMessage shouldBeEqualTo firstMessage
 *     appender.size shouldBeEqualTo 1

 *     val secondMessage = "Second message - ${System.currentTimeMillis()}"
 *     log.debug { secondMessage }
 *     appender.lastMessage shouldBeEqualTo secondMessage
 *     appender.size shouldBeEqualTo 2
 * }
 * ```
 *
 * NOTE: 단 parallel 테스트 시에는 제대로 Logger를 casting 할 수 없습니다.
 * HINT : http://www.slf4j.org/codes.html#substituteLogger
 */
class InMemoryLogbackAppender private constructor(name: String): AppenderBase<ILoggingEvent>() {

    companion object: KLogging() {

        @JvmStatic
        operator fun invoke(name: String = "root"): InMemoryLogbackAppender = InMemoryLogbackAppender(name)

        @JvmStatic
        operator fun invoke(clazz: Class<*>): InMemoryLogbackAppender = invoke(clazz.canonicalName)

        @JvmStatic
        operator fun invoke(kclazz: KClass<*>): InMemoryLogbackAppender = invoke(kclazz.qualifiedName!!)
    }

    private val logger by lazy(LazyThreadSafetyMode.PUBLICATION) {
        var logger = LoggerFactory.getLogger(name)
        while (logger !is ch.qos.logback.classic.Logger) {
            Thread.sleep(1)
            logger = LoggerFactory.getLogger(name)
        }

        LoggerFactory.getLogger(name) as ch.qos.logback.classic.Logger
    }

    private val events = mutableListOf<ILoggingEvent>()

    val size: Int get() = events.size
    val lastMessage: String? get() = events.lastOrNull()?.message
    val messages: List<String> get() = events.map { it.message }

    init {
        start()
        logger.addAppender(this)
    }

    override fun append(event: ILoggingEvent?) {
        event?.let { events.add(it) }
    }

    override fun stop() {
        logger.detachAppender(this)
        clear()
        super.stop()
    }

    fun clear() {
        events.clear()
    }
}
