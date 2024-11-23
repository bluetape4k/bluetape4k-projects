package io.bluetape4k.logging

import io.bluetape4k.logging.internal.KLoggerFactory
import org.slf4j.Logger

/**
 * 클래스의 Companion Object에서 [KLogging]을 상속받으면 [org.slf4j.Logger]를 전역적으로 사용할 수 있습니다.
 *
 * ```
 * class SomeClass {
 *     companion object: KLogging()
 *
 *     fun someMethod() {
 *          log.debug { "someMethod" }
 *     }
 * }
 * ```
 */
open class KLogging {

    val log: Logger by lazy(LazyThreadSafetyMode.NONE) { KLoggerFactory.logger(this.javaClass) }

}
