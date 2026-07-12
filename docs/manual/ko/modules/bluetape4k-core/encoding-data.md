---
title: Encoding과 데이터 경계
description: Byte, String, URL-safe Base64, Hex 변환에서 format과 failure를 명시합니다.
manualId: bluetape4k-core
chapterId: encoding-data
---

# Encoding과 데이터 경계

## 해결할 문제

Binary 값을 text protocol, cache key, log용 identifier로 옮길 때는 같은 bytes가 양쪽에서 같은 의미로 복원되어야 합니다. `bluetape4k-core`의 codec helper는 nullable 입력, UTF-8 문자열 변환, URL-safe Base64와 Hex의 규칙을 짧은 API로 고정합니다.

Encoding은 암호화가 아닙니다. Base64나 Hex로 바꾼 API key와 개인 정보는 여전히 secret입니다.

## Mental model

```text
domain value -> charset로 bytes 확정 -> wire format encode -> text
text -> wire format decode -> bytes -> 같은 charset으로 domain value 복원
```

문자열과 bytes의 경계에는 charset이 있고, bytes와 전송 문자열의 경계에는 encoding format이 있습니다. 두 결정을 하나라도 암묵적으로 두면 다른 언어 또는 서비스와 연동할 때 round-trip이 깨집니다.

## API와 실제 계약

| 작업 | API | 계약 |
| --- | --- | --- |
| URL-safe Base64 문자열 생성 | `encodeBase64String()` | `Base64.getUrlEncoder()` 사용 |
| URL-safe Base64 복원 | `decodeBase64String()` | `Base64.getUrlDecoder()` 사용 |
| Hex 문자열 생성 | `encodeHexString()` | JDK `HexFormat` 사용 |
| Hex 복원 | `decodeHexString()` | 잘못된 문자는 parser 예외로 전달 |
| nullable 입력 | 위 extension | `null`은 empty bytes/string으로 정규화 |

URL-safe Base64는 일반 MIME Base64와 alphabet이 다를 수 있습니다. 외부 protocol이 padding 여부나 표준 Base64 alphabet을 요구한다면 상대편 명세와 실제 fixture로 먼저 호환성을 고정해야 합니다.

## 완전한 round-trip 예제

```kotlin
import io.bluetape4k.codec.decodeBase64String
import io.bluetape4k.codec.decodeHexString
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.codec.encodeHexString

val payload = "주문:42"

val base64 = payload.encodeBase64String()
check(base64.decodeBase64String().toString(Charsets.UTF_8) == payload)

val hex = payload.encodeToByteArray().encodeHexString()
check(hex.decodeHexString().toString(Charsets.UTF_8) == payload)
```

문자열 helper는 UTF-8 변환을 포함합니다. Binary protocol을 다룬다면 `String`으로 우회하지 말고 `ByteArray` overload를 사용해 불필요한 allocation과 charset 추측을 피합니다.

## 어떤 포맷을 선택할까

| 요구 | 권장 | 이유 |
| --- | --- | --- |
| URL, cookie, compact text field로 binary 전달 | URL-safe Base64 | Hex보다 짧고 URL alphabet에 맞음 |
| checksum, 짧은 binary ID, 사람이 비교할 dump | Hex | 길지만 한 눈에 비교하기 쉬움 |
| 사람이 읽는 domain identifier | 명시적인 domain format | Base64/Hex는 의미를 설명하지 않음 |
| 비밀 보호 | 암호화·서명·secret store | Encoding은 confidentiality/integrity를 제공하지 않음 |

Base58, Base62, Url62도 별도 목적을 갖지만, 상대 시스템의 format contract 없이 “더 짧아 보인다”는 이유로 선택하지 않습니다.

## Failure 계약

- `null`과 blank input은 empty 결과로 정규화됩니다. `null` 자체가 오류인 API라면 codec 호출 전에 validation해야 합니다.
- malformed Base64와 Hex는 JDK decoder/parser 예외를 숨기지 않습니다.
- decoder 실패를 empty bytes로 바꾸면 “빈 payload”와 “손상된 payload”를 구분할 수 없으므로 경계에서 명시적으로 매핑합니다.
- encode 결과를 로그에 남길 때 원본의 보안 등급은 그대로 유지합니다.

```kotlin
fun decodeExternalToken(encoded: String?): ByteArray {
    val token = requireNotNull(encoded) { "token is required" }
    return runCatching { token.decodeBase64String() }
        .getOrElse { cause -> throw IllegalArgumentException("invalid token encoding", cause) }
}
```

## 운영과 테스트

Payload size, malformed-input 수, protocol version을 관찰합니다. 대용량 payload를 매번 byte array와 string으로 복제한다면 streaming codec을 검토합니다. 서로 다른 언어와 통신할 때는 ASCII fixture, UTF-8 fixture, empty input, padding/특수문자 fixture를 contract test에 둡니다.

## Source와 representative test

- [`StringEncoderSupport.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/codec/StringEncoderSupport.kt)
- [`Base64StringEncoder.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/codec/Base64StringEncoder.kt)
- [`HexStringEncoder.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/codec/HexStringEncoder.kt)
- [`StringEncoderSupportTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/codec/StringEncoderSupportTest.kt)
- [`AbstractStringEncoderTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/codec/AbstractStringEncoderTest.kt)

검증 실패 정책은 [검증과 불변식](./validation.md), 전체 조립은 [Core 실전 레시피](./recipes.md)에서 이어집니다.
