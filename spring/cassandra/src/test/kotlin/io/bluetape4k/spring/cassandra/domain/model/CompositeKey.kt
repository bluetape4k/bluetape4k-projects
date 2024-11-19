package io.bluetape4k.spring.cassandra.domain.model

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.io.Serializable

/**
 * 엔티티에 복합키 적용 예제
 *
 * @see [TypeWithCompositeKey]
 */
@PrimaryKeyClass
data class CompositeKey(
    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 1, name = "first_name")
    val firstName: String = "",

    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 2)
    val lastname: String = "",
): Serializable
