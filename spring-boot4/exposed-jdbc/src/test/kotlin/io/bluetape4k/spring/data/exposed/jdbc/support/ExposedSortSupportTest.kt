package io.bluetape4k.spring.data.exposed.jdbc.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.toCamelCase
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.toExposedOrderBy
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.toSnakeCase
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort

class ExposedSortSupportTest {

    companion object : KLogging()

    private object TestTable : LongIdTable("test_items") {
        val name = varchar("name", 255)
        val created_at = varchar("created_at", 64)
        val age = integer("age")
    }

    @Test
    fun `toSnakeCase converts camelCase to snake_case`() {
        toSnakeCase("createdAt") shouldBeEqualTo "created_at"
        toSnakeCase("firstName") shouldBeEqualTo "first_name"
        toSnakeCase("name") shouldBeEqualTo "name"
        toSnakeCase("myLongPropertyName") shouldBeEqualTo "my_long_property_name"
    }

    @Test
    fun `toCamelCase converts snake_case to camelCase`() {
        toCamelCase("created_at") shouldBeEqualTo "createdAt"
        toCamelCase("first_name") shouldBeEqualTo "firstName"
        toCamelCase("name") shouldBeEqualTo "name"
        toCamelCase("my_long_property_name") shouldBeEqualTo "myLongPropertyName"
    }

    @Test
    fun `toExposedOrderBy maps ASC sort to Exposed SortOrder ASC`() {
        val sort = Sort.by(Sort.Direction.ASC, "name")
        val orderBy = sort.toExposedOrderBy(TestTable)

        orderBy shouldHaveSize 1
        orderBy[0].second shouldBeEqualTo SortOrder.ASC
        orderBy[0].first shouldBeEqualTo TestTable.name
    }

    @Test
    fun `toExposedOrderBy maps DESC sort to Exposed SortOrder DESC`() {
        val sort = Sort.by(Sort.Direction.DESC, "age")
        val orderBy = sort.toExposedOrderBy(TestTable)

        orderBy shouldHaveSize 1
        orderBy[0].second shouldBeEqualTo SortOrder.DESC
        orderBy[0].first shouldBeEqualTo TestTable.age
    }

    @Test
    fun `toExposedOrderBy maps camelCase property to snake_case column`() {
        val sort = Sort.by(Sort.Direction.ASC, "createdAt")
        val orderBy = sort.toExposedOrderBy(TestTable)

        orderBy shouldHaveSize 1
        orderBy[0].second shouldBeEqualTo SortOrder.ASC
        orderBy[0].first shouldBeEqualTo TestTable.created_at
    }

    @Test
    fun `toExposedOrderBy handles multiple sort orders`() {
        val sort = Sort.by(
            Sort.Order.asc("name"),
            Sort.Order.desc("age")
        )
        val orderBy = sort.toExposedOrderBy(TestTable)

        orderBy shouldHaveSize 2
        orderBy[0].first shouldBeEqualTo TestTable.name
        orderBy[0].second shouldBeEqualTo SortOrder.ASC
        orderBy[1].first shouldBeEqualTo TestTable.age
        orderBy[1].second shouldBeEqualTo SortOrder.DESC
    }

    @Test
    fun `toExposedOrderBy skips unknown property and logs warning`() {
        val sort = Sort.by(Sort.Direction.ASC, "nonExistentColumn")
        val orderBy = sort.toExposedOrderBy(TestTable)

        // Unknown property is skipped - no exception thrown
        orderBy shouldHaveSize 0
    }

    @Test
    fun `toExposedOrderBy with unsorted returns empty array`() {
        val sort = Sort.unsorted()
        val orderBy = sort.toExposedOrderBy(TestTable)

        orderBy shouldHaveSize 0
    }
}
