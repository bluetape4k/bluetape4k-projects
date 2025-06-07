package io.bluetape4k.aws.s3.transfer

import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.model.DownloadRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.nio.file.Path

inline fun <T> downloadRequest(
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    initializer: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<T> {
    return DownloadRequest.builder()
        .apply(initializer)
        .responseTransformer(responseTransformer)
        .build()
}

fun <T> downloadRequestOf(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    getObjectRequestBuilder: GetObjectRequest.Builder.() -> Unit = {},
): DownloadRequest<T> {
    return downloadRequest(responseTransformer) {
        getObjectRequest {
            it.bucket(bucket)
            it.key(key)
            getObjectRequestBuilder(it)
        }
    }
}

fun downloadRequestOf(
    bucket: String,
    key: String,
    downloadPath: Path,
    initializer: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<GetObjectResponse> {
    return downloadRequest(AsyncResponseTransformer.toFile(downloadPath)) {
        getObjectRequest {
            it.bucket(bucket)
            it.key(key)
        }
        initializer()
    }
}


inline fun uploadRequest(initializer: UploadRequest.Builder.() -> Unit): UploadRequest {
    return UploadRequest.builder().apply(initializer).build()
}

fun uploadRequestOf(
    putObjectRequest: PutObjectRequest,
    requestBody: AsyncRequestBody,
    initializer: UploadRequest.Builder.() -> Unit = {},
): UploadRequest {
    return uploadRequest {
        putObjectRequest(putObjectRequest)
        requestBody(requestBody)
        initializer()
    }
}
