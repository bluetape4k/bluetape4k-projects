package io.bluetape4k.geoip2

import io.bluetape4k.geoip2.finder.GeoipCountryFinder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.net.InetAddress

class DatabaseReaderExtensionsTest: AbstractGeoipTest() {

    companion object: KLogging()

    @Test
    fun `tryCity 확장 함수로 city 조회`() {
        val ipAddress = InetAddress.getByName("8.8.8.8")
        val cityResponse = Geoip.cityDatabase.tryFindCity(ipAddress)

        cityResponse.isSuccess.shouldBeTrue()
        cityResponse
            .getOrNull()
            ?.country()
            ?.name()
            .shouldNotBeNull()
    }

    @Test
    fun `tryCountry 확장 함수로 country 조회`() {
        val ipAddress = InetAddress.getByName("8.8.8.8")
        val countryResponse = Geoip.countryDatabase.tryFindCountry(ipAddress)

        countryResponse.isSuccess.shouldBeTrue()
        countryResponse
            .getOrNull()
            ?.country()
            ?.name()
            .shouldNotBeNull()
    }

    @Test
    fun `tryCity 확장 함수 - private IP는 null 반환`() {
        val ipAddress = InetAddress.getByName("127.0.0.1")
        val cityResponse = Geoip.cityDatabase.tryFindCity(ipAddress)

        cityResponse.isSuccess.shouldBeFalse()
    }

    @Test
    fun `tryCountry 확장 함수 - private IP는 null 반환`() {
        val ipAddress = InetAddress.getByName("127.0.0.1")
        val countryResponse = Geoip.countryDatabase.tryFindCountry(ipAddress)

        countryResponse.isSuccess.shouldBeFalse()
    }

    @Test
    fun `GeoLocation fromLocation - 모든 필드 확인`() {
        val ipAddress = InetAddress.getByName("8.8.8.8")
        val cityResponse = Geoip.cityDatabase.tryFindCity(ipAddress).getOrNull()

        cityResponse.shouldNotBeNull()

        val geoLocation = GeoLocation.fromLocation(cityResponse.location())

        geoLocation.latitude.shouldNotBeNull()
        geoLocation.longitude.shouldNotBeNull()
    }

    @Test
    fun `Address fromCity - 모든 필드 확인`() {
        val ipAddress = InetAddress.getByName("8.8.8.8")
        val cityResponse = Geoip.cityDatabase.tryFindCity(ipAddress).getOrNull()

        cityResponse.shouldNotBeNull()

        val address = Address.fromCity(ipAddress, cityResponse)

        address.ipAddress.shouldNotBeNull()
        address.country.shouldNotBeNull()
        address.continent.shouldNotBeNull()
        address.countryIsoCode.shouldNotBeNull()
        address.geoLocation.shouldNotBeNull()
    }

    @Test
    fun `Address fromCountry - 필드 확인`() {
        val ipAddress = InetAddress.getByName("8.8.8.8")
        val countryResponse = Geoip.countryDatabase.tryFindCountry(ipAddress).getOrNull()

        countryResponse.shouldNotBeNull()

        val address = Address.fromCountry(ipAddress, countryResponse)

        address.ipAddress.shouldNotBeNull()
        address.country.shouldNotBeNull()
        address.continent.shouldNotBeNull()
        address.countryIsoCode.shouldNotBeNull()
        // Country는 city 정보가 없음
        address.city.shouldBeNull()
    }

    @Test
    fun `Geoip Database 접근`() {
        Geoip.asnDatabase.shouldNotBeNull()
        Geoip.cityDatabase.shouldNotBeNull()
        Geoip.countryDatabase.shouldNotBeNull()
    }

    @Test
    fun `잘못된 IP 형식 처리`() {
        // 잘못된 IP는 InetAddress 생성 시 예외 발생
        runCatching {
            InetAddress.getByName("invalid-ip")
        }.isFailure.shouldBeTrue()
    }

    @Test
    fun `IPv6 주소 처리`() {
        val ipv6Address = InetAddress.getByName("2001:4860:4860::8888")
        val address = GeoipCountryFinder().findAddress(ipv6Address)

        // Google DNS IPv6
        address?.country.shouldNotBeNull()
    }
}
