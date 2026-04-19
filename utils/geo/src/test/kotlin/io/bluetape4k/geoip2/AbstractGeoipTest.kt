package io.bluetape4k.geoip2

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll

abstract class AbstractGeoipTest {

    companion object: KLogging() {

        /**
         * git-lfs 미설정 환경(CI 등)에서 mmdb 파일이 포인터로만 존재할 경우 테스트를 건너뜁니다.
         */
        @JvmStatic
        val isGeoDbAvailable: Boolean by lazy {
            try {
                Geoip.countryDatabase
                true
            } catch (_: Exception) {
                false
            }
        }

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

    @BeforeAll
    fun assumeGeoDbAvailable() {
        assumeTrue(isGeoDbAvailable, "MaxMind DB를 로드할 수 없습니다 (git-lfs pull 필요). 테스트를 건너뜁니다.")
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
