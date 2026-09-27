package io.bluetape4k.examples.cassandra.domain.model

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

@Table("bookReference")
data class BookReference(
    @field:PrimaryKey
    val isbn: String = "",

    var title: String = "",
    var references: MutableSet<String> = mutableSetOf(),
    var bookmarks: MutableList<String> = mutableListOf(),
    var credits: MutableMap<String, String> = mutableMapOf(),
): Serializable
