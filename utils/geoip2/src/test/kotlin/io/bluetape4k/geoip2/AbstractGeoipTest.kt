package io.bluetape4k.geoip2

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging

abstract class AbstractGeoipTest {

    companion object: KLogging() {
        /**
         * GeoIP2 는 JSON 포맷에 Snake Case 명명규칙을 사용합니다.
         */
        @JvmStatic
        protected val jsonMapper: JsonMapper by lazy {
            Jackson.defaultJsonMapper.apply {
                setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            }
        }
    }

    protected fun getIpAddresses() = listOf(
        "8.8.8.8",              // Google
        "172.217.161.174",
        "116.126.87.92",
        "15.165.181.38",
        "210.89.164.90",
    )

    protected fun CountryResponse.prettyPrint(): String = buildString {
        val response = this@prettyPrint
        append("country=").appendLine(response.country().name())
        append("registered country=").appendLine(response.registeredCountry().name())
        append("continent=").appendLine(response.continent().name())
        append("traits=").appendLine(response.traits())
    }

    protected fun CityResponse.prettyPrint(): String = buildString {
        val response = this@prettyPrint
        append("country=").appendLine(response.country().name())
        append("city=").appendLine(response.city().name())
        append("location=").appendLine(response.location())
        append("continent=").appendLine(response.continent().name())
        append("traits=").appendLine(response.traits())
    }
}
