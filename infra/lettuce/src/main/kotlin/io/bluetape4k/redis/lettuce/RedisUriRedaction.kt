package io.bluetape4k.redis.lettuce

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val REDACTED_URI = "<redacted-uri>"

private val SENSITIVE_PARAMETER = Regex(
    "(?i)^(?:auth|api[_-]?key|access[_-]?token|credential(?:s)?|pass(?:word|wd)?|pwd|" +
            "refresh[_-]?token|secret|token|user(?:name)?)$"
)

/**
 * Redis URI를 로그에 남길 수 있는 형태로 변환합니다.
 *
 * authority userinfo와 query/semicolon 옵션의 민감한 값을 제거하고 호스트, 포트,
 * 경로와 안전한 옵션은 보존합니다. URI를 해석할 수 없거나 구조가 모호하면 전체를
 * [REDACTED_URI]로 치환해 fail-closed 동작을 보장합니다.
 */
fun String.redactUriCredentials(): String {
    if (isEmpty() || any { it.isWhitespace() || it.isISOControl() }) {
        return REDACTED_URI
    }

    val uri = try {
        URI(this)
    } catch (_: URISyntaxException) {
        return REDACTED_URI
    }

    val authority = uri.rawAuthority
    if (authority != null && (authority.count { it == '@' } > 1 || uri.host == null)) {
        return REDACTED_URI
    }

    return try {
        val rawSchemeSpecificPart = uri.rawSchemeSpecificPart
        val redactedSchemeSpecificPart = if (authority != null) {
            if (!rawSchemeSpecificPart.startsWith("//$authority")) {
                return REDACTED_URI
            }

            val authorityEnd = 2 + authority.length
            val redactedAuthority = authority.substringAfterLast('@', authority)
                .let { hostAuthority ->
                    if (uri.rawUserInfo == null) hostAuthority else "<redacted>@$hostAuthority"
                }
            "//$redactedAuthority" + redactPathAndQuery(rawSchemeSpecificPart.substring(authorityEnd))
        } else {
            redactPathAndQuery(rawSchemeSpecificPart)
        }

        val fragment = uri.rawFragment?.let { "#<redacted>" }.orEmpty()
        uri.scheme?.let { "$it:$redactedSchemeSpecificPart$fragment" }
            ?: "$redactedSchemeSpecificPart$fragment"
    } catch (_: IllegalArgumentException) {
        REDACTED_URI
    }
}

/** 로그용 URI 문자열을 반환하며 Redis 연결 자격증명을 제거합니다. */
fun URI.toRedactedLogString(): String = toString().redactUriCredentials()

private fun redactPathAndQuery(value: String): String {
    val queryStart = value.indexOf('?')
    if (queryStart < 0) {
        return redactDelimited(value, ';')
    }

    val path = redactDelimited(value.substring(0, queryStart), ';')
    val query = redactDelimited(value.substring(queryStart + 1), '&', ';')
    return "$path?$query"
}

private fun redactDelimited(value: String, vararg delimiters: Char): String {
    val redacted = StringBuilder(value.length)
    var segmentStart = 0

    value.forEachIndexed { index, character ->
        if (character in delimiters) {
            redacted.append(redactParameter(value.substring(segmentStart, index)))
            redacted.append(character)
            segmentStart = index + 1
        }
    }
    redacted.append(redactParameter(value.substring(segmentStart)))
    return redacted.toString()
}

private fun redactParameter(parameter: String): String {
    if (parameter.isEmpty()) return parameter

    val equalsIndex = parameter.indexOf('=')
    val rawName = if (equalsIndex < 0) parameter else parameter.substring(0, equalsIndex)
    val decodedName = URLDecoder.decode(rawName, StandardCharsets.UTF_8)
    if (!SENSITIVE_PARAMETER.matches(decodedName)) return parameter

    return if (equalsIndex < 0) {
        "$rawName=<redacted>"
    } else {
        parameter.substring(0, equalsIndex + 1) + "<redacted>"
    }
}
