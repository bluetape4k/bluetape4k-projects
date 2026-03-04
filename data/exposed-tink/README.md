# Module bluetape4k-exposed-tink

Exposed 컬럼 암복호화를 [Google Tink](https://developers.google.com/tink)로 처리하기 위한 모듈입니다.

## 개요

`bluetape4k-exposed-tink`는 JetBrains Exposed의 컬럼 값을 Google Tink 라이브러리를 통해
인증 암호화(AEAD, Authenticated Encryption with Associated Data)로 저장하는 기능을 제공합니다.

Google Tink는 Google에서 개발한 현대적인 암호화 라이브러리로, 오용하기 어렵고 잘못된 사용을
방지하는 설계 철학을 가지고 있습니다. 이 모듈은 두 가지 암호화 방식을 지원합니다:

- **AEAD** (비결정적): 동일 평문이라도 매번 다른 암호문 생성 → 높은 보안
- **Deterministic AEAD** (결정적): 동일 평문 → 항상 동일 암호문 → 인덱스/검색 가능

## Jasypt vs Google Tink 비교

| 비교 항목              | `exposed-jasypt`            | `exposed-tink` (AEAD)    | `exposed-tink` (DAEAD)     |
|--------------------|-----------------------------|--------------------------|---------------------------|
| **암호화 알고리즘**       | AES/RC4/3DES (구형)           | AES-GCM, ChaCha20-Poly1305 (현대) | AES-256-SIV (현대)          |
| **결정적 암호화**        | ✅ (항상 동일한 암호문)             | ❌ (매번 다른 암호문)          | ✅ (항상 동일한 암호문)           |
| **인증(Tamper 감지)**  | ❌                            | ✅ AEAD                  | ✅ AEAD                    |
| **WHERE 조건 검색**    | ✅                            | ❌                        | ✅                          |
| **인덱스 생성**         | ✅                            | ❌                        | ✅                          |
| **패턴 분석 위험**       | ⚠️ 있음                        | ✅ 없음                    | ⚠️ 있음 (결정적이므로)           |
| **표준 준수**          | ⚠️ 구형 방식                     | ✅ NIST/IETF 표준           | ✅ NIST/IETF 표준             |
| **Google 권장**      | ❌                            | ✅                        | ✅                          |

### Google Tink를 선택해야 하는 이유

1. **인증(Authentication) 내장**: AEAD는 암호화와 함께 데이터 무결성을 보장합니다.
   DB에 저장된 암호문이 조작되면 복호화 시 즉시 감지됩니다. Jasypt는 이 기능이 없습니다.

2. **현대적인 알고리즘**: AES-256-GCM, ChaCha20-Poly1305, AES-256-SIV 등
   NIST/IETF에서 권장하는 최신 알고리즘을 사용합니다.

3. **오용 방지 설계**: 취약한 알고리즘 선택을 원천 차단하는 API 설계로,
   보안 전문가가 아니어도 안전하게 사용할 수 있습니다.

4. **두 가지 모드 지원**: 상황에 맞게 AEAD(보안 중심)와 DAEAD(검색 가능) 중 선택할 수 있습니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-tink:${version}")
}
```

## 기본 사용법

### 1. 컬럼 정의

```kotlin
import io.bluetape4k.exposed.core.tink.*
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Users: IntIdTable("users") {
    val name = varchar("name", 100)

    // ① 비결정적 AEAD — 검색 불필요한 민감 정보 (비밀번호 힌트, 개인 메모 등)
    val memo = tinkAeadVarChar("memo", 512).nullable()

    // ② 결정적 DAEAD — 검색이 필요한 식별 정보 (이메일, 주민번호 등)
    val email = tinkDaeadVarChar("email", 512).index()

    // ③ 바이너리 AEAD — 바이너리 민감 데이터 (공개키, 인증서 등)
    val publicKey = tinkAeadBinary("public_key", 1024).nullable()

    // ④ 바이너리 DAEAD — 검색 가능한 바이너리 (지문, 해시값 등)
    val fingerprint = tinkDaeadBinary("fingerprint", 128).nullable()
}
```

### 2. 삽입 — 자동 암호화

```kotlin
transaction {
    val id = Users.insertAndGetId {
        it[name] = "홍길동"
        it[memo] = "VIP 고객"        // 자동으로 AEAD 암호화
        it[email] = "hong@example.com" // 자동으로 DAEAD 암호화
        it[publicKey] = rsaPublicKey.encoded
        it[fingerprint] = sha256(biometricData)
    }
}
```

### 3. 조회 — 자동 복호화

```kotlin
transaction {
    val user = Users.selectAll().where { Users.id eq 1 }.single()

    val name = user[Users.name]   // "홍길동"
    val memo = user[Users.memo]   // "VIP 고객" (자동 복호화)
    val email = user[Users.email] // "hong@example.com" (자동 복호화)
}
```

### 4. DAEAD 컬럼 조건 검색

```kotlin
// DAEAD는 결정적이므로 WHERE 조건 및 인덱스 사용 가능
transaction {
    val user = Users.selectAll()
        .where { Users.email eq "hong@example.com" }
        .singleOrNull()
}
```

> **⚠️ 주의**: AEAD(`tinkAeadVarChar`, `tinkAeadBinary`) 컬럼은 비결정적이므로
> `WHERE col = value` 형태의 검색이 동작하지 않습니다.
> 검색이 필요한 컬럼에는 반드시 `tinkDaead*` 변형을 사용하세요.

## 알고리즘 선택 가이드

### AEAD 알고리즘 (`tinkAeadVarChar`, `tinkAeadBinary`)

```kotlin
import io.bluetape4k.tink.aead.TinkAeads

