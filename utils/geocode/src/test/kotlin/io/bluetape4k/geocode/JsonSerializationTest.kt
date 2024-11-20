package io.bluetape4k.geocode

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.geocode.bing.BingAddress
import io.bluetape4k.geocode.google.GoogleAddress
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.readValueOrNull
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import java.util.*

@RandomizedTest
class JsonSerializationTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val objectMapper = Jackson.defaultJsonMapper

    @RepeatedTest(REPEAT_SIZE)
    fun `Bing address 직렬화`(@RandomValue address: BingAddress) {
        val json = objectMapper.writeAsString(address)!!

        val actual = objectMapper.readValueOrNull<BingAddress>(json)
        actual shouldBeEqualTo address
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Google address 직렬화`(@RandomValue address: GoogleAddress) {
        val json = objectMapper.writeAsString(address)!!

        val actual = objectMapper.readValueOrNull<GoogleAddress>(json)
        actual shouldBeEqualTo address
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `GeocodeResponse 직렬화`(@RandomValue address: GoogleAddress) {
        val response = GeocodeResponse(
            address = address,
            geocode = Geocode(37.123, 127.456),
            locale = Locale.KOREA,
        )
        val json = objectMapper.writeAsString(response)!!

        val actual = objectMapper.readValue<GeocodeResponse<GoogleAddress>>(json)
        actual shouldBeEqualTo response
    }

    // 이 방식보다 GoogleGeocodeResponse, BingGeocodeResponse 등의 sub class 를 만드는 것이 더 안정적임
    // Spring에서 Generic Type을 인식하지 못하는 경우가 있음
    class GeocodeResponse<T: Address>(
        val address: T?,
        val geocode: Geocode,
        val locale: Locale,
    ): AbstractValueObject() {

        override fun equalProperties(other: Any): Boolean {
            return other is GeocodeResponse<*> &&
                    address == other.address &&
                    geocode == other.geocode &&
                    locale == other.locale
        }

        override fun buildStringHelper(): ToStringBuilder {
            return super.buildStringHelper()
                .add("address", address)
                .add("geocode", geocode)
                .add("locale", locale)
        }
    }
}
