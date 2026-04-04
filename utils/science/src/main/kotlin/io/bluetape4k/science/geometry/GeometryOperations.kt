package io.bluetape4k.science.geometry

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import kotlin.math.atan2

/**
 * JTS 기하학 연산을 위한 공유 [GeometryFactory] 인스턴스입니다.
 *
 * ```kotlin
 * val p = DEFAULT_GEOMETRY_FACTORY.createPoint(Coordinate(1.0, 2.0))
 * println(p.x) // 1.0
 * println(p.y) // 2.0
 * ```
 */
val DEFAULT_GEOMETRY_FACTORY = GeometryFactory()

/**
 * 두 JTS [Point] 사이의 유클리드 거리를 반환합니다.
 *
 * ```kotlin
 * val p1 = DEFAULT_GEOMETRY_FACTORY.createPoint(Coordinate(0.0, 0.0))
 * val p2 = DEFAULT_GEOMETRY_FACTORY.createPoint(Coordinate(3.0, 4.0))
 * println(distanceBetween(p1, p2)) // 5.0
 * ```
 *
 * @param p1 첫 번째 점
 * @param p2 두 번째 점
 * @return 유클리드 거리
 */
fun distanceBetween(p1: Point, p2: Point): Double =
    p1.distance(p2)

/**
 * 두 JTS [Point] 사이의 각도(도, degree)를 반환합니다.
 *
 * +X 축으로부터 시계 반대 방향으로 증가합니다.
 *
 * ```kotlin
 * val origin = DEFAULT_GEOMETRY_FACTORY.createPoint(Coordinate(0.0, 0.0))
 * val east   = DEFAULT_GEOMETRY_FACTORY.createPoint(Coordinate(1.0, 0.0))
 * val north  = DEFAULT_GEOMETRY_FACTORY.createPoint(Coordinate(0.0, 1.0))
 * println(angleBetween(origin, east))  // 0.0   (동쪽)
 * println(angleBetween(origin, north)) // 90.0  (북쪽)
 * ```
 *
 * @param p1 첫 번째 점
 * @param p2 두 번째 점
 * @return 각도 (0~360도)
 */
fun angleBetween(p1: Point, p2: Point): Double {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val rad = atan2(dy, dx)
    var degree = Math.toDegrees(rad)
    if (degree < 0) degree += 360.0
    if (degree >= 360) degree -= 360.0
    return degree
}

/**
 * 두 선분의 교차점을 반환합니다. 교차점이 없으면 `null`을 반환합니다.
 *
 * 선분 1: (p1 → p2), 선분 2: (p3 → p4)
 *
 * ```kotlin
 * val gf = DEFAULT_GEOMETRY_FACTORY
 * val p1 = gf.createPoint(Coordinate(0.0, 0.0))
 * val p2 = gf.createPoint(Coordinate(2.0, 2.0))
 * val p3 = gf.createPoint(Coordinate(0.0, 2.0))
 * val p4 = gf.createPoint(Coordinate(2.0, 0.0))
 * val pt = getIntersectPoint(p1, p2, p3, p4)
 * println(pt?.x) // 1.0
 * println(pt?.y) // 1.0
 *
 * // 평행선: 교차점 없음
 * val a1 = gf.createPoint(Coordinate(0.0, 0.0))
 * val a2 = gf.createPoint(Coordinate(1.0, 0.0))
 * val b1 = gf.createPoint(Coordinate(0.0, 1.0))
 * val b2 = gf.createPoint(Coordinate(1.0, 1.0))
 * println(getIntersectPoint(a1, a2, b1, b2)) // null
 * ```
 *
 * @param p1 선분 1의 시작점
 * @param p2 선분 1의 끝점
 * @param p3 선분 2의 시작점
 * @param p4 선분 2의 끝점
 * @return 교차점 또는 null
 */
fun getIntersectPoint(p1: Point, p2: Point, p3: Point, p4: Point): Point? {
    val x1 = p1.x; val y1 = p1.y
    val x2 = p2.x; val y2 = p2.y
    val x3 = p3.x; val y3 = p3.y
    val x4 = p4.x; val y4 = p4.y

    val under = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
    if (under == 0.0) return null

    var t = (x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)
    var s = (x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)

    if (t == 0.0 && s == 0.0) return null

    t /= under
    s /= under

    if (t !in 0.0..1.0 || s !in 0.0..1.0) return null

    val x = x1 + t * (x2 - x1)
    val y = y1 + t * (y2 - y1)

    return DEFAULT_GEOMETRY_FACTORY.createPoint(org.locationtech.jts.geom.Coordinate(x, y))
}

/**
 * 이 값이 유효한 위도(-90~90) 범위 내에 있는지 여부를 반환합니다.
 *
 * ```kotlin
 * println(37.5665.isValidLatitude())  // true
 * println((-90.0).isValidLatitude())  // true
 * println(91.0.isValidLatitude())     // false
 * ```
 */
fun Double.isValidLatitude(): Boolean = this in -90.0..90.0

/**
 * 이 값이 유효한 경도(-180~180) 범위 내에 있는지 여부를 반환합니다.
 *
 * ```kotlin
 * println(126.9780.isValidLongitude())  // true
 * println(180.0.isValidLongitude())     // true
 * println(181.0.isValidLongitude())     // false
 * ```
 */
fun Double.isValidLongitude(): Boolean = this in -180.0..180.0
