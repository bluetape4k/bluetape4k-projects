package io.bluetape4k.geocode

import io.bluetape4k.support.requireNotBlank
import java.io.Serializable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * 위경도 정보를 나타내는 데이터 클래스입니다.
 *
 * @property latitude 위도
 * @property longitude 경도
 */
data class Geocode(
    val latitude: BigDecimal,
    val longitude: BigDecimal,
): Serializable {

    companion object {
        const val DEFAULT_SCALE: Int = 3

        @JvmField
        val DefaultMathContext = MathContext(12, RoundingMode.HALF_EVEN)

        @JvmStatic
        operator fun invoke(latitude: Double, longitude: Double): Geocode =
            Geocode(
                latitude = latitude.toBigDecimal(DefaultMathContext),
                longitude = longitude.toBigDecimal(DefaultMathContext)
            )

        @JvmStatic
        fun parse(geocode: String, delimiter: String = ","): Geocode {
            geocode.requireNotBlank("geocode")
            val splits = geocode.split(delimiter, ignoreCase = true, limit = 2)
            return Geocode(
                latitude = splits[0].toBigDecimal(DefaultMathContext),
                longitude = splits[1].toBigDecimal(DefaultMathContext)
            )
        }
    }

    val scale: Int get() = latitude.scale().coerceAtMost(longitude.scale())

    fun round(scale: Int = DEFAULT_SCALE, roundingMode: RoundingMode = DefaultMathContext.roundingMode): Geocode {
        return this.copy(
            latitude = latitude.setScale(scale, roundingMode),
            longitude = longitude.setScale(scale, roundingMode)
        )
    }

    override fun toString(): String = "$latitude,$longitude"
}
