package io.bluetape4k.vertx.sqlclient.schema

import io.vertx.sqlclient.templates.RowMapper


// NOTE: Vertx Row 의 Index는 0부터 시작한다 (java.sql.ResultSet 은 1 부터 시작)

@JvmField
val PersonMapper: RowMapper<Person> = RowMapper { row ->
    // 컬럼 순서나 alias 에 대한 대응이 없긴 하다 ㅠ.ㅠ
    Person(
        id = row.getInteger(0),
        firstName = row.getString(1),
        lastName = row.getString(2),
        birthDate = row.getLocalDate(3),
        employed = row.getBoolean(4),
        occupation = row.getString(5),
        addressId = row.getInteger(6)
    )
}

@JvmField
val PersonAddressMapper: RowMapper<PersonWithAddress> = RowMapper { row ->
    PersonWithAddress(
        id = row.getInteger(0),
        firstName = row.getString(1),
        lastName = row.getString(2),
        birthDate = row.getLocalDate(3),
        employed = row.getBoolean(4),
        occupation = row.getString(5),
        address = Address(
            id = row.getInteger(6),
            streetAddress = row.getString(7),
            city = row.getString(8),
            state = row.getString(9),
        )
    )
}
