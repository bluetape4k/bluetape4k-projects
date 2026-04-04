package io.bluetape4k.science.coords

import kotlin.math.abs

/**
 * 십진도(Decimal Degree)를 도·분([DM]) 형식으로 변환합니다.
 *
 * 부호는 도(degree)에만 반영되며, 분(minute)은 항상 양수입니다.
 *
 * ```kotlin
 * val dm = 37.5665.toDM()
 * println(dm.degree) // 37
 * println(dm.minute) // 33.99 (approx)
 *
 * val dmNeg = (-33.8688).toDM()
 * println(dmNeg.degree) // -33
 * println(dmNeg.minute) // 52.128 (approx, 양수)
 * ```
 */
fun Double.toDM(): DM {
    val abs = abs(this)
    val d = abs.toInt()
    val m = (abs - d) * 60.0
    return DM(if (this < 0.0) -d else d, m)
}

/**
 * 십진도(Decimal Degree)를 도·분·초([DMS]) 형식으로 변환합니다.
 *
 * 부호는 도(degree)에만 반영되며, 분(minute)과 초(second)는 항상 양수입니다.
 *
 * ```kotlin
 * val dms = 126.9780.toDMS()
 * println(dms.degree) // 126
 * println(dms.minute) // 58
 * println(dms.second) // 40.8 (approx)
 *
 * val dmsNeg = (-74.006).toDMS()
 * println(dmsNeg.degree) // -74
 * println(dmsNeg.minute) // 0  (양수)
 * println(dmsNeg.second) // 21.6 (approx, 양수)
 * ```
 */
fun Double.toDMS(): DMS {
    val abs = abs(this)
    val d = abs.toInt()
    val m = ((abs - d) * 60.0).toInt()
    val s = ((abs - d) * 60.0 - m) * 60.0
    return DMS(if (this < 0.0) -d else d, m, s)
}

/**
 * [DM] 형식의 좌표를 십진도(Decimal Degree)로 변환합니다.
 *
 * 부호는 도(degree)에서 결정되며, 분(minute)은 항상 양수로 취급합니다.
 *
 * ```kotlin
 * val dm = DM(degree = 37, minute = 33.99)
 * println(dm.toDegree()) // 37.5665 (approx)
 *
 * val dmNeg = DM(degree = -33, minute = 52.128)
 * println(dmNeg.toDegree()) // -33.8688 (approx)
 * ```
 */
fun DM.toDegree(): Double {
    val sign = if (degree < 0) -1.0 else 1.0
    return degree + sign * minute / 60.0
}

/**
 * [DMS] 형식의 좌표를 십진도(Decimal Degree)로 변환합니다.
 *
 * 부호는 도(degree)에서 결정되며, 분(minute)과 초(second)는 항상 양수로 취급합니다.
 *
 * ```kotlin
 * val dms = DMS(degree = 126, minute = 58, second = 40.8)
 * println(dms.toDegree()) // 126.978 (approx)
 *
 * val dmsNeg = DMS(degree = -74, minute = 0, second = 21.6)
 * println(dmsNeg.toDegree()) // -74.006 (approx)
 * ```
 */
fun DMS.toDegree(): Double {
    val sign = if (degree < 0) -1.0 else 1.0
    return degree + sign * (minute / 60.0 + second / 3600.0)
}
