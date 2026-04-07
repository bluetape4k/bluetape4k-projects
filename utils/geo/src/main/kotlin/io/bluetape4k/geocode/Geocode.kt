package io.bluetape4k.geocode

import io.bluetape4k.geocode.Geocode.Companion.DefaultMathContext
import io.bluetape4k.geocode.Geocode.Companion.parse
import io.bluetape4k.support.requireNotBlank
import java.io.Serializable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * 위경도 정보를 나타내는 데이터 클래스입니다.
 *
 * ## 동작/계약
 * - [latitude], [longitude]는 [BigDecimal] 정밀도로 보관됩니다.
 * - [parse]는 `"lat,lon"` 형식 문자열을 파싱하며 blank 입력 시 예외를 던집니다.
 * - [round]는 반올림된 새 인스턴스를 반환하고 원본 객체를 mutate하지 않습니다.
 *
 * ```kotlin
 * val g = Geocode.parse("37.5665,126.9780")
 * // g.round(3).toString() == "37.567,126.978"
 * ```
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
        /**
         * double 위경도로 [Geocode]를 생성합니다.
         *
         * ## 동작/계약
         * - [DefaultMathContext]를 사용해 BigDecimal로 변환합니다.
         */
        operator fun invoke(latitude: Double, longitude: Double): Geocode =
            Geocode(
                latitude = latitude.toBigDecimal(DefaultMathContext),
                longitude = longitude.toBigDecimal(DefaultMathContext)
            )

        @JvmStatic
                /**
                 * 문자열 표현의 위경도를 파싱합니다.
                 *
                 * ## 동작/계약
                 * - [geocode]가 blank면 [IllegalArgumentException]이 발생합니다.
                 * - [delimiter] 기준으로 2개 조각을 분리해 위도/경도로 해석합니다.
                 *
                 * ```kotlin
                 * val geocode = Geocode.parse("37.5665,126.9780")
                 * // geocode.latitude.toDouble() == 37.5665
                 * ```
                 */
        fun parse(geocode: String, delimiter: String = ","): Geocode {
            geocode.requireNotBlank("geocode")
            val splits = geocode.split(delimiter, ignoreCase = true, limit = 2)
            return Geocode(
                latitude = splits[0].toBigDecimal(DefaultMathContext),
                longitude = splits[1].toBigDecimal(DefaultMathContext)
            )
        }
    }

    /** 현재 위도/경도 중 작은 소수점 스케일입니다. */
    val scale: Int get() = latitude.scale().coerceAtMost(longitude.scale())

    /**
     * 위경도 값을 지정 스케일로 반올림한 새 [Geocode]를 반환합니다.
     *
     * ## 동작/계약
     * - 원본 객체를 변경하지 않고 새 인스턴스를 반환합니다.
     * - 반올림 규칙은 [roundingMode]를 따릅니다.
     *
     * ```kotlin
     * val geocode = Geocode(37.5665, 126.9780)
     * val rounded = geocode.round(2)
     * // rounded.latitude.scale() == 2
     * ```
     */
    fun round(scale: Int = DEFAULT_SCALE, roundingMode: RoundingMode = DefaultMathContext.roundingMode): Geocode {
        return this.copy(
            latitude = latitude.setScale(scale, roundingMode),
            longitude = longitude.setScale(scale, roundingMode)
        )
    }

    override fun toString(): String = "$latitude,$longitude"
}
