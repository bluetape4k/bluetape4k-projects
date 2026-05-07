package io.bluetape4k.batch.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

/**
 * [CheckpointJson] JSON 직렬화/역직렬화 엣지 케이스 검증.
 *
 * ## 검증 항목
 * - malformed JSON 입력 시 예외 발생
 * - 존재하지 않는 className 사용 시 ClassNotFoundException 발생
 * - 알 수 없는 추가 필드는 무시하고 파싱 성공
 */
class CheckpointJsonEdgeCaseTest {

    companion object : KLogging()

    private val sut: CheckpointJson = CheckpointJson.jackson3()

    /**
     * malformed JSON 입력 시 명확한 예외 발생 검증.
     *
     * Jackson 파서는 문법적으로 잘못된 JSON을 [Exception]의 서브클래스로 던진다.
     * [CheckpointJson.read]가 예외를 감추지 않고 호출자에게 전파해야 한다.
     */
    @Test
    fun `malformed JSON 입력 시 예외 발생`() {
        val malformedJson = "{malformed_json}"

        assertFailsWith<Exception> {
            sut.read(malformedJson)
        }.shouldNotBeNull()
    }

    /**
     * 존재하지 않는 className 필드 값 사용 시 ClassNotFoundException 발생 검증.
     *
     * [TypedCheckpoint.className]이 classpath에 없는 클래스를 가리키면
     * [Class.forName] 호출에서 [ClassNotFoundException]이 발생해야 한다.
     */
    @Test
    fun `존재하지 않는 className 사용 시 ClassNotFoundException 발생`() {
        // TypedCheckpoint 봉투 형식: {"className":"...", "payload":"..."}
        val jsonWithUnknownClass = """{"className":"com.nonexistent.UnknownClass","payload":"42"}"""

        assertFailsWith<ClassNotFoundException> {
            sut.read(jsonWithUnknownClass)
        }.shouldNotBeNull()
    }

    /**
     * 알 수 없는 추가 필드는 무시하고 파싱 성공 검증.
     *
     * Jackson 기본 설정은 unknown field를 무시(FAIL_ON_UNKNOWN_PROPERTIES=false)하므로
     * [TypedCheckpoint] 봉투에 알려지지 않은 필드가 있어도 파싱에 성공해야 한다.
     * 알려진 필드(className, payload) 값이 정확히 매핑되는지도 확인한다.
     */
    @Test
    fun `unknown field 는 무시하고 파싱 성공`() {
        // TypedCheckpoint 봉투에 unknownExtra 필드 추가
        val jsonWithExtra = """{"className":"java.lang.Long","payload":"99","unknownExtra":999}"""

        val restored = sut.read(jsonWithExtra)

        restored.shouldNotBeNull()
        restored shouldBeInstanceOf Long::class
        restored shouldBeEqualTo 99L
    }
}
