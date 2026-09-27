package io.bluetape4k.support

/**
 * 문자열을 unicode escape 형태로 변경한다 (범 -> "\uBC94", "제갈" -> "\uC81C\uAC08")
 */
fun <T: CharSequence> T.toUnicodeEscape(): String =
    map { "\\u%04X".format(it.code) }.joinToString("")
