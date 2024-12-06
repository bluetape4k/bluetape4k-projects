package io.bluetape4k.javers.examples

import io.bluetape4k.javers.createEntityInstanceId
import io.bluetape4k.javers.getEntityTypeMapping
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeEqualTo
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.diff.custom.CustomValueComparator
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.TypeName
import org.javers.core.metamodel.`object`.InstanceId
import org.javers.core.metamodel.type.EntityType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.Serializable

@Suppress("UNUSED_VARIABLE")
class MappingExamples {

    companion object: KLogging()

    private lateinit var javers: Javers

    @BeforeEach
    fun beforeEach() {
        javers = JaversBuilder.javers().build()
    }

    @TypeName("Person")
    data class Person(
        @Id var namme: String? = null,
        var position: String? = null,
    ): Serializable


    @Test
    fun `@TypeName 이 적용된 클래스는 EntityType으로 매핑됩니다`() {
        val personType = javers.getTypeMapping<EntityType>(Person::class.java)
        val personType2 = javers.getEntityTypeMapping<Person>()

        val bob = Person("Bob", "Dev")
        val bobId = personType.createIdFromInstance(bob)

        log.debug { "EntityType of Person\n${personType.prettyPrint()}" }
        log.debug { "Id of bob=${bobId.value()}" }

        bobId.value() shouldBeEqualTo "Person/Bob"
        bobId shouldBeInstanceOf InstanceId::class
    }

    @TypeName("Entity")
    data class Entity(@Id var id: Point, var data: String? = null)

    class Point(val x: Double = 0.0, val y: Double = 0.0) {
        fun myToString(): String = "(${x.toInt()},${y.toInt()})"
    }

    class PointComparator: CustomValueComparator<Point> {
        override fun equals(a: Point, b: Point): Boolean = a.myToString() == b.myToString()
        override fun toString(value: Point): String = value.myToString()
    }

    @Test
    fun `comprex id 를 비교할 때 toString() 을 사용합니다`() {
        val p1 = Point(1.0, 3.0)
        val p2 = Point(1.0, 3.0)

        val entity1 = Entity(p1)
        val entity2 = Entity(p2)

        // toString() 값으로 비교하는데, data class 가 아니라 메모리 주소가 포함되므로, 둘은 같지 않다
        log.debug { "p1 == p2 ?" + (p1 == p2) }
        p1 shouldNotBeEqualTo p2

        val globalId1 = javers.createEntityInstanceId(entity1).value()  // Entity/1.0,3.0
        val globalId2 = javers.createEntityInstanceId(entity2).value()  // Entity/1.0,3.0

        log.debug { "InstanceId of entity1 = $globalId1" }
        log.debug { "InstanceId of entity2 = $globalId2" }

        globalId1 shouldBeEqualTo globalId2

        // Javers 에서 2개의 엔틴티의 변화를 찾을 수 있습니다.
        javers.compare(entity1, entity2).changes.shouldBeEmpty()
    }

    @Test
    fun `complex id 를 custom 하게 생성하기 위해 CustomValueComparator 을 사용할 수 있습니다`() {
        val entity = Entity(Point(1.0 / 3, 4.0 / 3))

        // Javers 기본 reflectToString function 사용 시
        val id = javers.createEntityInstanceId(entity)
        log.debug { "id.value=${id.value()}" }
        id.value() shouldBeEqualTo "Entity/0.3333333333333333,1.3333333333333333"

        // Custom toString 함수를 제공하면, 2개의 entity 값을 custom 하게 비교할 수 있습니다.
        val customJavers = JaversBuilder.javers()
            .registerValue(Point::class.java, PointComparator())
            .build()

        val entity2 = Entity(Point(1.1 / 3, 4.1 / 3))

        val id2 = customJavers.createEntityInstanceId(entity2)
        log.debug { "id2.value=${id2.value()}" }
        id2.value() shouldBeEqualTo "Entity/(0,1)"

        val diff = customJavers.compare(entity, entity2)
        log.debug { "diff=${diff.prettyPrint()}" }
        diff.changes.shouldBeEmpty()
    }
}
