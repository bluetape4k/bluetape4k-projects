package io.bluetape4k.spring.data.exposed.jdbc.query

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.repository.UserJdbcRepository
import io.bluetape4k.spring.data.exposed.jdbc.repository.query.ExposedQueryMethod
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldStartWith
import org.junit.jupiter.api.Test
import org.springframework.data.projection.SpelAwareProxyProjectionFactory
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata

class ExposedQueryMethodTest {

    companion object : KLogging()

    private val metadata = DefaultRepositoryMetadata(UserJdbcRepository::class.java)
    private val factory = SpelAwareProxyProjectionFactory()

    private fun method(name: String, vararg paramTypes: Class<*>) =
        UserJdbcRepository::class.java.getMethod(name, *paramTypes)

    @Test
    fun `isAnnotatedQuery returns false for PartTree method`() {
        val m = method("findByName", String::class.java)
        val qm = ExposedQueryMethod(m, metadata, factory)

        qm.isAnnotatedQuery.shouldBeFalse()
        qm.getAnnotatedQuery().shouldBeNull()
    }

    @Test
    fun `isAnnotatedQuery returns true for @Query annotated method`() {
        val m = method("findByEmailNative", String::class.java)
        val qm = ExposedQueryMethod(m, metadata, factory)

        qm.isAnnotatedQuery.shouldBeTrue()
        qm.getAnnotatedQuery().shouldNotBeNull()
        qm.getAnnotatedQuery()!! shouldStartWith "SELECT"
    }

    @Test
    fun `getAnnotatedQuery returns the SQL from @Query annotation`() {
        val m = method("findByEmailNative", String::class.java)
        val qm = ExposedQueryMethod(m, metadata, factory)

        val sql = qm.getAnnotatedQuery()
        sql.shouldNotBeNull()
        sql.contains("users").shouldBeTrue()
    }

    @Test
    fun `getCountQuery returns null when countQuery is not set`() {
        val m = method("findByEmailNative", String::class.java)
        val qm = ExposedQueryMethod(m, metadata, factory)

        qm.getCountQuery().shouldBeNull()
    }

    @Test
    fun `getName returns the method name`() {
        val m = method("findByName", String::class.java)
        val qm = ExposedQueryMethod(m, metadata, factory)

        qm.name.shouldNotBeNull()
    }

    @Test
    fun `isCollectionQuery returns true for list-returning methods`() {
        val m = method("findByName", String::class.java)
        val qm = ExposedQueryMethod(m, metadata, factory)

        qm.isCollectionQuery.shouldBeTrue()
    }

    @Test
    fun `domainClass returns non-null for UserEntity method`() {
        val m = method("findByName", String::class.java)
        val qm = ExposedQueryMethod(m, metadata, factory)

        qm.entityInformation.javaType.shouldNotBeNull()
    }
}
