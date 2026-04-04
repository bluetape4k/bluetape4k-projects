package io.bluetape4k.science.coords

import java.util.concurrent.ConcurrentSkipListMap

/** UTM Zone 경도 구역의 크기 (6도) */
const val UTM_LONGITUDE_SIZE = 6

/** UTM Zone 위도 구역의 기본 크기 (8도, Band X 제외) */
const val UTM_LATITUDE_SIZE = 8

/** UTM Band X의 위도 구역 크기 (12도: 72°N ~ 84°N) */
const val UTM_LATITUDE_BAND_X_SIZE = 12

/** UTM Zone 최소 경도 구역 번호 */
const val UTM_LONGITUDE_MIN = 1

/** UTM Zone 최대 경도 구역 번호 */
const val UTM_LONGITUDE_MAX = 60

/**
 * UTM Zone 위도 구역 코드와 시작 위도의 매핑입니다.
 * I, O 문자는 혼동을 피하기 위해 제외됩니다.
 */
val UTM_LATITUDE_BANDS: Map<Char, Double> by lazy {
    linkedMapOf(
        'C' to -80.0,
        'D' to -72.0,
        'E' to -64.0,
        'F' to -56.0,
        'G' to -48.0,
        'H' to -40.0,
        'J' to -32.0,
        'K' to -24.0,
        'L' to -16.0,
        'M' to -8.0,
        'N' to 0.0,
        'P' to 8.0,
        'Q' to 16.0,
        'R' to 24.0,
        'S' to 32.0,
        'T' to 40.0,
        'U' to 48.0,
        'V' to 56.0,
        'W' to 64.0,
        'X' to 72.0,
    )
}

/**
 * 유효한 UTM 위도 구역 문자인지 여부
 *
 * ```kotlin
 * println('S'.isUtmLatitudeBand) // true
 * println('I'.isUtmLatitudeBand) // false (I는 제외)
 * println('O'.isUtmLatitudeBand) // false (O는 제외)
 * println('Z'.isUtmLatitudeBand) // false
 * ```
 */
val Char.isUtmLatitudeBand: Boolean
    get() = UTM_LATITUDE_BANDS.containsKey(this.uppercaseChar())

/**
 * UTM Zone별 [BoundingBox] 캐시입니다.
 */
val UTM_ZONE_BOUNDING_BOXES: Map<UtmZone, BoundingBox> by lazy {
    val map = ConcurrentSkipListMap<UtmZone, BoundingBox>()
    for (lon in UTM_LONGITUDE_MIN..UTM_LONGITUDE_MAX) {
        for (lat in UTM_LATITUDE_BANDS.keys) {
            val zone = UtmZone(lon, lat)
            val longitude = lon.toUtmLongitude()
            val latitude = UTM_LATITUDE_BANDS[lat]!!
            // Band X는 72°N ~ 84°N으로 12도 구간 (나머지는 8도)
            val latHeight = if (lat == 'X') UTM_LATITUDE_BAND_X_SIZE.toDouble() else UTM_LATITUDE_SIZE.toDouble()
            map[zone] = BoundingBox(
                minLat = latitude,
                minLon = longitude,
                maxLat = latitude + latHeight,
                maxLon = longitude + UTM_LONGITUDE_SIZE,
            )
        }
    }
    map
}

/**
 * 위경도로부터 해당 [UtmZone]을 반환합니다.
 * (Zone/Band 판정만 수행; 좌표 변환은 `io.bluetape4k.science.projection` 패키지의 변환 함수 사용)
 *
 * ```kotlin
 * val zone = utmZoneOf(latitude = 37.5665, longitude = 126.9780)
 * println(zone.longitudeZone) // 52
 * println(zone.latitudeZone)  // S
 *
 * val nyZone = utmZoneOf(latitude = 40.7128, longitude = -74.0060)
 * println(nyZone.toString()) // 18T
 * ```
 *
 * @param latitude  위도
 * @param longitude 경도
 */
fun utmZoneOf(latitude: Double, longitude: Double): UtmZone {
    val zone = ((longitude + 180) / UTM_LONGITUDE_SIZE).toInt() + 1
    val band = utmLatitudeBand(latitude)
    return UtmZone(zone.coerceIn(UTM_LONGITUDE_MIN, UTM_LONGITUDE_MAX), band)
}

