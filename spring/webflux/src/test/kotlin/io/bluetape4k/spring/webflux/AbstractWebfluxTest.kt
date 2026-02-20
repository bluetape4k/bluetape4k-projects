package io.bluetape4k.spring.webflux

import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * Webflux 테스트 공통 기능을 제공하는 베이스 클래스입니다.
 */
abstract class AbstractWebfluxTest {

    companion object: KLoggingChannel()
}
