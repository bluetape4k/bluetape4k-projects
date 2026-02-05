package io.bluetape4k.aws.s3.transfer

import io.bluetape4k.aws.s3.model.toAsyncRequestBody
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.Download
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.DownloadRequest
import software.amazon.awssdk.transfer.s3.model.FileDownload
import software.amazon.awssdk.transfer.s3.model.FileUpload
import software.amazon.awssdk.transfer.s3.model.Upload
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener
import java.nio.file.Path

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 다운로드 받습니다.
 *
 * @param T
 * @param responseTransformer 응답을 변환할 transformer
 * @param builder [DownloadRequest] 를 구성하는 람다 함수
 * @return [Download] 인스턴스
 */
inline fun <T> S3TransferManager.download(
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    @BuilderInference builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): Download<T> {
    return download(downloadRequest(responseTransformer, builder))
}

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 다운로드 받습니다.
 *
 * @param T
 * @param bucket bucket name
 * @param key key
 * @param responseTransformer 응답을 변환할 비동기 transformer
 * @param getObjectRequestBuilder [GetObjectRequest.Builder] 를 구성하는 람다 함수
 * @return 다운로드한 S3 Object
 */
inline fun <T> S3TransferManager.download(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    @BuilderInference crossinline getObjectRequestBuilder: GetObjectRequest.Builder.() -> Unit = {},
): Download<T> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request = downloadRequestOf(bucket, key, responseTransformer, getObjectRequestBuilder)
    return download(request)
}

inline fun S3TransferManager.downloadAsByteArray(
    bucket: String,
    key: String,
    @BuilderInference crossinline getObjectRequestBuilder: (GetObjectRequest.Builder) -> Unit = {},
): Download<ResponseBytes<GetObjectResponse>> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return download(
        bucket,
        key,
        AsyncResponseTransformer.toBytes(),
        getObjectRequestBuilder
    )
}

inline fun S3TransferManager.downloadFile(
    bucket: String,
    key: String,
    objectPath: Path,
    @BuilderInference crossinline additionalDownloadRequest: DownloadFileRequest.Builder.() -> Unit = {
        addTransferListener(
            LoggingTransferListener.create()
        )
    },
): FileDownload {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return downloadFile { fileRequest ->
        fileRequest.getObjectRequest { gorb ->
            gorb.bucket(bucket)
            gorb.key(key)
        }
        fileRequest.destination(objectPath)
        additionalDownloadRequest(fileRequest)
    }
}

/**
 * [S3TransferManager]를 이용하여 객체를 S3에 업로드 합니다.
 *
 * @param bucket bucket name
 * @param key key
 * @param asyncRequestBody 업로드할 객체
 * @param additionalUploadRequest  추가로 구성할 [UploadRequest.Builder]를 구성하는 람다 함수
 * @receiver
 * @return
 */
inline fun S3TransferManager.upload(
    bucket: String,
    key: String,
    asyncRequestBody: AsyncRequestBody,
    @BuilderInference additionalUploadRequest: UploadRequest.Builder.() -> Unit = {},
): Upload {
    val request = uploadRequest {
        putObjectRequest {
            it.bucket(bucket)
            it.key(key)
        }
        requestBody(asyncRequestBody)
        additionalUploadRequest(this)
    }
    return upload(request)
}

inline fun S3TransferManager.uploadByteArray(
    bucket: String,
    key: String,
    content: ByteArray,
    @BuilderInference additionalUploadRequest: UploadRequest.Builder.() -> Unit = {},
): Upload {
    val request = uploadRequest {
        putObjectRequest {
            it.bucket(bucket)
            it.key(key)
        }
        requestBody(content.toAsyncRequestBody())
        additionalUploadRequest(this)
    }
    return upload(request)
}

fun S3TransferManager.uploadFile(
    bucket: String,
    key: String,
    source: Path,
    @BuilderInference uploadRequest: UploadFileRequest.Builder.() -> Unit = {
        addTransferListener(
            LoggingTransferListener.create()
        )
    },
): FileUpload {
    val request = UploadFileRequest.builder()
        .putObjectRequest {
            it.bucket(bucket)
            it.key(key)
        }
        .source(source)
        .apply(uploadRequest)
        .build()

    return uploadFile(request)
}
