package io.bluetape4k.jackson.text.datasources

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.LibraryName
import io.bluetape4k.io.toInputStream
import io.bluetape4k.jackson.text.AbstractJacksonTextTest
import io.bluetape4k.jackson.text.properties.JacksonProps
import io.bluetape4k.jackson.text.yaml.JacksonYaml
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import javax.sql.DataSource

class ParseNamedDataSourcePropertiesTest: AbstractJacksonTextTest() {

    companion object: KLogging()

    private val propsMapper: JavaPropsMapper by lazy { JacksonProps.defaultPropsMapper }
    private val propsFactory: JavaPropsFactory by lazy { JacksonProps.defaultPropsFactory }
    private val objectMapper: ObjectMapper by lazy { JacksonProps.defaultObjectMapper }

    private val yamlMapper: YAMLMapper by lazy { JacksonYaml.defaultYamlMapper }


    val default = Dbcp2DataSourceProperty(
        driverClassName = "mysql",
        url = "jdbc:mysql://localhost:3306/test",
        username = "sa",
        password = "password",

        maxTotal = 50,
        maxIdle = 40,
        minIdle = 10,
        maxWaitMillis = null, // 60_000,
        lifo = true
    )

    val read = HikariDataSourceProperty(
        driverClassName = "mariadb",
        url = "jdbc:mysql://localhost:3307/test",
        username = "sa",
        password = "password",
        connectionTimeout = 5_000,
        idleTimeout = null, // 60_000L,
        maxLifetime = 60_000L,
        properties = listOf("cachePropStmts=true", "prepStmtCacheSize=250", "propStmtCacheSqlLimit=2048")
    )

    @Nested
    inner class ParseProperties {

        private val properties = """
        |$LibraryName.datasources.default.connectionPool=dbcp2
        |$LibraryName.datasources.default.driverClassName=mysql
        |$LibraryName.datasources.default.url=jdbc:mysql://localhost:3306/test
        |$LibraryName.datasources.default.username=sa
        |$LibraryName.datasources.default.password=password
        |$LibraryName.datasources.default.maxTotal=50
        |$LibraryName.datasources.default.maxIdle=40
        |$LibraryName.datasources.default.minIdle=10
        |$LibraryName.datasources.default.maxWaitMillis=
        |$LibraryName.datasources.default.lifo=true
        |$LibraryName.datasources.default.connectionProperties=
        |$LibraryName.datasources.read.connectionPool=hikari
        |$LibraryName.datasources.read.driverClassName=mariadb
        |$LibraryName.datasources.read.url=jdbc:mysql://localhost:3307/test
        |$LibraryName.datasources.read.username=sa
        |$LibraryName.datasources.read.password=password
        |$LibraryName.datasources.read.connectionTimeout=5000
        |$LibraryName.datasources.read.idleTimeout=
        |$LibraryName.datasources.read.maxLifetime=60000
        |$LibraryName.datasources.read.properties.1=cachePropStmts=true
        |$LibraryName.datasources.read.properties.2=prepStmtCacheSize=250
        |$LibraryName.datasources.read.properties.3=propStmtCacheSqlLimit=2048
        |
        """.trimMargin()


        @Disabled("PropsMapper 에 버그가 있다. 미지정 속성은 null 이 되어야 하는데, maxWaitMillis=0 으로 지정된다.")
        @Test
        fun `generate datasource properties to properties format and parse`() {
            val kommons = Bluetape4kProperty(mapOf("default" to default, "read" to read))
            val root = RootProperty(kommons)

            val propertyString = propsMapper.writeValueAsString(root) // writeValueAsProperties
            log.debug { "properties=\n$propertyString\n------" }
            propertyString shouldBeEqualTo properties

            val parsedRoot = propsMapper.readValue<RootProperty>(propertyString)
            parsedRoot.bluetape4k.datasources.size shouldBeEqualTo 2

            val parsedDefault = parsedRoot.bluetape4k.datasources["default"]
            val parsedRead = parsedRoot.bluetape4k.datasources["read"]

            parsedDefault shouldBeEqualTo default
            parsedRead shouldBeEqualTo read
        }

        @Disabled("PropsMapper 에 버그가 있다. 미지정 속성은 null 이 되어야 하는데, maxWaitMillis=0 으로 지정된다.")
        fun `generate datasource properties to properties and parse`() {
            val kommons = Bluetape4kProperty(mapOf("default" to default, "read" to read))
            val root = RootProperty(kommons)

            val properties = propsMapper.writeValueAsProperties(root)
            log.debug { "properties=\n$properties\n------" }
            properties shouldBeEqualTo Properties().also { it.load(this.properties.toInputStream()) }

            val parsedRoot = propsMapper.readPropertiesAs(properties, RootProperty::class.java)
            parsedRoot.bluetape4k.datasources.size shouldBeEqualTo 2

            val parsedDefault = parsedRoot.bluetape4k.datasources["default"]
            val parsedRead = parsedRoot.bluetape4k.datasources["read"]

            parsedDefault shouldBeEqualTo default
            parsedRead shouldBeEqualTo read
        }
    }

