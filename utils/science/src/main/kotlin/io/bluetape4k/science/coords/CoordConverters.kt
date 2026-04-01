package io.bluetape4k.science.coords

import kotlin.math.abs

/**
 * 십진도(Decimal Degree)를 도·분([DM]) 형식으로 변환합니다.
 *
 * 부호는 도(degree)에만 반영되며, 분(minute)은 항상 양수입니다.
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
 */
fun DM.toDegree(): Double {
    val sign = if (degree < 0) -1.0 else 1.0
    return degree + sign * minute / 60.0
}

/**
 * [DMS] 형식의 좌표를 십진도(Decimal Degree)로 변환합니다.
 *
 * 부호는 도(degree)에서 결정되며, 분(minute)과 초(second)는 항상 양수로 취급합니다.
 */
fun DMS.toDegree(): Double {
    val sign = if (degree < 0) -1.0 else 1.0
    return degree + sign * (minute / 60.0 + second / 3600.0)
}
