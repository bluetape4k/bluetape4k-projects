package io.bluetape4k.aws.kotlin.dynamodb.examples.user

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import java.io.Serializable

data class User(
    val userId: String,
    val name: String,
    val email: String,
    val age: Int,
): Serializable


internal const val USER_TABLE_NAME = "user-test"


fun Map<String, AttributeValue>.toUser(): User = User(
    userId = this["userId"]?.asSOrNull() ?: "",
    name = this["name"]?.asSOrNull() ?: "",
    email = this["email"]?.asSOrNull() ?: "",
    age = this["age"]?.asNOrNull()?.toInt() ?: 0
)
