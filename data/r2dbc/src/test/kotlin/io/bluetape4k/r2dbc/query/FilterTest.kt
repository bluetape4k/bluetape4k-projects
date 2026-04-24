package io.bluetape4k.r2dbc.query

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class FilterTest {

    companion object : KLogging()

    // ─── Filter.Where ───────────────────────────────────────────────────

    @Test
    fun `Where - countLeaves 는 항상 1 을 반환한다`() {
        val where = Filter.Where("id = :id")
        where.countLeaves() shouldBeEqualTo 1
    }

    @Test
    fun `Where - 조건 문자열을 그대로 보관한다`() {
        val condition = "name like :name"
        val where = Filter.Where(condition)
        where.where shouldBeEqualTo condition
    }

    @Test
    fun `Where - toString 에 where 문자열이 포함된다`() {
        val where = Filter.Where("active = true")
        where.toString().shouldNotBeNull()
        where.toString().contains("active = true").shouldBeTrue()
    }

    @Test
    fun `Where - data class equals 동작`() {
        val w1 = Filter.Where("id = :id")
        val w2 = Filter.Where("id = :id")
        val w3 = Filter.Where("name = :name")
        (w1 == w2).shouldBeTrue()
        (w1 == w3).shouldBeFalse()
    }

    @Test
    fun `Where - Serializable 구현 확인`() {
        val where = Filter.Where("id = :id")
        where.shouldBeInstanceOf<java.io.Serializable>()
    }

    // ─── Filter.Group (empty) ────────────────────────────────────────

    @Test
    fun `Group - 빈 그룹의 countLeaves 는 0 을 반환한다`() {
        val group = Filter.Group()
        group.countLeaves() shouldBeEqualTo 0
    }

    @Test
    fun `Group - 기본 operator 는 and 이다`() {
        val group = Filter.Group()
        group.operator shouldBeEqualTo "and"
    }

    @Test
    fun `Group - operator 를 or 로 지정할 수 있다`() {
        val group = Filter.Group("or")
        group.operator shouldBeEqualTo "or"
    }

    // ─── Filter.Group (with Where leaves) ───────────────────────────

    @Test
    fun `Group - Where 하나를 포함하면 countLeaves 는 1 이다`() {
        val group = Filter.Group(
            "and",
            mutableListOf(Filter.Where("id = :id"))
        )
        group.countLeaves() shouldBeEqualTo 1
    }

    @Test
    fun `Group - Where 두 개를 포함하면 countLeaves 는 2 이다`() {
        val group = Filter.Group(
            "and",
            mutableListOf(
                Filter.Where("id = :id"),
                Filter.Where("active = true")
            )
        )
        group.countLeaves() shouldBeEqualTo 2
    }

    // ─── Nested Group ─────────────────────────────────────────────

    @Test
    fun `Group - 중첩 그룹의 countLeaves 는 합산된다`() {
        val inner = Filter.Group(
            "or",
            mutableListOf(
                Filter.Where("name like :name"),
                Filter.Where("email like :email")
            )
        )
        val outer = Filter.Group(
            "and",
            mutableListOf(
                Filter.Where("active = true"),
                inner
            )
        )
        // outer: 1 (active) + inner: 2 (name, email) = 3
        outer.countLeaves() shouldBeEqualTo 3
    }

    @Test
    fun `Group - filters 에 동적으로 추가하면 countLeaves 에 반영된다`() {
        val group = Filter.Group()
        group.countLeaves() shouldBeEqualTo 0

        group.filters.add(Filter.Where("id = :id"))
        group.countLeaves() shouldBeEqualTo 1

        group.filters.add(Filter.Where("active = :active"))
        group.countLeaves() shouldBeEqualTo 2
    }

    @Test
    fun `Group - toString 에 operator 와 filters 정보가 포함된다`() {
        val group = Filter.Group("and", mutableListOf(Filter.Where("x = :x")))
        val str = group.toString()
        str.contains("and").shouldBeTrue()
        str.contains("x = :x").shouldBeTrue()
    }

    @Test
    fun `Group - Serializable 구현 확인`() {
        val group = Filter.Group()
        group.shouldBeInstanceOf<java.io.Serializable>()
    }

    // ─── Sealed type checks ───────────────────────────────────────────

    @Test
    fun `Filter - Where 는 Filter sealed 타입의 인스턴스이다`() {
        val filter: Filter = Filter.Where("id = :id")
        filter.shouldBeInstanceOf<Filter.Where>()
    }

    @Test
    fun `Filter - Group 은 Filter sealed 타입의 인스턴스이다`() {
        val filter: Filter = Filter.Group()
        filter.shouldBeInstanceOf<Filter.Group>()
    }
}
