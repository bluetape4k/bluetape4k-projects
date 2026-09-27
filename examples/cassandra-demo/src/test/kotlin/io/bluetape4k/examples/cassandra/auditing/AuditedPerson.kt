package io.bluetape4k.examples.cassandra.auditing

import io.bluetape4k.spring.cassandra.model.AbstractCassandraAuditable
import org.springframework.data.annotation.Id
import org.springframework.data.cassandra.core.mapping.Table

@Table
data class AuditedPerson(
    @field:Id
    private var id: Long = 0L,
): AbstractCassandraAuditable<String, Long>() {

    override fun getId(): Long = id

    override fun setId(id: Long) {
        this.id = id
    }
}
