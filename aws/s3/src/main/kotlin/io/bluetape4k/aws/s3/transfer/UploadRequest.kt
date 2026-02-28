package io.bluetape4k.aws.s3.transfer

import io.bluetape4k.aws.s3.model.putObjectRequestOf
import io.bluetape4k.io.exists
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.io.File
import java.nio.file.Path

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

inline fun uploadFileRequestOf(
    bucket: String,
    key: String,
    source: Path,
    @BuilderInference builder: UploadFileRequest.Builder.() -> Unit,
): UploadFileRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")
    require(source.exists()) { "source[$source] does not exist." }

    return uploadFileRequest {
        putObjectRequest(putObjectRequestOf(bucket, key))
        source(source)
        builder()
    }
}

inline fun uploadFileRequestOf(
    bucket: String,
    key: String,
    source: File,
    @BuilderInference builder: UploadFileRequest.Builder.() -> Unit,
): UploadFileRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")
    require(source.exists()) { "source[$source] does not exist." }

    return uploadFileRequest {
        putObjectRequest(putObjectRequestOf(bucket, key))
        source(source)
        builder()
    }
}

inline fun uploadDirectoryRequest(
    @BuilderInference builder: UploadDirectoryRequest.Builder.() -> Unit,
): UploadDirectoryRequest =
    UploadDirectoryRequest.builder().apply(builder).build()


inline fun uploadDirectoryRequestOf(
    bucket: String,
    source: Path,
    @BuilderInference builder: UploadDirectoryRequest.Builder.() -> Unit,
): UploadDirectoryRequest {
    bucket.requireNotBlank("bucket")
    require(source.exists()) { "source[$source] does not exist." }

    return uploadDirectoryRequest {
        bucket(bucket)
        source(source)
        builder()
    }
}