// AES-256-GCM (기본값) — 범용 권장, 하드웨어 가속 지원
val col1 = tinkAeadVarChar("col1", 512, TinkAeads.AES256_GCM)

// AES-128-GCM — 성능이 중요한 경우
val col2 = tinkAeadVarChar("col2", 512, TinkAeads.AES128_GCM)

// ChaCha20-Poly1305 — 하드웨어 AES 가속이 없는 환경 (모바일, 임베디드)
val col3 = tinkAeadVarChar("col3", 512, TinkAeads.CHACHA20_POLY1305)

// XChaCha20-Poly1305 — 더 큰 nonce(192bit)로 nonce 재사용 위험 최소화
val col4 = tinkAeadVarChar("col4", 512, TinkAeads.XCHACHA20_POLY1305)
```

### Deterministic AEAD 알고리즘 (`tinkDaeadVarChar`, `tinkDaeadBinary`)

```kotlin
import io.bluetape4k.tink.daead.TinkDaeads

// AES-256-SIV (유일한 옵션, 결정적 AEAD 표준)
val col5 = tinkDaeadVarChar("col5", 512, TinkDaeads.AES256_SIV)
```

| 알고리즘 | 용도 | 특징 |
|---------|------|------|
| AES-256-GCM | **기본 권장** | 빠름, 하드웨어 가속, NIST 표준 |
| AES-128-GCM | 성능 중시 | AES-256보다 빠르지만 키 길이 짧음 |
| ChaCha20-Poly1305 | 모바일/임베디드 | HW 가속 없어도 빠름 |
| XChaCha20-Poly1305 | 고보안 | 더 큰 nonce, nonce 충돌 위험 ↓ |
| AES-256-SIV | 검색 가능 암호화 | 결정적, 인증 포함, 검색 가능 |

## 컬럼 길이 안내

암호화 후 원본보다 데이터가 커지므로 충분한 길이를 설정해야 합니다.

| 알고리즘 | 오버헤드 | 권장 배수 |
|---------|---------|---------|
| AES-GCM | +28 bytes (12 IV + 16 Tag) + Base64 인코딩 | 원본의 약 1.5~2배 |
| ChaCha20-Poly1305 | +28 bytes + Base64 인코딩 | 원본의 약 1.5~2배 |
| AES-256-SIV | +16 bytes (Tag) + Base64 인코딩 | 원본의 약 1.5~2배 |

```kotlin
// 예: 이메일 최대 254자 → Base64(254+28) ≈ 376자 → 여유있게 512 권장
val email = tinkDaeadVarChar("email", 512).index()

