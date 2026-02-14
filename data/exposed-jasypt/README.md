# Module bluetape4k-exposed-jasypt

Exposed 컬럼 암복호화를 Jasypt로 처리하기 위한 모듈입니다.

## 주요 기능

- **결정적 암호화 컬럼 타입**: 동일 입력에 동일 암호문
- **문자열/바이너리 암호화**: `VARCHAR`, `BINARY` 컬럼 지원
- **검색/인덱스 활용 가능성**: 조건절 사용 시나리오 대응

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-jasypt:${version}")
}
```

## 주요 기능 상세

- `JasyptVarCharColumnType.kt`
- `JasyptBinaryColumnType.kt`
- `Tables.kt`

## 주의사항

결정적 암호화는 인덱스/검색에 유리하지만 보안 요구사항에 따라 적합성 검토가 필요합니다.
