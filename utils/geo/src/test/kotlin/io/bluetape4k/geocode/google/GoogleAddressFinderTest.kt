package io.bluetape4k.geocode.google

import io.bluetape4k.geocode.AbstractGeocodeTest
import io.bluetape4k.geocode.Geocode
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class GoogleAddressFinderTest: AbstractGeocodeTest() {

    companion object: KLoggingChannel()

    val addressFinder by lazy { GoogleAddressFinder() }

    @BeforeEach
    fun skipIfNoApiKey() {
        assumeTrue(
            System.getenv("GOOGLE_GEOCODE_API_KEY").isNullOrBlank().not(),
            "GOOGLE_GEOCODE_API_KEY 환경변수가 없어 테스트를 건너뜁니다."
        )
    }

    @ParameterizedTest(name = "find seoul with scaled {0}")
    @ValueSource(ints = [1, 2, 3, 4])
    fun `find address of seoul`(scale: Int) {
        val address = addressFinder.findAddress(seoul.round(scale))
        log.debug { "seoul addres=$address" }
        verifySeoul(address)
    }

    @ParameterizedTest(name = "find by geocode {0}")
    @MethodSource("getGeocodes")
    fun `find address for geocode`(geocode: Geocode) {
        val address = addressFinder.findAddress(geocode)
        log.debug { "geocode=$geocode, addres=$address" }
        address.shouldNotBeNull()
        address.country shouldBeEqualTo "대한민국"
    }

    @ParameterizedTest(name = "find seoul with scaled {0}")
    @ValueSource(ints = [1, 2, 3, 4])
    fun `async find address of seoul`(scale: Int) = runSuspendIO {
        val address = addressFinder.suspendFindAddress(seoul.round(scale))
        log.debug { "addres=$address" }
        verifySeoul(address)
    }

    @ParameterizedTest(name = "find by geocode {0}")
    @MethodSource("getGeocodes")
    fun `async find address for geocode`(geocode: Geocode) = runSuspendIO {
        val address = addressFinder.suspendFindAddress(geocode)
        log.debug { "geocode=$geocode, addres=$address" }
        address.shouldNotBeNull()
        address.country shouldBeEqualTo "대한민국"
    }
}
