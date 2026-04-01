package io.bluetape4k.science.coords

/**
 * 십진도(Decimal Degree)를 도·분([DM]) 형식으로 변환합니다.
 */
fun Double.toDM(): DM {
    val d = this.toInt()
    val m = (this - d) * 60.0
    return DM(d, m)
}

/**
 * 십진도(Decimal Degree)를 도·분·초([DMS]) 형식으로 변환합니다.
 */
fun Double.toDMS(): DMS {
    val d = this.toInt()
    val m = ((this - d) * 60.0).toInt()
    val s = ((this - d) * 60.0 - m) * 60.0
    return DMS(d, m, s)
}

/**
 * [DM] 형식의 좌표를 십진도(Decimal Degree)로 변환합니다.
 */
fun DM.toDegree(): Double = degree + minute / 60.0

/**
 * [DMS] 형식의 좌표를 십진도(Decimal Degree)로 변환합니다.
 */
fun DMS.toDegree(): Double = degree + minute / 60.0 + second / 3600.0
