package io.bluetape4k.examples.vertx.sqlclient.model

import io.vertx.sqlclient.templates.RowMapper
import io.vertx.sqlclient.templates.TupleMapper
import kotlin.reflect.full.memberProperties

@JvmField
val USER_ROW_MAPPER: RowMapper<User> = RowMapper { row ->
    User(
        id = row.getLong("id"),
        firstName = row.getString("first_name"),
        lastName = row.getString("last_name"),
    )
}

@JvmField
val USER_TUPLE_MAPPER: TupleMapper<User> = TupleMapper.mapper { user ->
    // Reflection 을 이용
    user.javaClass.kotlin.memberProperties.associate {
        it.name to it.get(user)
    }
}
