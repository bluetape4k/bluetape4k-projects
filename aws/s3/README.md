# Module bluetape4k-aws-s3

AWS SDK V2 S3 사용 시 기본 기능 외에 추가적인 기능을 제공합니다.

## 주요 기능

* S3Client Upload/Download Extension methods
* S3AsyncClient Upload/Download 하는 Extension methods
* S3TransferManager Upload/Download 하는 Extension method

## S3Client extension methods

getAsByteArray, getAsFile 처럼 S3 Object 를 바로 사용할 수 있는 함수 제공

```kotlin
/**
 * S3 Object 를 download 한 후, ByteArray 로 반환합니다.
 *
 * @param getObjectRequest 요청 정보 Builder
 * @return 다운받은 S3 Object의 ByteArray 형태의 정보
 */
fun S3Client.getAsByteArray(getObjectRequest: (GetObjectRequest.Builder) -> Unit): ByteArray {
    val response = getObject(getObjectRequest)
    return response.readAllBytes()
}

fun S3Client.getAsFile(
    path: Path,
    getObjectRequest: (GetObjectRequest.Builder) -> Unit,
): GetObjectResponse {
    return getObject(getObjectRequest, ResponseTransformer.toFile(path))
}
```

S3 Server 에 다양한 Contents 를 Upload 하는 방법 제공

```kotlin

/**
 * S3 서버로 [body]를 Upload 합니다.
 *
 * @param body              Upload 할 [RequestBody]
 * @param putObjectRequest  PutObjectRequest builder
 * @return S3에 저장된 결과
 */
fun S3Client.put(
    body: RequestBody,
    putObjectRequest: (PutObjectRequest.Builder) -> Unit,
): PutObjectResponse {
    return putObject(putObjectRequest, body)
}

/**
 * S3 서버로 [bytes]를 Upload 합니다.
 *
 * @param bytes           Upload 할 data
 * @param putObjectRequest  PutObjectRequest builder
 * @return S3에 저장된 결과
 */
fun S3Client.putAsByteArray(
    bytes: ByteArray,
    putObjectRequest: (PutObjectRequest.Builder) -> Unit,
): PutObjectResponse {
    return put(RequestBody.fromBytes(bytes), putObjectRequest)
}

fun S3Client.putAsFile(
    path: Path,
    putObjectRequest: (PutObjectRequest.Builder) -> Unit,
): PutObjectResponse {
    return put(RequestBody.fromFile(path), putObjectRequest)
}
```

### S3Client Upload & Download

S3Client 를 이용하여 문자열 Content 를 Upload/Donwload 하는 예제입니다.

```kotlin
@Test
fun `put and get s3 object`() {
    val key = UUID.randomUUID().toString()
    val content = ParagraphRandomizer().randomValue

    val putResponse = s3Client.putAsByteArray(content.toUtf8Bytes()) { it.bucket(BUCKET_NAME).key(key) }
    // s3Client.putObject(putObjectRequest { bucket(BUCKET_NAME); key(key) }, RequestBody.fromBytes(content.toUtf8Bytes()))
    log.debug { "put=$putResponse" }

    val download = s3Client.getAsByteArray { it.bucket(BUCKET_NAME).key(key) }
    download.toUtf8String() shouldBeEqualTo content
}
```

S3Client 를 이용하여 파일을 Upload/Download 하는 예제입니다.

```kotlin
@ParameterizedTest
@MethodSource("getImageNames")
fun `upload and download binary file`(filename: String) {
    val key = "sync/$filename"
    val path = "$IMAGE_PATH/$filename"
    val file = File(path)
    file.exists().shouldBeTrue()

    val putResponse = s3Client.putAsFile(file.toPath()) { it.bucket(BUCKET_NAME).key(key) }
    putResponse.eTag().shouldNotBeEmpty()

    val downloadFile = File(tempDir, filename)
    val downloadPath = downloadFile.toPath()
    val getResponse = s3Client.getAsFile(downloadPath) { it.bucket(BUCKET_NAME).key(key) }
    getResponse.eTag() shouldBeEqualTo putResponse.eTag()

    log.debug { "download=${downloadFile.path}, size=${downloadFile.length()}" }
    downloadFile.exists().shouldBeTrue()
    downloadFile.length() shouldBeEqualTo file.length()
    downloadFile.deleteIfExists()
}
```

