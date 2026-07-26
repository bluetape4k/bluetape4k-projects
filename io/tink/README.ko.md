# bluetape4k-tink

[English](./README.md) | 한국어

Google [Tink](https://github.com/google/tink) 암호화 라이브러리를 Kotlin 관용적으로 래핑한 모듈입니다.

기존 `bluetape4k-crypto`(Jasypt 기반 PBE)와 독립적으로 동작하며, 현대적 인증 암호화 (AEAD) 알고리즘을 안전한 API로 제공합니다.

## Tink를 쓰는 이유

Google Tink는 cryptographic key와 그 key를 올바르게 사용하기 위한 algorithm parameter를 함께 다룹니다. raw JCA/JCE API를 직접 사용할 때 실수하기 쉬운 nonce 처리, ciphertext framing, primitive 선택 같은 세부 결정을 안전한 기본값으로 감춥니다.

`bluetape4k-tink`는 그 primitive 위에 Kotlin 친화적인 wrapper를 제공합니다:

- **안전한 기본값**: 범용 암호화에는 AEAD를 기본으로 사용하고, 검색 가능한 필드에 필요한 AES-256-SIV는 deterministic API로 분리합니다.
- **Associated Data 우선
  설계**: tenant, user, table, column, message context와 ciphertext를 묶을 수 있도록 associated data를 명시적으로 받습니다.
- **String/byte API 동시 제공**: `String` 메서드는 UTF-8 평문과 표준 Base64 암호문을 사용하고,
  `ByteArray` 메서드는 Tink의 raw output을 그대로 다룹니다.
- **Key rotation
  지원**: versioned keyset wrapper는 ciphertext 앞에 keyset version을 붙여 rotation 이후에도 이전 ciphertext를 복호화할 수 있게 합니다.
- **Redis 저장소 제공**: Redis를 선택한 인프라에서 사용할 수 있도록 Lettuce/Redisson 기반
  `VersionedKeysetStore` 구현체를 제공합니다.
- **테스트하기 쉬운 wrapper**: round-trip, associated-data, tamper, rotation, concurrency 테스트로 검증하기 쉬운 작은 API surface를 유지합니다.

## 다이어그램

### TinkEncryptor 클래스 계층

![TinkEncryptor 클래스 계층 다이어그램](../../docs/images/readme-diagrams/io-tink-diagram-01.png)

### AEAD encrypt/decrypt 흐름

![AEAD encrypt/decrypt 흐름 시퀀스 다이어그램](../../docs/images/readme-diagrams/io-tink-sequence-01.png)

## 추천 사용 시나리오

다음 경우 이 모듈을 사용하세요:

- **Context binding이 필요한 애플리케이션 데이터
  암호화**: `TinkAeads.AES256_GCM`을 사용하고 tenant ID, entity ID, column name 같은 associated data를 전달합니다.
- **검색 가능하면서 durable한 DB 컬럼 암호화**: persisted AES-SIV keyset store와
  `TinkDaeads.versioned(store)`를 사용합니다. 같은 평문은 같은 암호문이 된다는 점을 수용해야 합니다.
- **Key rotation 이후에도 기존 ciphertext 복호화가 필요할 때**: `TinkAeads.versioned(store)` 또는
  `TinkDaeads.versioned(store)`를 사용해 ciphertext에 keyset version을 포함합니다.
- **여러 서비스가 active keyset을 공유해야 할 때**: Redis 기반 `VersionedKeysetStore`를 사용하되 Redis를 secret store처럼 보호합니다.
- **암호화 없이 무결성 검증만 필요할 때**: `TinkMacs.HMAC_SHA256` 또는
  `TinkMacs.HMAC_SHA512_512BITTAG`를 사용합니다.
- **비밀이 아닌 checksum/digest가 필요할 때**: `TinkDigesters.SHA256` 이상 SHA-2 계열을 사용합니다. password 저장에는 plain digest를 사용하지 마세요.

권장 기본값:

- 결정적 조회가 반드시 필요한 경우가 아니라면 Deterministic AEAD보다 AEAD를 우선 사용합니다.
- ciphertext를 특정 context에 묶어야 한다면 안정적인 associated data를 항상 전달합니다.
- keyset은 보호된 secret storage에만 저장합니다. Redis keyset store에는 key material이 들어 있으므로 Redis 자체를 민감 인프라로 취급해야 합니다.
- 애플리케이션 임의 지점에서 새 singleton wrapper를 만들기보다 `rotateIfDue` 정책으로 rotation을 수행합니다.
- `TinkAeads`, `TinkDaeads`, `TinkMacs` singleton은 재시작 후 복호화가 필요 없는 ephemeral/test 용도에만 사용합니다.

## Anti-Patterns

다음 패턴은 피하세요:

- **cleartext keyset JSON을 일반 설정 파일, 로그, 보호되지 않은 Redis에 저장하지 마세요.**
  `toJsonKeyset()`은 Tink secret-key access를 사용하며 secret key material을 생성합니다.
- **durable ciphertext에 singleton ephemeral key를 사용하지
  마세요.** keyset을 저장/복원하지 않으면 새 JVM process는 이전 process가 만든 ciphertext를 복호화할 수 없습니다.
- **일반 암호화에 Deterministic AEAD를 사용하지 마세요.** 반복 평문의 equality가 노출됩니다. 통제된 검색 필드에만 사용하세요.
- **context가 중요한데 associated data를 생략하지 마세요.** associated data가 없으면 같은 key를 공유하는 다른 context로 ciphertext가 옮겨질 수 있습니다.
- **암호화와 복호화의 associated data를 다르게 쓰지 마세요.** Tink는 associated data를 인증하므로 값이 다르면 복호화가 실패합니다.
- **MAC을 암호화로 착각하지 마세요.** MAC은 무결성/인증을 검증하지만 데이터를 숨기지 않습니다.
- **보안 판단에 MD5나 SHA-1을 사용하지 마세요.** legacy digest format과의 호환을 위해서만 제공합니다.
- **공유 store lock 없이 key를 동시에 rotate하지
  마세요.** 제공되는 Redis store 또는 rotation을 직렬화하고 lock 내부에서 due 상태를 다시 확인하는 `VersionedKeysetStore` 구현체를 사용하세요.

## 특징

- **AEAD (인증 암호화)** — AES-256-GCM, AES-128-GCM, ChaCha20-Poly1305, XChaCha20-Poly1305
- **Deterministic AEAD** — AES-256-SIV (검색 가능한 암호화, DB 인덱스 필드 등)
- **MAC (메시지 인증 코드)** — HMAC-SHA256, HMAC-SHA512
- **Digest (해시)** — MD5, SHA-1, SHA-256, SHA-384, SHA-512 (JDK `MessageDigest` 기반, BouncyCastle 불필요)
- **Encrypt (통합 암호화 인터페이스)** — `TinkEncryptor`로 AEAD/DAEAD를 통합 사용
- Kotlin Extension 함수로 간결한 사용
- Thread-safe 1회 초기화 (`registerTink()`)
- `ByteArray` / `String` 입출력 모두 지원 (String 암호문은 Base64 인코딩)

## 의존성

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-tink:$bluetape4kVersion")
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

### Digest — 해시 다이제스트

BouncyCastle 없이 JDK `MessageDigest`만으로 해시 알고리즘을 사용합니다.
`bluetape4k-crypto`의 `Digesters`를 대체합니다.

```kotlin
import io.bluetape4k.tink.digest.TinkDigesters
import io.bluetape4k.tink.digest.tinkDigest
import io.bluetape4k.tink.digest.matchesTinkDigest

// 싱글턴 인스턴스 사용
val hash = TinkDigesters.SHA256.digest("Hello, World!")
val matches = TinkDigesters.SHA256.matches("Hello, World!", hash) // true

// 확장 함수
val hash2 = "Hello, World!".tinkDigest(TinkDigesters.SHA256)
"Hello, World!".matchesTinkDigest(hash2, TinkDigesters.SHA256) // true

// 사용 가능 알고리즘: MD5, SHA1, SHA256, SHA384, SHA512
```

### Encrypt — 통합 암호화 인터페이스

`TinkEncryptor` 인터페이스로 AEAD (비결정적)와 DAEAD (결정적) 암호화를 통합합니다.
`bluetape4k-crypto`의 `Encryptors`를 대체합니다.

```kotlin
import io.bluetape4k.tink.encrypt.TinkEncryptors
import io.bluetape4k.tink.encrypt.tinkEncrypt
import io.bluetape4k.tink.encrypt.tinkDecrypt

// 비결정적 암호화 (범용)
val encrypted = TinkEncryptors.AES256_GCM.encrypt("비밀 메시지")
val decrypted = TinkEncryptors.AES256_GCM.decrypt(encrypted)

// 현재 process 안의 결정적 암호화. durable DB 검색에는 singleton key를 사용하지 마세요.
val ct = TinkEncryptors.DETERMINISTIC_AES256_SIV.encrypt("검색 가능한 필드")
val ct2 = TinkEncryptors.DETERMINISTIC_AES256_SIV.encrypt("검색 가능한 필드")
// ct == ct2 (결정적)

// 확장 함수
val enc = "Hello".tinkEncrypt(TinkEncryptors.CHACHA20_POLY1305)
val dec = enc.tinkDecrypt(TinkEncryptors.CHACHA20_POLY1305)
```

## 알고리즘 선택 가이드

| 사용 목적                   | 권장 알고리즘              | 클래스                                                                  |
|-----------------------------|----------------------------|-------------------------------------------------------------------------|
| 범용 암호화                 | AES-256-GCM                | `TinkAeads.AES256_GCM` / `TinkEncryptors.AES256_GCM`                    |
| 하드웨어 AES 없는 환경      | XChaCha20-Poly1305         | `TinkAeads.XCHACHA20_POLY1305` / `TinkEncryptors.XCHACHA20_POLY1305`    |
| durable DB 컬럼 검색 암호화 | AES-256-SIV                | persisted AES-SIV keyset store를 사용하는 `TinkDaeads.versioned(store)` |
| 데이터 무결성 검증          | HMAC-SHA256                | `TinkMacs.HMAC_SHA256`                                                  |
| 고보안 무결성 검증          | HMAC-SHA512 (512비트 태그) | `TinkMacs.HMAC_SHA512_512BITTAG`                                        |
| 범용 해시                   | SHA-256                    | `TinkDigesters.SHA256`                                                  |
| 최고 수준 해시              | SHA-512                    | `TinkDigesters.SHA512`                                                  |

## 주의 사항

### AEAD vs Deterministic AEAD

- **AEAD** (`TinkAeads`): 매 암호화마다 랜덤 nonce 사용 → 동일 평문도 매번 다른 암호문 생성. **일반 데이터 보호에 권장.**
- **Deterministic AEAD** (`TinkDaeads`): 동일 평문 → 동일 암호문. 패턴 유출 가능성이 있으므로 **검색이 필요한 DB 필드에만 사용.**

### 키 관리

`TinkAeads`, `TinkDaeads`, `TinkMacs`의 싱글턴 인스턴스는 **애플리케이션 수명 동안 메모리에 보관되는 임시 키
**를 사용합니다. 재시작 후에도 복호화가 필요한 경우 키를 안전하게 직렬화하여 보관해야 합니다. 직렬화한 cleartext keyset에는 secret key material이 포함되므로 process 밖에 기록하기 전에 KMS/HSM envelope encryption 또는 그에 준하는 secret-storage 통제로 보호하세요.

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

`encrypt(String)` 반환값은 **Base64 (표준)** 인코딩된 암호문입니다.
`decrypt(String)` 입력도 동일한 Base64 형식이어야 합니다.

### Redis 기반 키 로테이션

`bluetape4k-tink`는 versioned keyset 추상화와 Redis 기반 저장소를 제공합니다. 저장되는 keyset JSON에는 key material이 포함되므로 Redis는 보호된 secret storage처럼 운영해야 합니다. 접근 제어, 필요한 경우 TLS, 암호화된 백업, 제한된 진단 로그, retention 정책을 key-management 요구 수준에 맞추세요.

```kotlin
import com.google.crypto.tink.aead.AesGcmKeyManager
import io.bluetape4k.tink.keyset.redis.LettuceVersionedKeysetStore

val store = LettuceVersionedKeysetStore(connection, "user-email", AesGcmKeyManager.aes256GcmTemplate())
val aead = TinkAeads.versioned(store)

val encrypted = aead.encrypt("hello")
store.rotate()

// rotation 이전 암호문도 version prefix를 이용해 계속 복호화 가능
val decrypted = aead.decrypt(encrypted)
```

## 모듈 구조

```
io.bluetape4k.tink
├── TinkSupport.kt                          # 초기화, 헬퍼 함수, 상수
├── keyset/                                 # 버전 관리 keyset/rotation 지원
│   ├── VersionedKeysetHandle.kt            # version + createdAt + KeysetHandle
│   ├── VersionedKeysetStore.kt             # 저장소 추상화
│   ├── TinkKeysetJsonSupport.kt            # KeysetHandle JSON 직렬화/복원
│   ├── VersionedTinkAead.kt                # version prefix 기반 AEAD 래퍼
│   └── VersionedTinkDaead.kt               # version prefix 기반 DAEAD 래퍼
├── aead/                                   # AEAD (인증 암호화)
│   ├── TinkAead.kt                         # AEAD 래퍼 클래스
│   ├── TinkAeads.kt                        # 팩토리 싱글턴
│   └── TinkAeadExtensions.kt              # 확장 함수
├── daead/                                  # Deterministic AEAD (결정적 암호화)
│   ├── TinkDeterministicAead.kt            # DAEAD 래퍼 클래스
│   └── TinkDaeads.kt                       # 팩토리 싱글턴
├── mac/                                    # MAC (메시지 인증 코드)
│   ├── TinkMac.kt                          # MAC 래퍼 클래스
│   ├── TinkMacs.kt                         # 팩토리 싱글턴
│   └── TinkMacExtensions.kt               # 확장 함수
├── digest/                                 # Digest (해시) — NEW
│   ├── TinkDigester.kt                     # JDK MessageDigest 래퍼 클래스
│   ├── TinkDigesters.kt                    # 팩토리 싱글턴 (MD5, SHA1, SHA256, SHA384, SHA512)
│   └── TinkDigesterExtensions.kt           # 확장 함수
└── encrypt/                                # Encrypt (통합 인터페이스) — NEW
    ├── TinkEncryptor.kt                    # 통합 암복호화 인터페이스
    ├── TinkAeadEncryptor.kt                # AEAD 기반 비결정적 구현체
    ├── TinkDaeadEncryptor.kt               # DAEAD 기반 결정적 구현체
    ├── TinkEncryptors.kt                   # 팩토리 싱글턴
    └── TinkEncryptorExtensions.kt          # 확장 함수
```

## bluetape4k-crypto 와의 차이

> **`bluetape4k-crypto`는 @Deprecated 되었습니다.** 신규 개발에서는 `bluetape4k-tink`를 사용하세요.

| 항목            | `bluetape4k-crypto` (Deprecated) | `bluetape4k-tink`                    |
|-----------------|----------------------------------|--------------------------------------|
| 기반 라이브러리 | Jasypt + BouncyCastle            | Google Tink + JDK                    |
| 암호화 방식     | PBE (Password-Based)             | AEAD (인증 암호화)                   |
| 인증            | 없음 (AES-CBC)                   | 내장 (GCM/Poly1305/SIV)              |
| 결정적 암호화   | 불가                             | AES-SIV로 지원                       |
| MAC             | 별도                             | HMAC-SHA256/512 내장                 |
| 해시            | BouncyCastle 필요                | JDK MessageDigest (추가 의존성 없음) |
| 통합 인터페이스 | 없음                             | `TinkEncryptor` (AEAD/DAEAD 통합)    |
| 의존성          | Jasypt + BouncyCastle            | Google Tink만                        |

### 마이그레이션 가이드

| `bluetape4k-crypto`                    | `bluetape4k-tink`                                |
|----------------------------------------|--------------------------------------------------|
| `Digesters.SHA256.digest(data)`        | `TinkDigesters.SHA256.digest(data)`              |
| `Digesters.SHA256.matches(data, hash)` | `TinkDigesters.SHA256.matches(data, hash)`       |
| `"hello".digest(Digesters.SHA256)`     | `"hello".tinkDigest(TinkDigesters.SHA256)`       |
| `Encryptors.AES.encrypt(data)`         | `TinkEncryptors.AES256_GCM.encrypt(data)`        |
| `Encryptors.AES.decrypt(data)`         | `TinkEncryptors.AES256_GCM.decrypt(data)`        |
| `"hello".encrypt(Encryptors.AES)`      | `"hello".tinkEncrypt(TinkEncryptors.AES256_GCM)` |
| `Encryptors.DeterministicAES`          | `TinkEncryptors.DETERMINISTIC_AES256_SIV`        |
