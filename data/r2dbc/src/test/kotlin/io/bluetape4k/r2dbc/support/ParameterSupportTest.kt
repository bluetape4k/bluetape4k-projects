package io.bluetape4k.r2dbc.support

import io.r2dbc.spi.Parameter
import io.r2dbc.spi.Parameters
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [ParameterSupport]의 toParameter 변환 함수들을 검증합니다.
 *
 * - null 값은 typed-null [Parameter]로 변환됨
 * - 이미 [Parameter] 타입이면 그대로 반환됨
 * - 일반 값은 [Parameters.in]으로 래핑됨
 */
class ParameterSupportTest {

    /**
     * 일반 값은 Parameters.in으로 래핑된 Parameter를 반환해야 합니다.
     */
    @Test
    fun `toParameter - 일반 값을 Parameter 로 변환한다`() {
        val param = "hello".toParameter()

        param.shouldNotBeNull()
        param.shouldBeInstanceOf<Parameter>()
        param.value shouldBeEqualTo "hello"
    }

    /**
     * 이미 Parameter 타입이면 동일 객체를 그대로 반환해야 합니다.
     * 불필요한 재래핑을 방지합니다.
     */
    @Test
    fun `toParameter - 이미 Parameter 타입이면 그대로 반환한다`() {
        val original = Parameters.`in`("world")
        val param = original.toParameter()

        param shouldBeEqualTo original
    }

    /**
     * null 값에 타입을 지정하면 typed-null Parameter가 반환되어야 합니다.
     * 타입 정보를 유지해야 R2DBC 드라이버가 올바른 컬럼 타입으로 바인딩합니다.
     * InferredParameter.type은 Raw Class가 아닌 래핑된 타입을 반환하므로 null 여부만 확인합니다.
     */
    @Test
    fun `toParameter(type) - null 값을 typed null Parameter 로 변환한다`() {
        val param = null.toParameter(String::class.java)

        param.shouldNotBeNull()
        param.shouldBeInstanceOf<Parameter>()
        param.value.shouldBeNull()
    }

    /**
     * null이 아닌 값에 타입을 지정하면 값이 래핑된 Parameter가 반환되어야 합니다.
     */
    @Test
    fun `toParameter(type) - 값이 있을 때 Parameters_in 으로 래핑한다`() {
        val param = 42.toParameter(Int::class.java)

        param.shouldNotBeNull()
        param.shouldBeInstanceOf<Parameter>()
        param.value shouldBeEqualTo 42
    }

    /**
     * Class.toParameter()는 해당 타입의 null Parameter를 반환해야 합니다.
     * InferredParameter.type은 Raw Class가 아닌 래핑된 타입을 반환하므로 null 여부만 확인합니다.
     */
    @Test
    fun `Class toParameter - null Parameter 를 반환한다`() {
        val param = Long::class.java.toParameter()

        param.shouldNotBeNull()
        param.shouldBeInstanceOf<Parameter>()
        param.value.shouldBeNull()
    }
}
