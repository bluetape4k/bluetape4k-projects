# 공통 Digest API 설계

## 상태

사용자 검토용 설계 초안이다. 설계 독립 리뷰와 통합 리뷰를 통과한 뒤 사용자 검토를 받고 구현 계획을 작성한다.

## 문제와 목표

`bluetape4k-workshop/commerce/*` 여러 모듈에서 SHA-256을 직접 계산한다. 일부 코드는 `MessageDigest`를 사용하고, 일부는 `bluetape4k-tink`의 `TinkDigester`를 사용한다. 계산과 출력 인코딩 선택이 분산돼 같은 기능을 반복 구현하며 소비자가 공통 API를 사용할 수 없다.

`bluetape4k-projects`의 `io/io` 모듈에 알고리즘 독립 공통 인터페이스와 JDK 구현체를 제공한다. 기존 `io/tink`의 `TinkDigester`는 그 인터페이스를 구현하는 호환 어댑터로 유지한다. Workshop의 SHA-256 소비자는 Projects API에 의존하도록 옮긴다.

Google Tink의 MAC은 공유 대칭 키로 인증 태그를 계산하고 검증하는 기본 연산(primitive)이다. 키 없는 SHA-256과는 출력과 보안 의미가 다르므로 일반 digest API를 MAC 구현으로 설명하지 않는다. [Tink MAC 문서](https://developers.google.com/tink/mac)는 MAC을 데이터 인증 연산으로 정의한다.

## 현재 기준 정보

- `io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigester.kt`는 `java.security.MessageDigest`를 사용한다. `digest(String)`은 UTF-8 입력과 프로젝트 Base64 codec을 사용하고, 현재 기본 결과는 URL-safe Base64이며 padding을 포함한다. SHA-256 입력 `abc`의 기존 결과는 `ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0=`이다.
- `TinkDigester`는 원시 다이제스트 바이트, 문자열 Base64, 소문자 16진수, Base64 검증, 16진수 검증을 제공한다. 이 API와 `TinkDigesters`의 기존 알고리즘 접근점을 유지한다.
- `io/io/build.gradle.kts`에는 `compileOnly(project(":bluetape4k-tink"))`가 있지만 `io/io/src`의 구현은 Tink 타입을 참조하지 않는다. 공통 API를 `io/io`에 두고 `io/tink`가 이를 구현하려면 이 의존성을 제거하고 의존 방향을 `io/tink -> io/io`로 둬 순환을 막아야 한다.
- Workshop `commerce/*`에는 키 없는 SHA-256을 쓰는 production 소비자가 9개 모듈, 29개 파일, 35개 호출 표현으로 확인됐다. 이 중 JDK `MessageDigest` 직접 사용은 7개 모듈·27개 파일·32개 호출이고, 기존 `TinkDigester` 경유는 2개 모듈·2개 파일·3개 호출이다. `pre-generated-voucher-pool`은 이미 `bluetape4k-io`를 선언한다.
- Projects는 `2.2.0` 개발선을 사용하고 Workshop의 의존성 catalog는 Bluetape4k `2.1.0`을 가리킨다. 현재 원격 저장소에서 `2.2.0-SNAPSHOT` artifact를 조회할 수 없다. 이 설계는 artifact publication, release, tag, PR 생성을 승인하지 않는다.
- 기존 Workshop 이슈 #989와 Projects 이슈 #1649는 이미 닫혀 있다. 이번 작업은 기존 이슈의 범위를 다시 등록하지 않고, 공통 API 제공과 나머지 소비자 이전을 다룬다.

## 범위

### 포함

- `io/io`에 `Digester`, 상태를 가진 `DigestSession`, JDK 기반 `JdkDigester`, SHA-256 기본 접근점 `DigestUtils.SHA256`을 추가한다.
- 다이제스트 바이트, UTF-8 문자열 입력, 증분 update, 소문자 16진수, Base64 인코딩 및 검증을 제공한다.
- Base64 URL-safe/standard 알파벳과 padding 여부를 명시하는 API를 제공한다.
- 기존 `TinkDigester`의 public 시그니처와 결과를 유지하면서 공통 `Digester` 인터페이스를 구현하게 한다.
- Workshop의 일반 SHA-256 생성 코드를 새 `bluetape4k-io` API로 이전한다.

### 제외

- Google Tink MAC, PRF, 서명, 암호화 키 관리의 변경.
- HMAC, 키 기반 무결성 검증, 비밀번호 해시, 서명 검증의 SHA-256 이전.
- Workshop의 저장 데이터, event wire format, migration checksum, canonicalization, field order, framing, separator, hex 소문자화, truncation 규칙 변경.
- 신규 digest 알고리즘 목록 또는 별도 알고리즘 제공자 프레임워크.
- publication, stable release, tag, PR 생성, merge.

## 선택한 API 경계

공개 package는 `io.bluetape4k.io.digest`다. 소스 위치는 `io/io/src/main/kotlin/io/bluetape4k/io/digest/`이며 Gradle project 이름은 `:bluetape4k-io`다. 공개 파일은 `Digester.kt`, `DigestSession.kt`, `JdkDigester.kt`, `DigestUtils.kt`, `Base64Codec.kt`, `Base64Format.kt`로 나누고 각 public type/method의 한국어 KDoc을 작성한다. `JdkDigestSession`은 이 package의 internal 구현으로 둔다.

`Digester`는 불변 알고리즘 제공자이며 여러 작업에서 공유할 수 있다. 각 `newSession()` 호출은 독립적인 `MessageDigest` 상태를 가진 새 `DigestSession`을 반환한다. 세션은 변경 가능하고 스레드 안전하지 않으므로 작업 단위마다 만든다. 여러 조각을 순서대로 `update`하면 배열을 합치는 중간 복사 없이 기존 계산을 표현할 수 있다. 기존 코드에 구분자나 길이 접두사가 있으면 그 바이트 조합을 그대로 유지한다.

개념적인 public API 시그니처는 다음과 같다. 아래 선언은 공개 type과 method의 완결된 시그니처이며, 설명을 위한 코드이므로 method 구현 본문만 생략했다. 같은 기능의 Java overload는 실제 공개 메서드로 명시하며 Kotlin default parameter에 의존하지 않는다.

```kotlin
interface Digester {
    val algorithm: String
    fun newSession(): DigestSession

    fun digest(data: ByteArray): ByteArray
    fun digestUtf8(data: String): ByteArray
    fun digestBase64(data: ByteArray): String
    fun digestBase64(data: ByteArray, format: Base64Format): String
    fun digestBase64Utf8(data: String): String
    fun digestBase64Utf8(data: String, format: Base64Format): String
    fun digestHex(data: ByteArray): String
    fun digestHexUtf8(data: String): String
    fun matches(data: ByteArray, expectedDigest: ByteArray): Boolean
    fun matchesHex(data: ByteArray, expected: String): Boolean
    fun matchesHexUtf8(data: String, expected: String): Boolean
    fun matchesBase64(data: ByteArray, expected: String): Boolean
    fun matchesBase64(data: ByteArray, expected: String, format: Base64Format): Boolean
}

interface DigestSession {
    fun update(data: ByteArray): DigestSession
    fun update(data: ByteArray, offset: Int, length: Int): DigestSession
    fun update(value: Byte): DigestSession
    fun digest(): ByteArray
    fun digestHex(): String
}

object DigestUtils {
    @JvmField val SHA256: Digester = JdkDigester("SHA-256")
}

class JdkDigester(override val algorithm: String) : Digester

enum class Base64Format {
    STANDARD_PADDED,
    STANDARD_UNPADDED,
    URL_SAFE_PADDED,
    URL_SAFE_UNPADDED,
}

object Base64Codec {
    @JvmStatic fun encode(data: ByteArray): String
    @JvmStatic fun encode(data: ByteArray, format: Base64Format): String
    @JvmStatic fun decode(data: String): ByteArray
    @JvmStatic fun decode(data: String, format: Base64Format): ByteArray
}
```

Java 소비자는 `DigestUtils.SHA256` 필드와 format 생략/명시 overload를 직접 호출한다. `Base64Codec` 공개 함수는 `@JvmStatic`으로 노출한다. `io/io/src/test/java/io/bluetape4k/io/digest/`에 실제 Java 호출 및 JVM descriptor 확인 fixture를 둔다.

Kotlin 사용 예시는 입력 표현과 wire 형식을 구분한다.

```kotlin
val rawHash = DigestUtils.SHA256.digest(payloadBytes)
val textHash = DigestUtils.SHA256.digestUtf8("문자열 입력은 UTF-8")
val fingerprint = DigestUtils.SHA256.digestBase64(payloadBytes, Base64Format.URL_SAFE_UNPADDED)
val multipartHash = DigestUtils.SHA256.newSession()
    .update(firstField)
    .update(separatorByte)
    .update(secondField)
    .digest()
```

마지막 예시는 기존 계약이 `firstField || separatorByte || secondField`였을 때만 같은 입력 바이트를 만든다. 기존 코드에 길이 접두사, canonical serialization 또는 별도 framing이 있으면 이전 호출도 동일한 바이트를 만들어야 한다.

문자열 convenience API는 UTF-8을 고정한다. `digest(String)`을 공통 인터페이스에 추가하지 않는다. 기존 `TinkDigester.digest(String): String`과 JVM에서 반환 타입이 충돌하기 때문이다. 공통 API의 문자열 함수는 `digestUtf8`처럼 입력 인코딩을 드러내고, 기존 어댑터의 `digest(String)`은 현재 Base64 결과를 그대로 유지한다.

`DigestSession.digest()`는 현재 버퍼를 종료해 결과를 반환하고 JDK `MessageDigest.digest()`와 동일하게 세션을 초기 상태로 되돌린다. `update`에 전달한 배열은 호출 중 처리하며 호출자 배열을 보관하지 않는다. `offset`/`length`가 입력 배열 범위를 벗어나면 JDK가 던지는 범위 예외를 그대로 노출한다. 지원하지 않는 알고리즘 이름은 세션 생성 시 JDK `NoSuchAlgorithmException`으로 드러난다.

`DigestSession.digestHex()`도 현재 버퍼를 같은 방식으로 소비하고 소문자 16진수로 반환한 뒤 세션을 초기 상태로 되돌린다. 따라서 `digestHex()` 뒤의 다음 `update`는 이전 입력에 이어지지 않고 새 계산을 시작한다.

## Base64 형식과 기본값

형식은 하나의 enum으로 고정해 alphabet과 padding을 실수로 따로 조합하지 않게 한다.

| 형식 | 알파벳 | 인코딩 padding |
|---|---|---|
| `STANDARD_PADDED` | `+`, `/` | 포함 |
| `STANDARD_UNPADDED` | `+`, `/` | 생략 |
| `URL_SAFE_PADDED` | `-`, `_` | 포함 |
| `URL_SAFE_UNPADDED` | `-`, `_` | 생략 |

기본값은 `URL_SAFE_PADDED`다. 이는 `TinkDigester.digest(String)`의 기존 동작을 보존한다. 형식을 생략한 호출은 호환성을 위한 편의값이고, 영속 데이터·wire format·외부 계약은 형식을 명시한다.

`io/io`는 새 codec 의존성을 추가하지 않고 JDK `java.util.Base64`를 사용한다. `encode(bytes, format)`은 표의 padding 규칙을 정확히 출력한다. `decode(text, format)`은 선택한 alphabet과 padding mode만 허용하고 공백, 다른 alphabet, 선택 형식과 다른 padding, 정규 인코딩으로 되돌릴 수 없는 pad bits를 `IllegalArgumentException`으로 거부한다. 구현은 선택한 JDK decoder로 decode한 뒤 같은 형식으로 다시 encode해 입력과 정확히 같은지 확인한다.

신규 API의 `matchesBase64`는 decoder가 던진 malformed-input `IllegalArgumentException`만 `false`로 바꾼다. digest/provider 오류 등 다른 예외는 숨기지 않는다. 길이가 틀린 유효 Base64 digest는 raw digest 비교 결과에 따라 `false`다. raw digest 비교는 `MessageDigest.isEqual`을 사용한다. 기존 `TinkDigester.matches(String, String)`은 기존 decode 계약을 그대로 보존한다.

## 구현과 의존 방향

1. `io/io`는 `io.bluetape4k.io.digest` package에 독립적인 `Digester` 계약, `DigestSession`, JDK 구현, Base64 codec, `DigestUtils.SHA256`을 소유한다. API KDoc은 한국어로 작성하고 `io/io/README.md`와 `README.ko.md`에 각각 해당 locale 예제를 추가한다.
2. `io/tink`는 `api(project(":bluetape4k-io"))` 방향으로 의존한다. `TinkDigester`는 기존 생성자와 메서드를 유지하고 하나의 `JdkDigester` 위임체로 공통 인터페이스를 구현한다. `algorithmName` 공개 property는 `Digester.algorithm`에 매핑하며 기존 JVM getter를 보존한다. 이 클래스가 실제 Tink primitive를 사용한다고 문서화하지 않는다.
3. 기존 `TinkDigester` 공개 메서드는 다음처럼 위임하거나 호환 구현을 유지한다.

| 기존 API | 신규 구현 경로 | 유지할 계약 |
|---|---|---|
| `algorithmName` | `Digester.algorithm`과 같은 값 | Kotlin property와 JVM `getAlgorithmName()` |
| `digest(ByteArray)` | 위임체 `digest(ByteArray)` | 원시 다이제스트 바이트 |
| `digest(String)` | UTF-8 → `digestBase64Utf8(..., URL_SAFE_PADDED)` | 기존 Base64 문자열 결과 |
| `digestHex(String)` | `digestHexUtf8(String)` | UTF-8 입력과 소문자 16진수 |
| `matches(ByteArray, ByteArray)` | 위임체 `matches(ByteArray, ByteArray)` | constant-time 다이제스트 비교 |
| `matchesHex(String, String)` | `matchesHexUtf8(String, String)` | 길이·소문자 형식과 다이제스트 바이트 일치 |
| `matches(String, String)` | 기존 Base64 decode 동작 유지 후 위임체 `matches` | 잘못된 기대값은 `false`; 기존 입력 허용 범위 유지 |
| `toString()` | 어댑터 자체 구현 | `TinkDigester(algorithm=<name>)` |
| `TinkDigesters` presets | 기존 lazy preset 구성 유지 | 이름, property type, 접근 방식 |

### Workshop 호출별 migration mapping

현재 production source scan은 9개 commerce 모듈, 29개 파일을 찾았다. 아래에서 `bytes`는 기존 바이트 배열 그대로, `UTF-8`은 기존 문자열을 UTF-8로 인코딩한 값이다. 여러 필드를 조합하는 구분자, 길이 접두사, 정렬 순서, 바이트 순서, nullable 표시는 Workshop 호출부가 계속 담당한다. 신규 API는 이 규칙을 정규화하거나 바꾸지 않는다.

| 모듈 / 파일 및 줄 | 기존 입력과 framing | 현재 결과 표현 | 보존할 대체 호출 |
|---|---|---|---|
| `commerce/concert-ticket-flash-sale/src/main/kotlin/io/bluetape4k/workshop/commerce/ticket/config/TicketMigrationRunner.kt:64` | SQL 리소스 원시 바이트 | 소문자 16진수 체크섬 | `digestHex(bytes)` |
| `commerce/concert-ticket-flash-sale/src/main/kotlin/io/bluetape4k/workshop/commerce/ticket/idempotency/IdempotencyFingerprint.kt:45` | UTF-8 문자열 | URL-safe unpadded Base64 | `digestBase64Utf8(value, URL_SAFE_UNPADDED)` |
| `commerce/concert-ticket-flash-sale/src/main/kotlin/io/bluetape4k/workshop/commerce/ticket/purchase/internal/PurchaseService.kt:922,934` | effect ID 문자열 `ticket-effect-v1:<operationId>:<disposition>`; advisory key 문자열 `ticket-active-guard-v1\0<saleId>\0<kind>\0<subjectId>` | 각각 raw digest bytes, 앞 8 bytes의 big-endian `Long` | 각 기존 문자열을 `digestUtf8(value)`에 그대로 전달하고 advisory 경로는 기존 8-byte 변환을 유지 |
| `commerce/concert-ticket-flash-sale/src/main/kotlin/io/bluetape4k/workshop/commerce/ticket/purchase/internal/RefundService.kt:372` | refund effect UTF-8 문자열 | 원시 다이제스트 바이트 | `digestUtf8(value)` |
| `commerce/event-sourced-promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/eventsourced/domain/EventContract.kt:347–352` | 순서가 있는 필드를 `|`로 join한 UTF-8 문자열 | 소문자 16진수 | `digestHexUtf8(canonicalMaterial)` |
| `commerce/event-sourced-promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/eventsourced/idempotency/EventSourcedIdempotency.kt:74–80` | UTF-8 문자열 | lowercase hex `ReceiptDigest` | `digestHexUtf8(value)` |
| `commerce/event-sourced-promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/eventsourced/snapshot/EventSnapshots.kt:261–265` | UTF-8 문자열 | lowercase hex | `digestHexUtf8(value)` |
| `commerce/order-lifecycle-fulfillment/src/main/kotlin/io/bluetape4k/workshop/commerce/order/idempotency/IdempotencyFingerprint.kt:50–54` | UTF-8 문자열 | 소문자 16진수 | `digestHexUtf8(value)` |
| `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/admission/VoucherPoolRedisSignals.kt:43–50` | domain, namespace, principal digest의 4-byte big-endian 길이 접두사와 바이트 순서 | versioned lowercase hex key | 기존 framing byte를 같은 순서로 `newSession().update(...)` 후 `digestHex()` |
| `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/application/CampaignBatchCommandService.kt:1787–1793,1805–1811,2415–2427` | `initialCheckpoint`: domain, provenance digest, 8-byte expected count; `advanceCheckpoint`: domain, previous digest, 8-byte ordinal, stable digest; `requestDigest`: domain, 8-byte first/source ordinals와 stable/payload digests | 앞의 두 raw `DigestValue` bytes, 마지막 lowercase hex | 각 함수의 기존 update 순서를 session에 복사하고 앞의 둘은 `digest()`, 마지막은 `digestHex()` |
| `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/application/VoucherPoolRevocationService.kt:1157,1562–1583` | preview token UTF-8 문자열; 별도 impact material은 pipe-delimited 순서 유지 | 각각 lowercase hex, raw digest bytes | 각각 `digestHexUtf8(value)`, `newSession()`의 기존 bytes 순서 후 `digest()` |
| `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/config/VoucherPoolKeyMaterialConfiguration.kt:578–585` | raw key-material bytes | lowercase hex fixture fingerprint; digest byte array를 변환 뒤 zeroize | `digest(bytes)` 후 기존 hex 변환을 수행하고 `finally`에서 결과 byte array를 zeroize |
| `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/config/VoucherPoolMigrationRunner.kt:63` | SQL resource raw bytes | hex checksum | `digestHex(bytes)` |
| `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/idempotency/VoucherPoolFingerprint.kt:85–107,128–131` | domain/operation, UTF-8 정렬 field 이름·값, null/value marker, length-prefixed bytes | raw `CommandFingerprint` bytes | 기존 framing이 만든 동일 byte sequence를 session에 전달하고 `digest()` |
| `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/idempotency/VoucherPoolIdempotencyRepository.kt:1466–1471` | domain UTF-8 bytes, zero byte, raw value bytes 순서 | raw digest bytes | session에 세 조각을 기존 순서대로 update한 뒤 `digest()` |
| `commerce/pre-generated-voucher-pool/src/main/kotlin/io/bluetape4k/workshop/commerce/voucherpool/web/OperatorAccessFilter.kt:139–141,269–270` | remote address와 nullable 문자열(null은 빈 문자열) | raw digest bytes | 기존 UTF-8 바이트를 `newSession()`에 동일 순서로 update한 뒤 `digest()` |
| `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/application/AllocationService.kt:509–516` | domain과 vararg 문자열을 NUL로 join한 UTF-8 문자열 | lowercase hex | `digestHexUtf8(material)` |
| `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/config/VoucherMigrationRunner.kt:59` | SQL resource raw bytes | hex checksum | `digestHex(bytes)` |
| `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/idempotency/IdempotencyFingerprint.kt:44` | UTF-8 문자열 | URL-safe unpadded Base64 | `digestBase64Utf8(value, URL_SAFE_UNPADDED)` |
| `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/persistence/EventInboxRepository.kt:262–267` | `tenantId:aggregateType:aggregateId` UTF-8 문자열 | 앞 8 digest bytes의 big-endian advisory-lock `Long` | `digestUtf8(value)` 뒤 같은 8-byte/endianness 변환 |
| `commerce/promotion-voucher-campaign/src/main/kotlin/io/bluetape4k/workshop/commerce/voucher/security/VoucherCodeService.kt:424–432` | prefix UTF-8 문자열 | 앞 두 digest bytes로 계산하는 기존 58^2 체크섬 | `digestUtf8(prefix)` 뒤 기존 modulo/Base58 변환 |
| `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/application/BillingCommandServices.kt:319–322` | source system과 event ID를 NUL로 join한 UTF-8 문자열 | lowercase hex | `digestHexUtf8(material)` |
| `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/application/CorrectionReconciliationServices.kt:326–329` | UTF-8 material 문자열 | lowercase hex | `digestHexUtf8(material)` |
| `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/eventstore/CanonicalEventHash.kt:31–47` | canonical newline-delimited event fields와 JSON UTF-8 | lowercase hex | canonicalization 결과를 그대로 `digestHexUtf8(canonicalEvent)` |
| `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/idempotency/CommandFingerprint.kt:69–72` | UTF-8 문자열 | lowercase hex | `digestHexUtf8(value)` |
| `commerce/usage-metering-billing-event-sourcing/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/eventsourcing/worker/ProjectionWorker.kt:283–289` | exception qualified class name, colon, message UTF-8 | lowercase hex | 기존 문자열 조합을 보존해 `digestHexUtf8(material)` |
| `commerce/usage-metering-billing-ledger/src/main/kotlin/io/bluetape4k/workshop/commerce/metering/idempotency/CommandFingerprint.kt:32–37` | canonical string UTF-8 | lowercase hex | `digestHexUtf8(canonical)` |
| `commerce/shared/src/main/kotlin/io/bluetape4k/workshop/commerce/shared/integration/CommerceIntegrationContracts.kt:12–19` | `PayloadDigest.of`와 `matches`의 UTF-8 문자열 | Tink adapter의 lowercase hex | `digestHexUtf8` / `matchesHexUtf8` |
| `commerce/reservation-control-plane/src/main/kotlin/io/bluetape4k/workshop/commerce/reservation/idempotency/IdempotencyFingerprint.kt:41–47` | domain과 fields를 NUL로 join한 UTF-8 문자열 | Tink adapter의 lowercase hex | `digestHexUtf8(material)` |

`VoucherPoolKeyMaterialConfiguration`은 digest byte zeroization을 보존하므로 편의 `digestHex`로 바꾸지 않는다. `VoucherCodeService`의 Base58 checksum도 출력 변환을 그대로 남기고 digest byte만 공통 API에서 얻는다. 두 경로 모두 공개 API가 기존 특수 동작을 흡수하지 않는 예외다.

4. `io/io`의 불필요한 `compileOnly(project(":bluetape4k-tink"))`를 제거해 역방향 의존과 cycle을 없앤다.
5. Workshop은 기존 `bluetape4k-io` catalog alias를 사용해 dependency를 직접 선언하고 `DigestUtils.SHA256`의 one-shot 또는 session API를 사용한다. 실제 호출별 입력 조합 순서와 반환 표현은 migration table대로 기존과 같아야 한다.
6. 각 공개 type/method의 한국어 KDoc, 두 README locale의 사용 예, Workshop consumer migration 예제를 acceptance에 포함한다.

## 호환성 및 보안 경계

- `TinkDigester` 생성자, `algorithmName` property, `digest(ByteArray)`, `digest(String)`, `digestHex(String)`, 두 `matches` overload, `matchesHex`, `toString`, `TinkDigesters` property 타입과 접근 방식을 보존한다.
- `digest(String)` 결과는 UTF-8 + URL-safe padded Base64로 유지한다. `matches(String, String)`은 기존처럼 malformed Base64를 `false`로 처리한다. `matchesHex`는 길이와 lowercase `0-9a-f` 형식을 검사한 뒤 constant-time digest byte 비교를 한다.
- 새 API는 일반 digest 출력만 제공한다. 일반 digest는 입력의 무결성/출처 인증을 하지 않는다. 공유 비밀을 가진 당사자 사이에서 인증 태그를 만들어 검증하려면 HMAC 같은 keyed MAC을 사용한다. 검증자가 비밀키를 가질 수 없고 공개 검증이 필요하면 서명을 사용한다. 키 없는 SHA-256은 어느 경우에도 인증을 대신하지 않는다.
- `JdkDigester`는 JDK가 제공하는 알고리즘 이름을 그대로 받는다. MD5와 SHA-1은 레거시 비보안 checksum 호환 목적만 허용하고 인증·collision resistance가 필요한 목적에는 사용하지 않도록 constructor KDoc에서 경고한다. 일반 소비자는 `DigestUtils.SHA256`을 우선 사용한다.
- `DigestSession`은 provider 내부 상태를 안전하게 지우는 `clear`/`close` 계약을 제공하지 않으며, `digest()`/`digestHex()`의 reset은 secure erasure 보장이 아니다. 이 API를 민감 데이터의 안전한 메모리 폐기 수단으로 사용하지 않는다. 호출자가 소유한 민감 `ByteArray`는 호출자가 관리하고 필요하면 직접 zeroize한다.
- 기존 `TinkDigester.matches(String, String)`은 호환성을 위해 기존 Base64 decoder 허용 범위를 유지하며 `Base64Format`별 alphabet, padding, canonical pad bits 검증을 보장하지 않는다. 기존 메서드의 이 동작은 compatibility test로 고정한다. 프로토콜에서 Base64 표현의 canonical 형식도 검증해야 하면 새 `matchesBase64(data, expected, format)` 또는 `Base64Codec.decode(text, format)`을 사용한다. `TinkDigester.matches(String, String)`은 unkeyed digest 비교이므로 인증 수단으로 사용하지 않는다.
- Workshop 이전은 계산 입력의 framing이나 출력을 바꾸지 않는다. 독립 JDK oracle이 이미 있는 테스트는 oracle 구현을 새 API로 치환하지 않는다.
- 기존 public JVM surface를 확인하고, `TinkDigester`의 기존 바이트코드 소비자 호환성을 ABI 도구 또는 사전 컴파일 consumer로 검증한다.

## Workshop artifact 순서

소스 변경 순서는 Projects API 구현·검증 후 Workshop 소비자 이전이다. 다만 현재 Workshop catalog는 `2.1.0`을 고정하고 `2.2.0-SNAPSHOT`은 원격에서 조회되지 않는다. 따라서 Workshop의 새 dependency version을 사용할 때는 Projects artifact가 실제로 사용 가능한 시점이 필요하다.

Workshop의 로컬 targeted compile/test는 Gradle CLI composite-build 옵션 `--include-build <projects-worktree>`로 실행한다. Projects build의 `group:name`은 `io.github.bluetape4k:bluetape4k-io`이고 Workshop catalog alias도 같은 module 좌표를 요청한다. `dependencyInsight` 결과에서 선택된 component가 Projects worktree의 `:bluetape4k-io`인지 확인한다. 이 방식은 Workshop `settings.gradle.kts`와 catalog를 임시 수정하지 않는다. 확인 뒤 `git status`, `git diff --check`, `git diff -- settings.gradle.kts gradle/libs.versions.toml`을 읽어 임시 설정이 남지 않았음을 증명한다.

각 이전 대상 module은 아래 dependency selection 확인을 거친 뒤 targeted test를 실행한다.

```bash
./gradlew --include-build "$PROJECTS_WORKTREE" -p "$WORKSHOP_WORKTREE" \
  :<module>:dependencyInsight \
  --dependency io.github.bluetape4k:bluetape4k-io \
  --configuration compileClasspath
```

읽어야 할 결과는 요청된 module 좌표가 포함된 dependency edge와 선택 결과의 `project :bluetape4k-io` 경로다. 결과가 published `2.1.0` artifact를 고르면 composite substitution 증거가 아니므로 대상 테스트를 완료된 것으로 처리하지 않는다.

이번 작업은 publication을 자동으로 수행하지 않는다. 중앙에서 Projects `2.2.0-SNAPSHOT` artifact를 사용할 수 있기 전에는 외부 CI/기본 checkout에서 Workshop dependency resolution이 완료됐다고 주장하지 않는다. 이 publication 경계는 코드 구현과 별도 승인 사항으로 남긴다.

Workshop migration compile/test에서 기존 bytes 또는 wire 표현이 달라지면 해당 변경을 중단한다. 실패 지점을 입력/framing/output table에 기록하고 task가 추가한 Workshop migration 파일만 migration 직전 tree로 되돌린다. Projects API 변경은 보존한다. CLI composite-build 인자 외에 `/tmp`의 task 전용 설정 파일이 추가된 경우 이를 삭제한 뒤 Workshop branch status/diff와 catalog/settings 비변경을 다시 확인하고, 보정된 매핑으로 migration을 재개한다.

## 실패와 재시도 동작

- `JdkDigester`가 지원하지 않는 알고리즘을 받으면 JDK의 `NoSuchAlgorithmException`이 발생한다. 이를 다른 알고리즘으로 대체하거나 예외를 숨기지 않는다.
- Base64 decoder는 선택한 alphabet/padding 형식과 맞지 않거나 공백, non-canonical pad bits가 있는 입력을 `IllegalArgumentException`으로 거부한다. 신규 `matchesBase64`는 decoder의 해당 예외만 `false`로 바꾸고 다른 provider/runtime 오류는 전파한다. 기존 `matches(String, String)`은 레거시 decode 동작과 malformed 기대값 `false` 결과를 보존한다.
- 각 digest 호출은 별도 세션을 사용한다. 공유 `Digester` 인스턴스 상태를 재사용하지 않아 요청 간 byte 혼합이 발생하지 않는다.
- 같은 세션에서 `update → digest → update → digest` 및 `update → digestHex → update → digestHex`를 수행한 결과는 각 입력의 별도 one-shot 결과와 같아야 한다. `digest()`와 `digestHex()`는 각각 결과 반환 후 세션을 초기 상태로 되돌린다.
- Workshop의 기존 데이터 계약을 보존하지 못하는 테스트가 발견되면 해당 사용처 이전을 중단하고 framing/output 차이를 바로잡는다. 데이터를 재작성하거나 읽기 호환을 임의로 도입하지 않는다.

## 대안과 결정

| 대안 | 평가 | 결정 |
|---|---|---|
| 각 Workshop 모듈이 `MessageDigest`를 계속 직접 호출 | 중복이 남고 일관된 API가 만들어지지 않는다. | 기각 |
| `io/tink`에만 SHA-256 utility를 두고 이를 `io/io`에서 사용 | 의존 방향이 역전되고 `io`의 독립성을 약화한다. | 기각 |
| `io/io` 인터페이스와 별도의 실제 Tink SHA-256 backend 제공 | Google Tink MAC/PRF는 일반 unkeyed digest와 다른 primitive여서 의미가 일치하지 않는다. | 기각 |
| `io/io` 공통 인터페이스 + JDK 구현, `io/tink`의 기존 API는 호환 adapter | 독립적인 공통 계약을 제공하고 기존 호출을 보존하며 dependency cycle을 피한다. | 채택 |

## 검증 가능한 완료 조건

1. `io/io` public API가 raw bytes, UTF-8 convenience, 증분 update, lowercase hex, 네 Base64 형식, malformed 입력 정책을 테스트한다.
2. SHA-256 알려진 벡터 `abc`가 raw bytes, lowercase hex `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`, URL-safe padded Base64 `ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0=`와 일치한다.
3. 네 Base64 형식 각각의 alphabet/padding round-trip과 wrong alphabet, padding 불일치, 공백, malformed input, non-canonical pad bits 거부를 테스트한다. Hex 검증은 길이 오류, uppercase, 일치/불일치를 다룬다.
4. `matchesBase64`는 malformed decode의 `IllegalArgumentException`만 `false`로 바꾸며 다른 오류는 전파한다. raw/hex digest 비교는 `MessageDigest.isEqual`을 사용한다.
5. 여러 번 update한 결과가 one-shot digest와 일치하고, 한 세션에서 `digest()` 또는 `digestHex()` 후 update/reuse한 결과도 각각의 독립 one-shot 결과와 같으며 세션 간 결과가 섞이지 않는다.
6. 기존 `TinkDigesterTest` 결과와 public JVM signatures가 보존되며 신규 interface 경로도 같은 결과를 낸다. `getAlgorithmName()` descriptor, URL-safe padded golden output, 기존 `matches(String, String)`의 decoder 허용 동작과 malformed 입력의 `false` 결과를 확인한다.
7. Java consumer fixture가 `DigestUtils.SHA256`, `Base64Codec` static API, format 기본/명시 overload를 컴파일·실행하고 해당 JVM descriptor를 확인한다.
8. 공개 API 한국어 KDoc이 각 type/method에 있고 session reset과 secure erasure 비보장, strict `matchesBase64`와 legacy `TinkDigester.matches(String, String)`의 차이를 설명한다. `io/io/README.md`와 `README.ko.md`에 해당 언어의 사용 예가 있고 Workshop migration 예제는 raw bytes, UTF-8, 증분 update, 명시적 Base64 형식을 구분한다.
9. migration table의 unkeyed SHA-256 production 소비자 29개 파일·35개 호출이 공통 API를 사용한다. HMAC, test oracle, fixture 데이터, 기존 field order/framing/digest bytes/Base64/hex/truncation 결과는 변경되지 않는다.
10. `JdkDigester` KDoc은 MD5/SHA-1을 비보안 레거시 checksum 목적으로만 안내하고 일반 소비자는 `DigestUtils.SHA256` 사용을 권장한다. keyed MAC과 공개 검증이 필요한 signature의 선택 기준도 문서에 있다.
11. Workshop의 각 영향 module은 Projects worktree를 포함한 `--include-build` targeted compile/test와 `dependencyInsight`로 검증하고 선택된 component가 Projects `:bluetape4k-io`인지 확인한다. Settings/catalog에 임시 변경이 없고 rollback 경로가 남아 있다. 원격 artifact가 없어 기본 CI에서 검증할 수 없는 상태도 기록한다.
12. `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/DigestBenchmark.kt`를 추가하고 `@State(Scope.Thread)`, `@BenchmarkMode(Mode.Throughput)`, `@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)`, `@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)`를 적용한다. 이는 기존 `SameConditionCompressorBenchmark.kt`의 측정 설정을 따른다. `@Param("64", "1024", "65536")` 입력을 `@Setup`에서 준비한다. 다음 네 경로를 비교한다: 매 호출에서 `MessageDigest.getInstance("SHA-256")`를 만드는 직접 one-shot baseline, `DigestUtils.SHA256.digest(payload)` one-shot, 매 호출에서 새 세션을 만드는 분할 입력 incremental, 미리 만든 thread-local 단일 세션을 반복 재사용하는 `update → digest` 경로. 분할 입력을 합친 바이트가 one-shot payload와 같은지 `@Setup`에서 검증한다. one-shot과 세션별 경로는 매 호출 세션 생성 비용을 포함하고, 세션 재사용 경로만 세션 생성 비용을 측정 구간 밖으로 둔다. `./gradlew :bluetape4k-io:benchmark`로 실행하며 JVM·OS·CPU, 실행 명령, 각 경로의 throughput 결과를 `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/README.md`에 기록한다. 성능 향상 주장은 하지 않으며 이 측정은 반복 호출/증분 경로의 회귀 진단으로 취급한다.
13. Projects와 Workshop targeted tests, Java/JVM API 확인, `git diff --check`가 통과하고 두 저장소 diff에 의도하지 않은 파일이 없다.
14. PR, merge, tag, release, Projects `2.2.0-SNAPSHOT` artifact 게시를 수행하지 않는다.

## 근거 자료

- `bluetape4k-projects/io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigester.kt`
- `bluetape4k-projects/io/tink/src/main/kotlin/io/bluetape4k/tink/digest/TinkDigesters.kt`
- `bluetape4k-projects/io/tink/src/test/kotlin/io/bluetape4k/tink/digest/TinkDigesterTest.kt`
- `bluetape4k-projects/io/io/build.gradle.kts`, `bluetape4k-projects/io/tink/build.gradle.kts`
- `bluetape4k-projects/io/io/src/test/kotlin/io/bluetape4k/io/benchmark/SameConditionCompressorBenchmark.kt`, `bluetape4k-projects/io/io/src/test/kotlin/io/bluetape4k/io/benchmark/README.md`
- Projects issue [#1649](https://github.com/bluetape4k/bluetape4k-projects/issues/1649) 및 PR [#1678](https://github.com/bluetape4k/bluetape4k-projects/pull/1678)
- Workshop issue [#989](https://github.com/bluetape4k/bluetape4k-workshop/issues/989) 및 PR [#995](https://github.com/bluetape4k/bluetape4k-workshop/pull/995)
- [Google Tink MAC 문서](https://developers.google.com/tink/mac), 2026-11-23 확인
