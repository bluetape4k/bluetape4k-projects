// GENERATED FILE - DO NOT EDIT.
// Source: config/publishing-signing/PublishingSigningKeySupport.kt
// Synchronize with: python3 scripts/sync-publishing-signing-support.py --write --check

package io.bluetape4k.gradle

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Base64

private const val PRIVATE_KEY_HEADER = "-----BEGIN PGP PRIVATE KEY BLOCK-----"
private const val PRIVATE_KEY_FOOTER = "-----END PGP PRIVATE KEY BLOCK-----"

private val strictBase64 = Regex(
    "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$",
)

data class NormalizedSigningKeyId(
    val value: String,
    val warning: String?,
)

fun normalizeSigningKeyId(raw: String): NormalizedSigningKeyId {
    val value = raw.trim()
    if (value.isBlank()) {
        return NormalizedSigningKeyId(value, null)
    }

    val prefix = when {
        value.startsWith("0x") -> "0x"
        value.startsWith("0X") -> "0X"
        else -> ""
    }
    val body = value.removePrefix(prefix)
    val isLongHexId = body.length == 16 && body.all(Char::isHexDigit)

    return if (isLongHexId) {
        NormalizedSigningKeyId(
            value = prefix + body.takeLast(8),
            warning = "Signing key ID used a 16-digit hexadecimal form; normalized to the trailing 8 digits.",
        )
    } else {
        NormalizedSigningKeyId(value, null)
    }
}

fun resolveSigningKeyId(raw: String): String = normalizeSigningKeyId(raw).value

/**
 * OpenPGP private key의 전송 인코딩만 정규화합니다.
 *
 * 실제 key 유효성은 Gradle signing parser가 검증합니다.
 */
fun resolveSigningKey(raw: String): String {
    if (raw.isBlank()) {
        return raw
    }

    val normalizedArmor = raw.replace("\\n", "\n")
    if (normalizedArmor.isAsciiArmoredPrivateKey()) {
        return normalizedArmor
    }

    val encoded = raw.trim()
    if (!strictBase64.matches(encoded)) {
        return raw
    }

    val decoded = runCatching {
        val bytes = Base64.getDecoder().decode(encoded)
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    }.getOrNull() ?: return raw

    val decodedArmor = decoded.replace("\\n", "\n")
    return decodedArmor.takeIf(String::isAsciiArmoredPrivateKey) ?: raw
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun String.isAsciiArmoredPrivateKey(): Boolean {
    if (any { it.code > 0x7F }) {
        return false
    }
    val trimmed = trim()
    return trimmed.startsWith(PRIVATE_KEY_HEADER) &&
        trimmed.endsWith(PRIVATE_KEY_FOOTER)
}
