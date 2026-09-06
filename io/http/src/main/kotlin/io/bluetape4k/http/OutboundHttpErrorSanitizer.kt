package io.bluetape4k.http

/** 영속화할 outbound HTTP 오류 문자열의 최대 길이입니다. */
const val OUTBOUND_HTTP_ERROR_MAX_LENGTH: Int = 240

private const val REDACTED_OUTBOUND_HTTP_ERROR_VALUE: String = "[redacted]"

private val credentialLikeOutboundErrorPattern =
    Regex("(?i)[\\\"']?\\b(authorization|cookie|token|secret|api[-_ ]?key)\\b[\\\"']?\\s*[:=]?\\s*.*$")

private val outboundErrorLineSeparatorPattern = Regex("\\R")

/**
 * outbound HTTP 오류를 DB 등에 저장할 수 있는 제한된 문자열로 정제합니다.
 *
 * 결과는 항상 `HTTP <statusCode>`로 시작합니다. [rawMessage]가 null 또는 blank이면
 * status만 반환하고, 여러 줄이면 첫 줄만 사용합니다. 첫 줄의 `Authorization`,
 * `Cookie`, `Token`, `Secret`, `API-Key` 계열 값은 대소문자를 구분하지 않고
 * `[redacted]`로 치환합니다. quoted label, `:`와 `=` delimiter, delimiter가 생략된 표현과
 * 주변 공백을 허용하며, credential label을 발견하면 malformed·공백 포함 값에서도 누출되지
 * 않도록 그 뒤의 첫 줄 나머지를 fail-closed로 제거합니다. 원문은 반환 가능한 문자 수까지만
 * 먼저 잘라서 정규식과 줄 구분자 탐색도 제한된 입력에서만 수행합니다.
 *
 * 반환 문자열은 status prefix를 포함해 [OUTBOUND_HTTP_ERROR_MAX_LENGTH]자를 넘지 않습니다.
 * HTTP status 분류, retry 정책, 저장소와 트랜잭션 수명주기는 호출자가 관리해야 합니다.
 *
 * @param statusCode 응답 또는 합성 HTTP status code
 * @param rawMessage 정제할 nullable 원문 오류 메시지
 * @return credential 값과 추가 줄을 제거한 제한 문자열
 */
fun sanitizeOutboundHttpError(
    statusCode: Int,
    rawMessage: String?,
): String {
    val statusPrefix = "HTTP $statusCode"
    val messageBudget = (OUTBOUND_HTTP_ERROR_MAX_LENGTH - statusPrefix.length - 1)
        .coerceAtLeast(0)
    val safeMessage = rawMessage
        ?.take(messageBudget)
        ?.substringBeforeLineSeparator()
        ?.replace(credentialLikeOutboundErrorPattern, "$1:$REDACTED_OUTBOUND_HTTP_ERROR_VALUE")
        ?.takeIf { it.isNotBlank() }

    return listOfNotNull(statusPrefix, safeMessage)
        .joinToString(" ")
        .take(OUTBOUND_HTTP_ERROR_MAX_LENGTH)
}

private fun String.substringBeforeLineSeparator(): String =
    outboundErrorLineSeparatorPattern.find(this)
        ?.range
        ?.first
        ?.let { endIndex -> substring(0, endIndex) }
        ?: this
