# Module bluetape4k-aws-kotlin-s3

AWS SDK for Kotlin S3 사용 시 파일/객체 작업을 쉽게 수행하는 확장 라이브러리입니다.

## 주요 기능

- **S3Client 확장 함수**: Bucket/Object 생성/조회/삭제 보조
- **복사/이동 지원**: Object Copy/Move 유틸
- **요청 모델 빌더 지원**: Put/Get/Delete/List 요청 생성 보조
- **로깅 보조**: S3 작업 추적을 위한 로깅 유틸

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-s3:${version}")
}
```

## 주요 기능 상세

- `S3ClientExtensions.kt`, `S3ClientSupport.kt`
- `S3ClientGet.kt`, `S3ClientPut.kt`, `S3ClientDelete.kt`
- `S3ClientCopy.kt`, `S3ClientMove.kt`
- `model/*.kt`
