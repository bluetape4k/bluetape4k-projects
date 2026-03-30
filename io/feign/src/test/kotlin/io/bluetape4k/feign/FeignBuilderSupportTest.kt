package io.bluetape4k.feign

import feign.Client
import feign.Request
import feign.Request.HttpMethod
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class FeignBuilderSupportTest {

    @Test
    fun `feignBuilder 는 기본 설정으로도 builder 를 생성한다`() {
        val builder = feignBuilder { }

        builder.shouldNotBeNull()
    }

    @Test
    fun `feignRequestOf 는 body null 이면 본문 없는 요청을 생성한다`() {
        val request = feignRequestOf(
            url = "https://example.com/health",
            httpMethod = HttpMethod.GET,
        )

        request.httpMethod() shouldBeEqualTo HttpMethod.GET
        request.body().shouldBeNull()
    }

    @Test
    fun `feingBuilderOf deprecated wrapper 는 feignBuilderOf 와 동일한 타입의 builder 를 반환한다`() {
        val markerClient = Client { _, _ -> error("not used") }

        val builder = feignBuilderOf(client = markerClient)
        val deprecatedBuilder = feingBuilderOf(client = markerClient)

        builder::class shouldBeEqualTo deprecatedBuilder::class
    }
}
