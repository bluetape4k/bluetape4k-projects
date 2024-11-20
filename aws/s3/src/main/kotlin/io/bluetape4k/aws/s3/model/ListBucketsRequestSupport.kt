package io.bluetape4k.aws.s3.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.s3.model.ListBucketsRequest

inline fun listBucketsRequest(
    builder: ListBucketsRequest.Builder.() -> Unit = {},
): ListBucketsRequest {
    return ListBucketsRequest.builder().apply(builder).build()
}

fun listBucketsRequestOf(
    configrationBuilder: AwsRequestOverrideConfiguration.Builder.() -> Unit = {},
): ListBucketsRequest {
    return listBucketsRequest {
        overrideConfiguration(configrationBuilder)
    }
}
