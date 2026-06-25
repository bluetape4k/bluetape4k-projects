package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.core.type.ListType
import com.datastax.oss.driver.api.core.type.UserDefinedType
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class DataTypeSupportTest {

    @Test
    fun `isCollectionType 는 collection 타입만 true 이다`() {
        DataTypes.listOf(DataTypes.INT).isCollectionType.shouldBeTrue()
        DataTypes.setOf(DataTypes.TEXT).isCollectionType.shouldBeTrue()
        DataTypes.mapOf(DataTypes.TEXT, DataTypes.INT).isCollectionType.shouldBeTrue()
        DataTypes.INT.isCollectionType.shouldBeFalse()
    }

    @Test
    fun `potentiallyFreeze 는 non frozen UDT를 freeze 한다`() {
        val udt = userDefinedTypeOf("ks", "user_type") {
            withField("name", DataTypes.TEXT)
        }

        val frozen = udt.potentiallyFreeze()

        frozen.shouldBeInstanceOf<UserDefinedType>()
        frozen.isFrozen.shouldBeTrue()
    }

    @Test
    fun `potentiallyFreeze 는 nested UDT를 포함한 collection element 를 freeze 한다`() {
        val udt = userDefinedTypeOf("ks", "coord_type") {
            withField("x", DataTypes.INT)
            withField("y", DataTypes.INT)
        }
        val listType = DataTypes.listOf(udt)

        val frozenList = listType.potentiallyFreeze()

        frozenList.shouldBeInstanceOf<ListType>()
        val elementType = frozenList.elementType
        elementType.shouldBeInstanceOf<UserDefinedType>()
        elementType.isFrozen.shouldBeTrue()
    }
}
