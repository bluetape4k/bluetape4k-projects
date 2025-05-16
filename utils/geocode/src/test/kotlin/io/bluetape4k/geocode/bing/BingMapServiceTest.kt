package io.bluetape4k.geocode.bing

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.geocode.AbstractGeocodeTest
import io.bluetape4k.geocode.Geocode
import io.bluetape4k.geocode.bing.BingMapModel.toBingAddress
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BingMapServiceTest: AbstractGeocodeTest() {

    companion object: KLoggingChannel()

    private val client = BingMapService.getBingMapFeignClient()
    private val coroutineClient = BingMapService.getBingMapFeignCoroutineClient()

    @Test
    fun `get BingMapApi feign client`() {
        val client = BingMapService.getBingMapFeignCoroutineClient()
        client.shouldNotBeNull()
    }

    @ParameterizedTest(name = "find address by geocode {0}")
    @MethodSource("getGeocodes")
    fun `get location by geocode`(geocode: Geocode) {
        val location = client.locations(
            geocode.latitude.toDouble(),
            geocode.longitude.toDouble(),
        )
        log.debug { "location=$location" }
        location.shouldNotBeNull()

        val address = location.toBingAddress()
        address.shouldNotBeNull()
        address.name.shouldNotBeNull().shouldNotBeEmpty()
        address.country shouldBeEqualTo "Republic of Korea"
    }

    @ParameterizedTest(name = "find address in coroutines by geocode {0}")
    @MethodSource("getGeocodes")
    fun `get location in coroutines by geocode`(geocode: Geocode) = runSuspendIO {
        val location = coroutineClient.locations(
            geocode.latitude.toDouble(),
            geocode.longitude.toDouble(),
        )
        log.debug { "result=$location" }
        location.shouldNotBeNull()

        val address = location.toBingAddress()
        address.shouldNotBeNull()
        address.name.shouldNotBeNull().shouldNotBeEmpty()
        address.country shouldBeEqualTo "Republic of Korea"
    }

    @Test
    fun `bing map location parsing`() {
        val locationJson = Resourcex.getString("bing/location.json")
        val jsonMapper = Jackson.defaultJsonMapper
        val location = jsonMapper.readValue<BingMapModel.Location>(locationJson)

        val address = location.toBingAddress()
        log.debug { "address=$address" }
        address.shouldNotBeNull()
        address.name.shouldNotBeNull().shouldNotBeEmpty()
        address.country shouldBeEqualTo "Republic of Korea"
    }
}