## S3TransferManager extension methods

`S3TransferManager` 를 이용하여, Content 를 Upload/Download 하는 함수 제공

```kotlin
/**
 * S3 Object 를 다운로드 받습니다.
 *
 * @param T
 * @param responseTransformer 응답을 변환할 transformer
 * @param transferRequestOverrideConfiguration Transfer 설정 정보
 * @param getObjectRequest object 요청 정보
 * @return [Download] 인스턴스
 */
fun <T> S3TransferManager.download(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    getObjectRequestBuilder: (GetObjectRequest.Builder) -> Unit = {},
): Download<T> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request = downloadRequestOf(bucket, key, responseTransformer, getObjectRequestBuilder)
    return download(request)
}
```

```kotlin
/**
 * Upload object to S3 Server
 *
 * @param asyncRequestBody 비동기 방식으로 Upload 하기 위한 정보. (eg [AsyncRequestBody.fromBytes])
 * @param transferRequestOverrideConfiguration Transfer 설정 정보
 * @param putObjectRequest Upload 요청 정보
 * @return [Upload] 인스턴스
 */
fun S3TransferManager.upload(
    asyncRequestBody: AsyncRequestBody,
    transferRequestOverrideConfiguration: TransferRequestOverrideConfiguration? = null,
    putObjectRequest: (PutObjectRequest.Builder) -> Unit,
): Upload {
    return upload { builder ->
        builder.requestBody(asyncRequestBody)
            .putObjectRequest(putObjectRequest)
            .run {
                transferRequestOverrideConfiguration?.let { overrideConfiguration(it) }
            }
    }
}
```

### S3TransferManager Exmaples

S3TransferManager 를 이용하여 문자열 및 파일을 Upload/Download 하는 예제입니다.

```kotlin
class S3TransferManagerTest: AbstractS3Test() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `upload and download text by transfer manager`() = runSuspendIO {
        val key = UUID.randomUUID().toString()
        val content = randomString()

        val upload = s3TransferManager.uploadByteArray(BUCKET_NAME, key, content.toUtf8Bytes())

        val completedUpload = upload.completionFuture().await()
        completedUpload.response().eTag().shouldNotBeEmpty()

        val download = s3TransferManager.downloadAsByteArray(BUCKET_NAME, key)

        val completedDownload = download.completionFuture().await()

        val downloadContent = completedDownload.result().asByteArray().toUtf8String()
        downloadContent shouldBeEqualTo content
    }

    @ParameterizedTest(name = "upload/download by transfer manager: {0}")
    @MethodSource("getImageNames")
    fun `upload and download file by transfer manager`(filename: String) = runSuspendIO {
        val key = "transfer/$filename"
        val path = "$IMAGE_PATH/$filename"
        val file = File(path)
        file.exists().shouldBeTrue()

        // Upload file by S3TransferManager
        val upload = s3TransferManager.uploadFile(BUCKET_NAME, key, file.toPath())
        val completedUpload = upload.completionFuture().await()
        completedUpload.response().eTag().shouldNotBeEmpty()

        // TempDir 에 파일을 다운로드 한다
        val downloadFile = File(tempDir, filename)
        val downloadPath = downloadFile.toPath()

        val download = s3TransferManager.downloadFile(BUCKET_NAME, key, downloadPath)
        download.completionFuture().await()

        log.debug { "downloadFile=$downloadFile, size=${downloadFile.length()}" }
        downloadFile.exists().shouldBeTrue()
        // downloadFile.length() shouldBeEqualTo file.length()
        downloadFile.deleteIfExists()
    }
}
```
