package io.bluetape4k.redis.lettuce

import java.net.URI

private val URI_CREDENTIALS = Regex("([a-zA-Z][a-zA-Z0-9+.-]*://)([^/\\s]+)@")

/** 로그에 URI를 기록할 때 authority의 자격증명을 제거합니다. */
fun String.redactUriCredentials(): String =
    replace(URI_CREDENTIALS) { match ->
        "${match.groupValues[1]}<redacted>@"
    }

/** 로그용 URI 문자열을 반환하며 authority의 자격증명을 제거합니다. */
fun URI.toRedactedLogString(): String = toString().redactUriCredentials()
