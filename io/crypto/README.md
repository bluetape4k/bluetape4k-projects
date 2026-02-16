# Module bluetape4k-crypto

## 개요

`bluetape4k-crypto`는 [Jasypt](http://www.jasypt.org/) 와 [BouncyCastle](https://www.bouncycastle.org/) 라이브러리를 Kotlin 환경에서 편리하게 사용할 수 있도록 래핑한 암호화 모듈입니다.

해시 다이제스트(Digest), 대칭형 암호화(Encryption), JCA Cipher 빌더를 제공하며, 멀티스레드 환경에서 안전하게 동작합니다.

## 주요 기능

### 1. 해시 다이제스트 (Digest)

SHA-256, SHA-512, MD5, Keccak 등 다양한 해시 알고리즘을 지원합니다.

```kotlin
import io.bluetape4k.crypto.digest.Digesters

// SHA-256 다이제스트
val digest = Digesters.SHA256.digest("Hello, World!")
val matches = Digesters.SHA256.matches("Hello, World!", digest)  // true

// Keccak-256 다이제스트 (블록체인에서 널리 사용)
val keccakDigest = Digesters.KECCAK256.digest("Hello, World!")
```

### 2. 대칭형 암호화 (Encryption)

AES, DES, TripleDES, RC2, RC4 등 PBE(Password Based Encryption) 알고리즘을 지원합니다.

```kotlin
import io.bluetape4k.crypto.encrypt.Encryptors

// AES-256 암호화/복호화
val encrypted = Encryptors.AES.encrypt("Hello, World!")
val decrypted = Encryptors.AES.decrypt(encrypted)  // "Hello, World!"

// 커스텀 비밀번호 사용
val encryptor = AES(password = "my-secret-password-12chars")
val encrypted2 = encryptor.encrypt("Sensitive Data")
```

### 3. 확장 함수

Kotlin 스타일의 편의 확장 함수를 제공합니다.

```kotlin
import io.bluetape4k.crypto.digest.digest
import io.bluetape4k.crypto.digest.matchesDigest
import io.bluetape4k.crypto.encrypt.encrypt
import io.bluetape4k.crypto.encrypt.decrypt

// Digest 확장 함수
val digest = "Hello".digest(Digesters.SHA256)
"Hello".matchesDigest(digest, Digesters.SHA256)  // true

// Encrypt 확장 함수
val encrypted = "Hello".encrypt(Encryptors.AES)
val decrypted = encrypted.decrypt(Encryptors.AES)  // "Hello"

// ByteArray 확장 함수
val bytes = "Hello".toByteArray()
val encryptedBytes = bytes.encrypt(Encryptors.AES)
val decryptedBytes = encryptedBytes.decrypt(Encryptors.AES)
```

### 4. JCA Cipher 빌더

JCA(Java Cryptography Architecture) Cipher를 빌더 패턴으로 쉽게 구성할 수 있습니다.

```kotlin
import io.bluetape4k.crypto.cipher.CipherBuilder
import io.bluetape4k.crypto.cipher.encrypt
import io.bluetape4k.crypto.cipher.decrypt
import javax.crypto.Cipher

val builder = CipherBuilder()
    .secretKeySize(16)
    .ivBytesSize(16)
    .algorithm("AES")
    .transformation("AES/CBC/PKCS5Padding")

val encryptCipher = builder.build(Cipher.ENCRYPT_MODE)
val decryptCipher = builder.build(Cipher.DECRYPT_MODE)

val encrypted = encryptCipher.encrypt("Hello".toByteArray())
val decrypted = decryptCipher.decrypt(encrypted)
```

## 알고리즘 비교

### Digest 알고리즘

| 알고리즘 | 해시 크기 | 보안 수준 | 권장 용도 |
|---------|---------|---------|---------|
| SHA-256 | 256비트 | 높음 | 범용 (권장) |
| SHA-384 | 384비트 | 높음 | 높은 보안 요구 |
| SHA-512 | 512비트 | 높음 | 최고 수준 보안 |
| KECCAK-256 | 256비트 | 높음 | 블록체인, SHA-3 호환 |
| KECCAK-384 | 384비트 | 높음 | SHA-3 호환 |
| KECCAK-512 | 512비트 | 높음 | SHA-3 호환 |
| SHA-1 | 160비트 | 낮음 | 레거시 호환만 |
| MD5 | 128비트 | 낮음 | 체크섬 (비보안) |

### Encryption 알고리즘

| 알고리즘 | PBE 알고리즘 | 보안 수준 | 권장 용도 |
|---------|------------|---------|---------|
| AES | PBEWITHHMACSHA256ANDAES_256 | 높음 | 범용 (권장) |
| TripleDES | PBEWithMD5AndTripleDES | 보통 | 레거시 호환 |
| DES | PBEWITHMD5ANDDES | 낮음 | 레거시 호환만 |
| RC2 | PBEWITHSHA1ANDRC2_128 | 낮음 | 레거시 호환만 |
| RC4 | PBEWITHSHA1ANDRC4_128 | 낮음 | 레거시 호환만 |

## 스레드 안전성

- **Digester**: Jasypt `PooledByteDigester` 사용 (풀 크기: 8)
- **Encryptor**: Jasypt `PooledPBEByteEncryptor` 사용 (풀 크기: 2 * CPU 수)
- **CipherBuilder**: 호출 시마다 새로운 `Cipher` 인스턴스 생성
- BouncyCastle 프로바이더 등록은 `ReentrantLock`으로 보호

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-crypto"))
}
```

내부적으로 다음 라이브러리를 사용합니다:

- [Jasypt](http://www.jasypt.org/) - Java Simplified Encryption
- [BouncyCastle](https://www.bouncycastle.org/) - bcprov, bcpkix

## 모듈 구조

```
io.bluetape4k.crypto
├── CryptographySupport.kt              # 유틸리티 (randomBytes, BouncyCastle 등록)
├── digest/
│   ├── Digester.kt                      # Digester 인터페이스
│   ├── AbstractDigester.kt              # 추상 구현 (PooledByteDigester 래핑)
│   ├── Digesters.kt                     # 팩토리 싱글턴
│   ├── DigesterExtensions.kt            # String/ByteArray 확장 함수
│   ├── SHA256.kt, SHA384.kt, SHA512.kt  # SHA-2 계열
│   ├── SHA1.kt, MD5.kt                  # 레거시 알고리즘
│   └── Keccak256.kt, Keccak384.kt, Keccak512.kt  # Keccak 계열
├── encrypt/
│   ├── Encryptor.kt                     # Encryptor 인터페이스
│   ├── AbstractEncryptor.kt             # 추상 구현 (PooledPBEByteEncryptor 래핑)
│   ├── Encryptors.kt                    # 팩토리 싱글턴
│   ├── EncryptorExtensions.kt           # String/ByteArray 확장 함수
│   ├── EncryptorSupport.kt              # 기본 비밀번호 설정
│   ├── AES.kt                           # AES-256 (권장)
│   ├── DES.kt, TripleDES.kt            # DES 계열
│   └── RC2.kt, RC4.kt                  # RC 계열
└── cipher/
    ├── CipherBuilder.kt                 # JCA Cipher 빌더
    └── CipherExtensions.kt             # Cipher 확장 함수
```

## 테스트

```bash
# 전체 테스트 실행
./gradlew :bluetape4k-crypto:test

# 특정 테스트 실행
./gradlew :bluetape4k-crypto:test --tests "io.bluetape4k.crypto.digest.DigesterTest"
./gradlew :bluetape4k-crypto:test --tests "io.bluetape4k.crypto.encrypt.EncryptorTest"
```

## 참고

- [Jasypt](http://www.jasypt.org/) - Java Simplified Encryption
- [BouncyCastle](https://www.bouncycastle.org/) - 암호화 프로바이더
- [JCA Reference Guide](https://docs.oracle.com/en/java/javase/21/security/java-cryptography-architecture-jca-reference-guide.html) - Java Cryptography Architecture
