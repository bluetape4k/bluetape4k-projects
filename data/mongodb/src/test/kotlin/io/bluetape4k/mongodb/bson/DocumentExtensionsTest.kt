package io.bluetape4k.mongodb.bson

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class DocumentExtensionsTest {

    companion object : KLoggingChannel()

    @Test
    fun `documentOf 키-값 쌍으로 Document 생성`() {
        val doc = documentOf(
            "name" to "Alice",
            "age" to 30,
            "active" to true
        )

        doc.getString("name") shouldBeEqualTo "Alice"
        doc.getInteger("age") shouldBeEqualTo 30
        doc.getBoolean("active").shouldBeTrue()
    }

    @Test
    fun `documentOf null 값 포함 Document 생성`() {
        val doc = documentOf(
            "name" to "Bob",
            "city" to null
        )

        doc.getString("name") shouldBeEqualTo "Bob"
        doc.get("city").shouldBeNull()
    }

    @Test
    fun `documentOf DSL 빌더로 Document 생성`() {
        val doc = documentOf {
            put("name", "Charlie")
            put("age", 25)
            put("tags", listOf("admin", "user"))
        }

        doc.getString("name") shouldBeEqualTo "Charlie"
        doc.getInteger("age") shouldBeEqualTo 25
        @Suppress("UNCHECKED_CAST")
        val tags = doc.get("tags") as? List<String>
        tags.shouldNotBeNull()
        tags shouldBeEqualTo listOf("admin", "user")
    }

    @Test
    fun `getAs reified 타입으로 값 조회`() {
        val doc = documentOf(
            "name" to "Dave",
            "age" to 40,
            "score" to 3.14
        )

        doc.getAs<String>("name") shouldBeEqualTo "Dave"
        doc.getAs<Int>("age") shouldBeEqualTo 40
        doc.getAs<Double>("score") shouldBeEqualTo 3.14
    }

    @Test
    fun `getAs 키가 없으면 null 반환`() {
        val doc = documentOf("name" to "Eve")

        doc.getAs<String>("missing").shouldBeNull()
    }

    @Test
    fun `getAs 타입이 맞지 않으면 null 반환`() {
        val doc = documentOf("age" to 30)

        doc.getAs<String>("age").shouldBeNull()
    }
}
