package io.bluetape4k.examples.cassandra.kotlin

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table("kotlin_people")
data class Person(
    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    val firstname: String? = "",

    val lastname: String = "Bae",
): Serializable
