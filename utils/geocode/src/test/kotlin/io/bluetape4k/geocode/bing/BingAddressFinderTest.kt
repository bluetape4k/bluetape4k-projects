package io.bluetape4k.geocode.bing

import io.bluetape4k.geocode.AbstractGeocodeTest
import io.bluetape4k.geocode.Geocode
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BingAddressFinderTest: AbstractGeocodeTest() {

    companion object: KLoggingChannel()

    private val addressFinder = BingAddressFinder()

    @ParameterizedTest(name = "find by geocode {0}")
    @MethodSource("getGeocodes")
    fun `find address for geocode`(geocode: Geocode) {
        val address = addressFinder.findAddress(geocode)
        log.debug { "geocode=$geocode, addres=$address" }
        address.shouldNotBeNull()
        address.country shouldBeEqualTo "Republic of Korea"
    }

    @ParameterizedTest(name = "find by geocode {0}")
    @MethodSource("getGeocodes")
    fun `async find address for geocode`(geocode: Geocode) = runSuspendIO {
        val address = addressFinder.findAddressAsync(geocode)
        log.debug { "geocode=$geocode, addres=$address" }
        address.shouldNotBeNull()
        address.country shouldBeEqualTo "Republic of Korea"
    }
}
