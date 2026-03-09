# Module bluetape4k-aws-s3

AWS SDK for Java v2 S3 작업(동기/비동기/코루틴/TransferManager)을 Kotlin 확장 함수로 단순화하는 라이브러리입니다.

## 주요 기능

- `S3Client` 확장: 업로드/다운로드/이동 보조 함수
- `S3AsyncClient` 확장: `CompletableFuture` 기반 비동기 보조 함수
- 코루틴 확장: Async API의 `suspend` 브릿지
- `S3TransferManager` 확장: 대용량 파일/바이트 전송 보조 함수
- 요청/바디 모델 유틸: `Put/Get/Delete/List` 요청 생성 및 `RequestBody` 변환

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-s3:${version}")
}
```

## 사용 예시

### 동기 클라이언트

```kotlin
import io.bluetape4k.aws.s3.*
import java.io.File

val s3Client: S3Client = // ...

val exists: Boolean = s3Client.existsBucket("my-bucket").getOrDefault(false)

s3Client.putAsString("my-bucket", "docs/readme.txt", "hello")
val content: String = s3Client.getAsString("my-bucket", "docs/readme.txt")

s3Client.putAsFile("my-bucket", "files/report.pdf", File("/tmp/report.pdf"))
s3Client.getAsFile("my-bucket", "files/report.pdf", File("/tmp/downloaded.pdf"))

// 비원자적 이동: copy 성공 후 delete 실패 가능
val moved = s3Client.moveObject("src-bucket", "src-key", "dest-bucket", "dest-key")

// 원자적 이동 보장 시도: delete 실패 시 목적지 객체 롤백 후 예외
val atomicMoved = s3Client.moveObjectAtomic("src-bucket", "src-key", "dest-bucket", "dest-key")
```

### 비동기 + 코루틴

```kotlin
import io.bluetape4k.aws.s3.*

suspend fun uploadAndRead(s3AsyncClient: S3AsyncClient) {
    s3AsyncClient.putAsString("my-bucket", "notes/hello.txt", "hello")
    val text = s3AsyncClient.getAsString("my-bucket", "notes/hello.txt")
    println(text)
}
```

### TransferManager

```kotlin
import io.bluetape4k.aws.s3.transfer.*

val transferManager: S3TransferManager = // ...

val upload = transferManager.uploadFileAsync(
    bucket = "my-bucket",
    key = "large/archive.zip",
    source = java.nio.file.Path.of("/tmp/archive.zip"),
)
upload.completionFuture().join()

val download = transferManager.downloadFileAsync(
    bucket = "my-bucket",
    key = "large/archive.zip",
    destination = java.nio.file.Path.of("/tmp/archive-downloaded.zip"),
)
download.completionFuture().join()
```

## 이동( Move ) API 주의사항

- `moveObject`, `moveObjectAsync`: **비원자적**입니다. 복사 성공 후 원본 삭제가 실패할 수 있습니다.
- `moveObjectAtomic`, `moveObjectAtomicAsync`: 삭제 실패 시 목적지 객체 삭제(롤백)를 시도하며, 롤백까지 실패하면 `IllegalStateException`을 발생시킵니다.

## 주요 소스 파일

| 파일 | 설명 |
|---|---|
| `S3ClientExtensions.kt` | 동기 `S3Client` 확장 함수 |
| `S3AsyncClientExtensions.kt` | `CompletableFuture` 기반 비동기 확장 함수 |
| `S3AsyncClientCoroutinesExtensions.kt` | `suspend` 브릿지 확장 함수 |
| `S3ClientFactory.kt` | `S3Client` / `S3AsyncClient` / `S3TransferManager` 팩토리 |
| `transfer/S3TransferManagerExtensions.kt` | TransferManager 비동기 확장 함수 |
| `transfer/S3TransferManagerCoroutinesExtensions.kt` | TransferManager 코루틴 확장 함수 |
| `transfer/UploadRequest.kt` | 업로드 요청 빌더 유틸 |
| `transfer/DownloadRequest.kt` | 다운로드 요청 빌더 유틸 |
| `model/PutObjectRequest.kt` | PutObject 요청 유틸 |
| `model/GetObjectRequest.kt` | GetObject 요청 유틸 |
| `model/DeleteObjectRequest.kt` | DeleteObject/DeleteObjects 요청 유틸 |
| `model/ListObjectsRequest.kt` | ListObjects 요청 유틸 |
| `model/ListBucketsRequest.kt` | ListBuckets 요청 유틸 |
| `model/RequestBody.kt` | 동기 `RequestBody` 유틸 |
| `model/AsyncRequestBody.kt` | 비동기 `AsyncRequestBody` 유틸 |
| `model/ObjectIdentifier.kt` | `ObjectIdentifier` 생성 유틸 |
| `model/MoveObjectResult.kt` | 이동 작업 결과 모델 |

