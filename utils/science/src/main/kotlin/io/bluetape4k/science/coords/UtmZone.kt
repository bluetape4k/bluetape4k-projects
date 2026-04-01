package io.bluetape4k.science.coords

/**
 * UTM Zone 식별자를 나타내는 클래스입니다.
 *
 * UTM(Universal Transverse Mercator) 좌표계에서 경도 구역(1~60)과
 * 위도 구역(C~X, I·O 제외)으로 구성됩니다.
 *
 * 예: 서울은 52S(126~132°E, 32~40°N) 또는 52T(40~48°N) 구역에 해당합니다.
 *
 * @param longitudeZone 경도 구역 번호 (1~60)
 * @param latitudeZone  위도 구역 문자 (C~X, I·O 제외)
 */
data class UtmZone(
    val longitudeZone: Int,
    val latitudeZone: Char,
): Comparable<UtmZone> {

    init {
        require(longitudeZone in UTM_LONGITUDE_MIN..UTM_LONGITUDE_MAX) {
            "경도 구역은 $UTM_LONGITUDE_MIN~$UTM_LONGITUDE_MAX 범위여야 합니다: $longitudeZone"
        }
        require(latitudeZone.uppercaseChar().isUtmLatitudeBand) {
            "위도 구역 문자가 유효하지 않습니다: $latitudeZone"
        }
    }

    override fun compareTo(other: UtmZone): Int {
        var diff = latitudeZone.compareTo(other.latitudeZone)
        if (diff == 0) diff = longitudeZone.compareTo(other.longitudeZone)
        return diff
    }

    override fun toString(): String = "$longitudeZone$latitudeZone"
}