/**
 * [GeoLocation]으로부터 해당 [UtmZone]을 반환합니다.
 *
 * ```kotlin
 * val seoul = GeoLocation(37.5665, 126.9780)
 * val zone = utmZoneOf(seoul)
 * println(zone.longitudeZone) // 52
 * println(zone.latitudeZone)  // S
 * ```
 *
 * @param location 위치 좌표
 */
fun utmZoneOf(location: GeoLocation): UtmZone =
    utmZoneOf(location.latitude, location.longitude)

/**
 * 위도로부터 UTM 위도 구역 문자를 반환합니다.
 * C~X (I, O 제외) 범위를 사용합니다.
 *
 * ```kotlin
 * println(utmLatitudeBand(37.5))   // S  (32~40°N)
 * println(utmLatitudeBand(0.0))    // N  (0~8°N)
 * println(utmLatitudeBand(-10.0))  // L  (-16~-8°N)
 * println(utmLatitudeBand(84.0))   // X  (72~84°N)
 * // utmLatitudeBand(85.0)         // throws IllegalArgumentException
 * ```
 *
 * @param latitude 위도 (-80.0 ~ 84.0)
 * @throws IllegalArgumentException 범위를 벗어난 위도 (UTM 유효 범위: -80.0 ~ 84.0)
 */
fun utmLatitudeBand(latitude: Double): Char {
    require(latitude in -80.0..84.0) {
        "UTM 유효 범위를 벗어난 위도입니다. latitude=$latitude (유효 범위: -80.0 ~ 84.0)"
    }
    val sorted = UTM_LATITUDE_BANDS.entries
        .sortedByDescending { it.value }

    for ((char, startLat) in sorted) {
        if (startLat <= latitude) return char
    }
    throw IllegalArgumentException("UTM 위도 구역을 찾을 수 없습니다. latitude=$latitude")
}

/**
 * UTM 경도 구역 번호를 십진 경도(서쪽 경계)로 변환합니다.
 *
 * ```kotlin
 * println(31.toUtmLongitude())  // 0.0  (Zone 31: 0~6°E)
 * println(32.toUtmLongitude())  // 6.0  (Zone 32: 6~12°E)
 * println(52.toUtmLongitude())  // 126.0 (Zone 52: 126~132°E)
 * ```
 */
fun Int.toUtmLongitude(): Double = (this - 31) * UTM_LONGITUDE_SIZE.toDouble()

/**
 * [UtmZone]에 해당하는 위경도 [BoundingBox]를 반환합니다.
 *
 * ```kotlin
 * val zone = UtmZone(52, 'S')
 * val bbox = zone.boundingBox()
 * println(bbox.minLon) // 126.0
 * println(bbox.maxLon) // 132.0
 * println(bbox.minLat) // 32.0
 * println(bbox.maxLat) // 40.0
 * ```
 */
fun UtmZone.boundingBox(): BoundingBox =
    UTM_ZONE_BOUNDING_BOXES[this]
        ?: error("해당 UTM Zone에 대한 BoundingBox를 찾을 수 없습니다. utm=$this")

/**
 * [UtmZone]의 특정 셀(cell)에 해당하는 [BoundingBox]를 계산합니다.
 *
 * ```kotlin
 * val zone = UtmZone(52, 'S')
 * // Zone 52S의 북서쪽 1°×1° 셀
 * val cell = zone.cellBoundingBox(size = 1.0, row = 0, col = 0)
 * println(cell.minLon) // 126.0
 * println(cell.maxLon) // 127.0
 * println(cell.maxLat) // 40.0
 * println(cell.minLat) // 39.0
 * ```
 *
 * @param size 셀의 크기 (위경도 단위)
 * @param row  셀의 행 인덱스 (0부터 시작, 북쪽에서 남쪽 방향)
 * @param col  셀의 열 인덱스 (0부터 시작, 서쪽에서 동쪽 방향)
 */
fun UtmZone.cellBoundingBox(size: Double, row: Int = 0, col: Int = 0): BoundingBox {
    require(size > 0.0) { "size는 양수여야 합니다: $size" }
    require(row >= 0) { "row는 0 이상이어야 합니다: $row" }
    require(col >= 0) { "col은 0 이상이어야 합니다: $col" }
    val utmBbox = boundingBox()
    val minLon = utmBbox.minLon + size * col
    val maxLat = utmBbox.maxLat - size * row
    return BoundingBox(
        minLat = maxLat - size,
        minLon = minLon,
        maxLat = maxLat,
        maxLon = minLon + size,
    )
}
