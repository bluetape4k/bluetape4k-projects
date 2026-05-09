package io.bluetape4k.spring.cassandra.model

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant

class AbstractCassandraModelTest {

    private class StringEntity(private var pk: String? = null) : AbstractCassandraPersistable<String>() {
        override fun getId(): String? = pk
        override fun setId(id: String) {
            pk = id
        }
    }

    private class AuditableEntity(private var pk: String? = null) : AbstractCassandraAuditable<String, String>() {
        override fun getId(): String? = pk
        override fun setId(id: String) {
            pk = id
        }
    }

    @Test
    fun `id가 null이면 신규 엔티티로 판단`() {
        val entity = StringEntity()
        entity.isNew().shouldBeTrue()
        entity.id.shouldBeNull()
    }

    @Test
    fun `id를 설정하면 신규 엔티티가 아님`() {
        val entity = StringEntity()
        entity.setId("user-1")
        entity.isNew().shouldBeFalse()
        entity.id shouldBeEqualTo "user-1"
    }

    @Test
    fun `동일한 id를 가진 두 엔티티는 동등`() {
        val e1 = StringEntity("id-1")
        val e2 = StringEntity("id-1")
        e1 shouldBeEqualTo e2
        e1.hashCode() shouldBeEqualTo e2.hashCode()
    }

    @Test
    fun `다른 id를 가진 두 엔티티는 동등하지 않음`() {
        val e1 = StringEntity("id-1")
        val e2 = StringEntity("id-2")
        e1 shouldNotBeEqualTo e2
    }

    @Test
    fun `id가 null인 엔티티는 다른 엔티티와 동등하지 않음`() {
        val e1 = StringEntity()
        val e2 = StringEntity()
        e1 shouldNotBeEqualTo e2
    }

    @Test
    fun `null과 동등하지 않음`() {
        val entity = StringEntity("id-1")
        entity.shouldNotBeNull()
    }

    @Test
    fun `동일 참조는 동등`() {
        val entity = StringEntity("id-1")
        entity shouldBeEqualTo entity
    }

    @Test
    fun `다른 타입과 동등하지 않음`() {
        val entity = StringEntity("id-1")
        val other: Any = "id-1"
        (entity == other).shouldBeFalse()
    }

    @Test
    fun `id가 있을 때 hashCode는 id hashCode와 동일`() {
        val entity = StringEntity("id-1")
        entity.hashCode() shouldBeEqualTo "id-1".hashCode()
    }

    @Test
    fun `id가 null일 때 hashCode는 identityHashCode`() {
        val entity = StringEntity()
        entity.hashCode() shouldBeEqualTo System.identityHashCode(entity)
    }

    @Test
    fun `toString에 클래스명과 id 포함`() {
        val entity = StringEntity("id-1")
        val str = entity.toString()
        str.contains("StringEntity").shouldBeTrue()
        str.contains("id-1").shouldBeTrue()
    }

    @Test
    fun `createdAt이 null이면 신규 엔티티`() {
        val entity = AuditableEntity("id-1")
        entity.isNew().shouldBeTrue()
    }

    @Test
    fun `createdAt 설정 후 신규 엔티티 아님`() {
        val entity = AuditableEntity("id-1")
        entity.setCreatedDate(Instant.now())
        entity.isNew().shouldBeFalse()
    }

    @Test
    fun `createdBy 설정 및 조회`() {
        val entity = AuditableEntity("id-1")
        entity.getCreatedBy().isPresent.shouldBeFalse()

        entity.setCreatedBy("debop")
        entity.getCreatedBy().get() shouldBeEqualTo "debop"
        entity.createdBy shouldBeEqualTo "debop"
    }

    @Test
    fun `createdDate 설정 및 조회`() {
        val entity = AuditableEntity("id-1")
        entity.getCreatedDate().isPresent.shouldBeFalse()

        val now = Instant.now()
        entity.setCreatedDate(now)
        entity.getCreatedDate().get() shouldBeEqualTo now
        entity.createdAt shouldBeEqualTo now
    }

    @Test
    fun `lastModifiedBy 설정 및 조회`() {
        val entity = AuditableEntity("id-1")
        entity.getLastModifiedBy().isPresent.shouldBeFalse()

        entity.setLastModifiedBy("mike")
        entity.getLastModifiedBy().get() shouldBeEqualTo "mike"
        entity.lastModifiedBy shouldBeEqualTo "mike"
    }

    @Test
    fun `lastModifiedDate 설정 및 조회`() {
        val entity = AuditableEntity("id-1")
        entity.getLastModifiedDate().isPresent.shouldBeFalse()

        val now = Instant.now()
        entity.setLastModifiedDate(now)
        entity.getLastModifiedDate().get() shouldBeEqualTo now
        entity.lastModifiedAt shouldBeEqualTo now
    }

    @Test
    fun `auditable 엔티티 동등성 비교`() {
        val e1 = AuditableEntity("id-1")
        val e2 = AuditableEntity("id-1")
        e1 shouldBeEqualTo e2
    }

    @Test
    fun `auditable 엔티티 전체 필드 설정`() {
        val entity = AuditableEntity("id-1")
        val now = Instant.now()

        entity.setCreatedBy("creator")
        entity.setCreatedDate(now)
        entity.setLastModifiedBy("modifier")
        entity.setLastModifiedDate(now)

        entity.createdBy shouldBeEqualTo "creator"
        entity.createdAt.shouldNotBeNull()
        entity.lastModifiedBy shouldBeEqualTo "modifier"
        entity.lastModifiedAt.shouldNotBeNull()
        entity.isNew().shouldBeFalse()
    }
}
