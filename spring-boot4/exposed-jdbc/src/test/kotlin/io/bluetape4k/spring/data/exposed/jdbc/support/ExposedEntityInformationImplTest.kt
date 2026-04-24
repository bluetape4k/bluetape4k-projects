package io.bluetape4k.spring.data.exposed.jdbc.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.domain.UserEntity
import io.bluetape4k.spring.data.exposed.jdbc.domain.Users
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.ExposedEntityInformationImpl
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExposedEntityInformationImplTest {

    companion object : KLogging()

    @Test
    fun `invoke factory creates info from UserEntity companion`() {
        val info = ExposedEntityInformationImpl<UserEntity, Long>(UserEntity::class.java)
        info.shouldNotBeNull()
        info.javaType shouldBeEqualTo UserEntity::class.java
    }

    @Test
    fun `entityClass is UserEntity companion object`() {
        val info = ExposedEntityInformationImpl<UserEntity, Long>(UserEntity::class.java)
        info.entityClass.shouldNotBeNull()
        info.entityClass shouldBeEqualTo UserEntity.Companion
    }

    @Test
    fun `table is Users table`() {
        val info = ExposedEntityInformationImpl<UserEntity, Long>(UserEntity::class.java)
        info.table shouldBeEqualTo Users
    }

    @Test
    fun `getIdType returns non-null type`() {
        val info = ExposedEntityInformationImpl<UserEntity, Long>(UserEntity::class.java)
        val idType = info.getIdType()
        idType.shouldNotBeNull()
    }

    @Test
    fun `invoke throws when class has no companion object`() {
        // Use a plain Java class that has no Kotlin companion
        assertThrows<IllegalStateException> {
            @Suppress("UNCHECKED_CAST")
            ExposedEntityInformationImpl<UserEntity, Long>(String::class.java as Class<UserEntity>)
        }
    }
}
