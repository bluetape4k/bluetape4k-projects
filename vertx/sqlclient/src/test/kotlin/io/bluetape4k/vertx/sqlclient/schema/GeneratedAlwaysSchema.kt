package io.bluetape4k.vertx.sqlclient.schema

import org.mybatis.dynamic.sql.SqlTable

object GeneratedAlwaysTable {

    val generatedAlways = GeneratedAlways()

    class GeneratedAlways: SqlTable("GeneratedAlways") {
        val id = column<Long>("id")
        val firstName = column<String>("first_name")
        val lastName = column<String>("last_name")
        val fullName = column<String>("full_name")
    }
}

data class GeneratedAlwaysRecord(
    var id: Long? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var fullName: String? = null,
): java.io.Serializable
