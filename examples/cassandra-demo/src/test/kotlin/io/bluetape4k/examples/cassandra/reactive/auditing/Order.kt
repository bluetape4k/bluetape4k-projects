package io.bluetape4k.examples.cassandra.reactive.auditing

import io.bluetape4k.spring.cassandra.model.AbstractCassandraAuditable
import io.bluetape4k.spring.cassandra.model.AbstractCassandraPersistable
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant


@Table
data class Order(
    @field:Id private var orderId: String = "",
): AbstractCassandraAuditable<String, String>() {

    override fun getId(): String = orderId
    override fun setId(id: String) {
        this.orderId = id
    }
}

@Table
data class CustomAuditableOrder(
    @field:PrimaryKey private var id: String = "",

    @field:CreatedBy var createdBy: String? = null,
    @field:CreatedDate var createdAt: Instant? = null,
    @field:LastModifiedBy var modifiedBy: String? = null,
    @field:LastModifiedDate var modifiedAt: Instant? = null,
): AbstractCassandraPersistable<String>() {

    override fun getId(): String = id

    override fun setId(id: String) {
        this.id = id
    }

    override fun isNew(): Boolean = createdAt == null
}
