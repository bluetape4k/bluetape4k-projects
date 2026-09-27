package io.bluetape4k.examples.cassandra.domain.model

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.BasicMapId
import org.springframework.data.cassandra.core.mapping.MapId
import org.springframework.data.cassandra.core.mapping.MapIdentifiable
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable

/**
 * HINT: Repository를 사용할 때에는 [MapId] 를 적용한 [MapIdentifiable] 을 사용하는 것을 추천합니다.
 *
 * @property firstname
 * @property lastname
 * @constructor Create empty Type with map id
 */
@Table
data class TypeWithMapId(
    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    val firstname: String = "",

    @field:PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 2)
    val lastname: String = "",
): MapIdentifiable, Serializable {

    override fun getMapId(): MapId =
        BasicMapId
            .id("firstname", firstname)
            .with("lastname", lastname)
}
