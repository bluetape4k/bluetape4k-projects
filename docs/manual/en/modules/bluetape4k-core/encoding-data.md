---
title: Encoding and data boundaries
description: Make byte, string, URL-safe Base64, Hex, and failure contracts explicit.
manualId: bluetape4k-core
chapterId: encoding-data
---

# Encoding and data boundaries

## Problem

When binary values cross a text protocol, cache key, or diagnostic boundary, both sides must reconstruct the same bytes. Core codec helpers make nullable-input, UTF-8 string, URL-safe Base64, and Hex behavior explicit with a small API.

Encoding is not encryption. API keys and personal data remain secrets after Base64 or Hex encoding.

## Mental model

```text
domain value -> choose charset -> bytes -> encode wire format -> text
text -> decode wire format -> bytes -> use the same charset -> domain value
```

The string/byte boundary chooses a charset. The byte/transport-text boundary chooses a format. Leaving either implicit is a common source of cross-language round-trip defects.

## API and concrete contract

| Task | API | Contract |
| --- | --- | --- |
| Produce URL-safe Base64 text | `encodeBase64String()` | Uses `Base64.getUrlEncoder()` |
| Restore URL-safe Base64 | `decodeBase64String()` | Uses `Base64.getUrlDecoder()` |
| Produce Hex text | `encodeHexString()` | Uses JDK `HexFormat` |
| Restore Hex | `decodeHexString()` | Propagates parser errors for malformed input |
| Handle nullable input | The extensions above | Normalizes `null` to empty bytes/text |

URL-safe Base64 can use a different alphabet from MIME Base64. If an external protocol specifies padding or the standard alphabet, lock compatibility with its specification and fixture before integration.

## Complete round-trip example

```kotlin
import io.bluetape4k.codec.decodeBase64String
import io.bluetape4k.codec.decodeHexString
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.codec.encodeHexString

val payload = "order:42"

val base64 = payload.encodeBase64String()
check(base64.decodeBase64String().toString(Charsets.UTF_8) == payload)

val hex = payload.encodeToByteArray().encodeHexString()
check(hex.decodeHexString().toString(Charsets.UTF_8) == payload)
```

String helpers include UTF-8 conversion. For binary protocols, stay on the `ByteArray` overloads rather than taking a detour through `String`; that avoids extra allocation and charset guessing.

## Choosing a format

| Requirement | Prefer | Why |
| --- | --- | --- |
| Binary in URLs, cookies, or compact text fields | URL-safe Base64 | Shorter than Hex and URL-oriented alphabet |
| Checksums, short binary IDs, human-comparable dumps | Hex | Longer but easy to compare visually |
| Human-readable domain identifier | An explicit domain format | Base64/Hex carries no domain meaning |
| Confidentiality or integrity | Encryption, signatures, and secret storage | Encoding provides neither property |

Base58, Base62, and Url62 have their own purposes. Do not select one merely because it looks shorter without a contract shared by both endpoints.

## Failure contract

- `null` and blank input normalize to an empty result. Validate before the codec when absence is an error.
- Malformed Base64 and Hex propagate JDK decoder/parser failures.
- Do not turn decoder failures into empty bytes; that conflates an empty payload with a damaged one.
- Logging encoded data keeps the original data classification.

```kotlin
fun decodeExternalToken(encoded: String?): ByteArray {
    val token = requireNotNull(encoded) { "token is required" }
    return runCatching { token.decodeBase64String() }
        .getOrElse { cause -> throw IllegalArgumentException("invalid token encoding", cause) }
}
```

## Operations and testing

Observe payload size, malformed-input counts, and protocol version. Consider a streaming codec when large payloads are repeatedly copied into byte arrays and strings. Cross-language contract tests should include ASCII, UTF-8, empty input, padding, and special-character fixtures.

## Source and representative tests

- [`StringEncoderSupport.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/codec/StringEncoderSupport.kt)
- [`Base64StringEncoder.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/codec/Base64StringEncoder.kt)
- [`HexStringEncoder.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/codec/HexStringEncoder.kt)
- [`StringEncoderSupportTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/codec/StringEncoderSupportTest.kt)
- [`AbstractStringEncoderTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/codec/AbstractStringEncoderTest.kt)

Continue with [Validation and invariants](./validation.md) for boundary failures and [Core recipes](./recipes.md) for an assembled flow.
