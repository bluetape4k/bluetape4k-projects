package io.bluetape4k.geoip2

import com.maxmind.geoip2.record.Location
import io.bluetape4k.geoip2.GeoLocation.Companion.fromLocation
import java.io.Serializable

/**
 * 위도, 경도 정보를 나타냅니다.
 *
 * ## 동작/계약
 * - 값 객체이며 생성 후 불변입니다.
 * - [fromLocation]은 MaxMind [Location] 필드를 그대로 매핑해 새 인스턴스를 반환합니다.
 * - 입력 [Location] 객체를 mutate하지 않습니다.
 *
 * ```kotlin
 * val geo = GeoLocation.fromLocation(location)
 * // geo.latitude == location.latitude()
 * ```
 *
 * @property latitude 위도
 * @property longitude 경도
 * @property timeZone 시간대
 * @property accuracyRadius 정확도 반경
 * @property averageIncome 평균 수익
 * @property populationDensity 인구밀도
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val timeZone: String? = null,
    val accuracyRadius: Int? = null,
    val averageIncome: Int? = null,
    val populationDensity: Int? = null,
): Serializable {

    companion object {
        /**
         * MaxMind GeoIP2의 [Location] 정보를 기반으로 [GeoLocation] 객체를 생성합니다.
         *
         * ## 동작/계약
         * - latitude/longitude/timeZone/정확도 관련 필드를 그대로 복사합니다.
         * - 입력 객체를 변경하지 않고 새 [GeoLocation]을 생성합니다.
         *
         * ```kotlin
         * val geo = GeoLocation.fromLocation(location)
         * // geo.longitude == location.longitude()
         * ```
         *
         * @param location Location 정보
         * @return GeoLocation 객체
         */
        @JvmStatic
        fun fromLocation(location: Location): GeoLocation {
            return GeoLocation(
                latitude = location.latitude(),
                longitude = location.longitude(),
                timeZone = location.timeZone(),
                accuracyRadius = location.accuracyRadius(),
                averageIncome = location.averageIncome(),
                populationDensity = location.populationDensity()
            )
        }
    }
}
