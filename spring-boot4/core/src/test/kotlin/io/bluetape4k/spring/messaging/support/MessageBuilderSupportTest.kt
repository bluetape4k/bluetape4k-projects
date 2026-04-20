package io.bluetape4k.spring.messaging.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.AbstractSpringTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class MessageBuilderSupportTest: AbstractSpringTest() {

    companion object: KLogging()

    @Test
    fun `message 함수는 페이로드를 가진 Message를 생성한다`() {
        val msg = message("hello")

        msg.payload shouldBeEqualTo "hello"
    }

    @Test
    fun `message 함수는 빌더 블록으로 헤더를 설정할 수 있다`() {
        val msg = message("payload") {
            setHeader("key1", "value1")
            setHeader("key2", 42)
        }

        msg.payload shouldBeEqualTo "payload"
        msg.headers["key1"] shouldBeEqualTo "value1"
        msg.headers["key2"] shouldBeEqualTo 42
    }

    @Test
    fun `messageOf는 message의 별칭으로 동일하게 동작한다`() {
        val msg = messageOf("test-payload")

        msg.payload shouldBeEqualTo "test-payload"
        msg.headers.shouldNotBeNull()
    }

    @Test
    fun `messageOf는 초기 헤더 맵을 설정할 수 있다`() {
        val headers = mapOf("a" to 1, "b" to "two")
        val msg = messageOf("payload", headers)

        msg.payload shouldBeEqualTo "payload"
        msg.headers["a"] shouldBeEqualTo 1
        msg.headers["b"] shouldBeEqualTo "two"
    }

    @Test
    fun `messageOf는 헤더 맵과 빌더 블록을 함께 사용할 수 있다`() {
        val msg = messageOf("payload", mapOf("a" to 1)) {
            setHeader("b", 2)
        }

        msg.payload shouldBeEqualTo "payload"
        msg.headers["a"] shouldBeEqualTo 1
        msg.headers["b"] shouldBeEqualTo 2
    }

    @Test
    fun `messageOf에서 빌더 블록이 헤더 맵의 같은 키를 덮어쓴다`() {
        val msg = messageOf("payload", mapOf("key" to "original")) {
            setHeader("key", "overridden")
        }

        msg.headers["key"] shouldBeEqualTo "overridden"
    }

    @Test
    fun `message로 다양한 페이로드 타입을 생성할 수 있다`() {
        val intMsg = message(42)
        intMsg.payload shouldBeEqualTo 42

        val listMsg = message(listOf(1, 2, 3))
        listMsg.payload shouldBeEqualTo listOf(1, 2, 3)

        data class Dto(val id: Long, val name: String)
        val dtoMsg = message(Dto(1L, "test"))
        dtoMsg.payload shouldBeEqualTo Dto(1L, "test")
    }
}
