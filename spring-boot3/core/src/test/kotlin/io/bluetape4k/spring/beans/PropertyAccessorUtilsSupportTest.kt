package io.bluetape4k.spring.beans

import io.bluetape4k.spring.AbstractSpringTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class PropertyAccessorUtilsSupportTest: AbstractSpringTest() {

    @Test
    fun `getPropertyName - 단순 경로에서 이름 추출`() {
        val name = "name".getPropertyName()
        name shouldBeEqualTo "name"
    }

    @Test
    fun `getPropertyName - 인덱스 표현에서 이름 추출`() {
        val name = "items[0]".getPropertyName()
        name shouldBeEqualTo "items"
    }

    @Test
    fun `isNestedOrIndexedProperty - 단순 이름은 false`() {
        "name".isNestedOrIndexedProperty().shouldBeFalse()
    }

    @Test
    fun `isNestedOrIndexedProperty - 중첩 경로는 true`() {
        "user.address".isNestedOrIndexedProperty().shouldBeTrue()
    }

    @Test
    fun `isNestedOrIndexedProperty - 인덱스 표현은 true`() {
        "items[0]".isNestedOrIndexedProperty().shouldBeTrue()
    }

    @Test
    fun `getFirstNestedPropertySeparatorIndex - 첫 번째 구분자 위치`() {
        val idx = "user.address.street".getFirstNestedPropertySeparatorIndex()
        idx shouldBeEqualTo 4
    }

    @Test
    fun `getFirstNestedPropertySeparatorIndex - 구분자 없으면 음수`() {
        val idx = "name".getFirstNestedPropertySeparatorIndex()
        (idx < 0).shouldBeTrue()
    }

    @Test
    fun `getLastNestedPropertySeparatorIndex - 마지막 구분자 위치`() {
        val idx = "user.address.street".getLastNestedPropertySeparatorIndex()
        idx shouldBeEqualTo 12
    }

    @Test
    fun `matchesProperty - 인덱스 표현과 베이스 이름 매칭`() {
        "items[0]".matchesProperty("items").shouldBeTrue()
    }

    @Test
    fun `matchesProperty - 다른 이름은 false`() {
        "items[0]".matchesProperty("other").shouldBeFalse()
    }

    @Test
    fun `canonicalPropertyName - 따옴표 제거`() {
        val canonical = "map['my.key']".canonicalPropertyName()
        canonical shouldBeEqualTo "map[my.key]"
    }

    @Test
    fun `canonicalPropertyNames - 배열 변환`() {
        val names = arrayOf("map['a']", "map['b']").canonicalPropertyNames()
        names.shouldNotBeNull()
        names!!.toList() shouldBeEqualTo listOf("map[a]", "map[b]")
    }
}
