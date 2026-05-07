package io.bluetape4k.netty

import io.bluetape4k.assertions.shouldBeInstanceOf
import org.junit.jupiter.api.Test

/**
 * [NettyTransportSupport]의 기능을 검증하는 테스트 클래스입니다.
 */
class NettyTransportSupportTest: AbstractNettyTest() {
    @Test
    fun `isPresentNettyTransportNativeEpoll은 Boolean을 반환한다`() {
        val result = isPresentNettyTransportNativeEpoll()
        result shouldBeInstanceOf Boolean::class
    }

    @Test
    fun `isPresentNettyTransportNativeKQueue는 Boolean을 반환한다`() {
        val result = isPresentNettyTransportNativeKQueue()
        result shouldBeInstanceOf Boolean::class
    }
}
