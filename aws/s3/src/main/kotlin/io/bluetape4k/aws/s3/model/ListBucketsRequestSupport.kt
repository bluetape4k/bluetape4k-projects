package io.bluetape4k.aws.s3.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.s3.model.ListBucketsRequest

inline fun listBucketsRequest(
    @BuilderInference builder: ListBucketsRequest.Builder.() -> Unit = {},
): ListBucketsRequest =
    ListBucketsRequest.builder().apply(builder).build()

fun listBucketsRequestOf(
    @BuilderInference configrationBuilder: AwsRequestOverrideConfiguration.Builder.() -> Unit = {},
): ListBucketsRequest =
    listBucketsRequest {
        overrideConfiguration(configrationBuilder)
    }
