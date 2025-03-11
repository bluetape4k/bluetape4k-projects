package io.bluetape4k.javers.examples

import io.bluetape4k.javers.diff.changesByType
import io.bluetape4k.javers.diff.isListChange
import io.bluetape4k.javers.diff.isNewObject
import io.bluetape4k.javers.diff.isObjectRemoved
import io.bluetape4k.javers.diff.objectsByChangeType
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.NewObject
import org.javers.core.diff.changetype.ObjectRemoved
import org.javers.core.diff.changetype.ReferenceChange
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.changetype.container.ListChange
import org.junit.jupiter.api.Test

class ObjectDiffExamples {

    companion object: KLogging()

    private val javers = JaversBuilder.javers().build()

    @Test
    fun `두 객체의 변화를 찾습니다`() {
        val tommyOld = Person("tommy", "Tommy Smart")
        val tommyNew = Person("tommy", "Tommy C. Smart")

        // 두 객체의 차이를 찾습니다.
        val diff = javers.compare(tommyOld, tommyNew)
        log.debug { diff.prettyPrint() }

        diff.changes shouldHaveSize 1  // name 변경

        val change = diff.changes.first()
        change shouldBeInstanceOf ValueChange::class
        val valueChange = change as ValueChange

        valueChange.left shouldBeEqualTo tommyOld.name
        valueChange.right shouldBeEqualTo tommyNew.name
    }

    @Test
    fun `엔티티의 컬레션에 요소를 추가하는 변화를 감지합니다`() {
        val oldBoss = Employee("Big Boss").apply {
            addSubordinates(Employee("Great Developer"))
        }
        val newBoss = Employee("Big Boss").apply {
            addSubordinates(Employee("Great Developer"))
            addSubordinates(Employee("Hired First"))
            addSubordinates(Employee("Hired Second"))
        }

        val diff = javers.compare(oldBoss, newBoss)
        log.debug { diff.prettyPrint() }

        diff.objectsByChangeType<NewObject>() shouldContainSame listOf(
            Employee("Hired First"),
            Employee("Hired Second")
        )

        diff.changes shouldHaveSize 11
        diff.changes.forEachIndexed { index, change ->
            log.debug { "change[$index]=$change" }
        }
        diff.changes[0].isNewObject.shouldBeTrue()
        diff.changes[1].isNewObject.shouldBeTrue()
        diff.changes[10].isListChange.shouldBeTrue()
    }

    @Test
    fun `엔티티의 컬레션에 요소를 삭제하는 변화를 감지합니다`() {
        val oldBoss = Employee("Big Boss").apply {
            addSubordinates(
                Employee("Great Developer"),
                Employee("Team Leader").apply {
                    addSubordinates(
                        Employee("Another Dev"),
                        Employee("To Be Fired")
                    )
                }
            )
        }
        val newBoss = Employee("Big Boss").apply {
            addSubordinates(
                Employee("Great Developer"),
                Employee("Team Leader").apply {
                    addSubordinates(Employee("Another Dev"))
                }
            )
        }

        val diff = javers.compare(oldBoss, newBoss)
        log.debug { diff.prettyPrint() }

        diff.changes.forEachIndexed { index, change ->
            log.debug { "change[$index]=$change" }
        }
        diff.changes.first().isObjectRemoved.shouldBeTrue()

        val removed = diff.changesByType<ObjectRemoved>()
        removed shouldHaveSize 1
        removed[0].affectedObject.isPresent.shouldBeTrue()
        removed[0].affectedObject.get() shouldBeEqualTo Employee("To Be Fired")

        val removedObjects = diff.objectsByChangeType<ObjectRemoved>()
        removedObjects shouldHaveSize 1
        removedObjects[0] shouldBeEqualTo Employee("To Be Fired")
    }

    @Test
    fun `속성이 변경된 것을 감지합니다`() {
        val oldBoss = Employee("Big Boss").apply {
            addSubordinates(
                Employee("Noisy Manager"),
                Employee("Great Developer", 10_000),
            )
        }
        val newBoss = Employee("Big Boss").apply {
            addSubordinates(
                Employee("Noisy Manager"),
                Employee("Great Developer", 20_000),
            )
        }

        val diff = javers.compare(oldBoss, newBoss)
        log.debug { diff.prettyPrint() }

        val valueChanges = diff.changesByType<ValueChange>()
        valueChanges shouldHaveSize 1

        with(valueChanges.first()) {
            affectedLocalId shouldBeEqualTo "Great Developer"
            propertyName shouldBeEqualTo "salary"
            left shouldBeEqualTo 10_000
            right shouldBeEqualTo 20_000
        }
    }

    @Test
    fun `부모 엔티티가 변경된 것을 감지합니다`() {
        val oldBoss = Employee("Big Boss").apply {
            addSubordinates(
                Employee("Manager One").apply {
                    addSubordinates(Employee("Great Developer"))
                },
                Employee("Manager Two")
            )
        }
        val newBoss = Employee("Big Boss").apply {
            addSubordinates(
                Employee("Manager One"),
                Employee("Manager Two").apply {
                    addSubordinates(Employee("Great Developer"))
                }
            )
        }

        val diff = javers.compare(oldBoss, newBoss)
        log.debug { diff.prettyPrint() }

        val changes = diff.changesByType<ReferenceChange>()
        changes shouldHaveSize 1

        with(changes[0]) {
            affectedLocalId shouldBeEqualTo "Great Developer"
            left.value() shouldBeEqualTo "Employee/Manager One"
            right.value() shouldBeEqualTo "Employee/Manager Two"
        }
    }

    @Test
    fun `@TypeName이 적용되지 않은 일반 클래스도 변화를 감지한다`() {
        val address1 = Address("New York", "5th Avenue")
        val address2 = Address("New York", "6th Avenue")

        val diff = javers.compare(address1, address2)
        log.debug { diff.prettyPrint() }

        diff.changes shouldHaveSize 1
        val changes = diff.changesByType<ValueChange>()
        with(changes[0]) {
            affectedGlobalId.value() shouldBeEqualTo "${Address::class.java.name}/"
            propertyName shouldBeEqualTo "street"
            left shouldBeEqualTo "5th Avenue"
            right shouldBeEqualTo "6th Avenue"
        }
    }

    @Test
    fun `최상위 컬렉션을 비교합니다`() {
        val oldList = listOf(Person("Tommy", "Tommy Smart"))
        val newList = listOf(Person("Tommy", "Tommy C. Smart"))

        val diff = javers.compare(oldList, newList)
        log.debug { diff.prettyPrint() }
        diff.changes shouldHaveSize 1

        val change = diff.changesByType<ListChange>().first()
        log.debug { change }

        change.changes shouldHaveSize 1
        change.left shouldBeEqualTo oldList
        change.right shouldBeEqualTo newList
    }
}
