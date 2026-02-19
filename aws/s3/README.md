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

## 사용 예시

### 동기 클라이언트

```kotlin
import io.bluetape4k.aws.s3.*

val s3Client: S3Client = // ...

// Bucket 존재 확인
val exists: Result<Boolean> = s3Client.existsBucket("my-bucket")

// Bucket 생성
s3Client.createBucket("my-bucket") {
    locationConstraint("ap-northeast-2")
}

// 업로드 - 문자열
s3Client.putAsString("my-bucket", "path/to/file.txt", "Hello, World!")

// 업로드 - ByteArray
s3Client.putAsByteArray("my-bucket", "path/to/data.bin", byteArray)

// 업로드 - 파일
s3Client.putAsFile("my-bucket", "path/to/file.pdf", File("/local/file.pdf"))

// 다운로드 - 문자열
val content: String = s3Client.getAsString("my-bucket", "path/to/file.txt")

// 다운로드 - ByteArray
val bytes: ByteArray = s3Client.getAsByteArray("my-bucket", "path/to/data.bin")

// 다운로드 - 파일
s3Client.getAsFile("my-bucket", "path/to/file.pdf", File("/local/downloaded.pdf"))

// 객체 이동
s3Client.moveObject("src-bucket", "src-key", "dest-bucket", "dest-key")

// 원자적 이동 (실패 시 롤백)
s3Client.moveObjectAtomic("src-bucket", "src-key", "dest-bucket", "dest-key")
```

### 비동기 클라이언트 (Coroutine)

```kotlin
import io.bluetape4k.aws.s3.*

val s3AsyncClient: S3AsyncClient = // ...

// Bucket 존재 확인
    suspend
fun checkBucket(): Boolean = s3AsyncClient.existsBucket("my-bucket")

// 업로드
suspend fun upload(content: String): PutObjectResponse =
    s3AsyncClient.putAsString("my-bucket", "path/to/file.txt", content)

// 다운로드
suspend fun download(): String =
    s3AsyncClient.getAsString("my-bucket", "path/to/file.txt")

// 파일 업로드
suspend fun uploadFile(file: File): PutObjectResponse =
    s3AsyncClient.putAsFile("my-bucket", "path/to/file.pdf", file)

// 파일 다운로드
suspend fun downloadFile(path: Path): GetObjectResponse =
    s3AsyncClient.getAsFile("my-bucket", "path/to/file.pdf", path)

// 객체 이동
suspend fun moveObject(): MoveObjectResult =
    s3AsyncClient.moveObject("src-bucket", "src-key", "dest-bucket", "dest-key")
```

### TransferManager (대용량 전송)

```kotlin
import io.bluetape4k.aws.s3.transfer.*

val transferManager: S3TransferManager = // ...

// 파일 업로드 (멀티파트)
val upload: FileUpload = transferManager.uploadFile(
    File("/large/file.zip"),
    "my-bucket",
    "path/to/large.zip"
)
upload.completionFuture().await()

// 파일 다운로드
val download: FileDownload = transferManager.downloadFile(
    "my-bucket",
    "path/to/large.zip",
    File("/local/large.zip")
)
download.completionFuture().await()
```

## 주요 기능 상세

| 파일                                                | 설명                     |
|---------------------------------------------------|------------------------|
| `S3ClientExtensions.kt`                           | 동기 클라이언트 확장 함수         |
| `S3AsyncClientExtensions.kt`                      | 비동기 클라이언트 확장 함수        |
| `S3AsyncClientCoroutinesExtensions.kt`            | 코루틴 확장 함수              |
| `S3Factory.kt`                                    | S3 클라이언트 팩토리           |
| `transfer/TransferManagerExtensions.kt`           | TransferManager 확장     |
| `transfer/TransferManagerCoroutinesExtensions.kt` | TransferManager 코루틴 확장 |
| `transfer/RequestSupport.kt`                      | 전송 요청 빌더               |
| `model/PutObjectRequestSupport.kt`                | Put 요청 빌더              |
| `model/GetObjectRequestSupport.kt`                | Get 요청 빌더              |
| `model/DeleteObjectRequestSupport.kt`             | Delete 요청 빌더           |
| `model/ListObjectsRequestSupport.kt`              | List 요청 빌더             |
| `model/RequestBodySupport.kt`                     | 동기 RequestBody 빌더      |
| `model/AsyncRequestBodySupport.kt`                | 비동기 RequestBody 빌더     |
| `model/ObjectIdentifierSupport.kt`                | ObjectIdentifier 빌더    |
| `model/MoveObjectResult.kt`                       | 이동 작업 결과               |
