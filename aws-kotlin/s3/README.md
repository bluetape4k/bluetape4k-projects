# Module bluetape4k-aws-kotlin-s3

AWS SDK for Kotlin S3 사용 시 파일/객체 작업을 쉽게 수행하는 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 합니다.

## 주요 기능

- **S3Client 확장 함수**: Bucket/Object 생성/조회/삭제 보조
- **복사/이동 지원**: Object Copy/Move 유틸
- **요청 모델 빌더 지원**: Put/Get/Delete/List 요청 생성 보조
- **Presigned URL 지원**: 시간 제한 다운로드 URL 생성
- **병렬 처리 지원**: 다중 객체 업로드/다운로드

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-s3:${version}")
}
```

## 사용 예시

### 다운로드 (GET)

```kotlin
import io.bluetape4k.aws.kotlin.s3.*

val s3Client: S3Client = // ...

// 객체 존재 확인
val exists = s3Client.existsObject("my-bucket", "path/to/file.txt")

// 문자열로 다운로드
val content: String? = s3Client.getAsString("my-bucket", "path/to/file.txt")

// ByteArray로 다운로드
val bytes: ByteArray? = s3Client.getAsByteArray("my-bucket", "path/to/data.bin")

// 파일로 다운로드
s3Client.getAsFile("my-bucket", "path/to/file.pdf", File("/local/file.pdf"))

// OutputStream으로 다운로드
val outputStream = ByteArrayOutputStream()
s3Client.getAsOutputStream("my-bucket", "path/to/file.txt", outputStream)

// 병렬 다운로드
val responses = s3Client.getAll(
    getObjectRequestOf("bucket", "key1"),
    getObjectRequestOf("bucket", "key2"),
    getObjectRequestOf("bucket", "key3")
).toList()
```

### 업로드 (PUT)

```kotlin
import io.bluetape4k.aws.kotlin.s3.*
import aws.smithy.kotlin.runtime.content.ByteStream

// 문자열 업로드
s3Client.putFromString("my-bucket", "path/to/file.txt", "Hello, World!")

// ByteArray 업로드
s3Client.putFromByteArray("my-bucket", "path/to/data.bin", byteArray)

// 파일 업로드
s3Client.putFromFile("my-bucket", "path/to/file.pdf", File("/local/file.pdf"))

// Path로 업로드
s3Client.putFromPath("my-bucket", "path/to/file.pdf", Paths.get("/local/file.pdf"))

// 메타데이터와 함께 업로드
s3Client.put(
    bucketName = "my-bucket",
    key = "path/to/file.txt",
    body = ByteStream.fromString("Hello, World!"),
    metadata = mapOf("author" to "john"),
    contentType = "text/plain"
)

// 병렬 업로드
val responses = s3Client.putAll(
    concurrency = 10,
    putRequest1, putRequest2, putRequest3
).toList()
```

### 복사 및 이동

```kotlin
import io.bluetape4k.aws.kotlin.s3.*

// 객체 복사
val copyResponse = s3Client.copy("src-bucket", "src-key", "dest-bucket", "dest-key")

// 객체 이동 (복사 후 원본 삭제)
val moveResponse = s3Client.move("src-bucket", "src-key", "dest-bucket", "dest-key")

// 빌더로 이동
s3Client.move(
    copyRequestBuilder = {
        bucket = "dest-bucket"
        key = "dest-key"
        copySource = "src-bucket/src-key"
    },
    deleteRequestBuilder = {
        bucket = "src-bucket"
        key = "src-key"
    }
)
```

### Presigned URL

```kotlin
import kotlin.time.Duration.Companion.minutes

// 다운로드용 Presigned URL 생성 (5분 유효)
val presignedRequest = s3Client.presignGetObject(
    bucketName = "my-bucket",
    key = "path/to/file.txt",
    duration = 5.minutes
)
println("Presigned URL: ${presignedRequest.url}")
```

### 버킷 관리

```kotlin
import io.bluetape4k.aws.kotlin.s3.*

// 버킷 존재 확인
val exists = s3Client.existsBucket("my-bucket")

// 버킷 생성
s3Client.createBucket("my-bucket")

// 버킷 삭제
s3Client.deleteBucket("my-bucket")

// 버킷 정책 조회
val policy = s3Client.tryGetBucketPolicy("my-bucket")
```

## 주요 기능 상세

| 파일                          | 설명                                            |
|-----------------------------|-----------------------------------------------|
| `S3ClientSupport.kt`        | 클라이언트 생성 팩토리                                  |
| `S3ClientGet.kt`            | 다운로드 확장 함수 (get, getAsString, getAsFile 등)    |
| `S3ClientPut.kt`            | 업로드 확장 함수 (put, putFromString, putFromFile 등) |
| `S3ClientDelete.kt`         | 삭제 확장 함수                                      |
| `S3ClientCopy.kt`           | 복사 확장 함수                                      |
| `S3ClientMove.kt`           | 이동 확장 함수                                      |
| `S3ClientBucket.kt`         | 버킷 관리 확장 함수                                   |
| `S3ClientLogger.kt`         | 로깅 보조                                         |
| `model/GetObject.kt`        | GetObject 요청 빌더                               |
| `model/PutObject.kt`        | PutObject 요청 빌더                               |
| `model/DeleteObject.kt`     | DeleteObject 요청 빌더                            |
| `model/CopyObject.kt`       | CopyObject 요청 빌더                              |
| `model/ListObjects.kt`      | ListObjects 요청 빌더                             |
| `model/HeadObject.kt`       | HeadObject 요청 빌더                              |
| `model/ObjectIdentifier.kt` | ObjectIdentifier 빌더                           |
| `model/S3Location.kt`       | S3 위치 정보                                      |
