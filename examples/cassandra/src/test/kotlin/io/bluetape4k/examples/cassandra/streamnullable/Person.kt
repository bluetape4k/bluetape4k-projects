package io.bluetape4k.examples.cassandra.streamnullable

import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.Table

@Table("stream_person")
data class Person(
    @field:Id val id: String = "",
    var firstname: String = "",
    var lastname: String = "",
): Comparable<Person> {
    override fun compareTo(other: Person): Int {
        return id.compareTo(other.id)
    }
}
