package io.bluetape4k.aws.s3.transfer

import io.bluetape4k.aws.s3.model.getObjectRequestOf
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.DownloadRequest
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.nio.file.Path

inline fun <T> downloadRequest(
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    @BuilderInference builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<T> {
    return DownloadRequest.builder()
        .apply(builder)
        .responseTransformer(responseTransformer)
        .build()
}

inline fun <T> downloadRequestOf(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    @BuilderInference crossinline builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<T> {
    val request = getObjectRequestOf(bucket, key)
    return downloadRequest(responseTransformer) {
        getObjectRequest(request)
        builder()
    }
}

inline fun downloadRequestOf(
    bucket: String,
    key: String,
    downloadPath: Path,
    @BuilderInference crossinline builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<GetObjectResponse> {
    return downloadRequestOf(bucket, key, AsyncResponseTransformer.toFile(downloadPath)) {
        builder()
    }
}

inline fun downloadFileRequestOf(
    bucket: String,
    key: String,
    destination: Path,
    @BuilderInference builder: DownloadFileRequest.Builder.() -> Unit = {},
): DownloadFileRequest =
    DownloadFileRequest.builder()
        .getObjectRequest(getObjectRequestOf(bucket, key))
        .destination(destination)
        .apply(builder)
        .build()


inline fun uploadRequest(
    @BuilderInference builder: UploadRequest.Builder.() -> Unit,
): UploadRequest =
    UploadRequest.builder().apply(builder).build()

inline fun uploadRequestOf(
    putObjectRequest: PutObjectRequest,
    requestBody: AsyncRequestBody,
    @BuilderInference builder: UploadRequest.Builder.() -> Unit = {},
): UploadRequest {
    return uploadRequest {
        putObjectRequest(putObjectRequest)
        requestBody(requestBody)
        builder()
    }
}

inline fun uploadFileRequest(
    @BuilderInference builder: UploadFileRequest.Builder.() -> Unit,
): UploadFileRequest =
    UploadFileRequest.builder().apply(builder).build()
