package io.bluetape4k.examples.cassandra.domain.model

import com.datastax.oss.driver.api.core.uuid.Uuids
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.util.*

@Table("user_tokens")
data class UserToken(

    @field:PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    val userId: UUID = Uuids.timeBased(),

    @field:PrimaryKeyColumn(name = "auth_token", type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    val token: UUID = Uuids.timeBased(),

    // HINT: @field:Column, @get:Column 을 해줘야 제대로 컬럼명이 적용됩니다.
    @field:Column("user_comment")
    var userComment: String? = null,

    @field: Column("admin_comment")
    var adminComment: String? = null,

    ): Serializable
