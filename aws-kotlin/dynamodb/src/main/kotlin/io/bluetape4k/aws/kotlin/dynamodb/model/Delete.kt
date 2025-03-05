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
    crossinline configurer: Delete.Builder.() -> Unit = {},
): Delete {
    tableName.requireNotBlank("tableName")

    return Delete {
        this.tableName = tableName
        this.key = key

        configurer()
    }
}

@JvmName("deleteOfAny")
inline fun deleteOf(
    tableName: String,
    key: Map<String, Any?>? = null,
    crossinline configurer: Delete.Builder.() -> Unit = {},
): Delete {
    tableName.requireNotBlank("tableName")

    return Delete {
        this.tableName = tableName
        this.key = key?.mapValues { it.toAttributeValue() }

        configurer()
    }
}

@JvmName("deleteRequestOfAttributeValue")
inline fun deleteRequestOf(
    key: Map<String, AttributeValue>,
    crossinline configurer: DeleteRequest.Builder.() -> Unit = {},
): DeleteRequest {
    key.requireNotEmpty("key")

    return DeleteRequest {
        this.key = key
        configurer()
    }
}

@JvmName("deleteRequestOfAny")
inline fun deleteRequestOf(
    key: Map<String, Any?>,
    crossinline configurer: DeleteRequest.Builder.() -> Unit = {},
): DeleteRequest {
    key.requireNotEmpty("key")

    return DeleteRequest.invoke {
        this.key = key.mapValues { it.toAttributeValue() }
        configurer()
    }
}
