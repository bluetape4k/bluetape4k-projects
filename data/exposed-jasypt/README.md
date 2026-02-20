# Module bluetape4k-exposed-jasypt

Exposed 컬럼 암복호화를 Jasypt로 처리하기 위한 모듈입니다.

## 개요

`bluetape4k-exposed-jasypt`는 JetBrains Exposed의 컬럼 값을 [Jasypt](http://www.jasypt.org/) 라이브러리를 통해 암호화하여 저장하는 기능을 제공합니다. 결정적 암호화(Deterministic Encryption)를 사용하여 동일한 평문은 항상 동일한 암호문으로 변환됩니다.

### 주요 기능

- **결정적 암호화 컬럼 타입**: 동일 입력에 동일 암호문 생성
- **문자열/바이너리 암호화**: `VARCHAR`, `BINARY` 컬럼 지원
- **검색/인덱스 활용 가능**: 조건절 사용 및 인덱스 생성 가능

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-jasypt:${version}")
    implementation("io.bluetape4k:bluetape4k-crypto:${version}")
}
```

## 기본 사용법

### 1. 암호화 컬럼 정의

```kotlin
import io.bluetape4k.exposed.core.jasypt.jasyptVarChar
import io.bluetape4k.exposed.core.jasypt.jasyptBinary
import io.bluetape4k.crypto.encrypt.Encryptors
import org.jetbrains.exposed.v1.core.dao.id.IdTable

object Users: IdTable<Long>("users") {
    val name = varchar("name", 100)

    // Jasypt 암호화 VARCHAR 컬럼
    val ssn = jasyptVarChar(
        name = "ssn",
        colLength = 512,
        encryptor = Encryptors.Jasypt
    )

    // Jasypt 암호화 BINARY 컬럼
    val privateKey = jasyptBinary(
        name = "private_key",
        colLength = 1024,
        encryptor = Encryptors.Jasypt
    )
}
```

### 2. 암호화 컬럼 사용

```kotlin
// 삽입 시 자동 암호화
Users.insert {
    it[name] = "John Doe"
    it[ssn] = "123-45-6789"  // 자동으로 암호화되어 저장
}

// 조회 시 자동 복호화
val user = Users.selectAll().where { Users.id eq 1L }.single()
val ssn = user[Users.ssn]  // 자동으로 복호화됨

// 검색 가능 (결정적 암호화이므로)
val userBySsn = Users.selectAll()
    .where { Users.ssn eq "123-45-6789" }
    .single()
```

## 결정적 암호화 특징

| 장점              | 단점                        |
|-----------------|---------------------------|
| WHERE 절에서 검색 가능 | 동일 평문 → 동일 암호문 (패턴 분석 가능) |
| 인덱스 생성 가능       | 보안 요구사항에 따라 부적합할 수 있음     |
| 정렬 가능           |                           |

## 주요 파일/클래스 목록

| 파일                           | 설명                |
|------------------------------|-------------------|
| `JasyptVarCharColumnType.kt` | VARCHAR 암호화 컬럼 타입 |
| `JasyptBinaryColumnType.kt`  | BINARY 암호화 컬럼 타입  |
| `Tables.kt`                  | 테이블 확장 함수         |

## 주의사항

1. **보안 고려사항**: 결정적 암호화는 인덱스/검색에 유리하지만, 동일한 평문이 항상 동일한 암호문으로 변환되므로 높은 보안이 필요한 경우에는 적합하지 않을 수 있습니다.

2. **컬럼 길이**: 암호화 후 평문보다 길어지므로 충분한 컬럼 길이를 지정해야 합니다.

3. **키 관리**: 암호화 키는 안전하게 관리해야 합니다.

## 테스트

```bash
./gradlew :bluetape4k-exposed-jasypt:test
```

## 참고

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Jasypt](http://www.jasypt.org/)
- [bluetape4k-crypto](../../bluetape4k/crypto/README.md)
