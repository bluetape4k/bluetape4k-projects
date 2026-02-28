package io.bluetape4k.aws.s3.transfer

import io.bluetape4k.aws.s3.model.getObjectRequestOf
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.DownloadRequest
import java.nio.file.Path


inline fun <T> downloadRequest(
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    @BuilderInference builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<T> =
    DownloadRequest.builder()
        .apply(builder)
        .responseTransformer(responseTransformer)
        .build()

inline fun <T> downloadRequestOf(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    @BuilderInference builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<T> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

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
    @BuilderInference builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<GetObjectResponse> =
    downloadRequestOf(
        bucket,
        key,
        AsyncResponseTransformer.toFile(downloadPath)
    ) {
        builder()
    }

inline fun downloadByteArrayRequestOf(
    bucket: String,
    key: String,
    @BuilderInference builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<ResponseBytes<GetObjectResponse>> =
    downloadRequestOf(
        bucket,
        key,
        AsyncResponseTransformer.toBytes()
    ) {
        builder()
    }

inline fun downloadFileRequest(
    @BuilderInference builder: DownloadFileRequest.Builder.() -> Unit = {},
): DownloadFileRequest =
    DownloadFileRequest.builder().apply(builder).build()

inline fun downloadFileRequestOf(
    bucket: String,
    key: String,
    destination: Path,
    @BuilderInference builder: DownloadFileRequest.Builder.() -> Unit = {},
): DownloadFileRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return downloadFileRequest {
        getObjectRequest(getObjectRequestOf(bucket, key))
        destination(destination)
        builder()
    }
}


inline fun downloadDirectoryRequest(
    @BuilderInference builder: DownloadDirectoryRequest.Builder.() -> Unit = {},
): DownloadDirectoryRequest =
    DownloadDirectoryRequest.builder().apply(builder).build()

inline fun downloadDirectoryRequestOf(
    bucket: String,
    destination: Path,
    @BuilderInference builder: DownloadDirectoryRequest.Builder.() -> Unit = {},
): DownloadDirectoryRequest {
    bucket.requireNotBlank("bucket")

    return downloadDirectoryRequest {
        bucket(bucket)
        destination(destination)

        builder()
    }
}
