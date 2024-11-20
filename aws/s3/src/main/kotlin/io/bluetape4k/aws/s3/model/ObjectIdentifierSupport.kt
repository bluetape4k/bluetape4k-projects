package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

inline fun objectIdentifier(
    key: String,
    initializer: ObjectIdentifier.Builder.() -> Unit = {},
): ObjectIdentifier {
    key.requireNotBlank("key")
    return ObjectIdentifier.builder()
        .key(key)
        .apply(initializer)
        .build()
}

fun objectIdentifierOf(
    key: String,
    versionId: String? = null,
    initializer: ObjectIdentifier.Builder.() -> Unit = {},
): ObjectIdentifier {
    return objectIdentifier(key) {
        versionId(versionId)
        initializer()
    }
}