    @Nested
    inner class ParseYaml {
        val yaml = """
            |---
            |$LibraryName:
            |  datasources:
            |    default: !<dbcp2>
            |      driverClassName: "mysql"
            |      url: "jdbc:mysql://localhost:3306/test"
            |      username: "sa"
            |      password: "password"
            |      maxTotal: 50
            |      maxIdle: 40
            |      minIdle: 10
            |      maxWaitMillis: null
            |      lifo: true
            |      connectionProperties: ""
            |    read: !<hikari>
            |      driverClassName: "mariadb"
            |      url: "jdbc:mysql://localhost:3307/test"
            |      username: "sa"
            |      password: "password"
            |      connectionTimeout: 5000
            |      idleTimeout: null
            |      maxLifetime: 60000
            |      properties:
            |      - "cachePropStmts=true"
            |      - "prepStmtCacheSize=250"
            |      - "propStmtCacheSqlLimit=2048"
            |
            """.trimMargin()

        @Test
        fun `generate datasource properties to yaml format and parse`() {
            val property = Bluetape4kProperty(mapOf("default" to default, "read" to read))
            val root = RootProperty(property)

            val yamlString = yamlMapper.writeValueAsString(root)

            log.debug { "yaml=\n$yamlString\n------" }
            yamlString shouldBeEqualTo yaml

            val parsedRoot = yamlMapper.readValue<RootProperty>(yamlString)

            parsedRoot.shouldNotBeNull()
            parsedRoot.bluetape4k.datasources.size shouldBeEqualTo 2

            val parsedDefault = parsedRoot.bluetape4k.datasources["default"]
            val parsedRead = parsedRoot.bluetape4k.datasources["read"]

            parsedDefault shouldBeEqualTo default
            parsedRead shouldBeEqualTo read
        }

        @Test
        fun `yaml data type can just string`() {
            val parsedRoot = yamlMapper.readValue<RootProperty>(yaml)

            parsedRoot.shouldNotBeNull()
            parsedRoot.bluetape4k.datasources.size shouldBeEqualTo 2

            val parsedDefault = parsedRoot.bluetape4k.datasources["default"]
            val parsedRead = parsedRoot.bluetape4k.datasources["read"]

            parsedDefault shouldBeEqualTo default
            parsedRead shouldBeEqualTo read
        }
    }


    data class RootProperty(val bluetape4k: Bluetape4kProperty)
    data class Bluetape4kProperty(val datasources: Map<String, DataSourceProperty<out DataSource>> = emptyMap())

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "connectionPool")
    @JsonSubTypes(
        JsonSubTypes.Type(value = Dbcp2DataSourceProperty::class),
        JsonSubTypes.Type(value = HikariDataSourceProperty::class)
    )
    interface DataSourceProperty<DS: DataSource> {
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
        val maxWaitMillis: Int? = null,

        val lifo: Boolean?,

        // Properties에서 값이 없는 경우에는 empty string으로 지정된다. Parsing 후에는 empty string으로 지정된다.
        var connectionProperties: String = "",

        ): DataSourceProperty<DataSource>

    @JsonTypeName("hikari")
    data class HikariDataSourceProperty(

        override val driverClassName: String,
        override val url: String,
        override val username: String?,
        override val password: String?,

        val connectionTimeout: Int?,
        val idleTimeout: Long?,
        val maxLifetime: Long?,

        val properties: List<String> = emptyList(),
    ): DataSourceProperty<DataSource>

    enum class ConnectionPoolType {
        DBPC2,
        HIKARI,
        MARIADB,
        TOMCAT
    }

}
