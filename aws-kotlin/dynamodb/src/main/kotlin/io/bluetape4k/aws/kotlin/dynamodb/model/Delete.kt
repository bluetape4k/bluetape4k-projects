package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Delete
import aws.sdk.kotlin.services.dynamodb.model.DeleteRequest
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

@JvmName("deleteOfAttributeValue")
fun deleteOf(
    tableName: String,
    key: Map<String, AttributeValue>? = null,
    configurer: Delete.Builder.() -> Unit = {},
): Delete {
    tableName.requireNotBlank("tableName")

    return Delete {
        this.tableName = tableName
        this.key = key

        configurer()
    }
}

@JvmName("deleteOfAny")
fun deleteOf(
    tableName: String,
    key: Map<String, Any?>? = null,
    configurer: Delete.Builder.() -> Unit = {},
): Delete {
    tableName.requireNotBlank("tableName")

    return Delete {
        this.tableName = tableName
        this.key = key?.mapValues { it.toAttributeValue() }

        configurer()
    }
}

@JvmName("deleteRequestOfAttributeValue")
fun deleteRequestOf(
    key: Map<String, AttributeValue>,
    configurer: DeleteRequest.Builder.() -> Unit = {},
): DeleteRequest {
    key.requireNotEmpty("key")

    return DeleteRequest {
        this.key = key
        configurer()
    }
}

@JvmName("deleteRequestOfAny")
fun deleteRequestOf(
    key: Map<String, Any?>,
    configurer: DeleteRequest.Builder.() -> Unit = {},
): DeleteRequest {
    key.requireNotEmpty("key")

    return DeleteRequest.invoke {
        this.key = key.mapValues { it.toAttributeValue() }
        configurer()
    }
}
