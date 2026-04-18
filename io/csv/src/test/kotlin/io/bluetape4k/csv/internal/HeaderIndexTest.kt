package io.bluetape4k.csv.internal

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class HeaderIndexTest {

    companion object : KLogging()

    @Test
    fun `of() creates index from array`() {
        val headers = arrayOf("id", "name", "age")
        val index = HeaderIndex.of(headers)

        index.indexOf("id") shouldBeEqualTo 0
        index.indexOf("name") shouldBeEqualTo 1
        index.indexOf("age") shouldBeEqualTo 2
    }

    @Test
    fun `first-wins on duplicate headers`() {
        val headers = arrayOf("id", "name", "id", "extra")
        val index = HeaderIndex.of(headers)

        // 첫 번째 "id" 인덱스(0)가 유지되어야 함
        index.indexOf("id") shouldBeEqualTo 0
        index.indexOf("name") shouldBeEqualTo 1
        index.indexOf("extra") shouldBeEqualTo 3
        index.size shouldBeEqualTo 3  // "id" 중복이므로 3개
    }

    @Test
    fun `indexOf returns null for unknown name`() {
        val headers = arrayOf("id", "name")
        val index = HeaderIndex.of(headers)

        index.indexOf("unknown").shouldBeNull()
        index.indexOf("ID").shouldBeNull()  // case-sensitive
        index.indexOf("").shouldBeNull()
    }

    @Test
    fun `nameOf returns header name by index`() {
        val headers = arrayOf("id", "name", "age")
        val index = HeaderIndex.of(headers)

        index.nameOf(0) shouldBeEqualTo "id"
        index.nameOf(1) shouldBeEqualTo "name"
        index.nameOf(2) shouldBeEqualTo "age"
    }

    @Test
    fun `nameOf returns null for out of range index`() {
        val headers = arrayOf("id", "name")
        val index = HeaderIndex.of(headers)

        index.nameOf(99).shouldBeNull()
        index.nameOf(-1).shouldBeNull()
    }

    @Test
    fun `names returns all header names in order`() {
        val headers = arrayOf("c", "a", "b")
        val index = HeaderIndex.of(headers)

        index.names shouldBeEqualTo listOf("c", "a", "b")
    }

    @Test
    fun `size returns correct count`() {
        val index = HeaderIndex.of(arrayOf("x", "y", "z"))
        index.size shouldBeEqualTo 3
    }

    @Test
    fun `size of empty array returns zero`() {
        val index = HeaderIndex.of(emptyArray())
        index.size shouldBeEqualTo 0
    }

    @Test
    fun `case-sensitive lookup`() {
        val headers = arrayOf("Name", "name", "NAME")
        val index = HeaderIndex.of(headers)

        // "Name" wins at 0, "name" and "NAME" are duplicates (dropped)
        index.indexOf("Name").shouldNotBeNull()
        index.indexOf("Name") shouldBeEqualTo 0
        // "name" 은 "Name"과 다른 키 → 인덱스 1
        index.indexOf("name") shouldBeEqualTo 1
        // "NAME" 은 "Name", "name"과 모두 다른 키 → 인덱스 2
        index.indexOf("NAME") shouldBeEqualTo 2
    }
}
