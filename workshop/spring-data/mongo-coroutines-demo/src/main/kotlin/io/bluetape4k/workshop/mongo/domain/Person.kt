package io.bluetape4k.workshop.mongo.domain

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import java.io.Serializable

data class Person @PersistenceCreator constructor(
    val firstname: String? = "Walter",
    val lastname: String? = "",
    val age: Int = 0,
    @Id val id: String?,
): Serializable {

    constructor(firstname: String?, lastname: String?, age: Int = 0): this(firstname, lastname, age, null)
}
