package io.bluetape4k.http

private const val MAX_OUTBOUND_ERROR_LENGTH = 240

private val credentialMarkerPattern = Regex(
    "(?i)\\b(authorization|cookie|token|secret|api[-_ ]?key)\\b\\s*[\\\"']?\\s*[:=]\\s*",
)

private data class CredentialReplacement(
    val start: Int,
    val endExclusive: Int,
    val replacement: String,
)

private data class ParsedCredentialValue(
    val endExclusive: Int,
    val unquoted: Boolean,
    val value: String,
)

/**
 * outbound 오류를 DB 또는 log에 보관할 수 있는 제한된 한 줄 요약으로 변환합니다.
 *
 * 첫 줄만 사용하고 credential-like marker의 값을 `[redacted]`로 치환합니다.
 * marker가 존재하지만 값이 비어 있거나 quote/escape 문법이 깨진 경우에는
 * status prefix만 반환해 fail-closed로 동작합니다. 호출자는 원본 Throwable을
 * 별도로 log하거나 persistence payload에 포함하지 않아야 합니다.
 */
fun sanitizeOutboundError(statusCode: Int, rawMessage: String?): String {
    val prefix = "HTTP $statusCode"
    val sanitizedLine = rawMessage
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.takeUnless { it.isBlank() }
        ?.let(::sanitizeFirstLine)
    return formatSanitizedError(prefix, sanitizedLine)
}

private fun formatSanitizedError(prefix: String, sanitizedLine: String?): String {
    if (sanitizedLine == null) return prefix
    val availableBodyLength = (MAX_OUTBOUND_ERROR_LENGTH - prefix.length - 1).coerceAtLeast(0)
    val body = sanitizedLine.takeUtf16Safe(availableBodyLength)
    return if (body.isEmpty()) prefix else "$prefix $body"
}

private fun sanitizeFirstLine(line: String): String? {
    val replacements = credentialMarkerPattern.findAll(line).map { marker ->
        parseReplacement(line, marker)
    }.toList()
    return if (replacements.any { it == null }) {
        null
    } else {
        applyReplacements(line, replacements.filterNotNull())
    }
}

private fun applyReplacements(line: String, replacements: List<CredentialReplacement>): String? {
    if (replacements.isEmpty()) return line

    val result = StringBuilder(line.length)
    var cursor = 0
    var overlapping = false
    replacements.forEach { replacement ->
        if (replacement.start < cursor) {
            overlapping = true
        } else {
            result.append(line, cursor, replacement.start)
            result.append(replacement.replacement)
            cursor = replacement.endExclusive
        }
    }
    if (!overlapping) result.append(line, cursor, line.length)
    return if (overlapping) null else result.toString()
}

private fun parseReplacement(line: String, marker: MatchResult): CredentialReplacement? {
    val key = marker.groupValues[1]
    val firstValue = parseValue(line, marker.range.last + 1)
    val parsedValue = firstValue?.let { value ->
        if (value.unquoted && value.value.equals("Bearer", ignoreCase = true)) {
            parseValue(line, skipWhitespace(line, value.endExclusive))
        } else {
            value
        }
    }
    return parsedValue
        ?.takeIf { isValueBoundary(line, it.endExclusive) }
        ?.let {
            CredentialReplacement(
                start = marker.range.first,
                endExclusive = it.endExclusive,
                replacement = "$key:[redacted]",
            )
        }
}

private fun parseValue(line: String, start: Int): ParsedCredentialValue? {
    if (start >= line.length) return null
    return when (line[start]) {
        '\'', '"' -> parseQuotedValue(line, start)
        else -> parseUnquotedValue(line, start)
    }
}

private fun parseQuotedValue(line: String, start: Int): ParsedCredentialValue? {
    val quote = line[start]
    var index = start + 1
    var contentLength = 0
    var malformed = false
    var parsed: ParsedCredentialValue? = null
    while (index < line.length && !malformed && parsed == null) {
        when (line[index]) {
            '\\' -> {
                if (index + 1 >= line.length) {
                    malformed = true
                } else {
                    index += 2
                    contentLength++
                }
            }

            quote -> {
                if (contentLength == 0) {
                    malformed = true
                } else {
                    parsed = ParsedCredentialValue(index + 1, unquoted = false, value = "")
                }
            }

            else -> {
                index++
                contentLength++
            }
        }
    }
    return parsed
}

private fun parseUnquotedValue(line: String, start: Int): ParsedCredentialValue? {
    if (start >= line.length || line[start] == ':' || line[start] == '=') return null

    var index = start
    var malformed = false
    while (index < line.length && !malformed) {
        when (val character = line[index]) {
            '\\', '\'', '"' -> malformed = true
            else -> {
                if (character.isWhitespace() || character == ',' || character == ';') break
                index++
            }
        }
    }
    return if (!malformed && index > start) {
        ParsedCredentialValue(index, unquoted = true, value = line.substring(start, index))
    } else {
        null
    }
}

private fun skipWhitespace(line: String, start: Int): Int {
    var index = start
    while (index < line.length && line[index].isWhitespace()) index++
    return index
}

private fun isValueBoundary(line: String, index: Int): Boolean =
    index >= line.length || line[index].isWhitespace() || line[index] in ",;}]})"

private fun String.takeUtf16Safe(maxLength: Int): String {
    val result = when {
        maxLength <= 0 || isEmpty() -> ""
        length <= maxLength -> this
        else -> {
            var end = maxLength
            if (end > 0 && end < length) {
                val previous = this[end - 1]
                val next = this[end]
                if (previous.isHighSurrogate() && next.isLowSurrogate()) end--
            }
            substring(0, end)
        }
    }
    return result
}
