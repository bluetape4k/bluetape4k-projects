package io.bluetape4k.logging

import io.bluetape4k.logging.internal.KLoggerFactory
import org.slf4j.Logger

/**
 * 클래스/컴패니언에서 상속해 `log` 프로퍼티를 사용하는 로깅 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - `log`는 첫 접근 시 1회 초기화되는 lazy 프로퍼티입니다.
 * - 로거 이름은 현재 인스턴스 클래스 기준으로 해석됩니다.
 * - 수신 객체를 변경하지 않고 로거 인스턴스만 캐시합니다.
 *
 * ```kotlin
 * class Service {
 *   companion object : KLogging()
 * }
 * // Service.log 로 바로 사용 가능
 * ```
 */
open class KLogging {

    /**
     * 현재 타입 기준으로 생성된 SLF4J 로거입니다.
     */
    val log: Logger by lazy { KLoggerFactory.logger(this.javaClass) }
}
