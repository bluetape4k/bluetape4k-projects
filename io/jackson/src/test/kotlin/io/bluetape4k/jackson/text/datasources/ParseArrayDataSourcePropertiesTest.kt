package io.bluetape4k.jackson.text.datasources

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.jackson.text.AbstractJacksonTextTest
import io.bluetape4k.jackson.text.JacksonText
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.Serializable

class ParseArrayDataSourcePropertiesTest: AbstractJacksonTextTest() {

    companion object: KLogging()

    val yamlMapper by lazy { JacksonText.Yaml.defaultMapper }
    val propsMapper by lazy { JacksonText.Props.defaultMapper }

    @Test
    fun `parse single datasource from yaml format`() {
        val yaml = """
            |bluetape4k:
            |    datasources:
            |        -   connectionPool: hikaricp
            |            driverClassName: org.h2.Driver
            |            url: jdbc:h2:mem:test
            |            username: sa
            |            password:
            |
            |            connectionTimeout: 30000
            |            idleTimeout: 600000
            |            maxLifetime: 1800000
            |
            |            properties:
            |                - cachePropStmts=true
            |                - prepStmtCacheSize=250
            |                - propStmtCacheSqlLimit=2048
            """.trimMargin()

        val root = yamlMapper.readValue<RootProperty>(yaml)
        root.shouldNotBeNull()
        root.bluetape4k.shouldNotBeNull()
        root.bluetape4k.datasources.size shouldBeEqualTo 1

        val hikariProperty = root.bluetape4k.datasources.first() as? HikariDataSourceProperty
        hikariProperty.shouldNotBeNull()

        hikariProperty.connectionTimeout!! shouldBeEqualTo 30000
        hikariProperty.idleTimeout!! shouldBeEqualTo 600000
        hikariProperty.maxLifetime!! shouldBeEqualTo 1800000

        hikariProperty.properties.size shouldBeEqualTo 3
        hikariProperty.properties shouldContainAll setOf(
            "cachePropStmts=true",
            "prepStmtCacheSize=250",
            "propStmtCacheSqlLimit=2048"
        )
    }

    @Test
    fun `parse multiple datasources from yaml file`() {
        val input = ClassLoader.getSystemResourceAsStream("datasource.yaml")
        input.shouldNotBeNull()

        val root = yamlMapper.readValue<RootProperty>(input)
        verifyMultipleDataSources(root)

        // Object 를 properties 나 yaml 포맷의 문자열로 생성할 수도 있습니다.
        val props = propsMapper.writeValueAsProperties(root)
        log.debug { "Yaml to Properties=$props" }
    }

    @Test
    fun `parse multiple datasources from properties file`() {
        val input = ClassLoader.getSystemResourceAsStream("datasource.properties")
        input.shouldNotBeNull()

        val root = propsMapper.readValue<RootProperty>(input)
        verifyMultipleDataSources(root)
    }

    private fun verifyMultipleDataSources(root: RootProperty) {
        root.shouldNotBeNull()
        root.bluetape4k.shouldNotBeNull()
        root.bluetape4k.datasources.size shouldBeEqualTo 2

        val hikaricp = root.bluetape4k.datasources[0] as HikariDataSourceProperty
        hikaricp.shouldNotBeNull()

        hikaricp.connectionTimeout!! shouldBeEqualTo 30000
        hikaricp.idleTimeout!! shouldBeEqualTo 600000
        hikaricp.maxLifetime!! shouldBeEqualTo 1800000

        hikaricp.properties.size shouldBeEqualTo 3
        hikaricp.properties shouldContainAll setOf(
            "cachePropStmts=true",
            "prepStmtCacheSize=250",
            "propStmtCacheSqlLimit=2048"
        )

        val dbcp2 = root.bluetape4k.datasources[1] as Dbcp2DataSourceProperty
        dbcp2.shouldNotBeNull()

        dbcp2.maxTotal!! shouldBeEqualTo 8
        dbcp2.maxIdle!! shouldBeEqualTo 8
        dbcp2.minIdle!! shouldBeEqualTo 0
        dbcp2.maxWaitMillis!! shouldBeEqualTo 100000
        dbcp2.lifo!!.shouldBeTrue()
    }

    data class RootProperty(val bluetape4k: Bluetape4kProperty)

    data class Bluetape4kProperty(val datasources: List<DataSourceProperty> = emptyList())

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "connectionPool")
    @JsonSubTypes(
        JsonSubTypes.Type(value = Dbcp2DataSourceProperty::class),
        JsonSubTypes.Type(value = HikariDataSourceProperty::class)
    )
    interface DataSourceProperty: Serializable {
        val driverClassName: String
        val url: String
        val username: String?
        val password: String?
    }

    @JsonTypeName(value = "dbcp2")
    data class Dbcp2DataSourceProperty(
        override val driverClassName: String,
        override val url: String,
        override val username: String?,
        override val password: String?,

        val maxTotal: Int?,
        val maxIdle: Int?,
        val minIdle: Int?,
        val maxWaitMillis: Int?,

        val lifo: Boolean?,

        val connectionProperties: String?,
    ): DataSourceProperty

    @JsonTypeName("hikaricp")
    data class HikariDataSourceProperty(
        override val driverClassName: String,
        override val url: String,
        override val username: String?,
        override val password: String?,

        val connectionTimeout: Int?,
        val idleTimeout: Long?,
        val maxLifetime: Long?,

        val properties: Set<String> = emptySet(),
    ): DataSourceProperty

    enum class ConnectionPoolType {
        DBPC2,
        HIKARICP,
        MARIADB,
        TOMCAT
    }
}
