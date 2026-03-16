package io.bluetape4k.geocode

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull

abstract class AbstractGeocodeTest {

    companion object: KLogging() {
        @JvmStatic
        protected val seoul: Geocode = Geocode(37.4921031, 127.0297768)

        @JvmStatic
        protected val hanam: Geocode = Geocode(37.564867, 127.184400)

        @JvmStatic
        protected val busan: Geocode = Geocode(35.158723, 129.160311)

        @JvmStatic
        protected val seolak: Geocode = Geocode(38.161021, 128.441024)

        @JvmStatic
        protected val naksan: Geocode = Geocode(38.123303, 128.630604)

        @JvmStatic
        protected fun getGeocodes(): List<Geocode> = listOf(seoul, hanam, busan, seolak, naksan)
    }

    protected fun verifySeoul(seoul: Address?) {
        seoul.shouldNotBeNull()
        seoul.city?.lowercase() shouldBeEqualTo "서울특별시"
        seoul.country?.lowercase() shouldBeEqualTo "대한민국"
    }

    protected fun verifyBusan(busan: Address?) {
        busan.shouldNotBeNull()
        busan.city?.lowercase() shouldBeEqualTo "부산"
        busan.country?.lowercase() shouldBeEqualTo "대한민국"
    }
}
