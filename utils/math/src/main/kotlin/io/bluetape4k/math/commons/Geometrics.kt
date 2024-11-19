package io.bluetape4k.math.commons

import java.awt.Point
import java.awt.geom.Point2D
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 직각삼각형의 빗변의 길이를 구하는 식이다 `sqrt(a^2 + b^2)`
 *
 * @param a 직각삼각형의 밑변
 * @param b 직각삼각형의 직각변
 * @return 빗변의 길이
 */
fun hypot(a: Double, b: Double): Double {

    if (a.approximateEqual(0.0)) {
        return b.absoluteValue
    }
    if (b.approximateEqual(0.0)) {
        return a.absoluteValue
    }

    return when {
        a.absoluteValue > b.absoluteValue -> {
            val r = b / a
            a.absoluteValue * sqrt(1.0 - r * r)
        }

        !b.approximateEqual(0.0)          -> {
            val r = a / b
            b.absoluteValue * sqrt(1.0 + r * r)
        }

        else                              -> 0.0
    }
}

/**
 * Moler-Morrison 법을 이용하여 직각삼각형의 빗변의 길이를 구한다
 *
 * @param a 직각삼각형의 밑변
 * @param b 직각삼각형의 직각변
 * @return 빗변의 길이
 */
fun hypot2(a: Double, b: Double): Double {
    var aa = max(a.absoluteValue, b.absoluteValue)
    var ba = min(a.absoluteValue, b.absoluteValue)

    if (ba.approximateEqual(0.0)) {
        return aa
    }

    for (i in 0 until 3) {
        var t = ba / aa
        t *= t
        t /= (4 + t)
        aa += 2 * aa * t
        ba *= t
    }
    return aa
}

fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    sqrt((x1 - x2).square() + (y1 - y2).square())

fun distance(p1: Point2D, p2: Point2D): Double =
    distance(p1.x, p1.y, p2.x, p2.y)

fun distance(p1: Point, p2: Point): Double =
    distance(p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble())
