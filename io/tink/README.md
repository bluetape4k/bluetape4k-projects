# bluetape4k-tink

Google [Tink](https://github.com/google/tink) 암호화 라이브러리를 Kotlin 관용적으로 래핑한 모듈입니다.

기존 `bluetape4k-crypto`(Jasypt 기반 PBE)와 독립적으로 동작하며, 현대적 인증 암호화(AEAD) 알고리즘을 안전한 API로 제공합니다.

## 특징

- **AEAD (인증 암호화)** — AES-256-GCM, AES-128-GCM, ChaCha20-Poly1305, XChaCha20-Poly1305
- **Deterministic AEAD** — AES-256-SIV (검색 가능한 암호화, DB 인덱스 필드 등)
- **MAC (메시지 인증 코드)** — HMAC-SHA256, HMAC-SHA512
- Kotlin Extension 함수로 간결한 사용
- Thread-safe 1회 초기화 (`registerTink()`)
- `ByteArray` / `String` 입출력 모두 지원 (String 암호문은 Base64 인코딩)

## 의존성

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.bluetape4k:bluetape4k-tink:$bluetape4kVersion")
}
```

## 빠른 시작

### AEAD — 인증 암호화 (AES-256-GCM)

```kotlin
import io.bluetape4k.tink.aead.TinkAeads

// 싱글턴 인스턴스 사용
val encrypted: String = TinkAeads.AES256_GCM.encrypt("안녕하세요, Tink!")
val decrypted: String = TinkAeads.AES256_GCM.decrypt(encrypted)
// decrypted == "안녕하세요, Tink!"

// 연관 데이터(Associated Data)로 컨텍스트 바인딩
val ad = "user-id=42".toByteArray()
val encryptedWithAd = TinkAeads.AES256_GCM.encrypt("비밀 데이터", ad)
val decryptedWithAd = TinkAeads.AES256_GCM.decrypt(encryptedWithAd, ad)

// 잘못된 AD로 복호화 시 GeneralSecurityException 발생
```

### AEAD — 확장 함수

```kotlin
import io.bluetape4k.tink.aead.TinkAeads
import io.bluetape4k.tink.aead.tinkEncrypt
import io.bluetape4k.tink.aead.tinkDecrypt

val aead = TinkAeads.AES256_GCM

// String 확장 함수
val encrypted = "민감한 정보".tinkEncrypt(aead)
val original = encrypted.tinkDecrypt(aead)

// ByteArray 확장 함수
val data = "Hello".toByteArray()
val cipherBytes = data.tinkEncrypt(aead)
val plainBytes = cipherBytes.tinkDecrypt(aead)
```

### AEAD — 커스텀 키 생성

```kotlin
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.aead.TinkAead
import com.google.crypto.tink.aead.AesGcmKeyManager

// 새 키를 생성하여 인스턴스 생성
val myAead = TinkAead(aeadKeysetHandle(AesGcmKeyManager.aes256GcmTemplate()))

// ChaCha20-Poly1305 사용 (하드웨어 AES 가속 없는 환경에 유리)
val chacha = TinkAeads.CHACHA20_POLY1305
val xchacha = TinkAeads.XCHACHA20_POLY1305
```

### Deterministic AEAD — 결정적 암호화 (AES-256-SIV)

동일한 평문 + 동일한 키 → 항상 동일한 암호문. DB 컬럼 암호화 + 인덱스 검색에 활용.

```kotlin
import io.bluetape4k.tink.daead.TinkDaeads

val daead = TinkDaeads.AES256_SIV

// 암호화
val ct1 = daead.encryptDeterministically("hong@example.com")
val ct2 = daead.encryptDeterministically("hong@example.com")
// ct1 == ct2 (결정적 특성)

// 복호화
val email = daead.decryptDeterministically(ct1)
// email == "hong@example.com"

// DB WHERE 절 조건 비교 예시
val searchCt = daead.encryptDeterministically(inputEmail)
// SELECT * FROM users WHERE encrypted_email = :searchCt
```

### MAC — 메시지 인증 코드

```kotlin
import io.bluetape4k.tink.mac.TinkMacs
import io.bluetape4k.tink.mac.computeTinkMac
import io.bluetape4k.tink.mac.verifyTinkMac

val mac = TinkMacs.HMAC_SHA256

// 태그 계산
val tag: ByteArray = mac.computeMac("중요한 데이터")

// 검증
val isValid: Boolean = mac.verifyMac(tag, "중요한 데이터")  // true
val isTampered: Boolean = mac.verifyMac(tag, "변조된 데이터") // false

// 확장 함수
val tag2 = "중요한 데이터".computeTinkMac(mac)
val ok = "중요한 데이터".verifyTinkMac(tag2, mac)  // true
```

## 알고리즘 선택 가이드

| 사용 목적           | 권장 알고리즘                | 클래스                              |
|-----------------|------------------------|----------------------------------|
| 범용 암호화          | AES-256-GCM            | `TinkAeads.AES256_GCM`           |
| 하드웨어 AES 없는 환경  | XChaCha20-Poly1305     | `TinkAeads.XCHACHA20_POLY1305`   |
| DB 컬럼 검색 가능 암호화 | AES-256-SIV            | `TinkDaeads.AES256_SIV`          |
| 데이터 무결성 검증      | HMAC-SHA256            | `TinkMacs.HMAC_SHA256`           |
| 고보안 무결성 검증      | HMAC-SHA512 (512비트 태그) | `TinkMacs.HMAC_SHA512_512BITTAG` |

## 주의 사항

### AEAD vs Deterministic AEAD

- **AEAD** (`TinkAeads`): 매 암호화마다 랜덤 nonce 사용 → 동일 평문도 매번 다른 암호문 생성. **일반 데이터 보호에 권장.**
- **Deterministic AEAD** (`TinkDaeads`): 동일 평문 → 동일 암호문. 패턴 유출 가능성이 있으므로 **검색이 필요한 DB 필드에만 사용.**

### 키 관리

`TinkAeads`, `TinkDaeads`, `TinkMacs`의 싱글턴 인스턴스는 **애플리케이션 수명 동안 메모리에 보관되는 임시 키
**를 사용합니다. 재시작 후에도 복호화가 필요한 경우 키를 안전하게 직렬화하여 보관해야 합니다.

```kotlin
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetWriter
import io.bluetape4k.tink.aeadKeysetHandle
import java.io.ByteArrayOutputStream

// 키 직렬화 (실제 운영에서는 KMS로 암호화하여 보관)
val keysetHandle = aeadKeysetHandle()
val outputStream = ByteArrayOutputStream()
CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(outputStream))
val keysetJson = outputStream.toString()
```

### String 암호문 형식

`encrypt(String)` 반환값은 **Base64(표준)** 인코딩된 암호문입니다.
`decrypt(String)` 입력도 동일한 Base64 형식이어야 합니다.

## bluetape4k-crypto 와의 차이

| 항목       | `bluetape4k-crypto`   | `bluetape4k-tink`     |
|----------|-----------------------|-----------------------|
| 기반 라이브러리 | Jasypt + BouncyCastle | Google Tink           |
| 암호화 방식   | PBE (Password-Based)  | AEAD (인증 암호화)         |
| 인증       | 없음 (AES-CBC)          | 내장 (GCM/Poly1305/SIV) |
| 결정적 암호화  | 불가                    | AES-SIV로 지원           |
| MAC      | 별도                    | HMAC-SHA256/512 내장    |
| 의존성      | 상호 독립                 | 상호 독립                 |
