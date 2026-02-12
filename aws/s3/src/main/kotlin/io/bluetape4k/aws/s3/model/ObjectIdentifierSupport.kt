package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

inline fun objectIdentifier(
    key: String,
    @BuilderInference builder: ObjectIdentifier.Builder.() -> Unit = {},
): ObjectIdentifier {
    key.requireNotBlank("key")
    return ObjectIdentifier.builder()
        .key(key)
        .apply(builder)
        .build()
}

inline fun objectIdentifierOf(
    key: String,
    versionId: String? = null,
    @BuilderInference builder: ObjectIdentifier.Builder.() -> Unit = {},
): ObjectIdentifier =
    objectIdentifier(key) {
        versionId(versionId)
        builder()
    }
