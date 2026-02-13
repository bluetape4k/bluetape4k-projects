package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Delete
import aws.sdk.kotlin.services.dynamodb.model.DeleteRequest
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

@JvmName("deleteOfAttributeValue")
inline fun deleteOf(
    tableName: String,
    key: Map<String, AttributeValue>? = null,
    @BuilderInference crossinline builder: Delete.Builder.() -> Unit = {},
): Delete {
    tableName.requireNotBlank("tableName")

    return Delete {
        this.tableName = tableName
        this.key = key

        builder()
    }
}

@JvmName("deleteOfAny")
inline fun deleteOf(
    tableName: String,
    key: Map<String, Any?>? = null,
    @BuilderInference crossinline builder: Delete.Builder.() -> Unit = {},
): Delete {
    tableName.requireNotBlank("tableName")

    return Delete {
        this.tableName = tableName
        this.key = key?.mapValues { it.value.toAttributeValue() }

        builder()
    }
}

@JvmName("deleteRequestOfAttributeValue")
inline fun deleteRequestOf(
    key: Map<String, AttributeValue>,
    @BuilderInference crossinline builder: DeleteRequest.Builder.() -> Unit = {},
): DeleteRequest {
    key.requireNotEmpty("key")

    return DeleteRequest {
        this.key = key

        builder()
    }
}

@JvmName("deleteRequestOfAny")
inline fun deleteRequestOf(
    key: Map<String, Any?>,
    @BuilderInference crossinline builder: DeleteRequest.Builder.() -> Unit = {},
): DeleteRequest {
    key.requireNotEmpty("key")

    return DeleteRequest {
        this.key = key.mapValues { it.value.toAttributeValue() }

        builder()
    }
}
