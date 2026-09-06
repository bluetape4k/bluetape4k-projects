# outbound 오류 정제 공용 API 설계

## 목표

`bluetape4k-http`에 framework-neutral한 순수 함수를 추가하여 outbound 호출
실패를 DB에 저장할 때 필요한 최소 진단 정보만 남기고 credential-like 값을
항상 `[redacted]`로 치환한다. Spring/Ktor caller의 HTTP 호출, retry, status
분류, transaction, cancellation 정책은 그대로 유지한다.

## 현재 근거

- workshop의 Spring·Ktor repository가 동일한 private regex와 240자/첫 줄/
  `HTTP <status>` 로직을 복사하고 있다.
- provider `io/http`에는 OkHttp header logging redaction과 OTel telemetry
  redaction은 있지만 persisted outbound-error 문자열 계약은 없다.
- 두 consumer 모두 `lastError`를 `varchar(240)`에 저장하므로 결과 상한은
  Kotlin `Char` 240자로 유지해야 한다.
- #1650은 `Authorization`, `Cookie`, `Token`, `Secret`, `API-Key`의 `:`/`=`와
  Bearer 변형, multiline, null/blank, raw secret 비노출을 public API/KDoc와
  테스트로 고정하도록 요구한다.

## 선택한 설계

파일: `io/http/src/main/kotlin/io/bluetape4k/http/OutboundErrorSanitizer.kt`

```kotlin
package io.bluetape4k.http

/**
 * outbound 오류를 DB/log에 보관할 수 있는 제한된 한 줄 요약으로 변환합니다.
 */
fun sanitizeOutboundError(statusCode: Int, rawMessage: String?): String
```

동작 순서는 다음과 같다.

1. 항상 `HTTP <statusCode>`를 prefix로 만든다. `Int` status의 prefix는
   240자보다 짧으므로 prefix 자체는 자르지 않는다.
2. `null`, blank, 빈 첫 줄은 prefix만 반환한다.
3. 원문의 첫 줄만 취하고 양 끝 공백을 제거한다. 개행 이후 내용은 결과에
   포함하지 않는다. 이 trim은 기존 두 consumer의 저장값에서 허용되는
   의도적인 정규화이며 exact test와 README에 고정한다.
4. 다음 key를 case-insensitive하게 찾아 `key:[redacted]`로 치환한다.
   `Authorization`, `Cookie`, `Token`, `Secret`, `API-Key`와 `API_Key`,
   `API Key` 변형을 지원하며 `:`/`=` 구분자와 선택적인 `Bearer`를 허용한다.
   key 표기의 원래 대소문자는 replacement에서 보존한다. 값은 공백 전까지의
   token 또는 escaped quote를 지원하는 single/double quoted value로 인식한다.
   key-value marker는 있지만 값이 비어 있거나 quote가 닫히지 않는 등 문법이
   깨진 경우에는 전체 first line을 버리고 status-only를 반환해 fail-closed한다.
5. prefix 길이를 제외한 남은 길이만큼 첫 줄을 자르되 UTF-16 surrogate pair를
   분할하지 않는 Kotlin `Char` 상한을 적용한다. 최종 결과는 240 UTF-16
   code units 이내이며 함수는 예외를 던지지 않는 순수 함수다.

예:

```kotlin
sanitizeOutboundError(503, "Authorization:=secret-token temporary outage")
// HTTP 503 Authorization:[redacted] temporary outage

sanitizeOutboundError(599, "timeout\nAuthorization: secret-token")
// HTTP 599 timeout

sanitizeOutboundError(422, null)
// HTTP 422
```

## 대안과 기각

1. **기존 `okhttp3` header redaction 재사용**
   - 기각: header 전용 marker와 범위가 다르고 HTTP client dependency 및
     logging semantics를 persisted error API에 끌어온다.
2. **OpenTelemetry exception redaction을 확장**
   - 기각: telemetry event의 보존 정책과 DB 저장 계약은 다르며 원본
     Throwable 재전파 의미를 바꾸면 caller 동작이 깨진다.
3. **Spring/Ktor별 공용 base class에 helper 추가**
   - 기각: 두 framework를 provider API에 결합하고 다른 caller 재사용을 막는다.

## 로그와 예외 경계

이 함수가 보장하는 secret-free 범위는 반환 문자열이다. caller는 원본
`Throwable`을 `log.warn(e)`나 DB/exception payload로 전달하지 않고 이 함수의
결과를 로그와 persistence에 사용해야 한다. `CancellationException`은 기존
caller 규칙대로 재전파하고 원본을 로그하지 않는다. provider API 자체는
Throwable을 받지 않으며 원본 예외를 만들거나 기록하지 않는다.

## 실패 모드와 대응

1. credential key의 대소문자·separator·Bearer·quoted/escaped 변형은 모두
   동일한 replacement로 처리해 인식된 credential raw value가 남지 않게 한다.
   marker가 있으나 malformed인 경우 status-only로 내려 fail-closed한다.
2. multiline 예외의 두 번째 줄에 secret이 있어도 첫 줄만 반환하여 저장하지
   않는다.
3. null/blank/malformed input은 예외 대신 status-only 또는 redacted first line을
   반환한다.
4. message가 매우 길어도 `take` 인자 음수 오류 없이 결과를 240자 이내로
   만들고, surrogate pair를 중간에서 자르지 않는다.
5. 정제 함수는 원본 예외를 log/throw하지 않으므로 caller가 retry/status/
   transaction/cancellation을 기존대로 제어한다.

## 호환성과 migration

- `bluetape4k-http`의 additive API이며 기존 header/OTel redaction을 변경하지
  않는다.
- Ktor consumer는 기존 HTTP dependency를 재사용한다. Spring consumer는
  중앙 catalog의 기존 `bluetape4k-http` alias를 `implementation`으로 추가한다.
- 두 consumer에서 private regex/constants/helper만 제거하고, outbound result의
  status·retry·DB update 및 transaction 경계는 그대로 둔다.
- provider artifact publication과 catalog/BOM 갱신이 끝나기 전에는 consumer
  PR을 만들지 않는다.

## 수용 기준

- [ ] public KDoc와 영/국문 README가 prefix, first-line, null/blank, redaction,
      240자 계약을 설명한다.
- [ ] 모든 key 변형, `:`/`=`, Bearer, boundary whitespace, multiline,
      malformed, Unicode/240자 경계가 exact unit test로 고정된다.
- [ ] 반환 문자열에 인식된 raw secret이 남지 않으며, caller migration이 원본
      Throwable을 로그/예외/DB payload로 전달하지 않음을 별도 테스트한다.
- [ ] 기존 HTTP header/OTel API 및 Spring/Ktor dependency 경계와 충돌하지
      않는다.
- [ ] 함수가 예외를 던지지 않고 caller retry/status/transaction/cancellation을
      변경하지 않는다.

## DoD

provider 구현·단위 테스트·문서·ABI/POM·정적/보안 검사 결과가 exact head에서
확인되고, CI와 독립 리뷰가 수렴한 뒤 PR merge-ready 상태로 보고한다.
publication, catalog 갱신, downstream consumer merge는 별도 후속 gate다.