// 예: 주민번호 14자 → Base64(14+28) ≈ 56자 → 128 충분
val ssn = tinkDaeadVarChar("ssn", 128)
```

## 실전 사용 예시

### 개인정보 보호가 필요한 사용자 테이블

```kotlin
object UserPrivacy: IntIdTable("user_privacy") {
    // 일반 컬럼
    val username = varchar("username", 50).uniqueIndex()
    val createdAt = datetime("created_at")

    // DAEAD — 검색/인덱스 필요 (로그인, 중복 체크 등)
    val email = tinkDaeadVarChar("email", 512).uniqueIndex()
    val phoneNumber = tinkDaeadVarChar("phone_number", 256).nullable()

    // AEAD — 검색 불필요한 민감 정보
    val ssn = tinkAeadVarChar("ssn", 256).nullable()
    val address = tinkAeadVarChar("address", 1024).nullable()
    val profileNote = tinkAeadVarChar("profile_note", 2048).nullable()

    // AEAD Binary — 바이너리 민감 데이터
    val profileImage = tinkAeadBinary("profile_image", 65536).nullable()
}
```

### 커스텀 키 사용 (운영 환경)

```kotlin
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.aead.TinkAead
import com.google.crypto.tink.aead.AesGcmKeyManager

// 별도 키로 인스턴스 생성 (키를 KMS 등 외부 저장소에서 로드 가능)
val customEncryptor = TinkAead(aeadKeysetHandle(AesGcmKeyManager.aes256GcmTemplate()))

object SensitiveData: IntIdTable("sensitive_data") {
    val secret = tinkAeadVarChar("secret", 512, customEncryptor)
}
```

## 주요 파일/클래스 목록

| 파일 | 설명 |
|------|------|
| `TinkAeadVarCharColumnType.kt` | AEAD VARCHAR 암호화 컬럼 타입 |
| `TinkAeadBinaryColumnType.kt` | AEAD VARBINARY 암호화 컬럼 타입 |
| `TinkDaeadVarCharColumnType.kt` | Deterministic AEAD VARCHAR 암호화 컬럼 타입 |
| `TinkDaeadBinaryColumnType.kt` | Deterministic AEAD VARBINARY 암호화 컬럼 타입 |
| `Tables.kt` | 테이블 확장 함수 (`tinkAeadVarChar` 등) |

## 주의사항

1. **AEAD는 검색 불가**: `tinkAeadVarChar`/`tinkAeadBinary`는 매번 다른 암호문을 생성하므로
   `WHERE col = value` 조건 검색이 동작하지 않습니다. 검색이 필요하면 `tinkDaead*`를 사용하세요.

2. **컬럼 길이**: 암호화 후 데이터가 늘어나므로 원본 최대 길이의 약 2배 이상으로 설정하세요.

3. **키 관리**: 암호화 키를 잃어버리면 데이터를 복호화할 수 없습니다.
   운영 환경에서는 Google Cloud KMS, AWS KMS 등 외부 KMS와 연동해 키를 안전하게 관리하세요.

4. **키 교체**: Tink는 키 교체(Key Rotation)를 지원합니다. 정기적인 키 교체로 보안을 강화할 수 있습니다.

5. **DAEAD의 패턴 노출**: Deterministic AEAD도 동일 평문 → 동일 암호문이므로, 값의 분포/패턴이
   노출될 수 있습니다. 유일값(이메일, 주민번호)에는 적합하지만 자주 반복되는 값에는 주의하세요.

## 테스트

```bash
./gradlew :bluetape4k-exposed-tink:test
```

## 참고

- [Google Tink 공식 문서](https://developers.google.com/tink)
- [Google Tink GitHub](https://github.com/google/tink)
- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [bluetape4k-tink](../../io/tink/README.md) — Tink 기반 암호화 유틸리티 모듈
- [bluetape4k-exposed-jasypt](../exposed-jasypt/README.md) — Jasypt 기반 암호화 컬럼 모듈 (구형)
