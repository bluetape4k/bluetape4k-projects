package io.bluetape4k.javers

import io.bluetape4k.javers.examples.Address
import io.bluetape4k.javers.examples.Person
import io.bluetape4k.javers.repository.jql.queryByClass
import io.bluetape4k.javers.repository.jql.queryByInstanceId
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.`object`.InstanceId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ValueObjectType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JaversExtensionsTest {

    companion object: KLogging()

    private lateinit var javers: Javers

    @BeforeEach
    fun beforeEach() {
        javers = JaversBuilder.javers().build()
    }

    @Test
    fun `getEntityTypeMapping - reified 타입으로 EntityType 매핑을 조회한다`() {
        val entityType = javers.getEntityTypeMapping<Person>()

        entityType shouldBeInstanceOf EntityType::class
        entityType.baseJavaType shouldBeEqualTo Person::class.java
        log.debug { "EntityType: ${entityType.prettyPrint()}" }
    }

    @Test
    fun `getValueObjectTypeMapping - reified 타입으로 ValueObjectType 매핑을 조회한다`() {
        val voType = javers.getValueObjectTypeMapping<Address>()

        voType shouldBeInstanceOf ValueObjectType::class
        voType.baseJavaType shouldBeEqualTo Address::class.java
        log.debug { "ValueObjectType: ${voType.prettyPrint()}" }
    }

    @Test
    fun `createEntityInstanceId - 엔티티 인스턴스로부터 InstanceId를 생성한다`() {
        val bob = Person("Bob", "Bob Smart")
        val id = javers.createEntityInstanceId(bob)

        id shouldBeInstanceOf InstanceId::class
        id.value() shouldBeEqualTo "Person/Bob"
        log.debug { "InstanceId: ${id.value()}" }
    }

    @Test
    fun `createEntityInstanceIdByEntityId - 로컬 ID로 InstanceId를 생성한다`() {
        val id = javers.createEntityInstanceIdByEntityId<Person>("Alice")

        id shouldBeInstanceOf InstanceId::class
        id.value() shouldBeEqualTo "Person/Alice"
        log.debug { "InstanceId by entityId: ${id.value()}" }
    }

    @Test
    fun `compareCollections - 두 컬렉션의 차이를 비교한다`() {
        val oldList = listOf(Person("Tommy", "Tommy Smart"))
        val newList = listOf(Person("Tommy", "Tommy C. Smart"))

        val diff = javers.compareCollections(oldList, newList)

        diff.changes.shouldNotBeEmpty()
        diff.changes.size shouldBeEqualTo 1
        log.debug { "Diff: ${diff.prettyPrint()}" }
    }

    @Test
    fun `compareCollections - 동일 컬렉션은 변경 사항이 없다`() {
        val list1 = listOf(Person("A", "Name A"), Person("B", "Name B"))
        val list2 = listOf(Person("A", "Name A"), Person("B", "Name B"))

        val diff = javers.compareCollections(list1, list2)

        diff.changes.size shouldBeEqualTo 0
    }

    @Test
    fun `latestSnapshotOrNull - 커밋된 엔티티의 최신 스냅샷을 조회한다`() {
        val bob = Person("Bob", "Bob Smart")
        javers.commit("author", bob)

        val snapshot = javers.latestSnapshotOrNull<Person>("Bob")

        snapshot.shouldNotBeNull()
        snapshot.globalId.value() shouldBeEqualTo "Person/Bob"
        log.debug { "Latest snapshot: ${snapshot.globalId.value()}, version=${snapshot.version}" }
    }

    @Test
    fun `latestSnapshotOrNull - 커밋되지 않은 엔티티는 null을 반환한다`() {
        val snapshot = javers.latestSnapshotOrNull<Person>("NonExistent")
        snapshot.shouldBeNull()
    }

    @Test
    fun `latestSnapshotOrNull - KClass 파라미터로 조회한다`() {
        val bob = Person("Bob", "Bob Smart")
        javers.commit("author", bob)

        val snapshot = javers.latestSnapshotOrNull("Bob", Person::class)

        snapshot.shouldNotBeNull()
        snapshot.globalId.value() shouldBeEqualTo "Person/Bob"
    }

    @Test
    fun `getShadow - 스냅샷을 Shadow로 변환한다`() {
        val bob = Person("Bob", "Bob Smart")
        val commit = javers.commit("author", bob)
        val snapshot = commit.snapshots.first()

        val shadow = javers.getShadow<Person>(snapshot)

        shadow.shouldNotBeNull()
        shadow.get().login shouldBeEqualTo "Bob"
        shadow.get().name shouldBeEqualTo "Bob Smart"
        log.debug { "Shadow: ${shadow.get()}" }
    }

    @Test
    fun `shadowFactory - Javers에서 ShadowFactory를 얻는다`() {
        val factory = javers.shadowFactory
        factory.shouldNotBeNull()
    }

    @Test
    fun `findShadowsAndSequence - JQL로 Shadow를 Sequence로 조회한다`() {
        val bob = Person("Bob", "Bob Smart")
        javers.commit("author", bob)

        bob.name = "Bob C. Smart"
        javers.commit("author", bob)

        val query = queryByInstanceId<Person>("Bob")
        val shadows = javers.findShadowsAndSequence<Person>(query).toList()

        shadows.size shouldBeGreaterOrEqualTo 2
        shadows.first().get().name shouldBeEqualTo "Bob C. Smart"
        log.debug { "Shadows count: ${shadows.size}" }
    }

    @Test
    fun `queryByClass를 사용하여 특정 타입의 스냅샷을 조회한다`() {
        val bob = Person("Bob", "Bob Smart")
        val alice = Person("Alice", "Alice Wonder")
        javers.commit("author", bob)
        javers.commit("author", alice)

        val query = queryByClass<Person>()
        val snapshots = javers.findSnapshots(query)

        snapshots.size shouldBeGreaterOrEqualTo 2
        log.debug { "Found ${snapshots.size} snapshots for Person" }
    }
}
