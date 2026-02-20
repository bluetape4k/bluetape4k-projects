package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.type.DataTypes
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
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

        frozen.shouldBeInstanceOf<com.datastax.oss.driver.api.core.type.UserDefinedType>()
        (frozen as com.datastax.oss.driver.api.core.type.UserDefinedType).isFrozen.shouldBeTrue()
    }

    @Test
    fun `potentiallyFreeze 는 nested UDT를 포함한 collection element 를 freeze 한다`() {
        val udt = userDefinedTypeOf("ks", "coord_type") {
            withField("x", DataTypes.INT)
            withField("y", DataTypes.INT)
        }
        val listType = DataTypes.listOf(udt)

        val frozenList = listType.potentiallyFreeze()

        frozenList.shouldBeInstanceOf<com.datastax.oss.driver.api.core.type.ListType>()
        val elementType = (frozenList as com.datastax.oss.driver.api.core.type.ListType).elementType
        elementType.shouldBeInstanceOf<com.datastax.oss.driver.api.core.type.UserDefinedType>()
        (elementType as com.datastax.oss.driver.api.core.type.UserDefinedType).isFrozen.shouldBeTrue()
    }
}
