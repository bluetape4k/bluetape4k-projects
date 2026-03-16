package io.bluetape4k.geohash.utils

/**
 * 문자열 왼쪽에 패딩을 추가하여 지정된 길이로 만듭니다.
 *
 * ```
 * padLeft("abc", 5, "0")  // "00abc"
 * padLeft("abc", 5, "-")  // "--abc"
 * padLeft("abcdef", 5, "0")  // "abcdef" (원래 문자연이 더 길면 그대로 반환)
 * ```
 *
 * @param s 원본 문자열
 * @param n 원하는 문자열 길이
 * @param pad 패딩에 사용할 문자열
 * @return 패딩된 문자열
 */
fun padLeft(
    s: String,
    n: Int,
    pad: String,
): String = String.format("%" + n + "s", s).replace(" ", pad)
