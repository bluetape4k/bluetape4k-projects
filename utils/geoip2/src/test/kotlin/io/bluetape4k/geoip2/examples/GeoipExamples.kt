package io.bluetape4k.geoip2.examples

import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import io.bluetape4k.geoip2.AbstractGeoipTest
import io.bluetape4k.geoip2.Geoip
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.InetAddress
import kotlin.jvm.optionals.getOrNull

class GeoipExamples: AbstractGeoipTest() {

    companion object: KLogging()

    @ParameterizedTest(name = "find country for {0}")
    @MethodSource("getIpAddresses")
    fun `find country by ip address`(host: String) {
        val countryResponse = findCountry(host)
        log.debug { "find country:\n${countryResponse?.prettyPrint()}" }

        if (countryResponse == null) {
            log.warn { "$host can't resolve country." }
        }
    }

    @ParameterizedTest(name = "find city for {0}")
    @MethodSource("getIpAddresses")
    fun `find city by ip address`(host: String) {
        val cityResponse = findCity(host)
        log.debug { "find city:\n${cityResponse?.prettyPrint()}" }

        if (cityResponse == null) {
            log.warn { "$host can't resolve city." }
        }
    }

    private fun findCountry(host: String): CountryResponse? {
        val ipAddress = InetAddress.getByName(host)
        return Geoip.countryDatabase.tryCountry(ipAddress).getOrNull()
    }

    private fun findCity(host: String): CityResponse? {
        val ipAddress = InetAddress.getByName(host)
        return Geoip.cityDatabase.tryCity(ipAddress).getOrNull()
    }
}
