# Module bluetape4k-aws-s3

AWS SDK for Java v2 S3 사용 시 업로드/다운로드/이동 작업을 쉽게 수행할 수 있는 확장 라이브러리입니다.

## 주요 기능

- **S3Client 확장**: 객체 업로드/다운로드/삭제 보조 함수
- **S3AsyncClient 확장**: 비동기 S3 작업 유틸
- **Coroutine 확장**: Async API의 suspend 브릿지
- **TransferManager 지원**: 대용량 전송 보조
- **요청/바디 빌더 지원**: `Put/Get/Delete/List` 요청 생성 유틸

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-s3:${version}")
}
```

## 주요 기능 상세

- `S3ClientExtensions.kt`
- `S3AsyncClientExtensions.kt`, `S3AsyncClientCoroutinesExtensions.kt`
- `transfer/TransferManagerExtensions.kt`, `transfer/TransferManagerCoroutinesExtensions.kt`
- `model/*RequestSupport.kt`, `model/*Support.kt`
